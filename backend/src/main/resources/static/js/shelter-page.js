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
    this.#bindActions();

    if (globalThis.window) {
      globalThis.window.addEventListener('online', () => this.onConnectivityRestored());
      globalThis.window.addEventListener('offline', () => this.onConnectivityLost());
    }

    await this.openShelters();
    const shelterId = this.#doc.body?.dataset.shelterId;
    if (shelterId) await this.selectShelter(shelterId);

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
        } else {
          this.showNotice(`Could not load shelters (HTTP ${res.status}).`, 'error');
        }
      }
    } catch {
      this.showConnectivity(false);
      if (this.#currentShelters.length === 0) {
        this.showNotice('Could not load shelters. Check the connection and try again.', 'error');
      }
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
          const operatorSelect = this.#doc.querySelector('[data-edit-shelter-form] [name="operatorId"]');
          if (operatorSelect) operatorSelect.value = view.shelter.operatorId ?? '';
          return view;
        }
        this.showNotice(`Could not load shelter details (HTTP ${res.status}).`, 'error');
      } catch {
        this.showConnectivity(false);
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

    const totalNum = Number(newTotal);
    const meta = await this.#sync.createMeta();
    const outcome = await this.#sync.updateOccupancy({ shelterId, newTotal: totalNum, meta });

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
      this.showShelterDetails(view);
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
        } else if (valRes.status < 500) {
          this.showNotice(`Shelter details could not be validated (HTTP ${valRes.status}).`, 'error');
          return;
        }
      } catch {
        this.showConnectivity(false);
      }
    }

    const meta = await this.#sync.createMeta();
    const outcome = await this.#sync.registerShelter({ details, meta });

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
        } else if (valRes.status < 500) {
          this.showNotice(`Shelter details could not be validated (HTTP ${valRes.status}).`, 'error');
          return;
        }
      } catch {
        this.showConnectivity(false);
      }
    }

    const meta = await this.#sync.createMeta();
    const outcome = await this.#sync.updateShelterDetails(shelterId, { details, meta });

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
      this.showShelterDetails(view);
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
    const neededCount = Number(needed);
    if (!Number.isInteger(neededCount) || neededCount <= 0) {
      this.showNotice('Enter a whole number greater than 0 for people needing places.', 'error');
      return;
    }

    const url = `${SHELTERS_PATH}/alternatives?district=${encodeURIComponent(district)}&needed=${encodeURIComponent(neededCount)}`;
    const res = await this.#fetchFn(url);
    if (res.ok) {
      const alternatives = await res.json();
      this.showNoShelterAvailable(alternatives);
      return alternatives;
    }
    throw new Error(`Could not find shelters with space (HTTP ${res.status}).`);
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

  #bindActions() {
    this.#doc.addEventListener('submit', (event) => {
      const form = event.target;
      if (!(form instanceof globalThis.HTMLFormElement)) return;

      if (form.matches('[data-occupancy-form]')) {
        event.preventDefault();
        this.#runAction(() =>
          this.enterNewOccupancy(form.dataset.shelterId, form.elements.namedItem('newTotal').value),
        );
      } else if (form.matches('[data-register-shelter-form]')) {
        event.preventDefault();
        const details = this.#readDetails(form);
        if (details) this.#runAction(() => this.enterShelterDetails(details));
      } else if (form.matches('[data-edit-shelter-form]')) {
        event.preventDefault();
        const details = this.#readDetails(form);
        if (details) {
          this.#runAction(() => this.editShelterDetails(form.dataset.shelterId, details));
        }
      } else if (form.matches('[data-alternatives-form]')) {
        event.preventDefault();
        this.#runAction(() =>
          this.findShelterWithSpace(
            form.elements.namedItem('needed').value,
            form.dataset.district,
          ),
        );
      }
    });

    this.#doc.addEventListener('click', (event) => {
      const syncButton = event.target.closest('[data-sync-now]');
      if (syncButton) {
        this.#runAction(() => this.syncNow());
        return;
      }

      const dismissButton = event.target.closest('[data-dismiss-pending]');
      if (dismissButton) {
        this.#runAction(() => this.dismissPending(dismissButton.dataset.dismissPending));
      }
    });
  }

  #readDetails(form) {
    const value = (name) => form.elements.namedItem(name)?.value ?? '';
    const capacity = value('capacity');
    if (capacity === '' || !Number.isInteger(Number(capacity))) {
      this.showShelterDetailsErrors(['Capacity must be a whole number greater than 0.']);
      return null;
    }
    return {
      name: value('name').trim() || null,
      address: value('address').trim() || null,
      district: form.dataset.district,
      capacity: Number(capacity),
      contactNumber: value('contactNumber').trim().replace(/\s+/g, '') || null,
      operatorId: value('operatorId') || null,
    };
  }

  async #runAction(action) {
    try {
      await action();
    } catch (error) {
      console.error('Shelter page action failed', error);
      this.showNotice(error.message || 'The shelter request failed. Please try again.', 'error');
    }
  }

  // UI Display Methods

  showConnectivity(online) {
    const banner = this.#doc.querySelector('.offline-banner');
    if (banner) {
      banner.hidden = online;
      banner.style.display = online ? 'none' : 'block';
      banner.textContent = online ? '' : 'Offline – entries are saved on this computer';
    }
  }

  showShelters(shelters) {
    const container = this.#doc.querySelector('.shelter-list');
    if (container) {
      container.replaceChildren(
        ...shelters.map((shelter) => {
          const id = shelter.shelterId ?? shelter.id;
          const row = this.#doc.createElement('tr');
          row.dataset.shelterId = id;
          for (const value of [shelter.name, shelter.capacity, shelter.currentOccupancy]) {
            const cell = this.#doc.createElement('td');
            cell.textContent = value;
            row.append(cell);
          }
          const statusCell = this.#doc.createElement('td');
          statusCell.textContent = this.#statusLabel(shelter.status);
          row.append(statusCell);
          const actionCell = this.#doc.createElement('td');
          const link = this.#doc.createElement('a');
          link.className = 'link';
          link.href = `/shelter/details?shelterId=${encodeURIComponent(id)}`;
          link.textContent = 'Select';
          actionCell.append(link);
          row.append(actionCell);
          return row;
        }),
      );
    }
  }

  showShelterDetails(view) {
    const detailsContainer = this.#doc.querySelector('.shelter-details-container');
    if (detailsContainer && view.shelter) {
      const shelter = view.shelter;
      detailsContainer.dataset.shelterId = shelter.shelterId ?? shelter.id;
      for (const [field, value] of Object.entries({
        name: shelter.name,
        address: shelter.address,
        district: this.#districtLabel(shelter.district),
        contactNumber: shelter.contactNumber,
        capacity: shelter.capacity,
        currentOccupancy: shelter.currentOccupancy,
        remainingCapacity: view.remainingCapacity,
        status: this.#statusLabel(shelter.status),
      })) {
        const target = detailsContainer.querySelector(`[data-shelter-field="${field}"]`);
        if (target) target.textContent = value ?? '';
      }
      const operatorSelect = this.#doc.querySelector('[data-edit-shelter-form] [name="operatorId"]');
      if (operatorSelect) operatorSelect.value = shelter.operatorId ?? '';
      if (!detailsContainer.querySelector('[data-shelter-field="name"]')) {
        detailsContainer.textContent = `${shelter.name} — Remaining: ${view.remainingCapacity}`;
      }
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
      container.replaceChildren();
      if (!alternatives || alternatives.length === 0) {
        container.textContent = 'No shelter in the district can accommodate this number of evacuees.';
        return;
      }
      for (const alternative of alternatives) {
        const shelter = alternative.shelter ?? alternative;
        const item = this.#doc.createElement('li');
        const link = this.#doc.createElement('a');
        link.className = 'link';
        link.href = `/shelter/details?shelterId=${encodeURIComponent(shelter.shelterId)}`;
        link.textContent = `${shelter.name} (${alternative.remainingCapacity} remaining)`;
        item.append(link);
        container.append(item);
      }
    }
  }

  showShelterDetailsErrors(errors) {
    const container = this.#doc.querySelector('.form-errors');
    if (container) {
      container.replaceChildren();
      for (const error of errors) {
        const message = this.#doc.createElement('p');
        message.className = 'error';
        message.textContent = error;
        container.append(message);
      }
      container.hidden = errors.length === 0;
    }
  }

  showShelterRegistered(view) {
    this.showNotice(`Shelter registered: ${view.shelter.name} with capacity ${view.shelter.capacity}.`);
  }

  showShelterUpdated(view) {
    const form = this.#doc.querySelector('[data-edit-shelter-form]');
    if (form) {
      for (const [name, value] of Object.entries({
        name: view.shelter.name,
        address: view.shelter.address,
        capacity: view.shelter.capacity,
        contactNumber: view.shelter.contactNumber,
        operatorId: view.shelter.operatorId ?? '',
      })) {
        const field = form.elements.namedItem(name);
        if (field) field.value = value ?? '';
      }
    }
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
      container.replaceChildren();
      const summary = this.#doc.createElement('p');
      summary.textContent = `${accepted} accepted, ${rejected} rejected`;
      container.append(summary);
      for (const result of results.filter((item) => item.outcome === SyncOutcome.REJECTED)) {
        const message = this.#doc.createElement('p');
        message.textContent = `: ${result.message ?? result.reason ?? 'The change was rejected.'}`;
        container.append(message);
      }
      container.hidden = results.length === 0;
      container.style.display = results.length === 0 ? 'none' : 'block';
    }
  }

  showPending(items) {
    const container = this.#doc.querySelector('.pending-list');
    if (container) {
      container.replaceChildren();
      for (const item of items) {
        const row = this.#doc.createElement('li');
        row.dataset.submissionId = item.meta.submissionId;
        row.append(this.#doc.createTextNode(`Status: ${item.status}`));
        if (item.rejectionReason) {
          row.append(this.#doc.createTextNode(` — ${item.rejectionReason}`));
        }
        if (item.status === 'REJECTED') {
          const dismiss = this.#doc.createElement('button');
          dismiss.type = 'button';
          dismiss.className = 'btn btn--secondary btn--small';
          dismiss.dataset.dismissPending = item.meta.submissionId;
          dismiss.textContent = 'Dismiss';
          row.append(' ', dismiss);
        }
        container.append(row);
      }
      const count = items.filter((item) => item.status === 'QUEUED').length;
      for (const target of this.#doc.querySelectorAll('.pending-count')) {
        target.textContent = count;
      }
      const empty = this.#doc.querySelector('.pending-empty');
      if (empty) empty.hidden = items.length !== 0;
      container.hidden = items.length === 0;
    }
  }

  #statusLabel(status) {
    return status === 'FULL' ? 'Full' : 'Open';
  }

  #districtLabel(district) {
    return String(district ?? '')
      .toLowerCase()
      .replaceAll('_', ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  showNotice(text, type = 'success') {
    const notice = this.#doc.querySelector('.page-notice');
    if (notice) {
      notice.hidden = false;
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

if (typeof document !== 'undefined' && document.body?.hasAttribute('data-shelter-page')) {
  startShelterPage().catch((error) => {
    console.error('Could not start the shelter page', error);
    const notice = document.querySelector('.page-notice');
    if (notice) {
      notice.textContent = 'Shelter page could not start. Reload the page and try again.';
      notice.className = 'notice page-notice notice--error';
      notice.style.display = 'block';
    }
  });
}
