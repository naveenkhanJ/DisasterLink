import * as Network from 'expo-network';
import { AppState } from 'react-native';
import { isOnline, isOnlineState, startConnectivityTriggers } from '../connectivityTriggers';

jest.mock('expo-network', () => ({
  getNetworkStateAsync: jest.fn(),
  addNetworkStateListener: jest.fn(),
}));

const network = Network as jest.Mocked<typeof Network>;

let networkListener: (state: Network.NetworkState) => void;
let appStateListener: (state: string) => void;
const removeNetwork = jest.fn();
const removeAppState = jest.fn();

/** A queue check that finds no QUEUED draft. */
const noQueue = async () => false;

/** Lets the promises of the start-up check settle. */
const settle = () => new Promise<void>((resolve) => setImmediate(() => resolve()));

beforeEach(() => {
  jest.clearAllMocks();
  network.addNetworkStateListener.mockImplementation(((listener: typeof networkListener) => {
    networkListener = listener;
    return { remove: removeNetwork };
  }) as never);
  jest.spyOn(AppState, 'addEventListener').mockImplementation(((
    _type: string,
    listener: typeof appStateListener,
  ) => {
    appStateListener = listener;
    return { remove: removeAppState };
  }) as never);
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('isOnlineState and isOnline', () => {
  it('SC2 18c: connected, with the internet reachable or not known, is online', () => {
    expect(isOnlineState({ isConnected: true, isInternetReachable: true })).toBe(true);
    expect(isOnlineState({})).toBe(true);
  });

  it('SC2 18c: not connected, or the internet not reachable, is offline', () => {
    expect(isOnlineState({ isConnected: false })).toBe(false);
    expect(isOnlineState({ isConnected: true, isInternetReachable: false })).toBe(false);
  });

  it('SC2 18c: isOnline asks expo-network and counts an unreadable state as offline', async () => {
    network.getNetworkStateAsync.mockResolvedValueOnce({ isConnected: true } as never);
    expect(await isOnline()).toBe(true);

    network.getNetworkStateAsync.mockRejectedValueOnce(new Error('no module'));
    expect(await isOnline()).toBe(false);
  });
});

describe('startConnectivityTriggers', () => {
  it('PR11: raises at start-up when the phone is online', async () => {
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: true } as never);
    const raised = jest.fn();

    startConnectivityTriggers(raised, noQueue);
    await settle();

    expect(raised).toHaveBeenCalledTimes(1);
  });

  it('PR11: raises nothing at start-up when the phone is offline', async () => {
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: false } as never);
    const raised = jest.fn();

    startConnectivityTriggers(raised, noQueue);
    await settle();

    expect(raised).not.toHaveBeenCalled();
  });

  it('SC2 18d: raises when the network changes from offline to online, not for online to online', async () => {
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: false } as never);
    const raised = jest.fn();
    startConnectivityTriggers(raised, noQueue);
    await settle();

    networkListener({ isConnected: true, isInternetReachable: true });
    networkListener({ isConnected: true, isInternetReachable: true });
    networkListener({ isConnected: false });
    networkListener({ isConnected: true });

    expect(raised).toHaveBeenCalledTimes(2);
  });

  it('FX-25: raises when the app returns to the foreground while online', async () => {
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: true } as never);
    const raised = jest.fn();
    startConnectivityTriggers(raised, noQueue);
    await settle();
    raised.mockClear();

    appStateListener('background');
    await settle();
    expect(raised).not.toHaveBeenCalled();

    appStateListener('active');
    await settle();
    expect(raised).toHaveBeenCalledTimes(1);
  });

  it('FX-25: does not raise when the app returns to the foreground offline', async () => {
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: false } as never);
    const raised = jest.fn();
    startConnectivityTriggers(raised, noQueue);
    await settle();

    appStateListener('active');
    await settle();

    expect(raised).not.toHaveBeenCalled();
  });

  it('stops: the subscriptions are removed and nothing is raised afterwards', async () => {
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: false } as never);
    const raised = jest.fn();
    const stop = startConnectivityTriggers(raised, noQueue);
    await settle();

    stop();
    networkListener({ isConnected: true });
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: true } as never);
    appStateListener('active');
    await settle();

    expect(removeNetwork).toHaveBeenCalledTimes(1);
    expect(removeAppState).toHaveBeenCalledTimes(1);
    expect(raised).not.toHaveBeenCalled();
  });
});
