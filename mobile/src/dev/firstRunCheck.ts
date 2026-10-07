/**
 * firstRunCheck.ts — REMOVE AFTER THE FIRST-RUN CHECK (S6). Development only.
 *
 * It settles the two points of implementation standards §10 that are believed but not verified: a
 * plain-HTTP `fetch` from Expo Go to the PC's LAN address, and a local notification in Expo Go on
 * iOS. It polls the demo citizen's outbox and shows the outcome as a local notification.
 * `App.tsx` calls it only when `__DEV__` is true and `EXPO_PUBLIC_FIRST_RUN_CHECK` is `1`.
 */
import { FIRST_RUN_TITLE } from '../constants';
import type { SkeletonApi } from '../api/skeletonApi';

/** Shows a local alert; `presentLocalAlert` in the app, a spy in tests. */
export type AlertFunction = (title: string, body: string) => Promise<void>;

/**
 * Polls the outbox and shows "Server reachable: n push entries" or "Server NOT reachable: error".
 *
 * @param api the skeleton's API
 * @param citizenId the demo citizen's identifier
 * @param alert shows a local alert
 */
export async function runFirstRunCheck(
  api: SkeletonApi,
  citizenId: string,
  alert: AlertFunction,
): Promise<void> {
  try {
    const entries = await api.pollOutbox(citizenId);
    await alert(FIRST_RUN_TITLE, `Server reachable: ${entries.length} push entries`);
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    await alert(FIRST_RUN_TITLE, `Server NOT reachable: ${reason}`);
  }
}
