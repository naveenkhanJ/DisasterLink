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
