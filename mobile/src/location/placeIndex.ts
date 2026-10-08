/**
 * placeIndex.ts — the type-ahead search over the bundled list of Sri Lankan populated places
 * (implementation-only; standards §10 "Manual location", P213; serves CH-27). The list is
 * `PLACE_DATA` (generated from GeoNames, CC BY 4.0, see THIRD-PARTY-NOTICES.md) with the sourced
 * `PLACE_SUPPLEMENT` beside it (standards §10 "Manual location", Supplement, P222); both are parsed once,
 * on first use, and the search never touches the network, so it works offline. `nearestPlace` finds the
 * place nearest to a point for the picker's live label and for the line under a GPS fix (P222).
 */
import type { GeoPoint } from '../api/reportApi';
import type { District } from '../api/types';
import {
  EARTH_RADIUS_KM,
  KM_PER_DEGREE,
  MAX_PLACE_SUGGESTIONS,
  MIN_PLACE_QUERY_LENGTH,
  NO_TOWN_RADIUS_KM,
} from '../constants';
import { resolveDistrict } from './LocationService';
import { PLACE_DATA } from './placeData';
import { PLACE_SUPPLEMENT } from './placeSupplement';

/** One suggestion of the type-ahead list. */
export interface PlaceMatch {
  /** The place's name, as GeoNames gives it. */
  name: string;
  /** Where it is. */
  point: GeoPoint;
  /** The district the point lies in, or `undefined` if it lies in no outline. */
  district: District | undefined;
}

/** A suggestion that also says how far the place is from the point searched. */
export type NearestPlace = PlaceMatch & {
  /** The distance from the point to the place, in kilometres. */
  distanceKm: number;
};

/** A place with its name prepared for matching. */
interface IndexedPlace {
  name: string;
  /** The normalised name. */
  key: string;
  latitude: number;
  longitude: number;
  population: number;
  /** Whether the place is from the sourced supplement (it ranks before GeoNames entries). */
  supplement: boolean;
}

/** The ranks of a match: the lower, the better. */
const STARTS_WITH = 0;
const WORD_STARTS_WITH = 1;
const CONTAINS = 2;

/** The degrees-to-radians factor. */
const RADIANS_PER_DEGREE = Math.PI / 180;

let index: IndexedPlace[] | undefined;

/**
 * Normalises a text for matching: lower case, no diacritics, dots or apostrophes, single spaces.
 *
 * @param text the text
 * @returns the normalised text
 */
export function normalisePlaceText(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[.'’‘`]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function parseLines(data: string, supplement: boolean, places: IndexedPlace[]): void {
  for (const line of data.split('\n')) {
    const [name, latitude, longitude, population] = line.split('|');
    if (name && latitude && longitude) {
      places.push({
        name,
        key: normalisePlaceText(name),
        latitude: Number(latitude),
        longitude: Number(longitude),
        population: Number(population) || 0,
        supplement,
      });
    }
  }
}

function buildIndex(): IndexedPlace[] {
  const places: IndexedPlace[] = [];
  parseLines(PLACE_SUPPLEMENT, true, places);
  parseLines(PLACE_DATA, false, places);
  return places;
}

/** The rank of a place for a normalised query, or `undefined` if it does not match. */
function rank(place: IndexedPlace, query: string): number | undefined {
  if (place.key.startsWith(query)) {
    return STARTS_WITH;
  }
  if (place.key.split(' ').some((word) => word.startsWith(query))) {
    return WORD_STARTS_WITH;
  }
  return place.key.includes(query) ? CONTAINS : undefined;
}

/**
 * Searches the places by name as the citizen types.
 *
 * Names that start with the query come first, then names where a word starts with it, then names that
 * contain it; within each group a supplement entry first, then the more populous place, then by name. A place with the same
 * normalised name and district as a better one is dropped.
 *
 * @param query what the citizen typed; fewer than two characters (after normalising) gives no result
 * @param limit the most suggestions to give
 * @returns the suggestions, best first
 */
export function searchPlaces(query: string, limit: number = MAX_PLACE_SUGGESTIONS): PlaceMatch[] {
  const text = normalisePlaceText(query);
  if (text.length < MIN_PLACE_QUERY_LENGTH) {
    return [];
  }
  index ??= buildIndex();
  const ranked: { place: IndexedPlace; rank: number }[] = [];
  for (const place of index) {
    const matched = rank(place, text);
    if (matched !== undefined) {
      ranked.push({ place, rank: matched });
    }
  }
  ranked.sort(
    (a, b) =>
      a.rank - b.rank ||
      Number(b.place.supplement) - Number(a.place.supplement) ||
      b.place.population - a.place.population ||
      (a.place.name < b.place.name ? -1 : a.place.name > b.place.name ? 1 : 0),
  );
  const seen = new Set<string>();
  const matches: PlaceMatch[] = [];
  for (const { place } of ranked) {
    if (matches.length >= limit) {
      break;
    }
    const point = { latitude: place.latitude, longitude: place.longitude };
    const district = resolveDistrict(point);
    const identity = `${place.key}|${district ?? ''}`;
    if (!seen.has(identity)) {
      seen.add(identity);
      matches.push({ name: place.name, point, district });
    }
  }
  return matches;
}

/**
 * Gives the distance between two points over the earth's surface (haversine).
 *
 * @param from one point
 * @param to the other point
 * @returns the distance in kilometres
 */
export function distanceKm(from: GeoPoint, to: GeoPoint): number {
  const latitudeDelta = (to.latitude - from.latitude) * RADIANS_PER_DEGREE;
  const longitudeDelta = (to.longitude - from.longitude) * RADIANS_PER_DEGREE;
  const a =
    Math.sin(latitudeDelta / 2) ** 2 +
    Math.cos(from.latitude * RADIANS_PER_DEGREE) *
      Math.cos(to.latitude * RADIANS_PER_DEGREE) *
      Math.sin(longitudeDelta / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
}

/**
 * Finds the place nearest to a point.
 *
 * Scans the whole index (GeoNames and supplement) with a latitude and longitude box of about `maxKm`
 * around the point, then measures the candidates by haversine; the closest wins, ties by population.
 * The district is the **pin's** (`resolveDistrict(point)`), the one a report gets (SC2 step 18), not the
 * place's own.
 *
 * @param point the point, for example the centre of the map
 * @param maxKm the farthest place that counts, in kilometres
 * @returns the nearest place with its distance, or `undefined` if none lies within `maxKm`
 */
export function nearestPlace(
  point: GeoPoint,
  maxKm: number = NO_TOWN_RADIUS_KM,
): NearestPlace | undefined {
  index ??= buildIndex();
  const latitudeBox = maxKm / KM_PER_DEGREE;
  const longitudeBox =
    maxKm / (KM_PER_DEGREE * Math.max(Math.cos(point.latitude * RADIANS_PER_DEGREE), 0.01));
  let best: { place: IndexedPlace; km: number } | undefined;
  for (const place of index) {
    if (
      Math.abs(place.latitude - point.latitude) > latitudeBox ||
      Math.abs(place.longitude - point.longitude) > longitudeBox
    ) {
      continue;
    }
    const km = distanceKm(point, { latitude: place.latitude, longitude: place.longitude });
    if (
      km <= maxKm &&
      (!best || km < best.km || (km === best.km && place.population > best.place.population))
    ) {
      best = { place, km };
    }
  }
  if (!best) {
    return undefined;
  }
  return {
    name: best.place.name,
    point: { latitude: best.place.latitude, longitude: best.place.longitude },
    district: resolveDistrict(point),
    distanceKm: best.km,
  };
}
