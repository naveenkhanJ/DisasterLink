/**
 * placeSearch.ts — finds a place by name for the manual-location map (implementation-only; standards
 * §10 "Manual location", P206; deviation D-UC2-2; WF2P-S03a). It uses the phone's geocoder
 * (`expo-location` `geocodeAsync`); the results in Sri Lanka depend on that geocoder.
 *
 * Serves CH-27 (SC2 A1 12c).
 */
import * as Location from 'expo-location';
import type { GeoPoint } from '../api/reportApi';

/** Appended to every query so that the geocoder looks for the place in the country. */
const COUNTRY_SUFFIX = ', Sri Lanka';

/**
 * Searches a place by name.
 *
 * The text `"<query>, Sri Lanka"` is geocoded: the suffix keeps the results in the country.
 *
 * @param query what the citizen typed
 * @returns the first result's point, or `undefined` if the query is empty or nothing was found
 * @throws Error whatever the geocoder throws (no network, no permission on Android)
 */
export async function searchPlace(query: string): Promise<GeoPoint | undefined> {
  const text = query.trim();
  if (text === '') {
    return undefined;
  }
  const results = await Location.geocodeAsync(`${text}${COUNTRY_SUFFIX}`);
  if (results.length === 0) {
    return undefined;
  }
  return { latitude: results[0].latitude, longitude: results[0].longitude };
}
