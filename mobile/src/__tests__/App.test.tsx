import { render, screen } from '@testing-library/react-native';
import React from 'react';
import { AppState, BackHandler } from 'react-native';
import { createAppServices } from '../app/appServices';
import { configureNotifications } from '../notifications/notifications';
import { startPushPolling } from '../notifications/pushPoller';
import { startConnectivityTriggers } from '../sync/connectivityTriggers';
import App from '../../App';

jest.mock('expo-font', () => ({ useFonts: jest.fn(() => [true]) }));
jest.mock('expo-status-bar', () => ({ StatusBar: () => null }));
jest.mock('react-native-safe-area-context', () => {
  const { View } = jest.requireActual('react-native');
  return { SafeAreaProvider: View, SafeAreaView: View };
});
jest.mock('../app/appServices', () => ({ createAppServices: jest.fn() }));
jest.mock('../notifications/notifications', () => ({
  configureNotifications: jest.fn(),
  presentLocalAlert: jest.fn(async () => undefined),
}));
jest.mock('../notifications/pushPoller', () => ({ startPushPolling: jest.fn() }));
jest.mock('../sync/connectivityTriggers', () => ({
  startConnectivityTriggers: jest.fn(),
  isOnline: jest.fn(async () => true),
}));

const HOME = {
  warnings: [],
  myReports: [],
  pendingDrafts: [],
  language: 'ENGLISH',
  fromCache: false,
};
/** Lets the pending promises of the start-up run. */
const tick = () => new Promise<void>((resolve) => setImmediate(() => resolve()));
const calls: string[] = [];
const stopTriggers = jest.fn();
const stopPolling = jest.fn();
let notificationsReady: () => void = () => undefined;
const skeletonApi = { pollOutbox: jest.fn(), acknowledge: jest.fn() };
const controller = {
  loadHome: jest.fn(async () => HOME),
  captureLocation: jest.fn(async () => undefined),
  syncPending: jest.fn(async () => []),
};
const findPending = jest.fn(async () => [] as { status: string }[]);
const locationAccess = { check: jest.fn(async () => 'granted'), request: jest.fn() };

describe('App start-up', () => {
  beforeEach(() => {
    calls.length = 0;
    stopTriggers.mockClear();
    stopPolling.mockClear();
    jest.mocked(createAppServices).mockReturnValue({
      env: { citizenId: 'CIT-KLT-0001', citizenLanguage: 'ENGLISH', citizenDistrict: 'KALUTARA' },
      skeletonApi,
      controller,
      store: { findPending },
      locationAccess,
      photos: {},
    } as unknown as ReturnType<typeof createAppServices>);
    jest.mocked(configureNotifications).mockImplementation(
      () =>
        new Promise<boolean>((resolve) => {
          calls.push('configureNotifications');
          notificationsReady = () => resolve(true);
        }),
    );
    jest.mocked(startConnectivityTriggers).mockImplementation(() => {
      calls.push('startConnectivityTriggers');
      return stopTriggers;
    });
    jest.mocked(startPushPolling).mockImplementation(() => {
      calls.push('startPushPolling');
      return stopPolling;
    });
  });

  it('SC2 §10 permission order: configureNotifications is awaited before the triggers and the polling start', async () => {
    await render(<App />);

    expect(calls).toEqual(['configureNotifications']);
    notificationsReady();
    await screen.findByText('Report Hazard');
    await tick();

    expect(calls).toEqual([
      'configureNotifications',
      'startConnectivityTriggers',
      'startPushPolling',
    ]);
  });

  it('SC2 steps 1-2: the home is opened at start-up and shown', async () => {
    await render(<App />);

    expect(await screen.findByText('Report Hazard')).toBeTruthy();
    expect(controller.loadHome).toHaveBeenCalled();
  });

  it('SC2 §10 start-up replay: the triggers call onConnectivityRestored, which synchronises', async () => {
    await render(<App />);
    notificationsReady();
    await tick();

    const raise = jest.mocked(startConnectivityTriggers).mock.calls[0][0];
    raise();
    await tick();

    expect(controller.syncPending).toHaveBeenCalledTimes(1);
  });

  it('SC1 step 15 / SC2 step 30: the poller is given the citizen, the API, the alert and decision handlers and the finished set-up', async () => {
    await render(<App />);
    notificationsReady();
    await tick();

    const options = jest.mocked(startPushPolling).mock.calls[0][0];
    expect(options.recipientId).toBe('CIT-KLT-0001');
    expect(options.api).toBe(skeletonApi);
    expect(typeof options.onAlert).toBe('function');
    expect(typeof options.onDecision).toBe('function');
    await expect(options.prepare?.()).resolves.toBe(true);
  });

  it('SC2 §10: both are stopped when the app unmounts', async () => {
    const view = await render(<App />);
    notificationsReady();
    await tick();

    await view.unmount();

    expect(stopTriggers).toHaveBeenCalledTimes(1);
    expect(stopPolling).toHaveBeenCalledTimes(1);
  });

  it('SC2 §10: an app that unmounts before the set-up is answered starts nothing', async () => {
    const view = await render(<App />);

    await view.unmount();
    notificationsReady();
    await tick();

    expect(calls).toEqual(['configureNotifications']);
  });

  it('SC2 §10: a failed notification set-up is logged and the app still starts its triggers and polling', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    jest.mocked(configureNotifications).mockImplementation(async () => {
      throw new Error('no permission');
    });

    await render(<App />);
    await tick();

    expect(warn).toHaveBeenCalled();
    expect(calls).toEqual(['startConnectivityTriggers', 'startPushPolling']);
  });

  it('SC2 E4 18i (P206): the triggers get a queue check that is true only when a draft is QUEUED', async () => {
    await render(<App />);
    notificationsReady();
    await tick();
    const hasQueuedDrafts = jest.mocked(startConnectivityTriggers).mock.calls[0][1];

    findPending.mockResolvedValueOnce([{ status: 'REJECTED' }]);
    expect(await hasQueuedDrafts()).toBe(false);
    findPending.mockResolvedValueOnce([{ status: 'REJECTED' }, { status: 'QUEUED' }]);
    expect(await hasQueuedDrafts()).toBe(true);
    findPending.mockResolvedValueOnce([]);
    expect(await hasQueuedDrafts()).toBe(false);
  });

  it('standards §10 item 1: Android Back is connected to the history, and removed on unmount', async () => {
    const remove = jest.fn();
    const add = jest.spyOn(BackHandler, 'addEventListener').mockReturnValue({ remove });

    const view = await render(<App />);

    expect(add).toHaveBeenCalledWith('hardwareBackPress', expect.any(Function));
    const press = add.mock.calls[0][1] as () => boolean;
    expect(press()).toBe(false); // the home does not handle Back: Android leaves the app
    await view.unmount();
    expect(remove).toHaveBeenCalledTimes(1);
  });

  it('standards §10 item 8 (G1): the app listens for AppState and reads the location again when it becomes active; removed on unmount', async () => {
    const remove = jest.fn();
    const listeners: ((state: string) => void)[] = [];
    const add = jest.spyOn(AppState, 'addEventListener').mockImplementation(((
      _type: string,
      listener: (state: string) => void,
    ) => {
      listeners.push(listener);
      return { remove };
    }) as never);
    const view = await render(<App />);
    await tick();
    const reads = controller.captureLocation.mock.calls.length;

    expect(add).toHaveBeenCalledWith('change', expect.any(Function));
    listeners.forEach((listener) => listener('background'));
    await tick();
    expect(controller.captureLocation.mock.calls.length).toBe(reads);
    listeners.forEach((listener) => listener('active'));
    await tick();

    expect(controller.captureLocation.mock.calls.length).toBeGreaterThan(reads);
    await view.unmount();
    expect(remove).toHaveBeenCalled();
  });
});
