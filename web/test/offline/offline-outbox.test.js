/**
 * Tests of the browser OfflineOutbox: one IndexedDB database, one store keyed by the submission id,
 * a client id made once. A fresh fake IndexedDB factory is made for each test.
 *
 * @jest-environment node
 */
import { IDBFactory } from 'fake-indexeddb';
import { PendingStatus, SubmissionAction } from '../../../backend/src/main/resources/static/js/offline/constants.js';
import {
  OfflineOutbox,
  newPendingSubmission,
  randomUuid,
  withStatus,
} from '../../../backend/src/main/resources/static/js/offline/offline-outbox.js';

function meta(submissionId, capturedAt) {
  return {
    clientId: 'c-1',
    userId: 'DO-COL-01',
    submissionId,
    capturedAt,
    pendingCount: 0,
  };
}

function item(submissionId, capturedAt) {
  return newPendingSubmission(
    SubmissionAction.UPDATE_OCCUPANCY,
    { shelterId: 'SHL-1', newTotal: 350 },
    meta(submissionId, capturedAt),
  );
}

describe('newPendingSubmission and withStatus', () => {
  test('a new item is QUEUED with no attempts and the arguments serialised without meta', () => {
    const created = item('s-1', '2026-05-21T14:05:00+05:30');

    expect(created.status).toBe(PendingStatus.QUEUED);
    expect(created.attempts).toBe(0);
    expect(created.action).toBe(SubmissionAction.UPDATE_OCCUPANCY);
    expect(JSON.parse(created.payload)).toEqual({ shelterId: 'SHL-1', newTotal: 350 });
    expect(created.meta.submissionId).toBe('s-1');
    expect(created).not.toHaveProperty('rejectionReason');
  });

  test('withStatus returns a copy and leaves the item unchanged', () => {
    const original = item('s-1', '2026-05-21T14:05:00+05:30');

    const copy = withStatus(original, PendingStatus.REJECTED);

    expect(copy.status).toBe(PendingStatus.REJECTED);
    expect(original.status).toBe(PendingStatus.QUEUED);
    expect(copy).not.toBe(original);
    expect(copy.payload).toBe(original.payload);
  });
});

describe('the client id', () => {
  test('is a UUID made once and the same in every instance over the same database', async () => {
    const indexedDB = new IDBFactory();

    const first = await OfflineOutbox.open({ indexedDB });
    const second = await OfflineOutbox.open({ indexedDB });

    expect(first.clientId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
    expect(second.clientId).toBe(first.clientId);
  });

  test('differs between two browsers (two databases)', async () => {
    const one = await OfflineOutbox.open({ indexedDB: new IDBFactory() });
    const other = await OfflineOutbox.open({ indexedDB: new IDBFactory() });

    expect(one.clientId).not.toBe(other.clientId);
  });

  test('comes from the injected UUID source the first time only', async () => {
    const indexedDB = new IDBFactory();
    const uuids = ['first-id', 'second-id'];

    const first = await OfflineOutbox.open({ indexedDB, uuid: () => uuids.shift() });
    const again = await OfflineOutbox.open({ indexedDB, uuid: () => uuids.shift() });

    expect(first.clientId).toBe('first-id');
    expect(again.clientId).toBe('first-id');
    expect(uuids).toEqual(['second-id']);
  });

  test('a database name keeps two outboxes of one browser apart', async () => {
    const indexedDB = new IDBFactory();

    const one = await OfflineOutbox.open({ indexedDB, name: 'one' });
    const other = await OfflineOutbox.open({ indexedDB, name: 'other' });

    expect(one.clientId).not.toBe(other.clientId);
  });

  test('the defaults are the browser global factory and crypto.randomUUID', async () => {
    const saved = globalThis.indexedDB;
    globalThis.indexedDB = new IDBFactory();
    try {
      const outbox = await OfflineOutbox.open();

      expect(outbox.clientId).toHaveLength(randomUuid().length);
    } finally {
      globalThis.indexedDB = saved;
    }
  });

  test('opening fails when the database cannot be opened', async () => {
    const failing = {
      open: () => {
        const request = { error: new Error('blocked') };
        setTimeout(() => request.onerror());
        return request;
      },
    };

    await expect(OfflineOutbox.open({ indexedDB: failing })).rejects.toThrow('blocked');
  });
});

describe('the items', () => {
  let outbox;

  beforeEach(async () => {
    outbox = await OfflineOutbox.open({ indexedDB: new IDBFactory() });
  });

  test('save then findPending returns QUEUED and REJECTED items in order of capture time', async () => {
    await outbox.save(item('late', '2026-05-21T14:09:00+05:30'));
    await outbox.save(withStatus(item('rejected', '2026-05-21T14:00:00+05:30'), PendingStatus.REJECTED));
    await outbox.save(item('early', '2026-05-21T08:31:00Z')); // 14:01 in Colombo

    const found = await outbox.findPending();

    expect(found.map((entry) => entry.meta.submissionId)).toEqual(['rejected', 'early', 'late']);
    expect(found.map((entry) => entry.status)).toEqual([
      PendingStatus.REJECTED,
      PendingStatus.QUEUED,
      PendingStatus.QUEUED,
    ]);
  });

  test('findPending of an empty store is empty', async () => {
    expect(await outbox.findPending()).toEqual([]);
  });

  test('saving an item with the same submission id replaces it', async () => {
    await outbox.save(item('s-1', '2026-05-21T14:05:00+05:30'));
    await outbox.save({ ...item('s-1', '2026-05-21T14:05:00+05:30'), attempts: 3 });

    const found = await outbox.findPending();

    expect(found).toHaveLength(1);
    expect(found[0].attempts).toBe(3);
  });

  test('remove deletes the item; removing an unknown id changes nothing', async () => {
    await outbox.save(item('s-1', '2026-05-21T14:05:00+05:30'));
    await outbox.save(item('s-2', '2026-05-21T14:06:00+05:30'));

    await outbox.remove('s-1');
    await outbox.remove('unknown');

    expect((await outbox.findPending()).map((entry) => entry.meta.submissionId)).toEqual(['s-2']);
  });

  test('markRejected sets the status and the reason and keeps the rest of the item', async () => {
    const original = item('s-1', '2026-05-21T14:05:00+05:30');
    await outbox.save(original);

    await outbox.markRejected('s-1', 'Capacity exceeded');

    const [stored] = await outbox.findPending();
    expect(stored.status).toBe(PendingStatus.REJECTED);
    expect(stored.rejectionReason).toBe('Capacity exceeded');
    expect(stored.payload).toBe(original.payload);
    expect(stored.meta).toEqual(original.meta);
  });

  test('markRejected of an unknown id changes nothing', async () => {
    await outbox.save(item('s-1', '2026-05-21T14:05:00+05:30'));

    await outbox.markRejected('unknown', 'reason');

    const [stored] = await outbox.findPending();
    expect(stored.status).toBe(PendingStatus.QUEUED);
  });

  test('a transaction error rejects the call', async () => {
    const broken = new OfflineOutbox({ transaction: () => failingTransaction('onerror') }, 'id');

    await expect(broken.save(item('s-1', '2026-05-21T14:05:00+05:30'))).rejects.toThrow('boom');
  });

  test('an aborted transaction rejects the call', async () => {
    const broken = new OfflineOutbox({ transaction: () => failingTransaction('onabort') }, 'id');

    await expect(broken.remove('s-1')).rejects.toThrow('boom');
  });
});

/** A transaction that fails with the given event as soon as its handlers are set. */
function failingTransaction(event) {
  const transaction = {
    error: new Error('boom'),
    objectStore: () => ({ put: () => ({}), delete: () => ({}) }),
  };
  setTimeout(() => transaction[event]());
  return transaction;
}
