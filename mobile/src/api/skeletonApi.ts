/**
 * skeletonApi.ts — the clients of the skeleton's endpoints (implementation standards §12.1):
 * the active warnings and the push outbox. The UC2 report endpoints are the use case's own.
 */
import { ACTIVE_WARNINGS_PATH, PUSH_OUTBOX_PATH } from '../constants';
import type { HttpClient } from './httpClient';
import type { ActiveWarning, District, PushEntry } from './types';

/** The calls the app makes to the skeleton's endpoints. */
export interface SkeletonApi {
  /**
   * `GET /api/warnings/active?district=` — the warnings that are active for a district, each with
   * its hazard's type and place name.
   *
   * @param district the citizen's district
   * @returns the active warnings, possibly none
   */
  getActiveWarnings(district: District): Promise<ActiveWarning[]>;
  /**
   * `GET /api/push/{recipientId}/outbox` — the recipient's undelivered push entries, oldest first;
   * polling changes nothing.
   *
   * @param recipientId the citizen's identifier
   * @returns the undelivered entries
   */
  pollOutbox(recipientId: string): Promise<PushEntry[]>;
  /**
   * `POST /api/push/{recipientId}/outbox/ack` — marks entries the app has shown as delivered.
   *
   * @param recipientId the citizen's identifier
   * @param ids the identifiers of the entries that were shown
   */
  acknowledge(recipientId: string, ids: string[]): Promise<void>;
}

/** The path of a recipient's outbox. */
function outboxPath(recipientId: string): string {
  return PUSH_OUTBOX_PATH.replace('{recipientId}', encodeURIComponent(recipientId));
}

/**
 * Builds the skeleton's API over an HTTP client.
 *
 * @param client the HTTP client
 * @returns the API
 */
export function createSkeletonApi(client: HttpClient): SkeletonApi {
  return {
    getActiveWarnings: (district) =>
      client.getJson<ActiveWarning[]>(
        `${ACTIVE_WARNINGS_PATH}?district=${encodeURIComponent(district)}`,
      ),
    pollOutbox: (recipientId) => client.getJson<PushEntry[]>(outboxPath(recipientId)),
    acknowledge: (recipientId, ids) =>
      client.postJson<void>(`${outboxPath(recipientId)}/ack`, { ids }),
  };
}
