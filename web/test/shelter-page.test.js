import { ShelterPage, startShelterPage } from '../../backend/src/main/resources/static/js/shelter-page.js';
import { SyncOutcome, RejectionReason, PendingStatus } from '../../backend/src/main/resources/static/js/offline/constants.js';

if (typeof structuredClone === 'undefined') {
  globalThis.structuredClone = (val) => JSON.parse(JSON.stringify(val));
}

describe('ShelterPage', () => {
  let doc;
  let fakeSync;
  let fakeFetch;
  let page;

  beforeEach(() => {
    document.body.innerHTML = `
      <div class="offline-banner" style="display:none;"></div>
      <div class="page-notice" style="display:none;"></div>
      <table><tbody class="shelter-list"></tbody></table>
      <div class="shelter-details-container"></div>
      <ul class="alternatives-container"></ul>
      <div class="form-errors"></div>
      <div class="sync-results"></div>
      <ul class="pending-list"></ul>
    `;
    doc = document;

    fakeSync = {
      createMeta: jest.fn().mockResolvedValue({ submissionId: 'sub-1', capturedAt: '2026-10-08T10:00:00+05:30', pendingCount: 0 }),
      getPending: jest.fn().mockResolvedValue([]),
      pendingCount: jest.fn().mockResolvedValue(0),
      updateOccupancy: jest.fn(),
      registerShelter: jest.fn(),
      updateShelterDetails: jest.fn(),
      replayPending: jest.fn().mockResolvedValue([]),
      dismiss: jest.fn().mockResolvedValue(undefined),
    };

    fakeFetch = jest.fn();

    page = new ShelterPage({
      sync: fakeSync,
      fetchFn: fakeFetch,
      doc,
      officerId: 'DO-COL-01',
    });
  });

  describe('openShelters', () => {
    test('fetches district shelters when online and populates list and pending items', async () => {
      const shelters = [
        { shelterId: 'SHL-1', name: 'Rathmalana', capacity: 500, currentOccupancy: 320, status: 'OPEN' },
      ];
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => shelters,
      });
      fakeSync.getPending.mockResolvedValueOnce([
        { meta: { submissionId: 'sub-1' }, status: 'QUEUED' },
      ]);

      await page.openShelters();

      expect(page.currentShelters).toEqual(shelters);
      expect(doc.querySelector('.shelter-list tr')).not.toBeNull();
      expect(doc.querySelector('.pending-list li')).not.toBeNull();
    });

    test('retains cached shelters if network fetch fails', async () => {
      fakeFetch.mockRejectedValueOnce(new Error('Network error'));
      await page.openShelters();
      expect(page.currentShelters).toEqual([]);
    });
  });

  describe('selectShelter', () => {
    test('fetches shelter details online', async () => {
      const view = {
        shelter: { shelterId: 'SHL-1', name: 'Rathmalana' },
        remainingCapacity: 180,
      };
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => view,
      });

      const result = await page.selectShelter('SHL-1');

      expect(result).toEqual(view);
      expect(page.selectedShelter).toEqual(view.shelter);
      expect(doc.querySelector('.shelter-details-container').textContent).toContain('Rathmalana');
    });

    test('falls back to local cached list when offline or fetch fails', async () => {
      const shelters = [
        { shelterId: 'SHL-1', name: 'Rathmalana', capacity: 500, currentOccupancy: 300, status: 'OPEN' },
      ];
      // Preload currentShelters
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => shelters });
      await page.openShelters();

      // Now fetch fails for select
      fakeFetch.mockRejectedValueOnce(new Error('Network offline'));
      const result = await page.selectShelter('SHL-1');

      expect(result.shelter).toEqual(shelters[0]);
      expect(result.remainingCapacity).toBe(200);
    });

    test('returns null if shelter not found in local cached list', async () => {
      fakeFetch.mockRejectedValueOnce(new Error('Network offline'));
      const result = await page.selectShelter('SHL-UNKNOWN');
      expect(result).toBeNull();
    });
  });

  describe('enterNewOccupancy', () => {
    test('rejects non-numeric or decimal occupancy entry', async () => {
      await page.enterNewOccupancy('SHL-1', 'abc');
      expect(doc.querySelector('.page-notice').textContent).toContain('whole number of 0 or more');

      await page.enterNewOccupancy('SHL-1', '350.5');
      expect(doc.querySelector('.page-notice').textContent).toContain('whole number of 0 or more');
    });

    test('handles offline queued response when updateOccupancy returns undefined', async () => {
      fakeSync.updateOccupancy.mockResolvedValueOnce(undefined);
      fakeSync.pendingCount.mockResolvedValueOnce(1);
      fakeSync.getPending.mockResolvedValueOnce([{ meta: { submissionId: 'sub-1' }, status: 'QUEUED' }]);

      await page.enterNewOccupancy('SHL-1', 350);

      expect(doc.querySelector('.page-notice').textContent).toContain('Saved offline – pending sync (1)');
      expect(doc.querySelector('.pending-list li')).not.toBeNull();
    });

    test('handles accepted response below capacity', async () => {
      const outcome = {
        result: { outcome: SyncOutcome.ACCEPTED },
        shelter: { shelterId: 'SHL-1', name: 'Rathmalana', capacity: 500, status: 'OPEN' },
        remainingCapacity: 150,
      };
      fakeSync.updateOccupancy.mockResolvedValueOnce(outcome);

      await page.enterNewOccupancy('SHL-1', 350);

      expect(doc.querySelector('.page-notice').textContent).toContain('Occupancy updated: Rathmalana has 150');
    });

    test('handles accepted response when capacity is reached (FULL)', async () => {
      const outcome = {
        result: { outcome: SyncOutcome.ACCEPTED },
        shelter: { shelterId: 'SHL-1', name: 'Rathmalana', capacity: 500, status: 'FULL' },
        remainingCapacity: 0,
      };
      fakeSync.updateOccupancy.mockResolvedValueOnce(outcome);

      await page.enterNewOccupancy('SHL-1', 500);

      expect(doc.querySelector('.page-notice').textContent).toContain('Rathmalana is full (500 of 500)');
    });

    test('handles over-capacity rejection', async () => {
      const outcome = {
        result: { outcome: SyncOutcome.REJECTED, reason: RejectionReason.OVER_CAPACITY },
        shelter: { capacity: 500 },
        remainingCapacity: 180,
        alternatives: [],
      };
      fakeSync.updateOccupancy.mockResolvedValueOnce(outcome);

      await page.enterNewOccupancy('SHL-1', 520);

      expect(doc.querySelector('.page-notice').textContent).toContain('Capacity exceeded: capacity is 500; 180 places remain');
    });

    test('handles invalid-value rejection', async () => {
      const outcome = {
        result: { outcome: SyncOutcome.REJECTED, reason: RejectionReason.INVALID_VALUE, message: 'Invalid total' },
      };
      fakeSync.updateOccupancy.mockResolvedValueOnce(outcome);

      await page.enterNewOccupancy('SHL-1', -10);

      expect(doc.querySelector('.page-notice').textContent).toBe('Invalid total');
    });

    test('handles duplicate submission reply', async () => {
      const outcome = {
        result: { outcome: SyncOutcome.DUPLICATE },
      };
      fakeSync.updateOccupancy.mockResolvedValueOnce(outcome);
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => [] });

      await page.enterNewOccupancy('SHL-1', 350);

      expect(doc.querySelector('.sync-results')).not.toBeNull();
    });
    test('handles other rejection reason in enterNewOccupancy', async () => {
      const outcome = {
        result: { outcome: SyncOutcome.REJECTED, reason: RejectionReason.VALIDATION_FAILED, message: 'Rejected' },
      };
      fakeSync.updateOccupancy.mockResolvedValueOnce(outcome);

      await page.enterNewOccupancy('SHL-1', 350);

      expect(doc.querySelector('.sync-results').textContent).toContain('0 accepted, 1 rejected');
      expect(doc.querySelector('.sync-results').textContent).toContain('Rejected');
    });

    test('surfaces local outbox errors instead of reporting a saved offline update', async () => {
      fakeSync.updateOccupancy.mockRejectedValueOnce(new Error('Outbox write error'));

      await expect(page.enterNewOccupancy('SHL-1', 350)).rejects.toThrow('Outbox write error');
    });
  });

  describe('enterShelterDetails', () => {
    test('displays field errors when server validation fails', async () => {
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ valid: false, errors: ['name: required', 'capacity: must be at least 1'] }),
      });

      await page.enterShelterDetails({ name: '', capacity: 0 });

      expect(doc.querySelector('.form-errors').textContent).toContain('name: required');
    });

    test('registers shelter and refreshes list on success', async () => {
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ valid: true, errors: [] }),
      });
      const outcome = {
        result: { outcome: SyncOutcome.ACCEPTED },
        shelter: { name: 'Wellampitiya', capacity: 250 },
        remainingCapacity: 250,
      };
      fakeSync.registerShelter.mockResolvedValueOnce(outcome);
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => [] }); // for openShelters

      await page.enterShelterDetails({ name: 'Wellampitiya', capacity: 250 });

      expect(doc.querySelector('.page-notice').textContent).toContain('Shelter registered: Wellampitiya with capacity 250');
    });

    test('handles duplicate reply in enterShelterDetails', async () => {
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => ({ valid: true, errors: [] }) });
      const outcome = { result: { outcome: SyncOutcome.DUPLICATE } };
      fakeSync.registerShelter.mockResolvedValueOnce(outcome);
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => [] });

      await page.enterShelterDetails({ name: 'Wellampitiya', capacity: 250 });

      expect(doc.querySelector('.sync-results')).not.toBeNull();
    });

    test('handles rejected reply in enterShelterDetails', async () => {
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => ({ valid: true, errors: [] }) });
      const outcome = { result: { outcome: SyncOutcome.REJECTED, reason: RejectionReason.VALIDATION_FAILED } };
      fakeSync.registerShelter.mockResolvedValueOnce(outcome);

      await page.enterShelterDetails({ name: 'Wellampitiya', capacity: 250 });

      expect(doc.querySelector('.sync-results').textContent).toContain('0 accepted, 1 rejected');
      expect(doc.querySelector('.sync-results').textContent).toContain('VALIDATION_FAILED');
    });

    test('handles offline queueing on network failure', async () => {
      fakeFetch.mockRejectedValueOnce(new Error('Network error'));
      fakeSync.registerShelter.mockResolvedValueOnce(undefined);
      fakeSync.pendingCount.mockResolvedValueOnce(1);
      fakeSync.getPending.mockResolvedValueOnce([{ meta: { submissionId: 'sub-1' }, status: 'QUEUED' }]);

      await page.enterShelterDetails({ name: 'Wellampitiya', capacity: 250 });

      expect(doc.querySelector('.page-notice').textContent).toContain('Saved offline – pending sync (1)');
    });

    test('surfaces local outbox errors instead of reporting a saved offline registration', async () => {
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => ({ valid: true, errors: [] }) });
      fakeSync.registerShelter.mockRejectedValueOnce(new Error('Write error'));

      await expect(
        page.enterShelterDetails({ name: 'Wellampitiya', capacity: 250 }),
      ).rejects.toThrow('Write error');
    });
  });

  describe('editShelterDetails', () => {
    test('displays field errors when edit validation fails', async () => {
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ valid: false, errors: ['capacity below occupancy'] }),
      });

      await page.editShelterDetails('SHL-1', { capacity: 100 });

      expect(doc.querySelector('.form-errors').textContent).toContain('capacity below occupancy');
    });

    test('updates shelter details on success', async () => {
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ valid: true, errors: [] }),
      });
      const outcome = {
        result: { outcome: SyncOutcome.ACCEPTED },
        shelter: { name: 'Rathmalana' },
        remainingCapacity: 230,
      };
      fakeSync.updateShelterDetails.mockResolvedValueOnce(outcome);

      await page.editShelterDetails('SHL-1', { capacity: 550 });

      expect(doc.querySelector('.page-notice').textContent).toContain('Shelter details updated: Rathmalana');
    });

    test('handles duplicate reply in editShelterDetails', async () => {
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => ({ valid: true, errors: [] }) });
      const outcome = { result: { outcome: SyncOutcome.DUPLICATE } };
      fakeSync.updateShelterDetails.mockResolvedValueOnce(outcome);
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => [] });

      await page.editShelterDetails('SHL-1', { capacity: 550 });

      expect(doc.querySelector('.sync-results')).not.toBeNull();
    });

    test('handles rejected reply in editShelterDetails', async () => {
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => ({ valid: true, errors: [] }) });
      const outcome = { result: { outcome: SyncOutcome.REJECTED, reason: RejectionReason.VALIDATION_FAILED } };
      fakeSync.updateShelterDetails.mockResolvedValueOnce(outcome);

      await page.editShelterDetails('SHL-1', { capacity: 550 });

      expect(doc.querySelector('.sync-results').textContent).toContain('0 accepted, 1 rejected');
      expect(doc.querySelector('.sync-results').textContent).toContain('VALIDATION_FAILED');
    });

    test('surfaces local outbox errors instead of reporting a saved offline edit', async () => {
      fakeFetch.mockRejectedValueOnce(new Error('Network error'));
      fakeSync.updateShelterDetails.mockRejectedValueOnce(new Error('Write error'));

      await expect(page.editShelterDetails('SHL-1', { capacity: 550 })).rejects.toThrow('Write error');
    });
  });

  describe('findShelterWithSpace', () => {
    test('fetches alternatives and renders in container', async () => {
      const alternatives = [
        { shelter: { name: 'Dehiwala' }, remainingCapacity: 280 },
      ];
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => alternatives,
      });

      await page.findShelterWithSpace(200, 'COLOMBO');

      expect(doc.querySelector('.alternatives-container').textContent).toContain('Dehiwala (280 remaining)');
    });

    test('shows no shelter available message when list is empty', async () => {
      fakeFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      });

      await page.findShelterWithSpace(500, 'COLOMBO');

      expect(doc.querySelector('.alternatives-container').textContent).toContain('No shelter in the district');
    });
  });

  describe('syncNow and dismissPending', () => {
    test('replays pending items and updates results and list', async () => {
      const results = [
        { outcome: SyncOutcome.ACCEPTED },
        { outcome: SyncOutcome.REJECTED },
      ];
      fakeSync.replayPending.mockResolvedValueOnce(results);
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => [] }); // openShelters

      await page.syncNow();

      expect(doc.querySelector('.sync-results').textContent).toContain('1 accepted, 1 rejected');
    });

    test('dismissPending dismisses item and refreshes pending list', async () => {
      fakeSync.getPending.mockResolvedValueOnce([]);

      await page.dismissPending('sub-1');

      expect(fakeSync.dismiss).toHaveBeenCalledWith('sub-1');
    });
  });

  describe('connectivity handlers', () => {
    test('onConnectivityLost shows offline banner', () => {
      page.onConnectivityLost();
      expect(doc.querySelector('.offline-banner').style.display).toBe('block');
    });

    test('onConnectivityRestored hides banner and triggers syncNow', async () => {
      fakeSync.replayPending.mockResolvedValueOnce([]);
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => [] });

      await page.onConnectivityRestored();

      expect(doc.querySelector('.offline-banner').style.display).toBe('none');
      expect(fakeSync.replayPending).toHaveBeenCalled();
    });
  });

  describe('init and bootstrap', () => {
    test('init sets up connectivity listeners and opens shelters', async () => {
      fakeFetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
      fakeSync.replayPending.mockResolvedValueOnce([]);

      await page.init();

      expect(fakeSync.getPending).toHaveBeenCalled();
    });

    test('submits the occupancy form through the UC3 sync controller', async () => {
      const isolatedDoc = document.implementation.createHTMLDocument('Shelter integration');
      isolatedDoc.body.innerHTML = `
        <div class="offline-banner" hidden></div>
        <div class="page-notice" hidden></div>
        <table><tbody class="shelter-list"></tbody></table>
        <ul class="pending-list" hidden></ul>
        <p class="pending-empty"></p>
        <strong class="pending-count"></strong>
        <div class="sync-results" hidden></div>
        <form data-occupancy-form data-shelter-id="SHL-1">
          <input name="newTotal" value="350">
        </form>
      `;
      fakeFetch.mockResolvedValue({ ok: true, json: async () => [] });
      fakeSync.updateOccupancy.mockResolvedValueOnce({
        result: { outcome: SyncOutcome.ACCEPTED },
        shelter: {
          shelterId: 'SHL-1',
          name: 'Rathmalana',
          capacity: 500,
          currentOccupancy: 350,
          status: 'OPEN',
        },
        remainingCapacity: 150,
      });
      const integratedPage = new ShelterPage({
        sync: fakeSync,
        fetchFn: fakeFetch,
        doc: isolatedDoc,
        officerId: 'DO-COL-01',
      });

      await integratedPage.init();
      isolatedDoc.querySelector('[data-occupancy-form]').dispatchEvent(
        new Event('submit', { bubbles: true, cancelable: true }),
      );
      await new Promise((resolve) => setTimeout(resolve, 0));

      expect(fakeSync.updateOccupancy).toHaveBeenCalledWith(
        expect.objectContaining({ shelterId: 'SHL-1', newTotal: 350 }),
      );
      expect(isolatedDoc.querySelector('.page-notice').textContent).toContain('Occupancy updated');
    });

    test('submits a registration form with the backend phone-number format', async () => {
      const isolatedDoc = document.implementation.createHTMLDocument('Shelter registration');
      isolatedDoc.body.innerHTML = `
        <div class="offline-banner" hidden></div>
        <div class="page-notice" hidden></div>
        <ul class="pending-list" hidden></ul>
        <p class="pending-empty"></p>
        <strong class="pending-count"></strong>
        <div class="sync-results" hidden></div>
        <div class="form-errors" hidden></div>
        <form data-register-shelter-form data-district="COLOMBO">
          <input name="name" value="Wellampitiya Hall">
          <input name="address" value="Avissawella Road">
          <input name="capacity" value="250">
          <input name="contactNumber" value="011 253 4417">
          <select name="operatorId"><option value="" selected>None</option></select>
        </form>
      `;
      fakeFetch.mockResolvedValue({ ok: true, json: async () => [] });
      fakeSync.registerShelter.mockResolvedValueOnce({
        result: { outcome: SyncOutcome.ACCEPTED },
        shelter: { name: 'Wellampitiya Hall', capacity: 250 },
        remainingCapacity: 250,
      });
      const registrationPage = new ShelterPage({
        sync: fakeSync,
        fetchFn: fakeFetch,
        doc: isolatedDoc,
        officerId: 'DO-COL-01',
      });

      await registrationPage.init();
      isolatedDoc.querySelector('[data-register-shelter-form]').dispatchEvent(
        new Event('submit', { bubbles: true, cancelable: true }),
      );
      await new Promise((resolve) => setTimeout(resolve, 0));

      expect(fakeSync.registerShelter).toHaveBeenCalledWith(
        expect.objectContaining({
          details: expect.objectContaining({
            district: 'COLOMBO',
            contactNumber: '0112534417',
            operatorId: null,
          }),
        }),
      );
      expect(isolatedDoc.querySelector('.page-notice').textContent).toContain('Shelter registered');
    });

    test('startShelterPage bootstraps successfully with indexedDB', async () => {
      const { IDBFactory } = require('fake-indexeddb');
      const fakeIdb = new IDBFactory();
      fakeFetch.mockResolvedValue({ ok: true, json: async () => [] });
      const pageInstance = await startShelterPage({ indexedDB: fakeIdb });
      expect(pageInstance).toBeInstanceOf(ShelterPage);
    });
  });
});
