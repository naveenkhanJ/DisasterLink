/**
 * pushPoller.ts — polls the push outbox and hands the entries to the app (implementation standards
 * §10 and §12.3; FX-13, R4G-13). WARNING_ALERT entries with the same `warningId` and
 * `warningLevel` within one batch become one alert; a REPORT_DECISION entry becomes a decision
 * notice; the entries handed over are acknowledged. The UI's `showAlert` and `showReportDecision`
 * are UC2-6's. Implementation-only module.
 */
import { AppState } from 'react-native';
import type { ReportDecisionNotice } from '../api/reportApi';
import type { SkeletonApi } from '../api/skeletonApi';
import type { PushEntry, WarningLevel } from '../api/types';
import { PUSH_POLL_INTERVAL_MS } from '../constants';
import { configureNotifications } from './notifications';

/** What the poller needs. */
export interface PushPollingOptions {
  /** The citizen whose outbox is polled. */
  recipientId: string;
  /** The skeleton's push calls. */
  api: Pick<SkeletonApi, 'pollOutbox' | 'acknowledge'>;
  /** Called once per warning and level in a batch (SC1 step 15; `receiveAlert`). */
  onAlert: (warningId: string, warningLevel: WarningLevel, text: string) => void;
  /** Called for each report decision (SC2 step 30; `receiveReportDecision`). */
  onDecision: (notice: ReportDecisionNotice) => void;
  /**
   * Awaited before the first poll: the permission must be answered before an alert is shown (§10).
   * Defaults to `configureNotifications`.
   */
  prepare?: () => Promise<unknown>;
  /** The time between two polls, in milliseconds. Defaults to `PUSH_POLL_INTERVAL_MS`. */
  intervalMs?: number;
}

/** Hands one polled batch over: merged alerts, then decisions; returns the ids handed over. */
function handOver(
  entries: PushEntry[],
  onAlert: PushPollingOptions['onAlert'],
  onDecision: PushPollingOptions['onDecision'],
): string[] {
  const alerts = new Map<string, PushEntry>();
  const ids: string[] = [];
  for (const entry of entries) {
    if (entry.kind === 'WARNING_ALERT' && entry.warningId && entry.warningLevel) {
      const key = `${entry.warningId}|${entry.warningLevel}`;
      if (!alerts.has(key)) {
        alerts.set(key, entry);
      }
      ids.push(entry.id);
    } else if (entry.kind === 'REPORT_DECISION' && entry.reportId && entry.status) {
      onDecision({
        reportId: entry.reportId,
        status: entry.status,
        reason: entry.reason,
        decidedAt: entry.decidedAt ?? entry.createdAt,
      });
      ids.push(entry.id);
    }
  }
  for (const entry of alerts.values()) {
    onAlert(entry.warningId as string, entry.warningLevel as WarningLevel, entry.text);
  }
  return ids;
}

/**
 * Starts polling once the notification set-up is done.
 *
 * @param options the recipient, the API and the callbacks
 * @returns a function that stops the polling
 */
export function startPushPolling(options: PushPollingOptions): () => void {
  const interval = options.intervalMs ?? PUSH_POLL_INTERVAL_MS;
  let timer: ReturnType<typeof setInterval> | undefined;
  let stopped = false;
  let failing = false;
  let polling = false;

  async function tick(): Promise<void> {
    // A poll can take up to the request timeout, longer than the interval: while one is in flight
    // (acknowledgement included) a tick is skipped, so the same entries are never handed over twice.
    if (stopped || polling || AppState.currentState !== 'active') {
      return;
    }
    polling = true;
    try {
      const entries = await options.api.pollOutbox(options.recipientId);
      failing = false;
      const ids = handOver(entries, options.onAlert, options.onDecision);
      if (ids.length > 0) {
        await options.api.acknowledge(options.recipientId, ids);
      }
    } catch (error) {
      if (!failing) {
        console.warn('[pushPoller] a poll failed, trying again at the next tick:', error);
      }
      failing = true;
    } finally {
      polling = false;
    }
  }

  void (options.prepare ?? configureNotifications)().then(() => {
    if (!stopped) {
      void tick();
      timer = setInterval(() => void tick(), interval);
    }
  });

  return () => {
    stopped = true;
    if (timer) {
      clearInterval(timer);
    }
  };
}
