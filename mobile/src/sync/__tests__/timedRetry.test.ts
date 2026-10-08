import * as Network from 'expo-network';
import { AppState } from 'react-native';
import { startConnectivityTriggers } from '../connectivityTriggers';

jest.mock('expo-network', () => ({
  getNetworkStateAsync: jest.fn(),
  addNetworkStateListener: jest.fn(),
}));

const network = Network as jest.Mocked<typeof Network>;
const INTERVAL = 30_000;
const QUEUED = async () => true;

let appStateListener: (state: string) => void;
const originalState = Object.getOwnPropertyDescriptor(AppState, 'currentState');

/** `AppState.currentState` is a plain data property, so it is replaced, not spied on. */
function setAppState(state: string): void {
  Object.defineProperty(AppState, 'currentState', {
    value: state,
    configurable: true,
    writable: true,
  });
}

/** Lets the start-up check and the pending promises settle under fake timers. */
async function flush(): Promise<void> {
  await jest.advanceTimersByTimeAsync(0);
}

/** Starts the triggers, lets the start-up check pass and forgets its call. */
async function start(
  hasQueuedDrafts: () => Promise<boolean>,
): Promise<{ raised: jest.Mock; stop: () => void }> {
  const raised = jest.fn();
  const stop = startConnectivityTriggers(raised, hasQueuedDrafts, INTERVAL);
  await flush();
  raised.mockClear();
  return { raised, stop };
}

beforeEach(() => {
  jest.useFakeTimers();
  jest.clearAllMocks();
  network.addNetworkStateListener.mockImplementation((() => ({ remove: jest.fn() })) as never);
  network.getNetworkStateAsync.mockResolvedValue({ isConnected: true } as never);
  setAppState('active');
  jest.spyOn(AppState, 'addEventListener').mockImplementation(((
    _type: string,
    listener: typeof appStateListener,
  ) => {
    appStateListener = listener;
    return { remove: jest.fn() };
  }) as never);
});

afterEach(() => {
  if (originalState) {
    Object.defineProperty(AppState, 'currentState', originalState);
  }
  jest.useRealTimers();
  jest.restoreAllMocks();
});

describe('the timed retry (standards §10, P206, D-UC2-1)', () => {
  it('SC2 E4 18i: no tick before the interval has passed', async () => {
    const { raised } = await start(QUEUED);

    await jest.advanceTimersByTimeAsync(INTERVAL - 1);

    expect(raised).not.toHaveBeenCalled();
  });

  it('SC2 E4 18i: every tick raises the callback while the phone is online and a draft is QUEUED', async () => {
    const { raised } = await start(QUEUED);

    await jest.advanceTimersByTimeAsync(INTERVAL);
    await jest.advanceTimersByTimeAsync(INTERVAL);

    expect(raised).toHaveBeenCalledTimes(2);
  });

  it('SC2 E4 18i: a tick raises nothing when no draft is QUEUED', async () => {
    const { raised } = await start(async () => false);

    await jest.advanceTimersByTimeAsync(INTERVAL);

    expect(raised).not.toHaveBeenCalled();
  });

  it('SC2 E4 18i: a tick raises nothing, and does not read the queue, when the phone is offline', async () => {
    const queued = jest.fn(QUEUED);
    const { raised } = await start(queued);
    network.getNetworkStateAsync.mockResolvedValue({ isConnected: false } as never);

    await jest.advanceTimersByTimeAsync(INTERVAL);

    expect(raised).not.toHaveBeenCalled();
    expect(queued).not.toHaveBeenCalled();
  });

  it('SC2 E4 18i: nothing is raised in the background; the timer resumes when the app is active again', async () => {
    const { raised } = await start(QUEUED);
    appStateListener('background');

    await jest.advanceTimersByTimeAsync(INTERVAL * 3);
    expect(raised).not.toHaveBeenCalled();

    appStateListener('active');
    await flush();
    raised.mockClear();
    await jest.advanceTimersByTimeAsync(INTERVAL);

    expect(raised).toHaveBeenCalledTimes(1);
  });

  it('SC2 E4 18i: the inactive state clears the timer too', async () => {
    const { raised } = await start(QUEUED);
    appStateListener('inactive');

    await jest.advanceTimersByTimeAsync(INTERVAL * 2);

    expect(raised).not.toHaveBeenCalled();
  });

  it('SC2 E4 18i: no timer is started when the app starts in the background', async () => {
    setAppState('background');
    const { raised } = await start(QUEUED);

    await jest.advanceTimersByTimeAsync(INTERVAL * 2);

    expect(raised).not.toHaveBeenCalled();
  });

  it('SC2 E4 18i: becoming active twice does not start two timers', async () => {
    const { raised } = await start(QUEUED);
    appStateListener('active');
    await flush();
    raised.mockClear();

    await jest.advanceTimersByTimeAsync(INTERVAL);

    expect(raised).toHaveBeenCalledTimes(1);
  });

  it('SC2 E4 18i: stop clears the timer', async () => {
    const { raised, stop } = await start(QUEUED);

    stop();
    await jest.advanceTimersByTimeAsync(INTERVAL * 2);

    expect(raised).not.toHaveBeenCalled();
    expect(jest.getTimerCount()).toBe(0);
  });

  it('SC2 E4 18i: a queue check that throws counts as no draft, with a warning', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    const { raised } = await start(async () => {
      throw new Error('storage');
    });

    await jest.advanceTimersByTimeAsync(INTERVAL);

    expect(raised).not.toHaveBeenCalled();
    expect(warn).toHaveBeenCalledTimes(1);
  });

  it('SC2 E4 18i: a tick that finishes after stop raises nothing', async () => {
    let release: (queued: boolean) => void = () => undefined;
    const { raised, stop } = await start(
      () => new Promise<boolean>((resolve) => (release = resolve)),
    );
    await jest.advanceTimersByTimeAsync(INTERVAL);

    stop();
    release(true);
    await flush();

    expect(raised).not.toHaveBeenCalled();
  });
});
