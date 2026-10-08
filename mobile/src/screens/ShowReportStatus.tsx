/**
 * ShowReportStatus.tsx — `showReportStatus(report)`: report ID, hazard type, capture time, status
 * (Verified, Rejected with the officer's reason, or Pending verification) and the decision time
 * (SC2 steps 31 and 32, A4 25h; WF2P-S06 and S06a). The labels stay English; the officer's reason
 * is shown as typed.
 *
 * Implements CH-25, CH-27 (FD-12).
 */
import React from 'react';
import { Text, View } from 'react-native';
import type { HazardReport } from '../api/reportApi';
import { Card, Icon, LabelValue, ScreenTitle, Tag, styles as ui } from '../components/ui';
import { Screen, screenStyles as s } from './Screen';
import {
  formatDateTime,
  hazardTypeIcon,
  hazardTypeLabel,
  REPORT_LOADS_ONLINE_TEXT,
  STATUS_TAGS,
} from './texts';

/**
 * Draws the report-status screen.
 *
 * @param props the report and the pull-to-refresh state and handler
 * @returns the screen
 */
export function ShowReportStatus({
  report,
  refreshing,
  onRefresh,
}: {
  report: HazardReport;
  refreshing: boolean;
  onRefresh: () => void;
}) {
  const tag = STATUS_TAGS[report.status];
  return (
    <Screen refreshing={refreshing} onRefresh={onRefresh}>
      <ScreenTitle title="Report status" />
      <Card style={{ paddingVertical: 4 }}>
        <LabelValue label="Report ID">{report.reportId}</LabelValue>
        <LabelValue label="Hazard type">
          <View style={s.rowCenter}>
            <Icon name={hazardTypeIcon(report.hazardType)} size={18} />
            <Text style={ui.value}>{hazardTypeLabel(report.hazardType)}</Text>
          </View>
        </LabelValue>
        <LabelValue label="Capture time">{formatDateTime(report.capturedAt)}</LabelValue>
        <LabelValue label="Status" last={!report.verificationTime && !report.rejectionReason}>
          <Tag label={tag.label} tone={tag.tone} icon={tag.icon} />
        </LabelValue>
        {report.verificationTime ? (
          <LabelValue label="Decision time" last={!report.rejectionReason}>
            {formatDateTime(report.verificationTime)}
          </LabelValue>
        ) : null}
        {report.rejectionReason ? (
          <LabelValue label="Reason" last>
            {report.rejectionReason}
          </LabelValue>
        ) : null}
      </Card>
    </Screen>
  );
}

/**
 * Draws the status screen when offline and no copy has the report: the title, the report ID and the
 * line that it loads when online (no banner and no Retry; standards section 10 item 17, deviation
 * D-UC2-6, P231). Pull-to-refresh stays available.
 *
 * @param props the report ID and the pull-to-refresh state and handler
 * @returns the screen
 */
export function ShowReportStatusOffline({
  reportId,
  refreshing,
  onRefresh,
}: {
  reportId: string;
  refreshing: boolean;
  onRefresh: () => void;
}) {
  return (
    <Screen refreshing={refreshing} onRefresh={onRefresh}>
      <ScreenTitle title="Report status" />
      <Card style={{ paddingVertical: 4 }}>
        <LabelValue label="Report ID" last>
          {reportId}
        </LabelValue>
      </Card>
      <Text style={ui.muted}>{REPORT_LOADS_ONLINE_TEXT}</Text>
    </Screen>
  );
}
