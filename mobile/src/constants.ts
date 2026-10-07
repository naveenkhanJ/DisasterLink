/**
 * constants.ts — the named values of the skeleton modules (implementation standards §10: no magic
 * values in code).
 */

/** The demo citizen of the app (standards §11): the Kalutara citizen with a device token. */
export const DEFAULT_CITIZEN_ID = 'CIT-KLT-0001';

/** How long a request may take before it counts as a network failure, in milliseconds (§12.1). */
export const REQUEST_TIMEOUT_MS = 15_000;

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

/** The value of `EXPO_PUBLIC_FIRST_RUN_CHECK` that switches the first-run check on. */
export const FIRST_RUN_CHECK_ON = '1';

/** The title of the first-run check's local notification. */
export const FIRST_RUN_TITLE = 'DisasterLink first-run check';
