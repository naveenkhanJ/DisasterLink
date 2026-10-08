/**
 * OfflineReportStore.ts — the phone's store of report drafts that wait for synchronisation (model
 * dictionary: `OfflineReportStore`; SC2 A2 and E8), kept in AsyncStorage. Its operation names are
 * those of the browser's `OfflineOutbox` (RQ1).
 */
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Crypto from 'expo-crypto';
import { CLIENT_ID_STORAGE_KEY, DRAFTS_STORAGE_KEY } from '../constants';
import type { ReportDraft } from '../report/ReportDraft';

/** The calls the capture controller makes on the phone's draft store. */
export interface OfflineReportStore {
  /**
   * Gives the identifier of this phone, made once and kept (`clientId`).
   *
   * @returns the identifier
   */
  getClientId(): Promise<string>;
  /**
   * Saves a draft: inserts it, or replaces the draft with the same `draftId`.
   *
   * @param draft the draft
   */
  save(draft: ReportDraft): Promise<void>;
  /**
   * Lists the drafts that are shown as waiting: the QUEUED and the REJECTED ones; replay sends only
   * the QUEUED ones (RC-05).
   *
   * @returns the drafts, in the order they were first saved
   */
  findPending(): Promise<ReportDraft[]>;
  /**
   * Removes a draft.
   *
   * @param draftId the identifier of the draft; an unknown one is ignored
   */
  remove(draftId: string): Promise<void>;
  /**
   * Marks a draft REJECTED with the server's reason; it is never replayed.
   *
   * @param draftId the identifier of the draft; an unknown one is ignored
   * @param reason the reason
   */
  markRejected(draftId: string, reason: string): Promise<void>;
}

/**
 * Builds the store over AsyncStorage.
 *
 * @returns the store
 */
export function createOfflineReportStore(): OfflineReportStore {
  // Every operation of this store runs after the one before it, so a read-modify-write of the draft
  // list cannot interleave with another (a submit and a running sync could drop each other's draft).
  let tail: Promise<unknown> = Promise.resolve();

  function inSequence<T>(operation: () => Promise<T>): Promise<T> {
    const result = tail.then(operation);
    tail = result.catch(() => undefined);
    return result;
  }

  async function readAll(): Promise<ReportDraft[]> {
    const text = await AsyncStorage.getItem(DRAFTS_STORAGE_KEY);
    return text ? (JSON.parse(text) as ReportDraft[]) : [];
  }

  async function writeAll(drafts: ReportDraft[]): Promise<void> {
    await AsyncStorage.setItem(DRAFTS_STORAGE_KEY, JSON.stringify(drafts));
  }

  return {
    getClientId: () =>
      inSequence(async () => {
        const existing = await AsyncStorage.getItem(CLIENT_ID_STORAGE_KEY);
        if (existing) {
          return existing;
        }
        const made = Crypto.randomUUID();
        await AsyncStorage.setItem(CLIENT_ID_STORAGE_KEY, made);
        return made;
      }),
    save: (draft) =>
      inSequence(async () => {
        const drafts = await readAll();
        const index = drafts.findIndex((stored) => stored.draftId === draft.draftId);
        if (index < 0) {
          drafts.push(draft);
        } else {
          drafts[index] = draft;
        }
        await writeAll(drafts);
      }),
    findPending: () => inSequence(readAll),
    remove: (draftId) =>
      inSequence(async () => {
        await writeAll((await readAll()).filter((stored) => stored.draftId !== draftId));
      }),
    markRejected: (draftId, reason) =>
      inSequence(async () => {
        const drafts = await readAll();
        await writeAll(
          drafts.map((stored) =>
            stored.draftId === draftId
              ? { ...stored, status: 'REJECTED' as const, rejectionReason: reason }
              : stored,
          ),
        );
      }),
  };
}
