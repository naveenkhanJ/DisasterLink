/**
 * offline-outbox.js — OfflineOutbox (model dictionary section 3, {client: browser, IndexedDB}).
 *
 * Keeps the shelter changes entered without a connection in one IndexedDB database with one object
 * store of pending submissions, keyed by `meta.submissionId`. The client identifier is a UUID made
 * once and kept in the same database (a second, tiny store), so every browser tab and every later
 * visit of the same browser sends the same `clientId`.
 */
import {
  CLIENT_ID_KEY,
  CLIENT_STORE,
  DB_NAME,
  DB_VERSION,
  ITEMS_KEY_PATH,
  ITEMS_STORE,
  PendingStatus,
} from './constants.js';

/**
 * A new UUID from the browser.
 * @returns {string} the UUID
 */
export function randomUuid() {
  return globalThis.crypto.randomUUID();
}

/**
 * Builds a queued item (model dictionary `PendingSubmission`).
 * @param {string} action a `SubmissionAction`
 * @param {object} args the arguments of the operation without `meta` (`shelterId` included)
 * @param {object} meta the `ClientMeta` of the submission
 * @returns {{action: string, payload: string, meta: object, status: string, attempts: number}}
 *     the item: `payload` is the serialised arguments, `status` QUEUED, `attempts` 0
 */
export function newPendingSubmission(action, args, meta) {
  return { action, payload: JSON.stringify(args), meta, status: PendingStatus.QUEUED, attempts: 0 };
}

/**
 * A copy of an item with another status (`PendingSubmission.withStatus`); the item is not changed.
 * @param {object} item the item
 * @param {string} status the new `PendingStatus`
 * @returns {object} the copy
 */
export function withStatus(item, status) {
  return { ...item, status };
}

/**
 * Wraps an IndexedDB request in a promise.
 * @param {IDBRequest} request the request
 * @returns {Promise<*>} the request's result
 */
function promisify(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

/**
 * Opens the database, creating its two object stores the first time.
 * @param {IDBFactory} indexedDB the IndexedDB factory
 * @param {string} name the database name
 * @returns {Promise<IDBDatabase>} the open database
 */
function openDatabase(indexedDB, name) {
  const request = indexedDB.open(name, DB_VERSION);
  request.onupgradeneeded = () => {
    request.result.createObjectStore(ITEMS_STORE, { keyPath: ITEMS_KEY_PATH });
    request.result.createObjectStore(CLIENT_STORE);
  };
  return promisify(request);
}

/**
 * Reads the client identifier, or stores a new one when there is none.
 * @param {IDBDatabase} database the open database
 * @param {function(): string} uuid makes a UUID
 * @returns {Promise<string>} the client identifier
 */
async function loadClientId(database, uuid) {
  const transaction = database.transaction(CLIENT_STORE, 'readwrite');
  const store = transaction.objectStore(CLIENT_STORE);
  const stored = await promisify(store.get(CLIENT_ID_KEY));
  if (stored !== undefined) {
    return stored;
  }
  const created = uuid();
  await promisify(store.put(created, CLIENT_ID_KEY));
  return created;
}

/** The pending shelter changes of this browser. */
export class OfflineOutbox {
  #database;

  /**
   * Use {@link OfflineOutbox.open}; the constructor takes an already open database.
   * @param {IDBDatabase} database the open database
   * @param {string} clientId the client identifier
   */
  constructor(database, clientId) {
    this.#database = database;
    /** @type {string} the identifier of this browser, stable across instances and visits */
    this.clientId = clientId;
  }

  /**
   * Opens (and, the first time, creates) the database and reads or creates the client identifier.
   * @param {{indexedDB?: IDBFactory, uuid?: function(): string, name?: string}} [options] the
   *     IndexedDB factory (default: the browser's), the UUID source and the database name
   * @returns {Promise<OfflineOutbox>} the outbox
   */
  static async open({ indexedDB = globalThis.indexedDB, uuid = randomUuid, name = DB_NAME } = {}) {
    const database = await openDatabase(indexedDB, name);
    return new OfflineOutbox(database, await loadClientId(database, uuid));
  }

  /**
   * Runs one request in a transaction and resolves when the transaction has completed.
   * @param {string} mode the transaction mode
   * @param {function(IDBObjectStore): IDBRequest} work starts the request on the items store
   * @returns {Promise<*>} the request's result
   */
  #transact(mode, work) {
    return new Promise((resolve, reject) => {
      const transaction = this.#database.transaction(ITEMS_STORE, mode);
      const request = work(transaction.objectStore(ITEMS_STORE));
      transaction.oncomplete = () => resolve(request.result);
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
  }

  /**
   * Saves an item, replacing the one with the same `meta.submissionId`.
   * @param {object} item the `PendingSubmission`
   * @returns {Promise<void>} resolves when the item is stored
   */
  async save(item) {
    await this.#transact('readwrite', (store) => store.put(item));
  }

  /**
   * The items still on this browser: QUEUED and REJECTED (both are shown; a replay sends only the
   * QUEUED ones), in order of their capture time.
   * @returns {Promise<object[]>} the items
   */
  async findPending() {
    const items = await this.#transact('readonly', (store) => store.getAll());
    return items.sort((a, b) => Date.parse(a.meta.capturedAt) - Date.parse(b.meta.capturedAt));
  }

  /**
   * Removes an item; nothing happens when there is none with that identifier.
   * @param {string} submissionId the `meta.submissionId` of the item
   * @returns {Promise<void>} resolves when the item is gone
   */
  async remove(submissionId) {
    await this.#transact('readwrite', (store) => store.delete(submissionId));
  }

  /**
   * Marks an item REJECTED with the reason the server gave; nothing happens when there is no item
   * with that identifier.
   * @param {string} submissionId the `meta.submissionId` of the item
   * @param {string} reason the rejection reason shown to the officer
   * @returns {Promise<void>} resolves when the item is stored
   */
  async markRejected(submissionId, reason) {
    await this.#transact('readwrite', (store) => {
      const request = store.get(submissionId);
      request.onsuccess = () => {
        if (request.result) {
          store.put({ ...withStatus(request.result, PendingStatus.REJECTED), rejectionReason: reason });
        }
      };
      return request;
    });
  }
}
