/**
 * locationAccess.ts — why there is no GPS fix: whether the location permission is refused, can still
 * be asked, or Location Services are off (implementation-only; standards §10 "Phone screens beyond
 * the frames" item 8, P206; WF2P-S03g). The screen uses it to show the right notice and button; the
 * signature of `LocationService.getCurrentLocation()` is unchanged.
 *
 * Serves CH-27 (SC2 A1 12a to 12b).
 */
import * as Location from 'expo-location';

/**
 * The state of the phone's location access, read in this order: Location Services off, permission
 * granted, permission not granted but it can be asked again, permission refused for good.
 */
export type LocationAccessState = 'granted' | 'askable' | 'blocked' | 'servicesOff';

/** The calls the screen makes on the phone's location access. */
export interface LocationAccess {
  /**
   * Reads the state of the location access.
   *
   * @returns the state; an error while reading counts as `servicesOff`
   */
  check(): Promise<LocationAccessState>;
  /**
   * Asks for the foreground location permission.
   *
   * @returns whether it was granted
   */
  request(): Promise<boolean>;
}

/**
 * Builds the location access over `expo-location`.
 *
 * @returns the access
 */
export function createLocationAccess(): LocationAccess {
  return {
    async check() {
      try {
        if (!(await Location.hasServicesEnabledAsync())) {
          return 'servicesOff';
        }
        const permission = await Location.getForegroundPermissionsAsync();
        if (permission.granted) {
          return 'granted';
        }
        return permission.canAskAgain ? 'askable' : 'blocked';
      } catch (error) {
        console.warn('[locationAccess] the location access could not be read:', error);
        return 'servicesOff';
      }
    },
    async request() {
      return (await Location.requestForegroundPermissionsAsync()).granted;
    },
  };
}
