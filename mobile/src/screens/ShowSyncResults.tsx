/**
 * ShowSyncResults.tsx — `showSyncResults(results)`: the result banner over the home screen after a
 * replay of the queued drafts: "Report sent — RPT-…, Pending verification" (A2 18f) or "Report not
 * accepted: <reason>" (E8 18k); WF2P-S01b and S01c.
 *
 * Implements CH-10, CH-27 (FD-12).
 */
import React from 'react';
import { Text } from 'react-native';
import type { SyncResult } from '../api/reportApi';
import { Banner } from '../components/ui';
import { colors, fonts } from '../theme';
import { syncResultText } from './texts';

/**
 * Draws the results of a synchronisation as one banner.
 *
 * @param props the results and the handler of the close button
 * @returns the banner
 */
export function ShowSyncResults({
  results,
  onClose,
}: {
  results: SyncResult[];
  onClose: () => void;
}) {
  const rejected = results.some((result) => result.outcome === 'REJECTED');
  return (
    <Banner kind={rejected ? 'error' : 'ok'} icon={rejected ? 'error' : 'check'} onClose={onClose}>
      {results.map((result) => (
        <Text
          key={result.submissionId}
          style={{
            fontFamily: fonts.ui,
            fontSize: 14,
            lineHeight: 20,
            fontWeight: '700',
            color: colors.text,
          }}
        >
          {syncResultText(result)}
        </Text>
      ))}
    </Banner>
  );
}
