/**
 * constants.ts — the named values of the skeleton modules (implementation standards §10: no magic
 * values in code).
 */
import type { District, Language } from './api/types';

/** The demo citizen of the app (standards §11): the Kalutara citizen with a device token. */
export const DEFAULT_CITIZEN_ID = 'CIT-KLT-0001';

/**
 * The demo citizen's registered district (CIT-KLT-0001 in the seed, standards §11): the fallback
 * district of a report and the district of the home screen's warnings.
 */
export const DEFAULT_CITIZEN_DISTRICT: District = 'KALUTARA';

/**
 * The demo citizen's preferred language in the seed: the app's starting language (standards §10,
 * PR13).
 */
export const DEFAULT_CITIZEN_LANGUAGE: Language = 'ENGLISH';

/** How long a request may take before it counts as a network failure, in milliseconds (§12.1). */
export const REQUEST_TIMEOUT_MS = 15_000;

/** The back end's port; `EXPO_PUBLIC_API_URL=auto` builds the API address with it. */
export const API_PORT = 8080;

/** The id of the Android notification channel that carries warnings (§10, Expo Go). */
export const WARNINGS_CHANNEL_ID = 'warnings';

/** The visible name of the warnings channel. */
export const WARNINGS_CHANNEL_NAME = 'Warnings';

/** The content type of a JSON body. */
export const JSON_CONTENT_TYPE = 'application/json';

/** The HTTP status that carries no body. */
export const HTTP_NO_CONTENT = 204;

/** The first HTTP status of a refused request. */
export const HTTP_ERROR_MIN = 400;

/** The path of the active-warnings endpoint (§12.1). */
export const ACTIVE_WARNINGS_PATH = '/api/warnings/active';

/** The path of a recipient's push outbox (§12.1); `{recipientId}` is replaced. */
export const PUSH_OUTBOX_PATH = '/api/push/{recipientId}/outbox';

/** The path of the report upload and of one report (§12.1); `/{reportId}` follows for one report. */
export const REPORTS_PATH = '/api/reports';

/** The path of a citizen's reports (§12.1); `{citizenId}` is replaced. */
export const CITIZEN_REPORTS_PATH = '/api/citizens/{citizenId}/reports';

/** The path of a citizen's language (§12.1); `{citizenId}` is replaced. */
export const CITIZEN_LANGUAGE_PATH = '/api/citizens/{citizenId}/language';

/** The southern edge of the box that counts as within Sri Lanka, in degrees (as the server's). */
export const SRI_LANKA_MIN_LATITUDE = 5.85;

/** The northern edge of the box that counts as within Sri Lanka, in degrees. */
export const SRI_LANKA_MAX_LATITUDE = 9.9;

/** The western edge of the box that counts as within Sri Lanka, in degrees. */
export const SRI_LANKA_MIN_LONGITUDE = 79.5;

/** The eastern edge of the box that counts as within Sri Lanka, in degrees. */
export const SRI_LANKA_MAX_LONGITUDE = 81.95;

/** The longest description of a report, in characters (SC2 step 16). */
export const MAX_DESCRIPTION_LENGTH = 500;

/** How often the app asks the server for push entries while it is active, in milliseconds (§10). */
export const PUSH_POLL_INTERVAL_MS = 5_000;

/** The quality of a picked photo, from 0 (small) to 1 (large): verified on the iPhone (P188). */
export const PHOTO_QUALITY = 0.7;

/** The key of the draft list in the phone's storage. */
export const DRAFTS_STORAGE_KEY = 'disasterlink.drafts';

/** The key of the phone's client identifier in its storage. */
export const CLIENT_ID_STORAGE_KEY = 'disasterlink.clientId';

/** The key of the cached home screen in the phone's storage. */
export const HOME_STORAGE_KEY = 'disasterlink.home';

/** The key of the citizen's chosen language in the phone's storage. */
export const LANGUAGE_STORAGE_KEY = 'disasterlink.language';

/** The text shown when the server refuses a photo that is too large (HTTP 413, §12.3). */
export const PHOTO_TOO_LARGE_MESSAGE = 'The photo is too large to send';

/** The HTTP status of a request that is larger than the server accepts. */
export const HTTP_PAYLOAD_TOO_LARGE = 413;

/** The separator of the errors in the message of a REJECTED receipt. */
export const ERROR_SEPARATOR = '; ';
