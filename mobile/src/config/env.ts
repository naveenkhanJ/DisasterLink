/**
 * env.ts — the app's settings from the Expo public environment (`.env`, see `.env.example`).
 *
 * Expo inlines `process.env.EXPO_PUBLIC_*` at bundle time only where the variable is written out in
 * full, so {@link getEnv} names each variable literally; {@link parseEnv} is the pure part that the
 * tests call.
 */
import { DEFAULT_CITIZEN_ID } from '../constants';

/** The settings of the app. */
export interface AppEnv {
  /** The server's base URL, for example `http://192.168.1.10:8080` (no trailing slash). */
  apiUrl: string;
  /** The identifier of the demo citizen. */
  citizenId: string;
}

/** The raw values as the environment gives them. */
export interface RawEnv {
  apiUrl: string | undefined;
  citizenId: string | undefined;
}

/**
 * Builds the settings from raw values.
 *
 * @param raw the values of `EXPO_PUBLIC_API_URL` and `EXPO_PUBLIC_CITIZEN_ID`
 * @returns the settings; the citizen defaults to `CIT-KLT-0001`
 * @throws Error with a message that names `EXPO_PUBLIC_API_URL` when it is missing or blank
 */
export function parseEnv(raw: RawEnv): AppEnv {
  const apiUrl = raw.apiUrl?.trim();
  if (!apiUrl) {
    throw new Error(
      'EXPO_PUBLIC_API_URL is not set. Copy mobile/.env.example to mobile/.env and put the PC\'s ' +
        'LAN address in it, for example http://192.168.1.10:8080, then restart Expo.',
    );
  }
  const citizenId = raw.citizenId?.trim();
  return {
    apiUrl: apiUrl.replace(/\/+$/, ''),
    citizenId: citizenId ? citizenId : DEFAULT_CITIZEN_ID,
  };
}

/**
 * Reads the settings from `process.env`.
 *
 * @returns the settings
 * @throws Error when `EXPO_PUBLIC_API_URL` is missing
 */
export function getEnv(): AppEnv {
  return parseEnv({
    apiUrl: process.env.EXPO_PUBLIC_API_URL,
    citizenId: process.env.EXPO_PUBLIC_CITIZEN_ID,
  });
}
