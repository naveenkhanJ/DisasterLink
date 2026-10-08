import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Crypto from 'expo-crypto';
import type { HomeView, ReportDraft } from '../../report/ReportDraft';
import { createHomeCache } from '../HomeCache';
import { createOfflineReportStore } from '../OfflineReportStore';

jest.mock('@react-native-async-storage/async-storage', () =>
  jest.requireActual('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);
jest.mock('expo-crypto', () => ({ randomUUID: jest.fn(() => 'uuid-1') }));

function draft(id: string, extra: Partial<ReportDraft> = {}): ReportDraft {
  return { draftId: id, locationFromGps: false, capturedAt: '2026-05-21T07:40:00.000Z', ...extra };
}

beforeEach(async () => {
  await AsyncStorage.clear();
  jest.clearAllMocks();
});

describe('OfflineReportStore', () => {
  it('SC2 A2: saves a draft and finds it again', async () => {
    const store = createOfflineReportStore();

    await store.save(draft('d-1', { status: 'QUEUED' }));
    await store.save(draft('d-2', { status: 'QUEUED' }));

    expect((await store.findPending()).map((d) => d.draftId)).toEqual(['d-1', 'd-2']);
  });

  it('SC2 E8: saving a draft with the same draftId replaces it and keeps its place', async () => {
    const store = createOfflineReportStore();
    await store.save(draft('d-1', { description: 'first', status: 'QUEUED' }));
    await store.save(draft('d-2', { status: 'QUEUED' }));

    await store.save(draft('d-1', { description: 'second', status: 'QUEUED', attempts: 1 }));

    const stored = await store.findPending();
    expect(stored.map((d) => d.draftId)).toEqual(['d-1', 'd-2']);
    expect(stored[0]).toMatchObject({ description: 'second', attempts: 1 });
  });

  it('SC2 E8: removes a draft and ignores an unknown one', async () => {
    const store = createOfflineReportStore();
    await store.save(draft('d-1'));

    await store.remove('unknown');
    await store.remove('d-1');

    expect(await store.findPending()).toEqual([]);
  });

  it('SC2 E8: marks a draft REJECTED with the reason and ignores an unknown one', async () => {
    const store = createOfflineReportStore();
    await store.save(draft('d-1', { status: 'QUEUED' }));
    await store.save(draft('d-2', { status: 'QUEUED' }));

    await store.markRejected('d-1', 'photo: not a readable JPEG or PNG image');
    await store.markRejected('unknown', 'x');

    const [first, second] = await store.findPending();
    expect(first).toMatchObject({
      status: 'REJECTED',
      rejectionReason: 'photo: not a readable JPEG or PNG image',
    });
    expect(second.status).toBe('QUEUED');
  });

  it('two saves started without awaiting in between both end up stored', async () => {
    const store = createOfflineReportStore();

    await Promise.all([
      store.save(draft('d-1', { status: 'QUEUED' })),
      store.save(draft('d-2', { status: 'QUEUED' })),
      store.save(draft('d-3', { status: 'QUEUED' })),
    ]);

    expect((await store.findPending()).map((d) => d.draftId)).toEqual(['d-1', 'd-2', 'd-3']);
  });

  it('a markRejected interleaved with a save keeps both changes, and so does a remove', async () => {
    const store = createOfflineReportStore();
    await store.save(draft('d-1', { status: 'QUEUED' }));
    await store.save(draft('d-2', { status: 'QUEUED' }));

    await Promise.all([
      store.markRejected('d-1', 'photo: …'),
      store.save(draft('d-3', { status: 'QUEUED' })),
      store.remove('d-2'),
    ]);

    const stored = await store.findPending();
    expect(stored.map((d) => [d.draftId, d.status])).toEqual([
      ['d-1', 'REJECTED'],
      ['d-3', 'QUEUED'],
    ]);
  });

  it('one failing operation does not stop the ones after it', async () => {
    const store = createOfflineReportStore();
    jest.spyOn(AsyncStorage, 'setItem').mockRejectedValueOnce(new Error('disk full'));

    const failing = store.save(draft('d-1'));
    const next = store.save(draft('d-2'));

    await expect(failing).rejects.toThrow('disk full');
    await next;
    expect((await store.findPending()).map((d) => d.draftId)).toEqual(['d-2']);
  });

  it('SC2 step 18: the client identifier is made once and kept', async () => {
    const store = createOfflineReportStore();

    const first = await store.getClientId();
    const second = await createOfflineReportStore().getClientId();

    expect(first).toBe('uuid-1');
    expect(second).toBe('uuid-1');
    expect(Crypto.randomUUID).toHaveBeenCalledTimes(1);
  });
});

describe('HomeCache', () => {
  const home: HomeView = {
    warnings: [],
    myReports: [],
    pendingDrafts: [],
    language: 'ENGLISH',
    fromCache: false,
  };

  it('SC2 A8: keeps the last home and gives it back', async () => {
    const cache = createHomeCache('ENGLISH');
    expect(await cache.loadHome()).toBeUndefined();

    await cache.saveHome(home);

    expect(await cache.loadHome()).toEqual(home);
  });

  it('PR13: the language starts as the configured one and changes with changeLanguage', async () => {
    const cache = createHomeCache('SINHALA');
    expect(await cache.getLanguage()).toBe('SINHALA');

    await cache.changeLanguage('TAMIL');

    expect(await cache.getLanguage()).toBe('TAMIL');
    expect(await createHomeCache('SINHALA').getLanguage()).toBe('TAMIL');
  });
});
