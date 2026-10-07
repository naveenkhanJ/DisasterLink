/**
 * notifications.ts — the app's notification set-up (implementation standards §10, Expo Go): the
 * foreground handler, the Android channel and the permission request, and a helper that shows a
 * local alert. The merge of polled entries into one alert per warning (`showAlert`) is UC2's.
 */
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { WARNINGS_CHANNEL_ID, WARNINGS_CHANNEL_NAME } from '../constants';

/**
 * Prepares notifications once at start-up: alerts and sounds are shown while the app is open
 * (without a handler nothing is shown in the foreground), the Android channel `warnings` exists
 * with high importance (Android 13 and later need a channel before they ask for permission), and
 * the permission is requested if it was not granted yet.
 *
 * @returns whether the app may show notifications
 */
export async function configureNotifications(): Promise<boolean> {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
    }),
  });
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync(WARNINGS_CHANNEL_ID, {
      name: WARNINGS_CHANNEL_NAME,
      importance: Notifications.AndroidImportance.HIGH,
    });
  }
  const existing = await Notifications.getPermissionsAsync();
  if (existing.granted) {
    return true;
  }
  const requested = await Notifications.requestPermissionsAsync();
  return requested.granted;
}

/**
 * Shows a notification at once, as a local notification on this device.
 *
 * @param title the title
 * @param body the text
 */
export async function presentLocalAlert(title: string, body: string): Promise<void> {
  await Notifications.scheduleNotificationAsync({
    content: { title, body, sound: true },
    trigger: null,
  });
}
