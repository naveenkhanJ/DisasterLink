/**
 * shelter-page.js — Client-side page script for UC3 Manage Emergency Shelters.
 *
 * Implements CH-05, CH-10, CH-28, CH-29 and CH-30.
 * Coordinates between the UI displays, OfflineSyncController, and backend API.
 */
import {
  SyncOutcome,
  RejectionReason,
  SHELTERS_PATH,
} from './offline/constants.js';
import { OfflineOutbox } from './offline/offline-outbox.js';
import { OfflineSyncController, browserIsOnline } from './offline/offline-sync.js';

export class ShelterPage {
  #sync;
  #fetchFn;
  #doc;
  #currentShelters = [];
  #selectedShelter = null;
  #officerId;

  /**
   * Creates a shelter page controller.
   *
   * @param {object} options initialization options
   * @param {OfflineSyncController} options.sync sync controller instance
   * @param {Function} [options.fetchFn] fetch implementation (defaults to window.fetch)
   * @param {Document} [options.doc] DOM document
   * @param {string} [options.officerId] current district officer ID
   */
  constructor({ sync, fetchFn = globalThis.fetch, doc = globalThis.document, officerId = 'DO-COL-01' }) {
    this.#sync = sync;
    this.#fetchFn = fetchFn;
    this.#doc = doc;
    this.#officerId = officerId;
  }

  get currentShelters() {
    return this.#currentShelters;
  }

  get selectedShelter() {
    return this.#selectedShelter;
  }

  /**
   * Initializes page event listeners and restores connectivity state.
   */
  async init() {
    if (globalThis.window) {
      globalThis.window.addEventListener('online', () => this.onConnectivityRestored());
      globalThis.window.addEventListener('offline', () => this.onConnectivityLost());
    }

    await this.openShelters();

    if (browserIsOnline()) {
      await this.onConnectivityRestored();
    } else {
      this.showConnectivity(false);
    }
  }

  /**
   * Opens shelter list, populates district shelters and pending offline submissions.
   */
  async openShelters() {
    try {
      if (browserIsOnline()) {
        const res = await this.#fetchFn(SHELTERS_PATH);
        if (res.ok) {
          this.#currentShelters = await res.json();
          this.showShelters(this.#currentShelters);
        }
      }
    } catch {
      // Keep existing cached shelters if fetch fails
    }

    if (this.#sync) {
      const pending = await this.#sync.getPending();
      this.showPending(pending);
    }
  }

  /**
   * Selects a shelter by identifier. Reads online or derives details offline.
   *
   * @param {string} shelterId the shelter identifier
   */
  async selectShelter(shelterId) {
    if (browserIsOnline()) {
      try {
        const res = await this.#fetchFn(`${SHELTERS_PATH}/${encodeURIComponent(shelterId)}`);
        if (res.ok) {
          const view = await res.json();
          this.#selectedShelter = view.shelter;
          this.showShelterDetails(view);
          return view;
        }
      } catch {
        // Fall back to offline list derivation
      }
    }

    // Offline mode: build view from cached list
    const found = this.#currentShelters.find((s) => (s.shelterId ?? s.id) === shelterId);
    if (found) {
      const remainingCapacity = Math.max(0, found.capacity - (found.currentOccupancy ?? 0));
      const view = { shelter: found, remainingCapacity };
      this.#selectedShelter = found;
      this.showShelterDetails(view);
      return view;
    }
    return null;
  }

  /**
   * Validates and submits a new total occupancy count for a shelter.
   *
   * @param {string} shelterId the shelter identifier
   * @param {number|string} newTotal the candidate occupancy count
   */
  async enterNewOccupancy(shelterId, newTotal) {
    if (newTotal === '' || newTotal === null || newTotal === undefined || isNaN(Number(newTotal)) || String(newTotal).includes('.')) {
      this.showInvalidOccupancy('Enter the total number of people now in the shelter as a whole number of 0 or more.');
      return;
    }

    const totalNum = parseInt(newTotal, 10);
    const meta = await this.#sync.createMeta();
    let outcome;
    try {
      outcome = await this.#sync.updateOccupancy({ shelterId, newTotal: totalNum, meta });
    } catch {
      outcome = undefined;
    }

    if (!outcome) {
      const count = await this.#sync.pendingCount();
      this.showSavedOffline(count);
      this.showPending(await this.#sync.getPending());
      if (!browserIsOnline()) {
        this.showConnectivity(false);
      }
      return;
    }

    const { result, shelter, remainingCapacity, alternatives } = outcome;
    if (result.outcome === SyncOutcome.ACCEPTED) {
      const view = { shelter, remainingCapacity };
      if (shelter.status === 'FULL') {
        this.showShelterFull(view);
      } else {
        this.showOccupancyUpdated(view);
      }
    } else if (result.outcome === SyncOutcome.REJECTED) {
      if (result.reason === RejectionReason.OVER_CAPACITY) {
        this.showCapacityExceeded(shelter?.capacity ?? 0, remainingCapacity, alternatives);
      } else if (result.reason === RejectionReason.INVALID_VALUE) {
        this.showInvalidOccupancy(result.message);
      } else {
        this.showSyncResults([result]);
      }
    } else if (result.outcome === SyncOutcome.DUPLICATE) {
      this.showSyncResults([result]);
      await this.openShelters();
    }
    return outcome;
  }

  /**
   * Registers a new emergency shelter with validation and offline support.
   *
   * @param {object} details shelter details
   */
  async enterShelterDetails(details) {
    if (browserIsOnline()) {
      try {
        const valRes = await this.#fetchFn(`${SHELTERS_PATH}/validation`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ details, shelterId: null }),
        });
        if (valRes.ok) {
          const valData = await valRes.json();
          if (!valData.valid) {
            this.showShelterDetailsErrors(valData.errors);
            return;
          }
        }
      } catch {
        // Continue to sync layer if offline
      }
    }

    const meta = await this.#sync.createMeta();
    let outcome;
    try {
      outcome = await this.#sync.registerShelter({ details, meta });
    } catch {
      outcome = undefined;
    }

    if (!outcome) {
      const count = await this.#sync.pendingCount();
      this.showSavedOffline(count);
      this.showPending(await this.#sync.getPending());
      if (!browserIsOnline()) {
        this.showConnectivity(false);
      }
      return;
    }

    const { result, shelter, remainingCapacity } = outcome;
    if (result.outcome === SyncOutcome.ACCEPTED) {
      const view = { shelter, remainingCapacity };
      this.showShelterRegistered(view);
      await this.openShelters();
    } else if (result.outcome === SyncOutcome.DUPLICATE) {
      this.showSyncResults([result]);
      await this.openShelters();
    } else {
      this.showSyncResults([result]);
    }
    return outcome;
  }

  /**
   * Edits details of an existing emergency shelter.
   *
   * @param {string} shelterId shelter identifier
   * @param {object} details updated details
   */
  async editShelterDetails(shelterId, details) {
    if (browserIsOnline()) {
      try {
        const valRes = await this.#fetchFn(`${SHELTERS_PATH}/validation`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ details, shelterId }),
        });
        if (valRes.ok) {
          const valData = await valRes.json();
          if (!valData.valid) {
            this.showShelterDetailsErrors(valData.errors);
            return;
          }
        }
      } catch {
        // Continue to sync layer
      }
    }

    const meta = await this.#sync.createMeta();
    let outcome;
    try {
      outcome = await this.#sync.updateShelterDetails(shelterId, { details, meta });
    } catch {
      outcome = undefined;
    }

    if (!outcome) {
      const count = await this.#sync.pendingCount();
      this.showSavedOffline(count);
      this.showPending(await this.#sync.getPending());
      if (!browserIsOnline()) {
        this.showConnectivity(false);
      }
      return;
    }

    const { result, shelter, remainingCapacity } = outcome;
    if (result.outcome === SyncOutcome.ACCEPTED) {
      const view = { shelter, remainingCapacity };
      this.showShelterUpdated(view);
    } else if (result.outcome === SyncOutcome.DUPLICATE) {
      this.showSyncResults([result]);
      await this.openShelters();
    } else {
      this.showSyncResults([result]);
    }
    return outcome;
  }

  /**
   * Finds alternative open shelters with space for evacuees.
   *
   * @param {number} needed number of spaces required
   * @param {string} [district] target district (defaults to selected shelter district)
   */
  async findShelterWithSpace(needed, district = this.#selectedShelter?.district ?? 'COLOMBO') {
    if (!browserIsOnline()) {
      this.showConnectivity(false);
      return;
    }

    const url = `${SHELTERS_PATH}/alternatives?district=${encodeURIComponent(district)}&needed=${encodeURIComponent(needed)}`;
    const res = await this.#fetchFn(url);
    if (res.ok) {
      const alternatives = await res.json();
      this.showNoShelterAvailable(alternatives);
      return alternatives;
    }
    this.showNoShelterAvailable([]);
    return [];
  }

  /**
   * Replays pending offline submissions to the server.
   */
  async syncNow() {
    if (!browserIsOnline()) {
      this.showConnectivity(false);
      return;
    }

    const results = await this.#sync.replayPending();
    this.showSyncResults(results);
    this.showPending(await this.#sync.getPending());
    await this.openShelters();
    return results;
  }

  /**
   * Dismisses a rejected item from the local outbox.
   *
   * @param {string} submissionId submission identifier
   */
  async dismissPending(submissionId) {
    await this.#sync.dismiss(submissionId);
    this.showPending(await this.#sync.getPending());
  }

  /**
   * Handler when network connectivity is restored.
   */
  async onConnectivityRestored() {
    this.showConnectivity(true);
    await this.syncNow();
  }

  /**
   * Handler when network connectivity is lost.
   */
  onConnectivityLost() {
    this.showConnectivity(false);
  }

  // UI Display Methods

  showConnectivity(online) {
    const banner = this.#doc.querySelector('.offline-banner');
    if (banner) {
      banner.style.display = online ? 'none' : 'block';
      banner.textContent = online ? '' : 'Offline – entries are saved on this computer';
    }
  }

  showShelters(shelters) {
    const container = this.#doc.querySelector('.shelter-list');
    if (container) {
      container.innerHTML = shelters
        .map(
          (s) =>
            `<tr data-shelter-id="${s.shelterId ?? s.id}"><td>${s.name}</td><td>${s.capacity}</td><td>${s.currentOccupancy}</td><td>${s.status}</td></tr>`
        )
        .join('');
    }
  }

  showShelterDetails(view) {
    const detailsContainer = this.#doc.querySelector('.shelter-details-container');
    if (detailsContainer && view.shelter) {
      detailsContainer.setAttribute('data-shelter-id', view.shelter.shelterId ?? view.shelter.id);
      detailsContainer.textContent = `${view.shelter.name} — Remaining: ${view.remainingCapacity}`;
    }
  }

  showOccupancyUpdated(view) {
    this.showNotice(`Occupancy updated: ${view.shelter.name} has ${view.remainingCapacity} spaces remaining.`);
  }

  showShelterFull(view) {
    this.showNotice(`${view.shelter.name} is full (${view.shelter.capacity} of ${view.shelter.capacity}).`, 'warn');
  }

  showCapacityExceeded(capacity, remainingCapacity, alternatives = []) {
    this.showNotice(`Capacity exceeded: capacity is ${capacity}; ${remainingCapacity} places remain.`, 'error');
  }

  showInvalidOccupancy(message) {
    this.showNotice(message, 'error');
  }

  showNoShelterAvailable(alternatives) {
    const container = this.#doc.querySelector('.alternatives-container');
    if (container) {
      if (!alternatives || alternatives.length === 0) {
        container.textContent = 'No shelter in the district can accommodate this number of evacuees.';
      } else {
        container.innerHTML = alternatives
          .map((a) => `<li>${a.shelter?.name ?? a.name} (${a.remainingCapacity} remaining)</li>`)
          .join('');
      }
    }
  }

  showShelterDetailsErrors(errors) {
    const container = this.#doc.querySelector('.form-errors');
    if (container) {
      container.innerHTML = errors.map((e) => `<p class="error">${e}</p>`).join('');
    }
  }

  showShelterRegistered(view) {
    this.showNotice(`Shelter registered: ${view.shelter.name} with capacity ${view.shelter.capacity}.`);
  }

  showShelterUpdated(view) {
    this.showNotice(`Shelter details updated: ${view.shelter.name} (remaining: ${view.remainingCapacity}).`);
  }

  showSavedOffline(pendingCount) {
    this.showNotice(`Saved offline – pending sync (${pendingCount})`, 'info');
  }

  showSyncResults(results) {
    const container = this.#doc.querySelector('.sync-results');
    if (container) {
      const accepted = results.filter((r) => r.outcome === SyncOutcome.ACCEPTED).length;
      const rejected = results.filter((r) => r.outcome === SyncOutcome.REJECTED).length;
      container.textContent = `${accepted} accepted, ${rejected} rejected`;
    }
  }

  showPending(items) {
    const container = this.#doc.querySelector('.pending-list');
    if (container) {
      container.innerHTML = items
        .map(
          (item) =>
            `<li data-submission-id="${item.meta.submissionId}">Status: ${item.status}${item.rejectionReason ? ' — ' + item.rejectionReason : ''}</li>`
        )
        .join('');
    }
  }

  showNotice(text, type = 'success') {
    const notice = this.#doc.querySelector('.page-notice');
    if (notice) {
      notice.textContent = text;
      notice.className = `notice page-notice notice--${type}`;
      notice.style.display = 'block';
    }
  }
}

/**
 * Helper to bootstrap the page module when loaded in browser.
 *
 * @param {object} [options] bootstrap options
 * @param {IDBFactory} [options.indexedDB] indexedDB factory
 * @returns {Promise<ShelterPage|undefined>}
 */
export async function startShelterPage({ indexedDB = globalThis.indexedDB } = {}) {
  if (typeof window === 'undefined' || !indexedDB) return undefined;
  const outbox = await OfflineOutbox.open({ indexedDB });
  const officerId = document.body?.getAttribute('data-officer-id') ?? 'DO-COL-01';
  const sync = new OfflineSyncController({
    outbox,
    fetchFn: (...args) => window.fetch(...args),
    userId: officerId,
  });
  const fetchFn = (typeof window !== 'undefined' && window.fetch) ? window.fetch.bind(window) : globalThis.fetch;
  const page = new ShelterPage({ sync, fetchFn, doc: document, officerId });
  await page.init();
  return page;
}
