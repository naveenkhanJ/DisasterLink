/**
 * env.ts — the app's settings from the Expo public environment (`.env`, see `.env.example`).
 *
 * Expo inlines `process.env.EXPO_PUBLIC_*` at bundle time only where the variable is written out in
 * full, so {@link getEnv} names each variable literally; {@link parseEnv} is the pure part that the
 * tests call.
 *
 * `EXPO_PUBLIC_API_URL` is either the PC's address, `http://<the PC's LAN IP>:8080`, or `auto`: the
 * app then takes the PC's address from the Expo development server, which runs on the same PC as the
 * back end and whose address Expo Go knows (`Constants.expoConfig.hostUri`, present only in
 * development; standards section 10, "Device networking"). {@link resolveApiUrl} does that.
 */
import Constants from 'expo-constants';
import { DISTRICTS, type District, type Language } from '../api/types';
import {
  API_PORT,
  DEFAULT_CITIZEN_DISTRICT,
  DEFAULT_CITIZEN_ID,
  DEFAULT_CITIZEN_LANGUAGE,
} from '../constants';

/** The value of `EXPO_PUBLIC_API_URL` that asks for the address of the Expo development server. */
const AUTO = 'auto';

/** The number of dot-separated numbers in an IPv4 address. */
const IPV4_PART_COUNT = 4;

/** The largest number in an IPv4 address. */
const IPV4_PART_MAX = 255;

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
  /**
   * `Constants.expoConfig.hostUri`: the address of the Expo development server, for example
   * `192.168.1.100:8081`; present only in development.
   */
  devServerHostUri: string | undefined;
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

/** Tells whether a host is an IPv4 address: four dot-separated whole numbers from 0 to 255. */
function isIpv4(host: string): boolean {
  const parts = host.split('.');
  return (
    parts.length === IPV4_PART_COUNT &&
    parts.every((part) => /^\d{1,3}$/.test(part) && Number(part) <= IPV4_PART_MAX)
  );
}

/** Tells whether `EXPO_PUBLIC_API_URL` is `auto` (compared after trimming, ignoring case). */
function isAuto(apiUrl: string | undefined): boolean {
  return apiUrl?.trim().toLowerCase() === AUTO;
}

/**
 * Works out the address of the back end from the setting of `EXPO_PUBLIC_API_URL`.
 *
 * An explicit address is kept (trimmed, without trailing slashes). `auto` takes the host of the Expo
 * development server, the PC that also runs the back end, and adds the back end's port: the host
 * part of `devServerHostUri` is what is left after dropping everything from the first `/` and then
 * the `:port`, and it must be an IPv4 address.
 *
 * @param apiUrl the value of `EXPO_PUBLIC_API_URL`
 * @param devServerHostUri `Constants.expoConfig.hostUri`, for example `192.168.1.100:8081`
 * @returns the back end's base URL without a trailing slash, for example `http://192.168.1.100:8080`
 * @throws Error that names `EXPO_PUBLIC_API_URL` when it is missing or blank, or is `auto` and the
 *   development server's host is missing or is not an IPv4 address (a tunnel host, `localhost`)
 */
export function resolveApiUrl(
  apiUrl: string | undefined,
  devServerHostUri: string | undefined,
): string {
  const setting = apiUrl?.trim();
  if (!setting) {
    throw new Error(
      'EXPO_PUBLIC_API_URL is not set. Copy mobile/.env.example to mobile/.env and set it to ' +
        "auto (the PC's address taken from the Expo development server) or to " +
        "http://<the PC's LAN IP>:8080, for example http://192.168.1.10:8080, then restart Expo.",
    );
  }
  if (!isAuto(setting)) {
    return setting.replace(/\/+$/, '');
  }
  const hostUri = (devServerHostUri ?? '').trim();
  const host = hostUri.split('/')[0].split(':')[0];
  if (!isIpv4(host)) {
    const found = host ? `"${hostUri}", which is not an IPv4 address` : 'nothing';
    throw new Error(
      `EXPO_PUBLIC_API_URL is auto, but the Expo development server's address was ${found}. ` +
        "Set EXPO_PUBLIC_API_URL to the PC's address, http://<the PC's LAN IP>:8080 - auto needs " +
        '`npx expo start` in LAN mode (not --tunnel).',
    );
  }
  return `http://${host}:${API_PORT}`;
}

/**
 * Builds the settings from raw values.
 *
 * @param raw the values of `EXPO_PUBLIC_API_URL`, `EXPO_PUBLIC_CITIZEN_ID`,
 *   `EXPO_PUBLIC_CITIZEN_DISTRICT` and `EXPO_PUBLIC_CITIZEN_LANGUAGE`, and the address of the Expo
 *   development server
 * @returns the settings; the back end's address is the explicit one or, for `auto`, the PC's address
 *   from the development server (see {@link resolveApiUrl}); the citizen defaults to `CIT-KLT-0001`,
 *   the district to `KALUTARA` and the language to `ENGLISH` (the seeded demo citizen)
 * @throws Error with a message that names `EXPO_PUBLIC_API_URL` when it is missing or blank, or is
 *   `auto` without a usable development-server address, or that names
 *   `EXPO_PUBLIC_CITIZEN_DISTRICT` / `EXPO_PUBLIC_CITIZEN_LANGUAGE` and lists the accepted values
 *   when one is not a district or a language
 */
export function parseEnv(raw: RawEnv): AppEnv {
  const apiUrl = resolveApiUrl(raw.apiUrl, raw.devServerHostUri);
  const citizenId = raw.citizenId?.trim();
  return {
    apiUrl,
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

/** What the settings page is called when the app's own name is not known. */
const FALLBACK_APP_NAME = 'this app';

/** The name of the app that holds the permission when the code runs inside Expo Go. */
const EXPO_GO_NAME = 'Expo Go';

/**
 * The name under which the phone's Settings list this app, for the lines that send the citizen
 * there (standards section 10 item 14, "Wording", P225).
 *
 * Source: `Constants.expoGoConfig` is "populated when running in Expo Go" (Expo documentation), so
 * the name is "Expo Go"; otherwise it is `Constants.expoConfig.name` ("DisasterLink", from
 * `app.json`), and "this app" when neither is known. The documentation does not say what
 * `expoGoConfig` holds in a development build; this function relies only on what it states.
 *
 * @returns the name to show in a sentence such as "Allow location access for <name> in Settings"
 */
export function hostAppName(): string {
  if (Constants.expoGoConfig) {
    return EXPO_GO_NAME;
  }
  return Constants.expoConfig?.name ?? FALLBACK_APP_NAME;
}

/**
 * Reads the settings from `process.env` and the Expo development server's address. When
 * `EXPO_PUBLIC_API_URL` is `auto`, the address that was built is logged once at INFO level.
 *
 * @returns the settings
 * @throws Error when `EXPO_PUBLIC_API_URL` is missing or is `auto` without a usable
 *   development-server address, or when `EXPO_PUBLIC_CITIZEN_DISTRICT` or
 *   `EXPO_PUBLIC_CITIZEN_LANGUAGE` is set to a value that is not accepted
 */
export function getEnv(): AppEnv {
  const apiUrl = process.env.EXPO_PUBLIC_API_URL;
  const env = parseEnv({
    apiUrl,
    citizenId: process.env.EXPO_PUBLIC_CITIZEN_ID,
    citizenDistrict: process.env.EXPO_PUBLIC_CITIZEN_DISTRICT,
    citizenLanguage: process.env.EXPO_PUBLIC_CITIZEN_LANGUAGE,
    devServerHostUri: Constants.expoConfig?.hostUri,
  });
  if (isAuto(apiUrl)) {
    console.info('[env] API address from the Expo development server:', env.apiUrl);
  }
  return env;
}
