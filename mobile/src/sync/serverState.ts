/**
 * serverState.ts — the holder of "the connection strip says Server not reachable"
 * (implementation-only; standards §10 item 11, P231).
 *
 * While the strip says "Server not reachable" the app's own calls count as offline: the capture
 * controller's `isOnline` reads this holder together with the phone's network, so the home reads
 * the saved copy, Submit queues and the synchronisation sends nothing, all with no request. The
 * holder follows the strip's state, set by the hook before it reloads, and not the last poll result,
 * so a poll that failed while there was no internet does not hold the calls back after the internet
 * returns.
 *
 * Serves CH-10 and CH-27.
 */

/** What the capture controller and the hook share about the server. */
export interface ServerState {
  /**
   * Tells whether the strip says "Server not reachable".
   *
   * @returns whether the server counts as down
   */
  isDown(): boolean;
  /**
   * Records the strip's state; the hook calls it on every change of the connection state.
   *
   * @param down whether the strip now says "Server not reachable"
   */
  setDown(down: boolean): void;
}

/**
 * Creates the holder.
 *
 * @returns a holder that starts with the server not down
 */
export function createServerState(): ServerState {
  let down = false;
  return {
    isDown: () => down,
    setDown: (next) => {
      down = next;
    },
  };
}
