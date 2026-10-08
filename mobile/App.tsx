/**
 * App.tsx — entry point of the citizen app (UC2). It builds the services once, awaits the
 * notification set-up before anything can alert (standards section 10, "Permission order"), then
 * starts the connectivity triggers (start-up replay, offline to online, foreground retry and the
 * 30 s timed retry while a draft is QUEUED, P206) and the push polling; both are stopped when the
 * app unmounts; the push poll's result and the network listener feed the shell's connection strip
 * (P213). It also connects Android's hardware Back button to the hook's `goBack` (the history
 * of the in-memory navigator). The composition root wires `hasQueuedDrafts` from the draft store;
 * that is wiring, not a boundary input. The screens are `CitizenApp`; the inputs of the screen
 * boundary are `useCitizenAppUI`.
 *
 * Implements CH-10, CH-25, CH-26, CH-27.
 */
import React, { useEffect, useState } from 'react';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import * as Network from 'expo-network';
import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { AppState, BackHandler, StyleSheet } from 'react-native';
import { createAppServices } from './src/app/appServices';
import { useCitizenAppUI } from './src/app/useCitizenAppUI';
import { configureNotifications } from './src/notifications/notifications';
import { startPushPolling } from './src/notifications/pushPoller';
import { CitizenApp } from './src/screens/CitizenApp';
import {
  isOnline,
  isOnlineState,
  startConnectivityTriggers,
} from './src/sync/connectivityTriggers';
import { colors } from './src/theme';

/**
 * The citizen app.
 *
 * @returns the app, or nothing while the fonts load
 */
export default function App() {
  const [services] = useState(createAppServices);
  const ui = useCitizenAppUI(services);
  const {
    openHome,
    goBack,
    onAppForeground,
    onNetworkChange,
    onServerReachability,
    onConnectivityRestored,
    receiveAlert,
    receiveReportDecision,
  } = ui;
  const [fontsLoaded] = useFonts({
    Manrope: require('./assets/fonts/Manrope-VariableFont_wght.ttf'),
    NotoSansSinhala: require('./assets/fonts/NotoSansSinhala-Regular.ttf'),
    NotoSansTamil: require('./assets/fonts/NotoSansTamil-Regular.ttf'),
  });

  useEffect(() => {
    void openHome();
  }, [openHome]);

  useEffect(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => goBack());
    return () => subscription.remove();
  }, [goBack]);

  useEffect(() => {
    // Coming back from Settings: the location access and the fix are read again (UC2-7b G1). Kept apart
    // from the connectivity triggers, which are about synchronisation.
    const subscription = AppState.addEventListener('change', (next) => {
      if (next === 'active') {
        void onAppForeground();
      }
    });
    return () => subscription.remove();
  }, [onAppForeground]);

  useEffect(() => {
    // The shell's connection strip follows the phone's network at once (standards §10 item 11, P213).
    let stopped = false;
    void isOnline().then((online) => {
      if (!stopped) {
        onNetworkChange(online);
      }
    });
    const subscription = Network.addNetworkStateListener((state) =>
      onNetworkChange(isOnlineState(state)),
    );
    return () => {
      stopped = true;
      subscription.remove();
    };
  }, [onNetworkChange]);

  useEffect(() => {
    let unmounted = false;
    let stopTriggers: (() => void) | undefined;
    let stopPolling: (() => void) | undefined;
    const notificationsReady = configureNotifications().catch((error: unknown) => {
      console.warn('[App] the notification set-up failed:', error);
      return false;
    });
    void notificationsReady.then(() => {
      if (unmounted) {
        return;
      }
      stopTriggers = startConnectivityTriggers(
        () => void onConnectivityRestored(),
        async () => (await services.store.findPending()).some((draft) => draft.status === 'QUEUED'),
      );
      stopPolling = startPushPolling({
        recipientId: services.env.citizenId,
        api: services.pollApi,
        onAlert: receiveAlert,
        onDecision: receiveReportDecision,
        onReachability: onServerReachability,
        prepare: () => notificationsReady,
      });
    });
    return () => {
      unmounted = true;
      stopTriggers?.();
      stopPolling?.();
    };
  }, [services, onConnectivityRestored, receiveAlert, receiveReportDecision, onServerReachability]);

  if (!fontsLoaded) {
    return null; // keep the splash screen until the three scripts' fonts are ready
  }
  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
        <StatusBar style="dark" />
        <CitizenApp ui={ui} />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.card },
});
