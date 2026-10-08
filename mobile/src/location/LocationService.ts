/**
 * LocationService.ts — the phone's location (model dictionary: `LocationService`; implementation
 * standards §10, "Device networking and location"): the real GPS fix through `expo-location`'s
 * foreground permission, and the district of a point by a point-in-polygon test against the 25
 * district outlines the officer map uses.
 */
import * as Location from 'expo-location';
import type { GeoPoint } from '../api/reportApi';
import { DISTRICTS, type District } from '../api/types';
import { DISTRICT_OUTLINES, type OutlineRing } from './districtOutlines';

/** The calls the capture controller makes on the phone's location. */
export interface LocationService {
  /**
   * Gets the current fix (SC2 steps 12 to 14).
   *
   * @returns the fix, or `undefined` if the permission is denied, the services are off or there is
   *   no fix (A1: the citizen then types the location)
   */
  getCurrentLocation(): Promise<GeoPoint | undefined>;
  /**
   * Finds the district a point lies in.
   *
   * @param location the point
   * @returns the district, or `undefined` if the point lies in no outline (the sea, a gap)
   */
  resolveDistrict(location: GeoPoint): District | undefined;
}

/** Even-odd test of a point against one ring of [longitude, latitude] points. */
function isInside(longitude: number, latitude: number, ring: OutlineRing): boolean {
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i];
    const [xj, yj] = ring[j];
    if (
      yi > latitude !== yj > latitude &&
      longitude < ((xj - xi) * (latitude - yi)) / (yj - yi) + xi
    ) {
      inside = !inside;
    }
  }
  return inside;
}

/**
 * Finds the district a point lies in (point-in-polygon on the district outlines).
 *
 * @param location the point
 * @returns the district, or `undefined` if the point lies in no outline
 */
export function resolveDistrict(location: GeoPoint): District | undefined {
  return DISTRICTS.find((district) =>
    DISTRICT_OUTLINES[district].some((ring) =>
      isInside(location.longitude, location.latitude, ring),
    ),
  );
}

/**
 * Builds the location service over `expo-location`.
 *
 * @returns the service
 */
export function createLocationService(): LocationService {
  return {
    async getCurrentLocation() {
      try {
        const permission = await Location.requestForegroundPermissionsAsync();
        if (!permission.granted || !(await Location.hasServicesEnabledAsync())) {
          return undefined;
        }
        const position = await Location.getCurrentPositionAsync({
          accuracy: Location.Accuracy.Balanced,
        });
        return {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracyMetres: position.coords.accuracy,
        };
      } catch {
        return undefined;
      }
    },
    resolveDistrict,
  };
}
