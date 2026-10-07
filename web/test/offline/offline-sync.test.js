/**
 * Tests of the browser OfflineSyncController with a real OfflineOutbox on fake IndexedDB and a fake
 * fetch and clock: every operation online and offline, replay of every kind of reply, the recomputed
 * pendingCount, single-flight, dismiss.
 *
 * @jest-environment node
 */
import { IDBFactory } from 'fake-indexeddb';
import {
  PendingStatus,
  RejectionReason,
  SubmissionAction,
  SyncOutcome,
} from '../../../backend/src/main/resources/static/js/offline/constants.js';
import { OfflineOutbox, newPendingSubmission } from '../../../backend/src/main/resources/static/js/offline/offline-outbox.js';
import {
  OfflineSyncController,
  browserIsOnline,
  toColomboIso,
} from '../../../backend/src/main/resources/static/js/offline/offline-sync.js';

const USER_ID = 'DO-COL-01';
const CLOCK = { now: () => new Date('2026-05-21T08:35:00Z') }; // 14:05 in Colombo
const DETAILS = {
  name: 'Wellampitiya Community Hall',
  address: 'Avissawella Road, Wellampitiya',
  district: 'COLOMBO',
  capacity: 250,
  contactNumber: '0112534417',
};

function reply(status, body) {
  return { status, json: async () => body };
}

function outcome(kind, extra = {}) {
  return { result: { submissionId: 's', outcome: kind, ...extra }, remainingCapacity: 150, alternatives: [] };
}

let outbox;
let fetchFn;
let online;
let counter;
let controller;

beforeEach(async () => {
  outbox = await OfflineOutbox.open({ indexedDB: new IDBFactory() });
  fetchFn = jest.fn();
  online = true;
  counter = 0;
  controller = new OfflineSyncController({
    outbox,
    fetchFn,
    clock: CLOCK,
    userId: USER_ID,
    isOnline: () => online,
    uuid: () => `uuid-${(counter += 1)}`,
  });
});

async function meta(extra = {}) {
  return { ...(await controller.createMeta()), ...extra };
}

async function queue(action, args, submissionId, capturedAt, status = PendingStatus.QUEUED) {
  const queued = newPendingSubmission(action, args, {
    clientId: outbox.clientId,
    userId: USER_ID,
    submissionId,
    capturedAt,
    pendingCount: 99,
  });
  await outbox.save({ ...queued, status });
}

describe('createMeta', () => {
  test('has the client id, the user, a new submission id, the capture time with +05:30 and the QUEUED count', async () => {
    await queue(SubmissionAction.UPDATE_OCCUPANCY, { shelterId: 'SHL-1', newTotal: 1 }, 'q-1', '2026-05-21T14:00:00+05:30');
    await queue(SubmissionAction.UPDATE_OCCUPANCY, { shelterId: 'SHL-1', newTotal: 2 }, 'r-1', '2026-05-21T14:01:00+05:30', PendingStatus.REJECTED);

    const created = await controller.createMeta();

    expect(created).toEqual({
      clientId: outbox.clientId,
      userId: USER_ID,
      submissionId: 'uuid-1',
      capturedAt: '2026-05-21T14:05:00+05:30',
      pendingCount: 1,
    });
  });

  test('every call makes a new submission id', async () => {
    const first = await controller.createMeta();
    const second = await controller.createMeta();

    expect(second.submissionId).not.toBe(first.submissionId);
  });

  test('uses the system clock and crypto.randomUUID by default', async () => {
    const defaults = new OfflineSyncController({ outbox, fetchFn, userId: USER_ID });

    const created = await defaults.createMeta();

    expect(created.submissionId).toMatch(/^[0-9a-f-]{36}$/);
    expect(created.capturedAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+05:30$/);
  });
});

describe('toColomboIso and browserIsOnline', () => {
  test('toColomboIso moves to Asia/Colombo across midnight', () => {
    expect(toColomboIso(new Date('2026-05-21T20:00:00Z'))).toBe('2026-05-22T01:30:00+05:30');
  });

  test('browserIsOnline is true without a navigator or with navigator.onLine true or absent', () => {
    const saved = Object.getOwnPropertyDescriptor(globalThis, 'navigator');
    try {
      Object.defineProperty(globalThis, 'navigator', { value: undefined, configurable: true });
      expect(browserIsOnline()).toBe(true);
      Object.defineProperty(globalThis, 'navigator', { value: { onLine: true }, configurable: true });
      expect(browserIsOnline()).toBe(true);
      Object.defineProperty(globalThis, 'navigator', { value: {}, configurable: true });
      expect(browserIsOnline()).toBe(true);
    } finally {
      Object.defineProperty(globalThis, 'navigator', saved);
    }
  });

  test('without the isOnline dependency, navigator.onLine false queues the change without calling fetch', async () => {
    const saved = Object.getOwnPropertyDescriptor(globalThis, 'navigator');
    const defaults = new OfflineSyncController({ outbox, fetchFn, clock: CLOCK, userId: USER_ID });
    try {
      Object.defineProperty(globalThis, 'navigator', { value: { onLine: false }, configurable: true });
      expect(browserIsOnline()).toBe(false);

      const result = await defaults.updateOccupancy({ shelterId: 'SHL-1', newTotal: 350, meta: await meta() });

      expect(result).toBeUndefined();
      expect(fetchFn).not.toHaveBeenCalled();
      expect(await defaults.pendingCount()).toBe(1);
    } finally {
      Object.defineProperty(globalThis, 'navigator', saved);
    }
  });
});

describe('writes while online', () => {
  test('updateOccupancy POSTs to the shelter\'s occupancy endpoint and returns the ShelterOutcome', async () => {
    const accepted = outcome(SyncOutcome.ACCEPTED);
    fetchFn.mockResolvedValue(reply(200, accepted));
    const created = await meta();

    const result = await controller.updateOccupancy({ shelterId: 'SHL-1', newTotal: 350, meta: created });

    expect(result).toBe(accepted);
    expect(fetchFn).toHaveBeenCalledTimes(1);
    const [url, options] = fetchFn.mock.calls[0];
    expect(url).toBe('/api/shelters/SHL-1/occupancy');
    expect(options.method).toBe('POST');
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(options.body)).toEqual({ newTotal: 350, meta: created });
    expect(await controller.pendingCount()).toBe(0);
  });

  test('registerShelter POSTs the submission to the collection', async () => {
    const accepted = outcome(SyncOutcome.ACCEPTED);
    fetchFn.mockResolvedValue(reply(200, accepted));
    const created = await meta();

    const result = await controller.registerShelter({ details: DETAILS, meta: created });

    expect(result).toBe(accepted);
    const [url, options] = fetchFn.mock.calls[0];
    expect(url).toBe('/api/shelters');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({ details: DETAILS, meta: created });
  });

  test('updateShelterDetails PUTs the submission to the shelter, encoding the identifier', async () => {
    fetchFn.mockResolvedValue(reply(200, outcome(SyncOutcome.ACCEPTED)));
    const created = await meta();

    await controller.updateShelterDetails('SHL 1/a', { details: DETAILS, meta: created });

    const [url, options] = fetchFn.mock.calls[0];
    expect(url).toBe('/api/shelters/SHL%201%2Fa');
    expect(options.method).toBe('PUT');
    expect(JSON.parse(options.body)).toEqual({ details: DETAILS, meta: created });
  });

  test('a REJECTED reply (HTTP 200) is returned as it is and is not queued', async () => {
    const rejected = outcome(SyncOutcome.REJECTED, { reason: RejectionReason.OVER_CAPACITY, message: 'Capacity exceeded' });
    fetchFn.mockResolvedValue(reply(200, rejected));

    const result = await controller.updateOccupancy({ shelterId: 'SHL-1', newTotal: 520, meta: await meta() });

    expect(result).toBe(rejected);
    expect(await controller.pendingCount()).toBe(0);
    expect(await controller.getPending()).toEqual([]);
  });

  test('a 4xx problem detail becomes a REJECTED VALIDATION_FAILED result with its detail and is not queued', async () => {
    fetchFn.mockResolvedValue(reply(400, { status: 400, detail: 'The request is not valid.' }));
    const created = await meta();

    const result = await controller.registerShelter({ details: DETAILS, meta: created });

    expect(result.result).toEqual({
      submissionId: created.submissionId,
      outcome: SyncOutcome.REJECTED,
      reason: RejectionReason.VALIDATION_FAILED,
      message: 'The request is not valid.',
    });
    expect(result.shelter).toBeUndefined();
    expect(result.alternatives).toEqual([]);
    expect(await controller.getPending()).toEqual([]);
  });

  test('a 4xx reply with no readable detail gets the default message', async () => {
    fetchFn.mockResolvedValueOnce({ status: 404, json: async () => { throw new Error('not json'); } });
    fetchFn.mockResolvedValueOnce(reply(404, {}));

    const unreadable = await controller.updateOccupancy({ shelterId: 'SHL-X', newTotal: 1, meta: await meta() });
    const noDetail = await controller.updateOccupancy({ shelterId: 'SHL-X', newTotal: 1, meta: await meta() });

    expect(unreadable.result.message).toBe('The server did not accept the change.');
    expect(noDetail.result.message).toBe('The server did not accept the change.');
  });
});

describe('writes that cannot reach the server', () => {
  function expectQueued(items, action, args, created) {
    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({ action, status: PendingStatus.QUEUED, attempts: 0, meta: created });
    expect(JSON.parse(items[0].payload)).toEqual(args);
    expect(items[0].payload).not.toContain('submissionId');
  }

  test('without a connection the change is queued, fetch is not called and the result is undefined', async () => {
    online = false;
    const created = await meta();

    const result = await controller.updateOccupancy({ shelterId: 'SHL-1', newTotal: 350, meta: created });

    expect(result).toBeUndefined();
    expect(fetchFn).not.toHaveBeenCalled();
    expectQueued(await controller.getPending(), SubmissionAction.UPDATE_OCCUPANCY, { shelterId: 'SHL-1', newTotal: 350 }, created);
    expect(await controller.pendingCount()).toBe(1);
  });

  test('a network error is treated as offline: the change is queued', async () => {
    fetchFn.mockRejectedValue(new TypeError('Failed to fetch'));
    const created = await meta();

    const result = await controller.registerShelter({ details: DETAILS, meta: created });

    expect(result).toBeUndefined();
    expectQueued(await controller.getPending(), SubmissionAction.REGISTER_SHELTER, { details: DETAILS }, created);
  });

  test('a 5xx reply is not a rejection: the change is queued', async () => {
    fetchFn.mockResolvedValue(reply(503, { status: 503 }));
    const created = await meta();

    const result = await controller.updateShelterDetails('SHL-1', { details: DETAILS, meta: created });

    expect(result).toBeUndefined();
    expectQueued(
      await controller.getPending(),
      SubmissionAction.UPDATE_SHELTER_DETAILS,
      { shelterId: 'SHL-1', details: DETAILS },
      created,
    );
  });
});

describe('replayPending', () => {
  const OCCUPANCY = SubmissionAction.UPDATE_OCCUPANCY;

  test('with nothing queued it sends nothing and returns no results', async () => {
    expect(await controller.replayPending()).toEqual([]);
    expect(fetchFn).not.toHaveBeenCalled();
  });

  test('an ACCEPTED or DUPLICATE result removes the item and is returned', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 1 }, 'a', '2026-05-21T14:00:00+05:30');
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 2 }, 'b', '2026-05-21T14:01:00+05:30');
    fetchFn.mockResolvedValueOnce(reply(200, outcome(SyncOutcome.ACCEPTED, { submissionId: 'a' })));
    fetchFn.mockResolvedValueOnce(reply(200, outcome(SyncOutcome.DUPLICATE, { submissionId: 'b' })));

    const results = await controller.replayPending();

    expect(results.map((result) => [result.submissionId, result.outcome])).toEqual([
      ['a', SyncOutcome.ACCEPTED],
      ['b', SyncOutcome.DUPLICATE],
    ]);
    expect(await controller.getPending()).toEqual([]);
  });

  test('a REJECTED result marks the item REJECTED with the message, keeps it, and is returned', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 900 }, 'a', '2026-05-21T14:00:00+05:30');
    fetchFn.mockResolvedValue(
      reply(200, outcome(SyncOutcome.REJECTED, { submissionId: 'a', reason: RejectionReason.OVER_CAPACITY, message: 'Capacity exceeded' })),
    );

    const results = await controller.replayPending();

    expect(results[0].reason).toBe(RejectionReason.OVER_CAPACITY);
    const [stored] = await controller.getPending();
    expect(stored.status).toBe(PendingStatus.REJECTED);
    expect(stored.rejectionReason).toBe('Capacity exceeded');
    expect(await controller.pendingCount()).toBe(0);
  });

  test('a REJECTED result without a message keeps the reason as the rejection text', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: -1 }, 'a', '2026-05-21T14:00:00+05:30');
    fetchFn.mockResolvedValue(reply(200, outcome(SyncOutcome.REJECTED, { reason: RejectionReason.INVALID_VALUE })));

    await controller.replayPending();

    expect((await controller.getPending())[0].rejectionReason).toBe(RejectionReason.INVALID_VALUE);
  });

  test('a 4xx reply marks the item REJECTED with the problem detail', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-X', newTotal: 1 }, 'a', '2026-05-21T14:00:00+05:30');
    fetchFn.mockResolvedValue(reply(404, { detail: 'The requested item does not exist.' }));

    const results = await controller.replayPending();

    expect(results[0]).toMatchObject({ submissionId: 'a', outcome: SyncOutcome.REJECTED, reason: RejectionReason.VALIDATION_FAILED });
    const [stored] = await controller.getPending();
    expect(stored.status).toBe(PendingStatus.REJECTED);
    expect(stored.rejectionReason).toBe('The requested item does not exist.');
  });

  test('a 5xx reply or a network error leaves the item QUEUED with one more attempt and gives no result', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 1 }, 'a', '2026-05-21T14:00:00+05:30');
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 2 }, 'b', '2026-05-21T14:01:00+05:30');
    fetchFn.mockResolvedValueOnce(reply(500, {}));
    fetchFn.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    const results = await controller.replayPending();

    expect(results).toEqual([]);
    const items = await controller.getPending();
    expect(items.map((item) => [item.meta.submissionId, item.status, item.attempts])).toEqual([
      ['a', PendingStatus.QUEUED, 1],
      ['b', PendingStatus.QUEUED, 1],
    ]);
  });

  test('sends only QUEUED items, oldest first, rebuilding each argument from its payload', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 2 }, 'second', '2026-05-21T14:02:00+05:30');
    await queue(SubmissionAction.UPDATE_SHELTER_DETAILS, { shelterId: 'SHL-7', details: DETAILS }, 'third', '2026-05-21T14:03:00+05:30');
    await queue(SubmissionAction.REGISTER_SHELTER, { details: DETAILS }, 'first', '2026-05-21T14:01:00+05:30');
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 5 }, 'old-rejection', '2026-05-21T14:00:00+05:30', PendingStatus.REJECTED);
    fetchFn.mockResolvedValue(reply(200, outcome(SyncOutcome.ACCEPTED)));

    await controller.replayPending();

    expect(fetchFn.mock.calls.map(([url, options]) => [options.method, url])).toEqual([
      ['POST', '/api/shelters'],
      ['POST', '/api/shelters/SHL-1/occupancy'],
      ['PUT', '/api/shelters/SHL-7'],
    ]);
    const bodies = fetchFn.mock.calls.map(([, options]) => JSON.parse(options.body));
    expect(bodies[0].details).toEqual(DETAILS);
    expect(bodies[1].newTotal).toBe(2);
    expect(bodies[2].details).toEqual(DETAILS);
    expect((await controller.getPending()).map((item) => item.meta.submissionId)).toEqual(['old-rejection']);
  });

  test('recomputes pendingCount at send time (the QUEUED items other than this one) and keeps capturedAt', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 1 }, 'a', '2026-05-21T14:00:00+05:30');
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 2 }, 'b', '2026-05-21T14:01:00+05:30');
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 3 }, 'c', '2026-05-21T14:02:00+05:30');
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 9 }, 'rejected', '2026-05-21T13:00:00+05:30', PendingStatus.REJECTED);
    fetchFn.mockResolvedValue(reply(200, outcome(SyncOutcome.ACCEPTED)));

    await controller.replayPending();

    const metas = fetchFn.mock.calls.map(([, options]) => JSON.parse(options.body).meta);
    expect(metas.map((sent) => sent.pendingCount)).toEqual([2, 1, 0]);
    expect(metas.map((sent) => sent.capturedAt)).toEqual([
      '2026-05-21T14:00:00+05:30',
      '2026-05-21T14:01:00+05:30',
      '2026-05-21T14:02:00+05:30',
    ]);
    expect(metas.map((sent) => sent.submissionId)).toEqual(['a', 'b', 'c']);
    expect(metas.every((sent) => sent.clientId === outbox.clientId && sent.userId === USER_ID)).toBe(true);
  });

  test('a call during a run joins that run; a later call starts a new one', async () => {
    await queue(OCCUPANCY, { shelterId: 'SHL-1', newTotal: 1 }, 'a', '2026-05-21T14:00:00+05:30');
    let release;
    fetchFn.mockImplementationOnce(() => new Promise((resolve) => { release = () => resolve(reply(200, outcome(SyncOutcome.ACCEPTED))); }));

    const first = controller.replayPending();
    const second = controller.replayPending();
    await new Promise((resolve) => setTimeout(resolve, 20));
    release();
    const [firstResults, secondResults] = await Promise.all([first, second]);

    expect(second).toBe(first);
    expect(secondResults).toBe(firstResults);
    expect(fetchFn).toHaveBeenCalledTimes(1);
    expect(await controller.replayPending()).toEqual([]);
  });
});

describe('getPending, dismiss and pendingCount', () => {
  test('getPending lists QUEUED and REJECTED items; pendingCount counts only QUEUED', async () => {
    await queue(SubmissionAction.UPDATE_OCCUPANCY, { shelterId: 'SHL-1', newTotal: 1 }, 'q', '2026-05-21T14:00:00+05:30');
    await queue(SubmissionAction.UPDATE_OCCUPANCY, { shelterId: 'SHL-1', newTotal: 2 }, 'r', '2026-05-21T14:01:00+05:30', PendingStatus.REJECTED);

    expect((await controller.getPending()).map((item) => item.status)).toEqual([PendingStatus.QUEUED, PendingStatus.REJECTED]);
    expect(await controller.pendingCount()).toBe(1);
  });

  test('dismiss removes a REJECTED item', async () => {
    await queue(SubmissionAction.UPDATE_OCCUPANCY, { shelterId: 'SHL-1', newTotal: 2 }, 'r', '2026-05-21T14:01:00+05:30', PendingStatus.REJECTED);

    await controller.dismiss('r');

    expect(await controller.getPending()).toEqual([]);
  });

  test('dismiss of a QUEUED item is refused and the item stays', async () => {
    await queue(SubmissionAction.UPDATE_OCCUPANCY, { shelterId: 'SHL-1', newTotal: 1 }, 'q', '2026-05-21T14:00:00+05:30');

    await expect(controller.dismiss('q')).rejects.toThrow('Only a rejected item can be dismissed: q');

    expect(await controller.pendingCount()).toBe(1);
  });

  test('dismiss of an unknown item is refused', async () => {
    await expect(controller.dismiss('nope')).rejects.toThrow('Only a rejected item can be dismissed: nope');
  });
});

describe('the client id across controllers', () => {
  test('two controllers over the same database send the same client id', async () => {
    const indexedDB = new IDBFactory();
    const first = await OfflineOutbox.open({ indexedDB });
    const second = await OfflineOutbox.open({ indexedDB });
    const one = new OfflineSyncController({ outbox: first, fetchFn, clock: CLOCK, userId: USER_ID });
    const other = new OfflineSyncController({ outbox: second, fetchFn, clock: CLOCK, userId: USER_ID });

    expect((await one.createMeta()).clientId).toBe((await other.createMeta()).clientId);
  });
});
