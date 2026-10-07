import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { configureNotifications, presentLocalAlert } from '../notifications';

jest.mock('expo-notifications', () => ({
  setNotificationHandler: jest.fn(),
  setNotificationChannelAsync: jest.fn(async () => null),
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  scheduleNotificationAsync: jest.fn(async () => 'id'),
  AndroidImportance: { HIGH: 4 },
}));

const mocked = Notifications as jest.Mocked<typeof Notifications>;

function setPlatform(os: 'ios' | 'android'): void {
  Object.defineProperty(Platform, 'OS', { configurable: true, get: () => os });
}

beforeEach(() => {
  jest.clearAllMocks();
  setPlatform('ios');
});

describe('configureNotifications', () => {
  it('sets a handler that shows the alert and plays the sound while the app is open', async () => {
    mocked.getPermissionsAsync.mockResolvedValue({ granted: true } as never);

    await configureNotifications();

    const handler = mocked.setNotificationHandler.mock.calls[0][0];
    const behaviour = await handler?.handleNotification({} as never);
    expect(behaviour).toMatchObject({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: true,
    });
  });

  it('creates the high-importance channel "warnings" on Android only', async () => {
    mocked.getPermissionsAsync.mockResolvedValue({ granted: true } as never);

    await configureNotifications();
    expect(mocked.setNotificationChannelAsync).not.toHaveBeenCalled();

    setPlatform('android');
    await configureNotifications();
    expect(mocked.setNotificationChannelAsync).toHaveBeenCalledWith('warnings', {
      name: 'Warnings',
      importance: 4,
    });
  });

  it('does not ask again when the permission was granted', async () => {
    mocked.getPermissionsAsync.mockResolvedValue({ granted: true } as never);

    const granted = await configureNotifications();

    expect(granted).toBe(true);
    expect(mocked.requestPermissionsAsync).not.toHaveBeenCalled();
  });

  it('asks for the permission when it was not granted and returns the answer', async () => {
    mocked.getPermissionsAsync.mockResolvedValue({ granted: false } as never);
    mocked.requestPermissionsAsync.mockResolvedValue({ granted: false } as never);

    expect(await configureNotifications()).toBe(false);
    expect(mocked.requestPermissionsAsync).toHaveBeenCalledTimes(1);

    mocked.requestPermissionsAsync.mockResolvedValue({ granted: true } as never);
    expect(await configureNotifications()).toBe(true);
  });
});

describe('presentLocalAlert', () => {
  it('schedules an immediate local notification with the title, the text and a sound', async () => {
    await presentLocalAlert('Title', 'Body');

    expect(mocked.scheduleNotificationAsync).toHaveBeenCalledWith({
      content: { title: 'Title', body: 'Body', sound: true },
      trigger: null,
    });
  });
});
