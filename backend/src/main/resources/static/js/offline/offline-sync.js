/**
 * offline-sync.js — OfflineSyncController (model dictionary section 5, Save Data Offline in the browser).
 *
 * Every UC3 write goes through this controller. Online it calls the JSON endpoint and returns its
 * `ShelterOutcome`; without a connection (or when the server cannot be reached) it queues a
 * `PendingSubmission` in the OfflineOutbox and returns `undefined`, and the page shows
 * `showSavedOffline`. `replayPending` sends the queued items to the operation their action names.
 * Plain ES module, no globals: every dependency is injected, so the tests use fakes.
 */
import {
  CLIENT_ERROR_MIN,
  DEFAULT_REJECTION_MESSAGE,
  JSON_HEADERS,
  MILLIS_PER_MINUTE,
  ISO_MILLIS_LENGTH,
  PendingStatus,
  RejectionReason,
  SERVER_ERROR_MIN,
  SHELTERS_PATH,
  SubmissionAction,
  SyncOutcome,
  TIME_ZONE_OFFSET,
  TIME_ZONE_OFFSET_MINUTES,
  occupancyPath,
  shelterPath,
} from './constants.js';
import { newPendingSubmission, randomUuid } from './offline-outbox.js';

/** The clock of the browser. */
const SYSTEM_CLOCK = Object.freeze({ now: () => new Date() });

/** What a send gives back when the server could not be reached or had a fault. */
const UNREACHABLE = Object.freeze({ unreachable: true });

/**
 * The operation each `SubmissionAction` names: how its request is built. This lookup table replaces a
 * switch (model dictionary section 1, `SubmissionAction`); `args` is the argument without `meta`.
 */
const OPERATIONS = Object.freeze({
  [SubmissionAction.UPDATE_OCCUPANCY]: {
    method: 'POST',
    path: (args) => occupancyPath(args.shelterId),
    body: (args, meta) => ({ newTotal: args.newTotal, meta }),
  },
  [SubmissionAction.REGISTER_SHELTER]: {
    method: 'POST',
    path: () => SHELTERS_PATH,
    body: (args, meta) => ({ details: args.details, meta }),
  },
  [SubmissionAction.UPDATE_SHELTER_DETAILS]: {
    method: 'PUT',
    path: (args) => shelterPath(args.shelterId),
    body: (args, meta) => ({ details: args.details, meta }),
  },
});

/**
 * Whether the browser says it has a connection.
 * @returns {boolean} `false` only when `navigator.onLine` is `false`
 */
export function browserIsOnline() {
  return globalThis.navigator?.onLine !== false;
}

/**
 * A time as ISO-8601 in Asia/Colombo with its offset, keeping the milliseconds, for example
 * `2026-05-21T14:05:00.123+05:30` (the server accepts both forms).
 * @param {Date} date the time
 * @returns {string} the text
 */
export function toColomboIso(date) {
  const shifted = new Date(date.getTime() + TIME_ZONE_OFFSET_MINUTES * MILLIS_PER_MINUTE);
  return shifted.toISOString().slice(0, ISO_MILLIS_LENGTH) + TIME_ZONE_OFFSET;
}

/**
 * Reads the problem detail of a refused request.
 * @param {Response} response the reply
 * @returns {Promise<string>} its `detail`, or the default message when it cannot be read
 */
async function problemDetail(response) {
  try {
    const problem = await response.json();
    return problem.detail ?? DEFAULT_REJECTION_MESSAGE;
  } catch {
    return DEFAULT_REJECTION_MESSAGE;
  }
}

/**
 * The reply of a write as the page sees it when the server refused the request itself (a 4xx problem
 * detail): a REJECTED `SyncResult`, reason VALIDATION_FAILED, message = the problem's `detail`.
 * @param {string} submissionId the submission that was refused
 * @param {string} message the problem detail
 * @returns {object} a `ShelterOutcome` with no shelter and no alternatives
 */
function refusedOutcome(submissionId, message) {
  return {
    result: {
      submissionId,
      outcome: SyncOutcome.REJECTED,
      reason: RejectionReason.VALIDATION_FAILED,
      message,
    },
    remainingCapacity: 0,
    alternatives: [],
  };
}

/** The browser-side control of every UC3 write. */
export class OfflineSyncController {
  #outbox;

  #fetchFn;

  #clock;

  #userId;

  #isOnline;

  #uuid;

  #replayRun;

  /**
   * Creates the controller.
   * @param {object} dependencies the injected dependencies
   * @param {OfflineOutbox} dependencies.outbox the pending store
   * @param {function(string, object): Promise<Response>} dependencies.fetchFn `fetch` (the page
   *     passes `(...args) => window.fetch(...args)`)
   * @param {{now: function(): Date}} [dependencies.clock] the clock (default: the system clock)
   * @param {string} dependencies.userId the acting officer, the demo district officer's identifier
   * @param {function(): boolean} [dependencies.isOnline] whether the browser has a connection
   *     (default: `navigator.onLine !== false`)
   * @param {function(): string} [dependencies.uuid] makes a UUID (default: `crypto.randomUUID`)
   */
  constructor({
    outbox,
    fetchFn,
    clock = SYSTEM_CLOCK,
    userId,
    isOnline = browserIsOnline,
    uuid = randomUuid,
  }) {
    this.#outbox = outbox;
    this.#fetchFn = fetchFn;
    this.#clock = clock;
    this.#userId = userId;
    this.#isOnline = isOnline;
    this.#uuid = uuid;
  }

  /**
   * Makes the `ClientMeta` of a new write.
   * @returns {Promise<{clientId: string, userId: string, submissionId: string, capturedAt: string,
   *     pendingCount: number}>} the meta: a new submission UUID, `capturedAt` = now with `+05:30`,
   *     `pendingCount` = the QUEUED items
   */
  async createMeta() {
    return {
      clientId: this.#outbox.clientId,
      userId: this.#userId,
      submissionId: this.#uuid(),
      capturedAt: toColomboIso(this.#clock.now()),
      pendingCount: await this.pendingCount(),
    };
  }

  /**
   * Sends an occupancy update, or queues it.
   * @param {{shelterId: string, newTotal: number, meta: object}} update the `OccupancyUpdate`
   * @returns {Promise<object|undefined>} the `ShelterOutcome`, or `undefined` when it was queued
   */
  updateOccupancy(update) {
    const { meta, ...args } = update;
    return this.#write(SubmissionAction.UPDATE_OCCUPANCY, args, meta);
  }

  /**
   * Sends a shelter registration, or queues it.
   * @param {{details: object, meta: object}} submission the `ShelterSubmission`
   * @returns {Promise<object|undefined>} the `ShelterOutcome`, or `undefined` when it was queued
   */
  registerShelter(submission) {
    const { meta, ...args } = submission;
    return this.#write(SubmissionAction.REGISTER_SHELTER, args, meta);
  }

  /**
   * Sends a change of shelter details, or queues it.
   * @param {string} shelterId the shelter
   * @param {{details: object, meta: object}} submission the `ShelterSubmission`
   * @returns {Promise<object|undefined>} the `ShelterOutcome`, or `undefined` when it was queued
   */
  updateShelterDetails(shelterId, submission) {
    const { meta, ...args } = submission;
    return this.#write(SubmissionAction.UPDATE_SHELTER_DETAILS, { shelterId, ...args }, meta);
  }

  /**
   * Sends every QUEUED item to the operation its action names, oldest first. The argument is rebuilt
   * from the item's payload with its meta, whose `pendingCount` is recomputed at send time (the
   * QUEUED items other than this one) and whose `capturedAt` is kept. A call during a run joins that
   * run.
   * @returns {Promise<object[]>} the `SyncResult` of every item the server answered (accepted,
   *     duplicate or rejected); an item the server could not be reached for stays queued and has no
   *     result
   */
  replayPending() {
    if (!this.#replayRun) {
      this.#replayRun = this.#replay().finally(() => {
        this.#replayRun = undefined;
      });
    }
    return this.#replayRun;
  }

  /**
   * The items still on this browser, QUEUED and REJECTED, for `showPending`.
   * @returns {Promise<object[]>} the `PendingSubmission`s
   */
  getPending() {
    return this.#outbox.findPending();
  }

  /**
   * Removes a REJECTED item the officer has read.
   * @param {string} submissionId the `meta.submissionId` of the item
   * @returns {Promise<void>} resolves when the item is removed
   * @throws {Error} when the item is not a REJECTED item (a QUEUED item is never dismissed)
   */
  async dismiss(submissionId) {
    const items = await this.#outbox.findPending();
    const item = items.find((candidate) => candidate.meta.submissionId === submissionId);
    if (item?.status !== PendingStatus.REJECTED) {
      throw new Error(`Only a rejected item can be dismissed: ${submissionId}`);
    }
    await this.#outbox.remove(submissionId);
  }

  /**
   * The number of changes waiting to be sent.
   * @returns {Promise<number>} the QUEUED items (REJECTED items are not counted)
   */
  async pendingCount() {
    const items = await this.#outbox.findPending();
    return items.filter((item) => item.status === PendingStatus.QUEUED).length;
  }

  async #write(action, args, meta) {
    if (this.#isOnline()) {
      const sent = await this.#send(action, args, meta);
      if (!sent.unreachable) {
        return sent.outcome;
      }
    }
    await this.#outbox.save(newPendingSubmission(action, args, meta));
    return undefined;
  }

  async #send(action, args, meta) {
    const operation = OPERATIONS[action];
    let response;
    try {
      response = await this.#fetchFn(operation.path(args), {
        method: operation.method,
        headers: JSON_HEADERS,
        body: JSON.stringify(operation.body(args, meta)),
      });
    } catch {
      return UNREACHABLE;
    }
    if (response.status >= SERVER_ERROR_MIN) {
      return UNREACHABLE;
    }
    if (response.status >= CLIENT_ERROR_MIN) {
      return { outcome: refusedOutcome(meta.submissionId, await problemDetail(response)) };
    }
    return { outcome: await response.json() };
  }

  async #replay() {
    const items = (await this.#outbox.findPending()).filter(
      (item) => item.status === PendingStatus.QUEUED,
    );
    const results = [];
    for (const item of items) {
      const result = await this.#replayOne(item);
      if (result) {
        results.push(result);
      }
    }
    return results;
  }

  async #replayOne(item) {
    const meta = { ...item.meta, pendingCount: (await this.pendingCount()) - 1 };
    const sent = await this.#send(item.action, JSON.parse(item.payload), meta);
    if (sent.unreachable) {
      await this.#outbox.save({ ...item, attempts: item.attempts + 1 });
      return undefined;
    }
    const { result } = sent.outcome;
    if (result.outcome === SyncOutcome.REJECTED) {
      await this.#outbox.markRejected(item.meta.submissionId, result.message ?? result.reason);
    } else {
      await this.#outbox.remove(item.meta.submissionId);
    }
    return result;
  }
}
