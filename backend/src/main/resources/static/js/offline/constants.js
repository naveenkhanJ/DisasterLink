/**
 * constants.js — the named values of the browser offline module (implementation standards section 10:
 * no magic values). Plain ES module, no globals.
 */

/** The path of the UC3 collection endpoint (registration, list). */
export const SHELTERS_PATH = '/api/shelters';

/**
 * The path of one shelter's endpoint (detail change).
 * @param {string} shelterId the identifier of the shelter
 * @returns {string} the path
 */
export function shelterPath(shelterId) {
  return `${SHELTERS_PATH}/${encodeURIComponent(shelterId)}`;
}

/**
 * The path of one shelter's occupancy endpoint.
 * @param {string} shelterId the identifier of the shelter
 * @returns {string} the path
 */
export function occupancyPath(shelterId) {
  return `${shelterPath(shelterId)}/occupancy`;
}

/** The name of the one IndexedDB database of the module. */
export const DB_NAME = 'disasterlink-offline';

/** The version of the database schema. */
export const DB_VERSION = 1;

/** The object store of the pending submissions, keyed by `meta.submissionId`. */
export const ITEMS_STORE = 'pendingSubmissions';

/** The object store that keeps the client identifier, created once. */
export const CLIENT_STORE = 'client';

/** The key of the client identifier in {@link CLIENT_STORE}. */
export const CLIENT_ID_KEY = 'clientId';

/** The key path of the pending submissions (their submission identifier). */
export const ITEMS_KEY_PATH = 'meta.submissionId';

/** The offset of Asia/Colombo that every `capturedAt` carries (model dictionary section 0.3). */
export const TIME_ZONE_OFFSET = '+05:30';

/** The offset of Asia/Colombo in minutes. */
export const TIME_ZONE_OFFSET_MINUTES = 330;

/** Milliseconds in a minute. */
export const MILLIS_PER_MINUTE = 60000;

/** The length of `YYYY-MM-DDTHH:mm:ss` in an ISO-8601 string. */
export const ISO_SECONDS_LENGTH = 19;

/** The first HTTP status of a client error (a refused request: a 4xx problem detail). */
export const CLIENT_ERROR_MIN = 400;

/** The first HTTP status of a server fault (not a rejection: the item stays queued). */
export const SERVER_ERROR_MIN = 500;

/** The headers of a JSON request. */
export const JSON_HEADERS = Object.freeze({ 'Content-Type': 'application/json' });

/** The message of a refused request whose problem detail could not be read. */
export const DEFAULT_REJECTION_MESSAGE = 'The server did not accept the change.';

/** The browser offline module's `SubmissionAction` (model dictionary section 1). */
export const SubmissionAction = Object.freeze({
  UPDATE_OCCUPANCY: 'UPDATE_OCCUPANCY',
  REGISTER_SHELTER: 'REGISTER_SHELTER',
  UPDATE_SHELTER_DETAILS: 'UPDATE_SHELTER_DETAILS',
});

/** The `PendingStatus` of an item in the browser store (model dictionary section 1). */
export const PendingStatus = Object.freeze({
  QUEUED: 'QUEUED',
  REJECTED: 'REJECTED',
});

/** The `SyncOutcome` of a result (model dictionary section 1). */
export const SyncOutcome = Object.freeze({
  ACCEPTED: 'ACCEPTED',
  DUPLICATE: 'DUPLICATE',
  REJECTED: 'REJECTED',
});

/** The `RejectionReason` of a REJECTED result (model dictionary section 1). */
export const RejectionReason = Object.freeze({
  OVER_CAPACITY: 'OVER_CAPACITY',
  INVALID_VALUE: 'INVALID_VALUE',
  VALIDATION_FAILED: 'VALIDATION_FAILED',
});
