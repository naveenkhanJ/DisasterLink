/**
 * CitizenApp.tsx — the app shell of the citizen app (`CitizenAppUI`): the header, the common error
 * banner (C1, drawn once here for every screen), the in-app alerts, the screen of the current route,
 * the decision card and the tab bar. The in-memory navigator is the `route` of the hook; there is
 * no navigation library.
 *
 * Implements CH-25, CH-26, CH-27 (WF2P-S01 to S06 and their states).
 */
import React, { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import type { CitizenAppUI } from '../app/useCitizenAppUI';
import { AppHeader, ServiceUnavailableBanner, SoonNotice, TabBar } from '../components/ui';
import { colors, space } from '../theme';
import { ChooseHazardType } from './ChooseHazardType';
import { ReportForm } from './ReportForm';
import { Screen } from './Screen';
import { ShowAlert } from './ShowAlert';
import { ShowHome } from './ShowHome';
import { ShowReportDecision } from './ShowReportDecision';
import { ShowReportStatus } from './ShowReportStatus';
import { ShowReview } from './ShowReview';
import { ShowSubmissionResult } from './ShowSubmissionResult';
import { SERVICE_UNAVAILABLE_TEXT } from './texts';

/** The screen of the current route. */
function CurrentScreen({ ui }: { ui: CitizenAppUI }) {
  const { route, draft } = ui;
  if (route.name === 'home') {
    return (
      <Screen>
        <ShowHome
          view={ui.home}
          currentLocation={ui.currentLocation}
          syncResults={ui.syncResults}
          draftDiscarded={ui.draftDiscarded}
          onSelectLanguage={(language) => void ui.selectLanguage(language)}
          onReportHazard={ui.startReport}
          onOpenReport={(reportId) => void ui.openReportStatus(reportId)}
          onOpenDraft={(draftId) => void ui.openDraft(draftId)}
          onDismissSyncResults={ui.dismissSyncResults}
          onDismissDiscarded={ui.dismissDiscarded}
        />
      </Screen>
    );
  }
  if (route.name === 'reportStatus') {
    return ui.report ? <ShowReportStatus report={ui.report} /> : null;
  }
  if (route.name === 'submissionResult') {
    return ui.result ? (
      <ShowSubmissionResult
        result={ui.result}
        draft={draft}
        onTrackStatus={(reportId) => void ui.openReportStatus(reportId)}
        onBackToHome={() => void ui.openHome()}
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
        onSelect={(type) => void ui.selectHazardType(type)}
      />
    );
  }
  if (route.name === 'review') {
    return (
      <ShowReview
        draft={draft}
        submitting={ui.submitting}
        onEdit={ui.editReport}
        onSubmit={() => void ui.submitReport()}
      />
    );
  }
  return (
    <ReportForm
      draft={draft}
      errors={ui.errors}
      locationUnavailable={ui.locationUnavailable}
      permissionDenied={ui.permissionDenied}
      onChangeHazardType={ui.changeHazardType}
      onEnterDescription={ui.enterDescription}
      onPickPhoto={(source) => void ui.pickPhoto(source)}
      onEnterLocation={ui.enterLocationManually}
      onReview={ui.reviewReport}
      onDiscardDraft={(draftId) => void ui.discardDraft(draftId)}
    />
  );
}

/**
 * Draws the app: the shell around the screen of the current route.
 *
 * @param props the state and inputs of the screen boundary
 * @returns the app
 */
export function CitizenApp({ ui }: { ui: CitizenAppUI }) {
  const [soon, setSoon] = useState<string | null>(null);
  const homeTab = ui.route.name === 'home' || ui.route.name === 'reportStatus';
  const bannerShown = ui.serviceUnavailable || (ui.route.name === 'home' && ui.home.error === true);
  return (
    <View style={styles.app}>
      <AppHeader />
      {soon ? <SoonNotice label={soon} onClose={() => setSoon(null)} /> : null}
      {bannerShown ? (
        <View style={styles.notices}>
          <ServiceUnavailableBanner
            text={SERVICE_UNAVAILABLE_TEXT}
            onRetry={() => void ui.retry()}
          />
        </View>
      ) : null}
      {ui.alerts.length > 0 ? (
        <View style={styles.notices}>
          {ui.alerts.map((alert) => (
            <ShowAlert key={alert.id} alert={alert} onClose={() => ui.dismissAlert(alert.id)} />
          ))}
        </View>
      ) : null}
      <View style={styles.body}>
        <CurrentScreen ui={ui} />
        {ui.decision ? (
          <ShowReportDecision
            decision={ui.decision}
            onOpen={() => void ui.openReportStatus(ui.decision?.notice.reportId ?? '')}
            onClose={ui.dismissDecision}
          />
        ) : null}
      </View>
      <TabBar
        active={homeTab ? 'Home' : 'Report'}
        onHome={() => void ui.openHome()}
        onReport={ui.startReport}
        onSoon={setSoon}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.card },
  body: { flex: 1, backgroundColor: colors.pageBg },
  notices: { gap: space.sm, padding: space.sm, backgroundColor: colors.pageBg },
});
