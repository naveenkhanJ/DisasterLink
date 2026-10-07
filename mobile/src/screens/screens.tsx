/**
 * screens.tsx — the citizen app's UC2 screens (frames-WF2.md WF2P-S01 … S06 and the P89 key states).
 *
 * Prototype (P89): static sample data, no behaviour. Each button only moves to the next screen of the
 * scenario so the flow can be followed. When wiring, each `show…` display becomes its own component
 * named after it (implementation-standards §4: ShowSubmissionResult.tsx for showSubmissionResult, …).
 */
import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { activeWarning, draft, hazardLabel, hazardTypes, location, report, type Language } from '../data/sample';
import { colors, fonts, radius, space } from '../theme';
import { Button, Card, ErrorText, Icon, LabelValue, PhotoPlaceholder, ScreenTitle, Tag, TextArea, styles as ui } from '../components/ui';

/** Screen identifiers = the wireframe IDs, so the prototype, the wireframes and the report captions line up. */
export type ScreenId =
  | 'WF2P-S01' | 'WF2P-S01e' | 'WF2P-S02' | 'WF2P-S03' | 'WF2P-S03e' | 'WF2P-S04' | 'WF2P-S05' | 'WF2P-S05a' | 'WF2P-S06';

export type Navigate = (to: ScreenId) => void;

/**
 * Prototype only (P95): the screenshot number and the file name to save each screen's screenshot as.
 * Officer pages are 01–33; the phone screens continue at 34. Remove with the ShotLabel in App.tsx when wiring.
 */
export const SHOT_NAMES: Record<ScreenId, string> = {
  'WF2P-S01': '34_WF2P-S01_home',
  'WF2P-S02': '35_WF2P-S02_chooseHazardType',
  'WF2P-S03': '36_WF2P-S03_reportForm',
  'WF2P-S03e': '37_WF2P-S03e_reportForm-validationErrors',
  'WF2P-S04': '38_WF2P-S04_reviewReport',
  'WF2P-S05': '39_WF2P-S05_reportSubmitted',
  'WF2P-S05a': '40_WF2P-S05a_savedOnThisPhone',
  'WF2P-S01e': '41_WF2P-S01e_decisionNotice',
  'WF2P-S06': '42_WF2P-S06_reportStatus',
};

/** The order in which the scenario visits the screens (used by the prototype's flow bar). */
export const FLOW: ScreenId[] = [
  'WF2P-S01', 'WF2P-S02', 'WF2P-S03', 'WF2P-S03e', 'WF2P-S04', 'WF2P-S05', 'WF2P-S05a', 'WF2P-S01e', 'WF2P-S06',
];

const FONT_FOR: Record<Language, string> = { Sinhala: fonts.sinhala, Tamil: fonts.tamil, English: fonts.ui };

function Screen({ children }: { children: React.ReactNode }) {
  return <ScrollView style={s.scroll} contentContainerStyle={s.content}>{children}</ScrollView>;
}

// ------------------------------------------------------------------ WF2P-S01 home (and S01e decision notice)
function LanguageSelector({ selected }: { selected: Language }) {
  return (
    <View>
      <Text style={ui.label}>Warning language</Text>
      <View style={s.segment}>
        {(['Sinhala', 'Tamil', 'English'] as Language[]).map((l) => {
          const on = l === selected;
          return (
            <Pressable key={l} accessibilityRole="radio" accessibilityState={{ selected: on }} style={[s.segmentOpt, on && s.segmentOn]}>
              {on ? <Icon name="check" size={14} color={colors.selectedText} /> : null}
              <Text style={[s.segmentText, on && { color: colors.selectedText, fontWeight: '800' }]}>{l}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

function WarningCard({ language }: { language: Language }) {
  const t = activeWarning.text[language];
  const font = FONT_FOR[language];
  return (
    <Card>
      <View style={s.rowCenter}>
        <Icon name="warning-triangle" size={20} />
        <Text style={s.cardTitle}>Active warnings</Text>
      </View>
      <View style={{ marginTop: space.sm }}><Tag label={activeWarning.level} tone="l2" /></View>
      <Text style={s.hazard}>{hazardLabel}</Text>
      <Text style={ui.muted}>{activeWarning.districts} · Valid until {activeWarning.validUntil}</Text>
      <Text style={[s.warningText, { fontFamily: font }]}>{t.message}</Text>
      <Text style={[s.warningText, { fontFamily: font }]}>{t.instructions}</Text>
    </Card>
  );
}

export function HomeScreen({ nav, notice = false }: { nav: Navigate; notice?: boolean }) {
  return (
    <View style={{ flex: 1 }}>
      <Screen>
        <View style={s.rowCenter}><Icon name="wifi" size={20} /><Tag label="Online" tone="ok" /></View>
        <LanguageSelector selected="Sinhala" />
        <WarningCard language="Sinhala" />
        <Button label="Report Hazard" icon="plus" big onPress={() => nav('WF2P-S02')} />
        <Card style={s.rowCenter}>
          <Icon name="pin" size={20} />
          <View>
            <Text style={s.strong}>Current location · {location.text}</Text>
            <Text style={ui.muted}>{location.source}</Text>
          </View>
        </Card>
        <View>
          <Text style={s.section}>My reports</Text>
          <Text style={[ui.muted, { fontSize: 15, marginTop: 6 }]}>No reports yet</Text>
        </View>
        <View>
          <Text style={s.section}>My reports — waiting on this phone</Text>
          <Text style={[ui.muted, { fontSize: 15, marginTop: 6 }]}>Nothing waiting</Text>
        </View>
      </Screen>
      {notice ? <DecisionNotice onOpen={() => nav('WF2P-S06')} onClose={() => nav('WF2P-S01')} /> : null}
    </View>
  );
}

/** showReportDecision(notice) — the decision notice over the home screen (SC2 step 30). */
function DecisionNotice({ onOpen, onClose }: { onOpen: () => void; onClose: () => void }) {
  return (
    <View style={s.noticeOverlay}>
      <Card style={s.noticeCard}>
        <View style={[s.rowCenter, { justifyContent: 'space-between' }]}>
          <View style={s.rowCenter}><Icon name="check" size={20} color={colors.ok.text} /><Text style={s.cardTitle}>Report decision</Text></View>
          <Pressable accessibilityLabel="Close" onPress={onClose}><Icon name="cross" size={20} /></Pressable>
        </View>
        <Text style={[s.warningText, { fontFamily: fonts.sinhala }]}>{report.decisionNoticeSinhala}</Text>
        <Button label="Open report" onPress={onOpen} style={{ alignSelf: 'flex-start', marginTop: space.sm }} />
      </Card>
    </View>
  );
}

// ------------------------------------------------------------------ WF2P-S02 choose hazard type
export function ChooseHazardTypeScreen({ nav }: { nav: Navigate }) {
  return (
    <Screen>
      <ScreenTitle title="Report a hazard" subtitle="What did you see?" />
      <Text style={ui.muted}>Capture time {draft.capturedAt} — recorded automatically</Text>
      {hazardTypes.map((h) => (
        <Pressable key={h.label} accessibilityRole="button" onPress={() => nav('WF2P-S03')} style={s.typeTile}>
          <View style={s.rowCenter}><Icon name={h.icon} size={26} /><Text style={s.typeText}>{h.label}</Text></View>
          <Icon name="chevron-right" size={20} color={colors.muted} />
        </Pressable>
      ))}
    </Screen>
  );
}

// ------------------------------------------------------------------ WF2P-S03 report form (and S03e errors)
export function ReportFormScreen({ nav, withErrors = false }: { nav: Navigate; withErrors?: boolean }) {
  const description = withErrors ? '' : draft.description;
  return (
    <Screen>
      <ScreenTitle title="Report a hazard" />
      <View style={s.typeRow}>
        <View style={s.rowCenter}><Text style={ui.label}>Hazard type</Text><Icon name="flood" size={20} /><Text style={s.strong}>{draft.hazardType}</Text></View>
        <Pressable onPress={() => nav('WF2P-S02')}><Text style={s.link}>Change</Text></Pressable>
      </View>
      <Text style={ui.muted}>Capture time {draft.capturedAt} — recorded automatically</Text>
      <TextArea label="Description *" value={description} counter={`${description.length} / ${draft.descriptionLimit}`}
        error={withErrors ? 'Enter a description of what you saw (at most 500 characters)' : undefined} />
      <View>
        <Text style={ui.label}>Photo * (1 required)</Text>
        <View style={[s.rowCenter, { alignItems: 'flex-end', marginTop: 4 }]}><PhotoPlaceholder width={150} height={96} /><Text style={ui.muted}>1 of 1</Text></View>
        <View style={[s.rowCenter, { marginTop: space.sm }]}>
          <Button label="Take photo" kind="secondary" icon="camera" style={{ flex: 1, paddingHorizontal: 8 }} />
          <Button label="Choose from gallery" kind="secondary" style={{ flex: 1.3, paddingHorizontal: 8 }} />
        </View>
        <Text style={[ui.muted, { marginTop: 4 }]}>A new photo replaces this one</Text>
      </View>
      <View style={s.rowCenter}>
        <Icon name="pin" size={22} />
        <View>
          <Text style={ui.label}>Location *</Text>
          <Text style={s.strong}>{location.text}</Text>
          <Text style={ui.muted}>{location.source}</Text>
        </View>
      </View>
      {withErrors ? <ErrorText>Please correct the field marked above</ErrorText> : null}
      <Button label="Review Report" big onPress={() => nav(withErrors ? 'WF2P-S03' : 'WF2P-S04')} />
    </Screen>
  );
}

// ------------------------------------------------------------------ WF2P-S04 review report
export function ReviewReportScreen({ nav }: { nav: Navigate }) {
  return (
    <Screen>
      <ScreenTitle title="Review your report" subtitle="Check the details before you send them." />
      <Card style={{ paddingVertical: 4 }}>
        <LabelValue label="Hazard type"><View style={s.rowCenter}><Icon name="flood" size={18} /><Text style={ui.value}>{draft.hazardType}</Text></View></LabelValue>
        <LabelValue label="Description"><Text style={[ui.value, { fontSize: 14, lineHeight: 20 }]}>{draft.description}</Text></LabelValue>
        <LabelValue label="Photo"><View style={[s.rowCenter, { alignItems: 'flex-end' }]}><PhotoPlaceholder width={160} height={110} /><Text style={ui.muted}>1 of 1</Text></View></LabelValue>
        <LabelValue label="Location"><Text style={ui.value}>{location.text}</Text><Text style={ui.muted}>{location.source}</Text></LabelValue>
        <LabelValue label="Capture time" last>{draft.capturedAt}</LabelValue>
      </Card>
      <View style={s.rowCenter}>
        <Button label="Edit" kind="secondary" big style={{ width: 110 }} onPress={() => nav('WF2P-S03')} />
        <Button label="Submit Report" big style={{ flex: 1 }} onPress={() => nav('WF2P-S05')} />
      </View>
    </Screen>
  );
}

// ------------------------------------------------------------------ WF2P-S05 submission result (and S05a saved offline)
export function SubmissionResultScreen({ nav, queued = false }: { nav: Navigate; queued?: boolean }) {
  return (
    <Screen>
      <View style={[s.resultIcon, queued ? s.resultIconQueued : null]}>
        <Icon name={queued ? 'clock' : 'check'} size={48} color={queued ? colors.warn.text : colors.ok.text} />
      </View>
      <Text style={s.headline}>{queued ? 'Saved on this phone — Pending synchronisation' : 'Report submitted'}</Text>
      <Card style={{ paddingVertical: 4 }}>
        {queued ? (
          <>
            <LabelValue label="Capture time">{draft.capturedAt}</LabelValue>
            <LabelValue label="Status" last><Tag label="Pending synchronisation" tone="warn" icon="clock" /></LabelValue>
          </>
        ) : (
          <>
            <LabelValue label="Report ID">{report.id}</LabelValue>
            <LabelValue label="Status" last><Tag label="Pending verification" tone="warn" /></LabelValue>
          </>
        )}
      </Card>
      {queued ? <Text style={[ui.muted, { fontSize: 15, lineHeight: 22 }]}>It will be sent automatically when the app is open and the connection returns</Text> : null}
      {queued
        ? <Button label="Back to home" big onPress={() => nav('WF2P-S01')} />
        : <Button label="Track report status" big onPress={() => nav('WF2P-S06')} />}
    </Screen>
  );
}

// ------------------------------------------------------------------ WF2P-S06 report status
export function ReportStatusScreen() {
  return (
    <Screen>
      <ScreenTitle title="Report status" />
      <Card style={{ paddingVertical: 4 }}>
        <LabelValue label="Report ID">{report.id}</LabelValue>
        <LabelValue label="Hazard type"><View style={s.rowCenter}><Icon name="flood" size={18} /><Text style={ui.value}>{draft.hazardType}</Text></View></LabelValue>
        <LabelValue label="Capture time">{draft.capturedAt}</LabelValue>
        <LabelValue label="Status"><Tag label="Verified" tone="ok" icon="check" /></LabelValue>
        <LabelValue label="Decision time" last>{report.decidedAt}</LabelValue>
      </Card>
    </Screen>
  );
}

const s = StyleSheet.create({
  scroll: { flex: 1, backgroundColor: colors.pageBg },
  content: { padding: space.lg, gap: 10 },
  rowCenter: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
  cardTitle: { fontFamily: fonts.ui, fontSize: 17, fontWeight: '800', color: colors.text },
  hazard: { fontFamily: fonts.ui, fontSize: 16, lineHeight: 22, fontWeight: '800', color: colors.text, marginTop: 6 },
  warningText: { fontSize: 14, lineHeight: 21, color: colors.text, marginTop: 6 },
  strong: { fontFamily: fonts.ui, fontSize: 15, lineHeight: 20, fontWeight: '700', color: colors.text },
  section: { fontFamily: fonts.ui, fontSize: 17, fontWeight: '700', color: colors.text },
  segment: { flexDirection: 'row', gap: 6, marginTop: 2 },
  segmentOpt: { flex: 1, height: 38, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 4, borderWidth: 1,
    borderColor: colors.borderStrong, borderRadius: radius.sm, backgroundColor: colors.card },
  segmentOn: { borderWidth: 2, borderColor: colors.selectedBorder, backgroundColor: colors.selectedBg },
  segmentText: { fontFamily: fonts.ui, fontSize: 14, fontWeight: '600', color: colors.text },
  typeTile: { height: 64, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: space.md,
    borderWidth: 1, borderColor: colors.borderStrong, borderRadius: 10, backgroundColor: colors.card },
  typeText: { fontFamily: fonts.ui, fontSize: 16, fontWeight: '600', color: colors.text },
  typeRow: { height: 44, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.border },
  link: { fontFamily: fonts.ui, fontSize: 14, fontWeight: '700', textDecorationLine: 'underline', color: colors.text },
  resultIcon: { alignSelf: 'center', marginTop: 32, width: 96, height: 96, borderRadius: 48, borderWidth: 2, borderColor: colors.ok.border,
    backgroundColor: colors.ok.bg, alignItems: 'center', justifyContent: 'center' },
  resultIconQueued: { borderColor: colors.warn.border, backgroundColor: colors.warn.bg },
  headline: { fontFamily: fonts.ui, fontSize: 22, lineHeight: 30, fontWeight: '800', textAlign: 'center', color: colors.text, marginVertical: space.sm },
  noticeOverlay: { position: 'absolute', top: space.sm, left: space.sm, right: space.sm },
  noticeCard: { borderColor: colors.ok.border, borderWidth: 2, shadowColor: '#000', shadowOpacity: 0.2, shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 }, elevation: 6 },
});
