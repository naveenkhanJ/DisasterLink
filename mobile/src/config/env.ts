/**
 * env.ts — the app's settings from the Expo public environment (`.env`, see `.env.example`).
 *
 * Expo inlines `process.env.EXPO_PUBLIC_*` at bundle time only where the variable is written out in
 * full, so {@link getEnv} names each variable literally; {@link parseEnv} is the pure part that the
 * tests call.
 */
import { DISTRICTS, type District, type Language } from '../api/types';
import {
  DEFAULT_CITIZEN_DISTRICT,
  DEFAULT_CITIZEN_ID,
  DEFAULT_CITIZEN_LANGUAGE,
} from '../constants';

/** The three languages, as the server names them. */
const LANGUAGES: readonly Language[] = ['SINHALA', 'TAMIL', 'ENGLISH'];

/** The settings of the app. */
export interface AppEnv {
  /** The server's base URL, for example `http://192.168.1.10:8080` (no trailing slash). */
  apiUrl: string;
  /** The identifier of the demo citizen. */
  citizenId: string;
  /** The demo citizen's registered district (a report's fallback, the home screen's warnings). */
  citizenDistrict: District;
  /** The demo citizen's language, the app's starting language. */
  citizenLanguage: Language;
}

/** The raw values as the environment gives them. */
export interface RawEnv {
  apiUrl: string | undefined;
  citizenId: string | undefined;
  citizenDistrict: string | undefined;
  citizenLanguage: string | undefined;
}

/**
 * Reads one choice variable: blank or missing gives the default, anything else must be one of the
 * accepted values (compared after trimming and upper-casing).
 *
 * @param name the variable's name, for the error message
 * @param value the raw value
 * @param accepted the accepted values
 * @param fallback the value for a blank or missing variable
 * @returns the accepted value
 * @throws Error that names the variable and lists the accepted values
 */
function parseChoice<T extends string>(
  name: string,
  value: string | undefined,
  accepted: readonly T[],
  fallback: T,
): T {
  const text = value?.trim().toUpperCase();
  if (!text) {
    return fallback;
  }
  const found = accepted.find((candidate) => candidate === text);
  if (!found) {
    throw new Error(`${name} is "${value?.trim()}", which is not one of: ${accepted.join(', ')}.`);
  }
  return found;
}

/**
 * Builds the settings from raw values.
 *
 * @param raw the values of `EXPO_PUBLIC_API_URL`, `EXPO_PUBLIC_CITIZEN_ID`,
 *   `EXPO_PUBLIC_CITIZEN_DISTRICT` and `EXPO_PUBLIC_CITIZEN_LANGUAGE`
 * @returns the settings; the citizen defaults to `CIT-KLT-0001`, the district to `KALUTARA` and the
 *   language to `ENGLISH` (the seeded demo citizen)
 * @throws Error with a message that names `EXPO_PUBLIC_API_URL` when it is missing or blank, or
 *   that names `EXPO_PUBLIC_CITIZEN_DISTRICT` / `EXPO_PUBLIC_CITIZEN_LANGUAGE` and lists the accepted
 *   values when one is not a district or a language
 */
export function parseEnv(raw: RawEnv): AppEnv {
  const apiUrl = raw.apiUrl?.trim();
  if (!apiUrl) {
    throw new Error(
      "EXPO_PUBLIC_API_URL is not set. Copy mobile/.env.example to mobile/.env and put the PC's " +
        'LAN address in it, for example http://192.168.1.10:8080, then restart Expo.',
    );
  }
  const citizenId = raw.citizenId?.trim();
  return {
    apiUrl: apiUrl.replace(/\/+$/, ''),
    citizenId: citizenId ? citizenId : DEFAULT_CITIZEN_ID,
    citizenDistrict: parseChoice(
      'EXPO_PUBLIC_CITIZEN_DISTRICT',
      raw.citizenDistrict,
      DISTRICTS,
      DEFAULT_CITIZEN_DISTRICT,
    ),
    citizenLanguage: parseChoice(
      'EXPO_PUBLIC_CITIZEN_LANGUAGE',
      raw.citizenLanguage,
      LANGUAGES,
      DEFAULT_CITIZEN_LANGUAGE,
    ),
  };
}

/**
 * Reads the settings from `process.env`.
 *
 * @returns the settings
 * @throws Error when `EXPO_PUBLIC_API_URL` is missing, or when `EXPO_PUBLIC_CITIZEN_DISTRICT` or
 *   `EXPO_PUBLIC_CITIZEN_LANGUAGE` is set to a value that is not accepted
 */
export function getEnv(): AppEnv {
  return parseEnv({
    apiUrl: process.env.EXPO_PUBLIC_API_URL,
    citizenId: process.env.EXPO_PUBLIC_CITIZEN_ID,
    citizenDistrict: process.env.EXPO_PUBLIC_CITIZEN_DISTRICT,
    citizenLanguage: process.env.EXPO_PUBLIC_CITIZEN_LANGUAGE,
  });
}
