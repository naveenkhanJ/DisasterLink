/**
 * ShowReview.tsx — `showReview(draft)`: the summary card with Edit and Submit Report (SC2 step 16,
 * A3 17a; WF2P-S04).
 *
 * Implements CH-27 (FD-12).
 */
import React from 'react';
import { Text, View } from 'react-native';
import type { ReportDraft } from '../report/ReportDraft';
import {
  Button,
  Card,
  Icon,
  LabelValue,
  PhotoTile,
  ScreenTitle,
  styles as ui,
} from '../components/ui';
import { Screen, screenStyles as s } from './Screen';
import {
  formatCoordinates,
  formatDateTime,
  hazardTypeIcon,
  hazardTypeLabel,
  locationSource,
} from './texts';

/**
 * Draws the review screen.
 *
 * @param props the draft, the handlers and whether a submission is in progress
 * @returns the screen
 */
export function ShowReview({
  draft,
  submitting,
  onEdit,
  onSubmit,
}: {
  draft: ReportDraft;
  submitting: boolean;
  onEdit: () => void;
  onSubmit: () => void;
}) {
  return (
    <Screen>
      <ScreenTitle title="Review your report" subtitle="Check the details before you send them." />
      <Card style={{ paddingVertical: 4 }}>
        <LabelValue label="Hazard type">
          <View style={s.rowCenter}>
            <Icon name={hazardTypeIcon(draft.hazardType)} size={18} />
            <Text style={ui.value}>{hazardTypeLabel(draft.hazardType)}</Text>
          </View>
        </LabelValue>
        <LabelValue label="Description">
          <Text style={[ui.value, { fontSize: 14, lineHeight: 20 }]}>{draft.description}</Text>
        </LabelValue>
        <LabelValue label="Photo">
          <View style={[s.rowCenter, { alignItems: 'flex-end' }]}>
            <PhotoTile uri={draft.photoUri} width={160} height={110} />
            <Text style={ui.muted}>1 of 1</Text>
          </View>
        </LabelValue>
        <LabelValue label="Location">
          {draft.location ? (
            <>
              <Text style={ui.value}>{formatCoordinates(draft.location)}</Text>
              <Text style={ui.muted}>{locationSource(draft.location, draft.locationFromGps)}</Text>
            </>
          ) : null}
        </LabelValue>
        <LabelValue label="Capture time" last>
          {formatDateTime(draft.capturedAt)}
        </LabelValue>
      </Card>
      <View style={s.rowCenter}>
        <Button label="Edit" kind="secondary" big style={{ width: 110 }} onPress={onEdit} />
        <Button
          label={submitting ? 'Sending…' : 'Submit Report'}
          big
          disabled={submitting}
          style={{ flex: 1 }}
          onPress={onSubmit}
        />
      </View>
    </Screen>
  );
}
