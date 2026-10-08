/**
 * ShowHome.tsx — `showHome(view)` with `showLocation` for the current location: the connectivity
 * row, the language selector (disabled while the home comes from the saved copy, N13), the active
 * warnings in the chosen language, "Report Hazard", the current location, "My reports" and "My
 * reports — waiting on this phone" (SC2 steps 1 to 4, A8 2a to 2d; WF2P-S01, S01a to S01d).
 *
 * Implements CH-25, CH-27 (FD-12).
 */
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { GeoPoint, HazardReport } from '../api/reportApi';
import type { ActiveWarning, Language } from '../api/types';
import type { LocationAccessState } from '../location/locationAccess';
import type { HomeView, ReportDraft } from '../report/ReportDraft';
import { Banner, Button, Card, Icon, Tag, styles as ui } from '../components/ui';
import { colors, fonts, radius, space } from '../theme';
import { FONT_FOR, screenStyles as s } from './Screen';
import {
  districtLabel,
  formatCoordinates,
  formatDateTime,
  hazardTypeLabel,
  LANGUAGE_OPTIONS,
  LEVEL_LABELS,
  LOCATION_ALLOW_TEXT,
  LOCATION_SERVICES_OFF_TEXT,
  LEVEL_TONES,
  localised,
  rejectionText,
  STATUS_TAGS,
} from './texts';

/** What the home screen needs. */
export interface ShowHomeProps {
  view: HomeView;
  currentLocation: GeoPoint | undefined;
  locationAccess: LocationAccessState | undefined;
  draftDiscarded: boolean;
  onSelectLanguage: (language: Language) => void;
  onReportHazard: () => void;
  onOpenReport: (reportId: string) => void;
  onOpenDraft: (draftId: string) => void;
  onDismissDiscarded: () => void;
}

/** The three-way selector; disabled while the home is the saved copy (language changes online only). */
function LanguageSelector({
  selected,
  disabled,
  onSelect,
}: {
  selected: Language;
  disabled: boolean;
  onSelect: (language: Language) => void;
}) {
  return (
    <View>
      <Text style={ui.label}>Warning language</Text>
      <View style={styles.segment}>
        {LANGUAGE_OPTIONS.map((option) => {
          const on = option.value === selected;
          return (
            <Pressable
              key={option.value}
              accessibilityRole="radio"
              accessibilityState={{ selected: on, disabled }}
              disabled={disabled}
              onPress={() => onSelect(option.value)}
              style={[
                styles.segmentOption,
                on && styles.segmentOn,
                disabled && styles.segmentDisabled,
              ]}
            >
              {on ? <Icon name="check" size={14} color={colors.selectedText} /> : null}
              <Text
                style={[
                  styles.segmentText,
                  on && { color: colors.selectedText, fontWeight: '800' },
                ]}
              >
                {option.label}
              </Text>
            </Pressable>
          );
        })}
      </View>
      {disabled ? (
        <Text style={[ui.muted, { marginTop: 4 }]}>
          Language can be changed when you are online
        </Text>
      ) : null}
    </View>
  );
}

function WarningBlock({ warning, language }: { warning: ActiveWarning; language: Language }) {
  const font = FONT_FOR[language];
  const districts = warning.targetDistricts.map(districtLabel).join(', ');
  return (
    <View style={{ marginTop: space.sm }}>
      <Tag label={LEVEL_LABELS[warning.warningLevel]} tone={LEVEL_TONES[warning.warningLevel]} />
      <Text style={styles.hazard}>
        {`${hazardTypeLabel(warning.hazardType)} — ${warning.locationName}`}
      </Text>
      <Text
        style={ui.muted}
      >{`${districts} · Valid until ${formatDateTime(warning.expiryTime)}`}</Text>
      <Text style={[s.warningText, { fontFamily: font }]}>
        {localised(warning.message, language)}
      </Text>
      <Text style={[s.warningText, { fontFamily: font }]}>
        {localised(warning.instructions, language)}
      </Text>
    </View>
  );
}

function ReportRow({ report, onOpen }: { report: HazardReport; onOpen: (id: string) => void }) {
  const tag = STATUS_TAGS[report.status];
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Open report ${report.reportId}`}
      onPress={() => onOpen(report.reportId)}
      style={styles.row}
    >
      <View style={{ flex: 1, gap: 2 }}>
        <Text style={s.strong}>{`${report.reportId} · ${hazardTypeLabel(report.hazardType)}`}</Text>
        <Text style={ui.muted}>{`Captured ${formatDateTime(report.capturedAt)}`}</Text>
        <Tag label={tag.label} tone={tag.tone} icon={tag.icon} />
      </View>
      <Icon name="chevron-right" size={20} color={colors.muted} />
    </Pressable>
  );
}

function PendingRow({ draft, onOpen }: { draft: ReportDraft; onOpen: (id: string) => void }) {
  const rejected = draft.status === 'REJECTED';
  return (
    <View style={styles.row}>
      <View style={{ flex: 1, gap: 2 }}>
        <Text style={s.strong}>{hazardTypeLabel(draft.hazardType)}</Text>
        <Text style={ui.muted}>{`Captured ${formatDateTime(draft.capturedAt)}`}</Text>
        {rejected ? (
          <>
            <Tag label="Rejected" icon="cross" />
            <Text style={[ui.muted, { fontWeight: '700' }]}>
              {rejectionText(draft.rejectionReason)}
            </Text>
          </>
        ) : (
          <Tag label="Pending synchronisation" tone="warn" icon="clock" />
        )}
      </View>
      {rejected ? (
        <Button label="Open" kind="secondary" onPress={() => onOpen(draft.draftId)} />
      ) : null}
    </View>
  );
}

/** Why the current location is not shown: the S03g sentence when the cause is known. */
function unavailableText(access: LocationAccessState | undefined): string {
  if (access === 'servicesOff') {
    return LOCATION_SERVICES_OFF_TEXT;
  }
  return access === 'askable' || access === 'blocked' ? LOCATION_ALLOW_TEXT : 'not available';
}

/** The current-location card (`showLocation`): coordinates and accuracy as text, no map. */
function CurrentLocation({
  location,
  access,
}: {
  location: GeoPoint | undefined;
  access: LocationAccessState | undefined;
}) {
  return (
    <Card style={s.rowCenter}>
      <Icon name="pin" size={20} />
      <View>
        <Text style={s.strong}>
          {location
            ? `Current location · ${formatCoordinates(location)}`
            : `Current location · ${unavailableText(access)}`}
        </Text>
        {location ? (
          <Text style={ui.muted}>
            {location.accuracyMetres == null
              ? 'GPS'
              : `GPS · accuracy ±${Math.round(location.accuracyMetres)} m`}
          </Text>
        ) : null}
      </View>
    </Card>
  );
}

/**
 * Draws the home screen.
 *
 * @param props the view and the handlers
 * @returns the screen content
 */
export function ShowHome(props: ShowHomeProps) {
  const { view } = props;
  return (
    <>
      {props.draftDiscarded ? (
        <Banner kind="ok" icon="check" onClose={props.onDismissDiscarded}>
          <Text style={styles.bannerText}>Draft discarded — nothing was sent</Text>
        </Banner>
      ) : null}
      {view.fromCache ? <Text style={ui.muted}>Showing the copy saved on this phone</Text> : null}
      <LanguageSelector
        selected={view.language}
        disabled={view.fromCache}
        onSelect={props.onSelectLanguage}
      />
      <Card>
        <View style={s.rowCenter}>
          <Icon name="warning-triangle" size={20} />
          <Text style={s.cardTitle}>Active warnings</Text>
        </View>
        {view.warnings.length === 0 ? (
          <Text style={[ui.muted, { fontSize: 15, marginTop: 6 }]}>No active warnings</Text>
        ) : (
          view.warnings.map((warning) => (
            <WarningBlock key={warning.warningId} warning={warning} language={view.language} />
          ))
        )}
      </Card>
      <Button label="Report Hazard" icon="plus" big onPress={props.onReportHazard} />
      <CurrentLocation location={props.currentLocation} access={props.locationAccess} />
      <View style={{ gap: 6 }}>
        <Text style={s.section}>My reports</Text>
        {view.myReports.length === 0 ? (
          <Text style={[ui.muted, { fontSize: 15 }]}>No reports yet</Text>
        ) : (
          view.myReports.map((report) => (
            <ReportRow key={report.reportId} report={report} onOpen={props.onOpenReport} />
          ))
        )}
      </View>
      <View style={{ gap: 6 }}>
        <Text style={s.section}>My reports — waiting on this phone</Text>
        {view.pendingDrafts.length === 0 ? (
          <Text style={[ui.muted, { fontSize: 15 }]}>Nothing waiting</Text>
        ) : (
          view.pendingDrafts.map((draft) => (
            <PendingRow key={draft.draftId} draft={draft} onOpen={props.onOpenDraft} />
          ))
        )}
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  segment: { flexDirection: 'row', gap: 6, marginTop: 2 },
  segmentOption: {
    flex: 1,
    height: 38,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: radius.sm,
    backgroundColor: colors.card,
  },
  segmentOn: {
    borderWidth: 2,
    borderColor: colors.selectedBorder,
    backgroundColor: colors.selectedBg,
  },
  segmentDisabled: { backgroundColor: '#E5E7EB', borderColor: colors.border },
  segmentText: { fontFamily: fonts.ui, fontSize: 14, fontWeight: '600', color: colors.text },
  hazard: {
    fontFamily: fonts.ui,
    fontSize: 16,
    lineHeight: 22,
    fontWeight: '800',
    color: colors.text,
    marginTop: 6,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: space.sm,
    padding: space.md,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.sm,
    backgroundColor: colors.card,
  },
  bannerText: {
    fontFamily: fonts.ui,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
    color: colors.text,
  },
});
