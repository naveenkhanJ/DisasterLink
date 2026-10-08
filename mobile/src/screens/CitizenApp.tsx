/**
 * CitizenApp.tsx — the app shell of the citizen app (`CitizenAppUI`): the header with its "‹ Back"
 * link, the slim loading row, the common error banner (C1, drawn once here for every screen), the
 * sync-result banner and the in-app alerts (both on every screen), the screen of the current route,
 * the decision card, the discard question and the tab bar. The in-memory navigator is the `route`
 * of the hook; there is no navigation library (standards §10 "Phone screens beyond the frames",
 * P206).
 *
 * Implements CH-25, CH-26, CH-27 (WF2P-S01 to S06 and their states).
 */
import React, { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useAutoDismiss } from '../app/useAutoDismiss';
import type { CitizenAppUI } from '../app/useCitizenAppUI';
import {
  AppHeader,
  LoadingRow,
  ServiceUnavailableBanner,
  SoonNotice,
  TabBar,
  styles as ui,
} from '../components/ui';
import { hostAppName } from '../config/env';
import {
  DISCARDED_NOTICE_MS,
  MAX_ALERTS_SHOWN,
  SENT_NOTICE_MS,
  SOON_NOTICE_MS,
} from '../constants';
import { colors, space } from '../theme';
import { ChooseHazardType } from './ChooseHazardType';
import { ConnectionStrip } from './ConnectionStrip';
import { DiscardQuestion } from './DiscardQuestion';
import { ReportForm } from './ReportForm';
import { Screen } from './Screen';
import { ShowAlert } from './ShowAlert';
import { ShowHome } from './ShowHome';
import { ShowReportDecision } from './ShowReportDecision';
import { ShowReportStatus } from './ShowReportStatus';
import { ShowReview } from './ShowReview';
import { ShowSubmissionResult } from './ShowSubmissionResult';
import { ShowSyncResults } from './ShowSyncResults';
import { moreAlertsText, SERVICE_UNAVAILABLE_TEXT } from './texts';

/**
 * The routes that have the Back icon in the header (standards §10 item 1): only below a tab's first
 * screen, so not the home or the hazard-type list (the first screen of the Report tab). The
 * confirmations (S05, S05a, S05b, S05c) have none either, on purpose: a submitted report ends the flow,
 * and going back to the review screen would offer a second submission; they keep "Track report status"
 * and "Back to home". This is a stated exception to "Back below a tab's first screen" (P214).
 */
const BACK_ROUTES: ReadonlySet<string> = new Set(['reportForm', 'review', 'reportStatus']);

/** The screen of the current route. */
function CurrentScreen({ ui: app }: { ui: CitizenAppUI }) {
  const { route, draft } = app;
  if (route.name === 'home') {
    return (
      <Screen refreshing={app.refreshing} onRefresh={() => void app.refreshHomeScreen()}>
        <ShowHome
          view={app.home}
          currentLocation={app.currentLocation}
          locationAccess={app.locationAccess}
          appName={hostAppName()}
          draftDiscarded={app.draftDiscarded}
          onSelectLanguage={(language) => void app.selectLanguage(language)}
          onAllowLocationAccess={() => void app.allowLocationAccess()}
          onReportHazard={app.startReport}
          onOpenReport={(reportId) => void app.openReportStatus(reportId)}
          onOpenDraft={(draftId) => void app.openDraft(draftId)}
          onDismissDiscarded={app.dismissDiscarded}
        />
      </Screen>
    );
  }
  if (route.name === 'reportStatus') {
    return app.report ? (
      <ShowReportStatus
        report={app.report}
        refreshing={app.refreshing}
        onRefresh={() => void app.refreshReportStatus()}
      />
    ) : null;
  }
  if (route.name === 'submissionResult') {
    return app.result ? (
      <ShowSubmissionResult
        result={app.result}
        draft={draft}
        sentReportId={app.sentReportId}
        onTrackStatus={(reportId) => void app.openReportStatusFrom(reportId, 'submissionResult')}
        onBackToHome={() => void app.openHome()}
      />
    ) : null;
  }
  if (!draft) {
    return null;
  }
  if (route.name === 'chooseHazardType') {
    return (
      <ChooseHazardType
        capturedAt={draft.capturedAt}
        onSelect={(type) => void app.selectHazardType(type)}
      />
    );
  }
  if (route.name === 'review') {
    return (
      <ShowReview
        draft={draft}
        submitting={app.submitting}
        onEdit={app.editReport}
        onSubmit={() => void app.submitReport()}
      />
    );
  }
  return (
    <ReportForm
      draft={draft}
      errors={app.errors}
      locationUnavailable={app.locationUnavailable}
      permissionDenied={app.permissionDenied}
      onChangeHazardType={app.changeHazardType}
      descriptionText={app.descriptionText}
      addressText={app.addressText}
      onEnterDescription={app.enterDescription}
      onEnterAddress={app.enterAddress}
      onPickPhoto={(source) => void app.pickPhoto(source)}
      onRemovePhoto={app.removePhoto}
      onEnterLocation={app.enterLocationManually}
      locationAccess={app.locationAccess}
      onAllowLocationAccess={() => void app.allowLocationAccess()}
      mapStart={app.mapStart}
      manualPlaceName={app.manualPlaceName}
      onNameManualPlace={app.nameManualPlace}
      changingLocation={app.changingLocation}
      onChangeManualLocation={app.changeManualLocation}
      onKeepManualLocation={app.keepManualLocation}
      onReview={app.reviewReport}
      onDiscardDraft={(draftId) => void app.discardDraft(draftId)}
    />
  );
}

/**
 * Draws the app: the shell around the screen of the current route.
 *
 * @param props the state and inputs of the screen boundary
 * @returns the app
 */
export function CitizenApp({ ui: app }: { ui: CitizenAppUI }) {
  const [soon, setSoon] = useState<{ label: string; at: number } | null>(null);
  const homeTab = app.route.name === 'home' || app.route.name === 'reportStatus';
  const bannerShown =
    app.serviceUnavailable || (app.route.name === 'home' && app.home.error === true);
  const shownAlerts = app.alerts.slice(-MAX_ALERTS_SHOWN);
  const hiddenAlerts = app.alerts.length - shownAlerts.length;
  // Notices that need no action fade (standards §10 item 12, P222); a sync result that was rejected,
  // alerts, the decision card, the C1 banner and the S03g notice stay until closed or fixed.
  const sentOnly =
    app.syncResults.length > 0 && app.syncResults.every((result) => result.outcome !== 'REJECTED');
  useAutoDismiss(soon !== null, SOON_NOTICE_MS, () => setSoon(null), soon?.at);
  useAutoDismiss(sentOnly, SENT_NOTICE_MS, app.dismissSyncResults, app.syncResults);
  useAutoDismiss(app.draftDiscarded, DISCARDED_NOTICE_MS, app.dismissDiscarded);
  const noticeShown = bannerShown || app.syncResults.length > 0 || app.alerts.length > 0;
  return (
    <View style={styles.app}>
      <AppHeader onBack={BACK_ROUTES.has(app.route.name) ? () => void app.goBack() : undefined} />
      <ConnectionStrip
        state={app.connection}
        savedCopy={app.route.name === 'home' && app.home.fromCache === true}
      />
      {app.loading ? <LoadingRow /> : null}
      {soon ? <SoonNotice label={soon.label} onClose={() => setSoon(null)} /> : null}
      {noticeShown ? (
        <View style={styles.notices}>
          {bannerShown ? (
            <ServiceUnavailableBanner
              text={SERVICE_UNAVAILABLE_TEXT}
              onRetry={() => void app.retry()}
              busy={app.retrying}
              retriedAt={app.retriedAt}
            />
          ) : null}
          {app.syncResults.length > 0 ? (
            <ShowSyncResults results={app.syncResults} onClose={app.dismissSyncResults} />
          ) : null}
          {shownAlerts.map((alert) => (
            <ShowAlert key={alert.id} alert={alert} onClose={() => app.dismissAlert(alert.id)} />
          ))}
          {hiddenAlerts > 0 ? (
            <Pressable accessibilityRole="button" onPress={() => app.requestLeave(app.openHome)}>
              <Text style={ui.muted}>{moreAlertsText(hiddenAlerts)}</Text>
            </Pressable>
          ) : null}
        </View>
      ) : null}
      <View style={styles.body}>
        <CurrentScreen ui={app} />
        {app.decision ? (
          <ShowReportDecision
            decision={app.decision}
            onOpen={() => {
              const reportId = app.decision?.notice.reportId ?? '';
              app.requestLeave(() => app.openReportStatus(reportId));
            }}
            onClose={app.dismissDecision}
          />
        ) : null}
      </View>
      <DiscardQuestion
        visible={app.discardQuestion}
        onDiscard={() => void app.confirmDiscard()}
        onKeepEditing={app.keepEditing}
      />
      <TabBar
        active={homeTab ? 'Home' : 'Report'}
        onHome={() => app.requestLeave(app.openHome)}
        onReport={() => app.requestLeave(app.startReport)}
        onSoon={(label) => setSoon({ label, at: Date.now() })}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.card },
  body: { flex: 1, backgroundColor: colors.pageBg },
  notices: { gap: space.sm, padding: space.sm, backgroundColor: colors.pageBg },
});
