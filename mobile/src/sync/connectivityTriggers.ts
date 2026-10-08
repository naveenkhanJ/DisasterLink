/**
 * connectivityTriggers.ts — raises "connectivity restored" for the replay of queued drafts
 * (implementation standards §10; PR11, FX-25): at start-up when the phone is online, when the
 * network changes from offline to online, and when the app returns to the foreground
 * (`AppState` becomes `active`) while online, so a draft left QUEUED by a 5xx is tried again.
 * Implementation-only module.
 */
import * as Network from 'expo-network';
import { AppState } from 'react-native';

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
 * Starts the three triggers.
 *
 * @param onConnectivityRestored called when a replay should run (start-up online, offline to
 *   online, foreground while online)
 * @returns a function that stops the triggers
 */
export function startConnectivityTriggers(onConnectivityRestored: () => void): () => void {
  let wasOnline = false;
  let stopped = false;

  function checkAndRaise(): void {
    void isOnline().then((online) => {
      wasOnline = online;
      if (online && !stopped) {
        onConnectivityRestored();
      }
    });
  }

  checkAndRaise();

  const networkSubscription = Network.addNetworkStateListener((state) => {
    const online = isOnlineState(state);
    if (online && !wasOnline && !stopped) {
      onConnectivityRestored();
    }
    wasOnline = online;
  });

  const appStateSubscription = AppState.addEventListener('change', (next) => {
    if (next === 'active') {
      checkAndRaise();
    }
  });

  return () => {
    stopped = true;
    networkSubscription.remove();
    appStateSubscription.remove();
  };
}
