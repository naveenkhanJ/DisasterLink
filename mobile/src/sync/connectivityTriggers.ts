/**
 * connectivityTriggers.ts — raises "connectivity restored" for the replay of queued drafts
 * (implementation standards §10; PR11, FX-25, P206). Four triggers: at start-up when the phone is
 * online; when the network changes from offline to online; when the app returns to the foreground
 * (`AppState` becomes `active`) while online; and, while the app is active, every
 * `QUEUED_RETRY_INTERVAL_MS` when the phone is online and a draft is QUEUED, so a draft left QUEUED
 * by a 5xx or an unreachable server is sent without the citizen leaving the app (deviation D-UC2-1).
 * Implementation-only module; serves CH-10 and CH-27.
 */
import * as Network from 'expo-network';
import { AppState } from 'react-native';
import { QUEUED_RETRY_INTERVAL_MS } from '../constants';

/**
 * Tells whether a network state counts as online: connected and, when known, the internet is
 * reachable.
 *
 * @param state the state `expo-network` reports
 * @returns whether the phone is online
 */
export function isOnlineState(state: Network.NetworkState): boolean {
  return state.isConnected !== false && state.isInternetReachable !== false;
}

/**
 * Asks the phone whether it is online (`expo-network`'s `getNetworkStateAsync`).
 *
 * @returns whether it is online; a state that cannot be read counts as offline
 */
export async function isOnline(): Promise<boolean> {
  try {
    return isOnlineState(await Network.getNetworkStateAsync());
  } catch {
    return false;
  }
}

/**
 * Starts the four triggers.
 *
 * @param onConnectivityRestored called when a replay should run (start-up online, offline to
 *   online, foreground while online, the timer while a draft is QUEUED)
 * @param hasQueuedDrafts tells whether at least one draft is QUEUED; one that throws counts as no
 * @param intervalMs the time between two timer ticks; defaults to `QUEUED_RETRY_INTERVAL_MS`
 * @returns a function that stops the triggers
 */
export function startConnectivityTriggers(
  onConnectivityRestored: () => void,
  hasQueuedDrafts: () => Promise<boolean>,
  intervalMs: number = QUEUED_RETRY_INTERVAL_MS,
): () => void {
  let wasOnline = false;
  let stopped = false;
  let timer: ReturnType<typeof setInterval> | undefined;

  async function retryQueued(): Promise<void> {
    if (!(await isOnline())) {
      return;
    }
    let queued = false;
    try {
      queued = await hasQueuedDrafts();
    } catch (error) {
      console.warn('[connectivityTriggers] the queue could not be read:', error);
    }
    if (queued && !stopped) {
      onConnectivityRestored();
    }
  }

  function startTimer(): void {
    if (!timer && !stopped) {
      timer = setInterval(() => void retryQueued(), intervalMs);
    }
  }

  function clearTimer(): void {
    if (timer) {
      clearInterval(timer);
      timer = undefined;
    }
  }

  function checkAndRaise(): void {
    void isOnline().then((online) => {
      wasOnline = online;
      if (online && !stopped) {
        onConnectivityRestored();
      }
    });
  }

  checkAndRaise();
  if (AppState.currentState === 'active') {
    startTimer();
  }

  const networkSubscription = Network.addNetworkStateListener((state) => {
    const online = isOnlineState(state);
    if (online && !wasOnline && !stopped) {
      onConnectivityRestored();
    }
    wasOnline = online;
  });

  const appStateSubscription = AppState.addEventListener('change', (next) => {
    if (next === 'active') {
      startTimer();
      checkAndRaise();
    } else {
      clearTimer();
    }
  });

  return () => {
    stopped = true;
    clearTimer();
    networkSubscription.remove();
    appStateSubscription.remove();
  };
}
