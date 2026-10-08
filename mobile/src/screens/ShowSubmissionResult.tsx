/**
 * ShowSubmissionResult.tsx — `showSubmissionResult(result)`: SUBMITTED with the report ID and
 * "Track report status" (SC2 steps 18 to 20, WF2P-S05), QUEUED as "Saved on this phone — Pending
 * synchronisation" with the capture time and no report ID (A2 18c, E6 19d, WF2P-S05a), INVALID as
 * "Report not accepted" with the errors and a line saying the app returns to the review screen by
 * itself (E6 19a to 19c, WF2P-S05b).
 *
 * Implements CH-10, CH-27 (FD-12).
 */
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { ReportDraft, SubmissionResult } from '../report/ReportDraft';
import { Button, Card, ErrorText, Icon, LabelValue, Tag, styles as ui } from '../components/ui';
import { colors, fonts, space } from '../theme';
import { Screen } from './Screen';
import { formatDateTime } from './texts';

/** The words and icon of each state. */
const STATES = {
  SUBMITTED: { headline: 'Report submitted', icon: 'check', tone: 'ok' },
  QUEUED: {
    headline: 'Saved on this phone — Pending synchronisation',
    icon: 'clock',
    tone: 'warn',
  },
  INVALID: { headline: 'Report not accepted', icon: 'cross', tone: 'error' },
} as const;

/**
 * Draws the confirmation screen.
 *
 * @param props the result, the draft it came from and the handlers
 * @returns the screen
 */
export function ShowSubmissionResult({
  result,
  draft,
  onTrackStatus,
  onBackToHome,
}: {
  result: SubmissionResult;
  draft: ReportDraft | undefined;
  onTrackStatus: (reportId: string) => void;
  onBackToHome: () => void;
}) {
  const state = STATES[result.state];
  const iconColor = state.tone === 'error' ? colors.errorText : colors[state.tone].text;
  return (
    <Screen>
      <View
        style={[
          styles.circle,
          state.tone === 'error'
            ? { borderColor: colors.errorBorder, backgroundColor: colors.selectedBg }
            : { borderColor: colors[state.tone].border, backgroundColor: colors[state.tone].bg },
        ]}
      >
        <Icon name={state.icon} size={48} color={iconColor} />
      </View>
      <Text style={styles.headline}>{state.headline}</Text>
      {result.state === 'SUBMITTED' ? (
        <>
          <Card style={{ paddingVertical: 4 }}>
            <LabelValue label="Report ID">{result.report?.reportId ?? ''}</LabelValue>
            <LabelValue label="Status" last>
              <Tag label="Pending verification" tone="warn" />
            </LabelValue>
          </Card>
          {result.report ? (
            <Button
              label="Track report status"
              big
              onPress={() => onTrackStatus(result.report?.reportId ?? '')}
            />
          ) : null}
        </>
      ) : null}
      {result.state === 'QUEUED' ? (
        <>
          <Card style={{ paddingVertical: 4 }}>
            {draft ? (
              <LabelValue label="Capture time">{formatDateTime(draft.capturedAt)}</LabelValue>
            ) : null}
            <LabelValue label="Status" last>
              <Tag label="Pending synchronisation" tone="warn" icon="clock" />
            </LabelValue>
          </Card>
          <Text style={[ui.muted, { fontSize: 15, lineHeight: 22 }]}>
            It will be sent automatically when the app is open and the connection returns
          </Text>
          <Button label="Back to home" big onPress={onBackToHome} />
        </>
      ) : null}
      {result.state === 'INVALID' ? (
        <>
          {result.errors.map((error) => (
            <ErrorText key={error}>{error}</ErrorText>
          ))}
          <Text style={ui.muted}>Returning to your report…</Text>
        </>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  circle: {
    alignSelf: 'center',
    marginTop: 32,
    width: 96,
    height: 96,
    borderRadius: 48,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headline: {
    fontFamily: fonts.ui,
    fontSize: 22,
    lineHeight: 30,
    fontWeight: '800',
    textAlign: 'center',
    color: colors.text,
    marginVertical: space.sm,
  },
});
