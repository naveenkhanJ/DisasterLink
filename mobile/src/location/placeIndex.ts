/**
 * placeIndex.ts — the type-ahead search over the bundled list of Sri Lankan populated places
 * (implementation-only; standards §10 "Manual location", P213; serves CH-27). The list is
 * `PLACE_DATA` (generated from GeoNames, CC BY 4.0, see THIRD-PARTY-NOTICES.md); it is parsed once,
 * on first use, and the search never touches the network, so it works offline.
 */
import type { GeoPoint } from '../api/reportApi';
import type { District } from '../api/types';
import { EARTH_RADIUS_KM, MAX_PLACE_SUGGESTIONS, MIN_PLACE_QUERY_LENGTH } from '../constants';
import { resolveDistrict } from './LocationService';
import { PLACE_DATA } from './placeData';

/** One suggestion of the type-ahead list. */
export interface PlaceMatch {
  /** The place's name, as GeoNames gives it. */
  name: string;
  /** Where it is. */
  point: GeoPoint;
  /** The district the point lies in, or `undefined` if it lies in no outline. */
  district: District | undefined;
}

/** A place with its name prepared for matching. */
interface IndexedPlace {
  name: string;
  /** The normalised name. */
  key: string;
  latitude: number;
  longitude: number;
  population: number;
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

function buildIndex(): IndexedPlace[] {
  const places: IndexedPlace[] = [];
  for (const line of PLACE_DATA.split('\n')) {
    const [name, latitude, longitude, population] = line.split('|');
    if (name && latitude && longitude) {
      places.push({
        name,
        key: normalisePlaceText(name),
        latitude: Number(latitude),
        longitude: Number(longitude),
        population: Number(population) || 0,
      });
    }
  }
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
 * contain it; within each group the more populous place first, then by name. A place with the same
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
