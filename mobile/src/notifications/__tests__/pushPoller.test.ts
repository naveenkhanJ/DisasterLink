import { AppState } from 'react-native';
import type { PushEntry } from '../../api/types';
import { configureNotifications } from '../notifications';
import { startPushPolling, type PushPollingOptions } from '../pushPoller';

jest.mock('../notifications', () => ({ configureNotifications: jest.fn(async () => true) }));

function alert(
  id: string,
  warningId: string,
  level: PushEntry['warningLevel'],
  text = id,
): PushEntry {
  return {
    id,
    kind: 'WARNING_ALERT',
    warningId,
    warningLevel: level,
    text,
    createdAt: '2026-05-21T08:30:00+05:30',
  };
}

function decision(id: string, extra: Partial<PushEntry> = {}): PushEntry {
  return {
    id,
    kind: 'REPORT_DECISION',
    reportId: 'RPT-2026-KLT-0847',
    status: 'REJECTED',
    reason: 'Not a flood',
    text: 'Your report was rejected',
    createdAt: '2026-05-21T08:31:00+05:30',
    decidedAt: '2026-05-21T08:30:30+05:30',
    ...extra,
  };
}

function setup(batches: (PushEntry[] | Error)[], extra: Partial<PushPollingOptions> = {}) {
  const queue = [...batches];
  const api = {
    pollOutbox: jest.fn(async () => {
      const next = queue.length > 1 ? queue.shift() : queue[0];
      if (next instanceof Error) {
        throw next;
      }
      return next ?? [];
    }),
    acknowledge: jest.fn(async () => undefined),
  };
  const onAlert = jest.fn();
  const onDecision = jest.fn();
  const prepare = jest.fn(async () => true);
  const stop = startPushPolling({
    recipientId: 'CIT-KLT-0001',
    api,
    onAlert,
    onDecision,
    prepare,
    intervalMs: 1000,
    ...extra,
  });
  return { api, onAlert, onDecision, prepare, stop };
}

function setAppState(state: string): void {
  Object.defineProperty(AppState, 'currentState', {
    value: state,
    configurable: true,
    writable: true,
  });
}

beforeEach(() => {
  jest.useFakeTimers();
  setAppState('active');
});

afterEach(() => {
  jest.useRealTimers();
  jest.restoreAllMocks();
});

describe('startPushPolling', () => {
  it('SC1 step 15: entries of the same warning and level in one batch become one alert', async () => {
    const { api, onAlert, stop } = setup([
      [
        alert('1', 'WRN-1', 'LEVEL_2_AMBER', 'push text'),
        alert('2', 'WRN-1', 'LEVEL_2_AMBER', 'audible text'),
      ],
    ]);

    await jest.advanceTimersByTimeAsync(0);

    expect(onAlert).toHaveBeenCalledTimes(1);
    expect(onAlert).toHaveBeenCalledWith('WRN-1', 'LEVEL_2_AMBER', 'push text');
    expect(api.acknowledge).toHaveBeenCalledWith('CIT-KLT-0001', ['1', '2']);
    stop();
  });

  it('R4G-13: the same warning at two levels in one batch gives two alerts', async () => {
    const { onAlert, stop } = setup([
      [alert('1', 'WRN-1', 'LEVEL_2_AMBER'), alert('2', 'WRN-1', 'LEVEL_3_RED')],
    ]);

    await jest.advanceTimersByTimeAsync(0);

    expect(onAlert.mock.calls.map((call) => [call[0], call[1]])).toEqual([
      ['WRN-1', 'LEVEL_2_AMBER'],
      ['WRN-1', 'LEVEL_3_RED'],
    ]);
    stop();
  });

  it('R4G-13: a warning that alerted before alerts again in a later batch', async () => {
    const { onAlert, stop } = setup([
      [alert('1', 'WRN-1', 'LEVEL_2_AMBER')],
      [alert('2', 'WRN-1', 'LEVEL_2_AMBER')],
    ]);

    await jest.advanceTimersByTimeAsync(0);
    await jest.advanceTimersByTimeAsync(1000);

    expect(onAlert).toHaveBeenCalledTimes(2);
    stop();
  });

  it('SC2 step 30: a REPORT_DECISION entry becomes a decision notice and is acknowledged', async () => {
    const { api, onDecision, onAlert, stop } = setup([[decision('9')]]);

    await jest.advanceTimersByTimeAsync(0);

    expect(onDecision).toHaveBeenCalledWith({
      reportId: 'RPT-2026-KLT-0847',
      status: 'REJECTED',
      reason: 'Not a flood',
      decidedAt: '2026-05-21T08:30:30+05:30',
    });
    expect(onAlert).not.toHaveBeenCalled();
    expect(api.acknowledge).toHaveBeenCalledWith('CIT-KLT-0001', ['9']);
    stop();
  });

  it('SC2 step 30: a decision without decidedAt uses the creation time', async () => {
    const { onDecision, stop } = setup([
      [decision('9', { decidedAt: undefined, reason: undefined })],
    ]);

    await jest.advanceTimersByTimeAsync(0);

    expect(onDecision).toHaveBeenCalledWith(
      expect.objectContaining({ decidedAt: '2026-05-21T08:31:00+05:30', reason: undefined }),
    );
    stop();
  });

  it('FX-13: entries that cannot be shown are not acknowledged, and an empty batch acknowledges nothing', async () => {
    const broken: PushEntry = {
      id: 'x',
      kind: 'WARNING_ALERT',
      text: 'no warning id',
      createdAt: 'now',
    };
    const { api, onAlert, stop } = setup([[broken], []]);

    await jest.advanceTimersByTimeAsync(0);
    await jest.advanceTimersByTimeAsync(1000);

    expect(onAlert).not.toHaveBeenCalled();
    expect(api.acknowledge).not.toHaveBeenCalled();
    stop();
  });

  it('FX-13: a poll that takes two and a half intervals is the only one in that time, and hands over once', async () => {
    const api = {
      pollOutbox: jest.fn(
        () =>
          new Promise<PushEntry[]>((resolve) =>
            setTimeout(() => resolve([alert('1', 'WRN-1', 'LEVEL_2_AMBER')]), 2500),
          ),
      ),
      acknowledge: jest.fn(async () => undefined),
    };
    const onAlert = jest.fn();
    const stop = startPushPolling({
      recipientId: 'CIT-KLT-0001',
      api,
      onAlert,
      onDecision: jest.fn(),
      prepare: async () => true,
      intervalMs: 1000,
    });

    await jest.advanceTimersByTimeAsync(0);
    await jest.advanceTimersByTimeAsync(2500);

    expect(api.pollOutbox).toHaveBeenCalledTimes(1);
    expect(onAlert).toHaveBeenCalledTimes(1);
    expect(api.acknowledge).toHaveBeenCalledTimes(1);
    await jest.advanceTimersByTimeAsync(1000);
    expect(api.pollOutbox).toHaveBeenCalledTimes(2);
    stop();
  });

  it('§10: waits for the notification set-up before the first poll', async () => {
    let finish: (value: boolean) => void = () => undefined;
    const prepare = jest.fn(() => new Promise<boolean>((resolve) => (finish = resolve)));
    const { api, stop } = setup([[]], { prepare });

    await jest.advanceTimersByTimeAsync(5000);
    expect(api.pollOutbox).not.toHaveBeenCalled();

    finish(true);
    await jest.advanceTimersByTimeAsync(0);
    expect(api.pollOutbox).toHaveBeenCalledTimes(1);
    stop();
  });

  it('§10: without a prepare option it awaits configureNotifications', async () => {
    const { api, stop } = setup([[]], { prepare: undefined });

    await jest.advanceTimersByTimeAsync(0);

    expect(configureNotifications).toHaveBeenCalled();
    expect(api.pollOutbox).toHaveBeenCalled();
    stop();
  });

  it('§10: polls at the interval, and a failed poll is logged once and does not stop the polling', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    const { api, onAlert, stop } = setup([
      new Error('down'),
      new Error('still down'),
      [alert('1', 'WRN-1', 'LEVEL_1_YELLOW')],
    ]);

    await jest.advanceTimersByTimeAsync(0);
    await jest.advanceTimersByTimeAsync(1000);
    await jest.advanceTimersByTimeAsync(1000);

    expect(api.pollOutbox).toHaveBeenCalledTimes(3);
    expect(warn).toHaveBeenCalledTimes(1);
    expect(onAlert).toHaveBeenCalledTimes(1);
    stop();
  });

  it('FX-13: a failed acknowledgement is logged and the next poll still runs', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    const { api, stop } = setup([[alert('1', 'WRN-1', 'LEVEL_1_YELLOW')], []]);
    api.acknowledge.mockRejectedValueOnce(new Error('ack failed'));

    await jest.advanceTimersByTimeAsync(0);
    await jest.advanceTimersByTimeAsync(1000);

    expect(warn).toHaveBeenCalledTimes(1);
    expect(api.pollOutbox).toHaveBeenCalledTimes(2);
    stop();
  });

  it('§10: does not poll while the app is not active', async () => {
    setAppState('background');
    const { api, stop } = setup([[]]);

    await jest.advanceTimersByTimeAsync(3000);

    expect(api.pollOutbox).not.toHaveBeenCalled();
    stop();
  });

  it('stops: no poll happens after the stop function, also before the set-up has finished', async () => {
    const first = setup([[]]);
    await jest.advanceTimersByTimeAsync(0);
    first.stop();
    first.api.pollOutbox.mockClear();
    await jest.advanceTimersByTimeAsync(3000);
    expect(first.api.pollOutbox).not.toHaveBeenCalled();

    let finish: (value: boolean) => void = () => undefined;
    const second = setup([[]], {
      prepare: jest.fn(() => new Promise<boolean>((resolve) => (finish = resolve))),
    });
    second.stop();
    finish(true);
    await jest.advanceTimersByTimeAsync(3000);
    expect(second.api.pollOutbox).not.toHaveBeenCalled();
  });
});
