/**
 * App.tsx — entry point of the citizen app UI prototype (UC2, P89).
 *
 * A minimal in-memory navigator moves between the wireframe screens so the scenario's flow can be followed;
 * there is no server, storage, GPS or camera behaviour yet. On the web preview the starting screen can be
 * picked with the URL hash, e.g. index.html#WF2P-S03.
 */
import React, { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { AppHeader, SoonNotice, TabBar } from './src/components/ui';
import {
  ChooseHazardTypeScreen, HomeScreen, ReportFormScreen, ReportStatusScreen, ReviewReportScreen, SubmissionResultScreen,
  FLOW, SHOT_NAMES, type ScreenId,
} from './src/screens/screens';
import { colors } from './src/theme';
import { createHttpClient } from './src/api/httpClient';
import { createSkeletonApi } from './src/api/skeletonApi';
import { getEnv } from './src/config/env';
import { FIRST_RUN_CHECK_ON } from './src/constants';
import { runFirstRunCheck } from './src/dev/firstRunCheck';
import { configureNotifications, presentLocalAlert } from './src/notifications/notifications';

/** Reads "#WF2P-S03" (web preview only); otherwise the app starts on the home screen. */
function initialScreen(): ScreenId {
  const hash = typeof window !== 'undefined' && window.location ? window.location.hash.replace('#', '').split('&')[0] : '';
  return (FLOW as string[]).includes(hash) ? (hash as ScreenId) : 'WF2P-S01';
}

/** Web preview screenshots pass "&shot" in the hash to hide the label; on a phone it is always available. */
const HIDE_SHOT_LABEL = typeof window !== 'undefined' && !!window.location && window.location.hash.includes('shot');

/**
 * Prototype only (P95): shows "NN / 42 · save as <name>" for 3 seconds whenever the screen changes, so each
 * screenshot can be named; ‹ › step through all nine screens in screenshot order (including the states that
 * no button reaches). Tap the app header to show it again. Delete this component when the app is wired.
 */
function ShotLabel({ screen, onGo }: { screen: ScreenId; onGo: (to: ScreenId) => void }) {
  const i = FLOW.indexOf(screen);
  return (
    <View style={styles.shot}>
      <Pressable accessibilityLabel="Previous screen" onPress={() => onGo(FLOW[Math.max(0, i - 1)])} style={styles.shotBtn}>
        <Text style={styles.shotText}>‹</Text>
      </Pressable>
      <Text style={[styles.shotText, { flex: 1, textAlign: 'center' }]}>
        {SHOT_NAMES[screen].slice(0, 2)} / 42 · save as{'\n'}<Text style={{ color: '#F59E0B' }}>{SHOT_NAMES[screen]}</Text>
      </Text>
      <Pressable accessibilityLabel="Next screen" onPress={() => onGo(FLOW[Math.min(FLOW.length - 1, i + 1)])} style={styles.shotBtn}>
        <Text style={styles.shotText}>›</Text>
      </Pressable>
    </View>
  );
}

export default function App() {
  const [screen, setScreen] = useState<ScreenId>(initialScreen);
  const [soon, setSoon] = useState<string | null>(null); // label of a signposted item the user pressed (P91)
  const [shotVisible, setShotVisible] = useState(!HIDE_SHOT_LABEL); // prototype-only screenshot label (P95)
  useEffect(() => {
    if (HIDE_SHOT_LABEL) { return undefined; }
    // Both changes are scheduled (not made in the effect body), as the React lint rule asks.
    const show = setTimeout(() => setShotVisible(true), 0);
    const hide = setTimeout(() => setShotVisible(false), 3000);
    return () => { clearTimeout(show); clearTimeout(hide); };
  }, [screen]);
  useEffect(() => {
    // Notification handler, Android channel and permission, once at start-up (standards §10).
    void configureNotifications();
    // Dev only; remove after the first-run check (S6).
    if (__DEV__ && process.env.EXPO_PUBLIC_FIRST_RUN_CHECK === FIRST_RUN_CHECK_ON) {
      const env = getEnv();
      void runFirstRunCheck(createSkeletonApi(createHttpClient(env.apiUrl)), env.citizenId, presentLocalAlert);
    }
  }, []);
  const [fontsLoaded] = useFonts({
    Manrope: require('./assets/fonts/Manrope-VariableFont_wght.ttf'),
    NotoSansSinhala: require('./assets/fonts/NotoSansSinhala-Regular.ttf'),
    NotoSansTamil: require('./assets/fonts/NotoSansTamil-Regular.ttf'),
  });
  if (!fontsLoaded) {
    return null; // keep the splash screen until the three scripts' fonts are ready
  }

  const nav = (to: ScreenId) => { setSoon(null); setScreen(to); };
  const homeTab = screen === 'WF2P-S01' || screen === 'WF2P-S01e' || screen === 'WF2P-S06';

  let body: React.ReactNode;
  switch (screen) {
    case 'WF2P-S01': body = <HomeScreen nav={nav} />; break;
    case 'WF2P-S01e': body = <HomeScreen nav={nav} notice />; break;
    case 'WF2P-S02': body = <ChooseHazardTypeScreen nav={nav} />; break;
    case 'WF2P-S03': body = <ReportFormScreen nav={nav} />; break;
    case 'WF2P-S03e': body = <ReportFormScreen nav={nav} withErrors />; break;
    case 'WF2P-S04': body = <ReviewReportScreen nav={nav} />; break;
    case 'WF2P-S05': body = <SubmissionResultScreen nav={nav} />; break;
    case 'WF2P-S05a': body = <SubmissionResultScreen nav={nav} queued />; break;
    case 'WF2P-S06': body = <ReportStatusScreen />; break;
  }

  return (
    <SafeAreaProvider>
    <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
      <StatusBar style="dark" />
      <AppHeader onPress={HIDE_SHOT_LABEL ? undefined : () => setShotVisible(true)} />
      {soon ? <SoonNotice label={soon} onClose={() => setSoon(null)} /> : null}
      <View style={styles.body}>{body}</View>
      <TabBar active={homeTab ? 'Home' : 'Report'} onHome={() => nav('WF2P-S01')} onReport={() => nav('WF2P-S02')} onSoon={setSoon} />
      {shotVisible ? <ShotLabel screen={screen} onGo={nav} /> : null}
    </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.card },
  body: { flex: 1, backgroundColor: colors.pageBg },
  shot: { position: 'absolute', left: 12, right: 12, bottom: 84, flexDirection: 'row', alignItems: 'center', gap: 6,
    paddingVertical: 8, paddingHorizontal: 6, borderRadius: 16, backgroundColor: '#0F172A' },
  shotBtn: { paddingHorizontal: 12, paddingVertical: 4 },
  shotText: { color: '#FFFFFF', fontSize: 13, fontWeight: '700' },
});
