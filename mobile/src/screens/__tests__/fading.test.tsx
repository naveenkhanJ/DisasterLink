import { act, fireEvent, screen } from '@testing-library/react-native';
import { AccessibilityInfo } from 'react-native';
import { makeRig, renderApp, run, ui } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const SENT = {
  submissionId: 'draft-1',
  outcome: 'ACCEPTED' as const,
  message: 'RPT-2026-COL-0905',
};
const SENT_TEXT = 'Report sent — RPT-2026-COL-0905, Pending verification';
const REJECTED = {
  submissionId: 'draft-2',
  outcome: 'REJECTED' as const,
  reason: 'VALIDATION_FAILED' as const,
  message: 'photo: not readable',
};

/** Moves the fake clock and lets the state updates settle. */
const advance = (ms: number) =>
  act(async () => {
    jest.advanceTimersByTime(ms);
  });

describe('notices that need no action fade (standards §10 item 12, P222; UC2-7e G7, D3)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.spyOn(AccessibilityInfo, 'isScreenReaderEnabled').mockResolvedValue(false);
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('WF2P-S01 (G7): "Profile — not available in this version" goes after 3 s', async () => {
    await renderApp(makeRig());
    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Profile/ })));
    expect(screen.getByText('Profile — not available in this version.')).toBeTruthy();

    await advance(2_999);
    expect(screen.getByText('Profile — not available in this version.')).toBeTruthy();
    await advance(1);

    expect(screen.queryByText('Profile — not available in this version.')).toBeNull();
  });

  it('WF2P-S01 (G7): pressing Profile again restarts the 3 s', async () => {
    await renderApp(makeRig());
    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Profile/ })));
    await advance(2_000);

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Profile/ })));
    await advance(2_000);
    expect(screen.getByText('Profile — not available in this version.')).toBeTruthy();
    await advance(1_000);

    expect(screen.queryByText('Profile — not available in this version.')).toBeNull();
  });

  it('WF2P-S01b (G7): "Report sent — …" goes after 6 s', async () => {
    const rig = makeRig();
    rig.controller.syncPending.mockResolvedValue([SENT]);
    await renderApp(rig);
    await run(() => ui().onConnectivityRestored());
    expect(screen.getByText(SENT_TEXT)).toBeTruthy();

    await advance(5_999);
    expect(screen.getByText(SENT_TEXT)).toBeTruthy();
    await advance(1);

    expect(screen.queryByText(SENT_TEXT)).toBeNull();
    expect(ui().syncResults).toEqual([]);
  });

  it('WF2P-S01b (G7): a new sync result restarts the 6 s', async () => {
    const rig = makeRig();
    rig.controller.syncPending.mockImplementation(async () => [SENT]);
    await renderApp(rig);
    await run(() => ui().onConnectivityRestored());
    await advance(4_000);

    await run(() => ui().onConnectivityRestored());
    await advance(4_000);
    expect(screen.getByText(SENT_TEXT)).toBeTruthy();
    await advance(2_000);

    expect(screen.queryByText(SENT_TEXT)).toBeNull();
  });

  it('WF2P-S01c (G7): a banner with a REJECTED result stays until it is closed', async () => {
    const rig = makeRig();
    rig.controller.syncPending.mockResolvedValue([REJECTED]);
    await renderApp(rig);
    await run(() => ui().onConnectivityRestored());

    await advance(60_000);

    expect(screen.getByText(/Report not accepted/)).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));
    expect(screen.queryByText(/Report not accepted/)).toBeNull();
  });

  it('WF2P-S01c (G7): a banner with one sent and one REJECTED result also stays', async () => {
    const rig = makeRig();
    rig.controller.syncPending.mockResolvedValue([SENT, REJECTED]);
    await renderApp(rig);
    await run(() => ui().onConnectivityRestored());

    await advance(60_000);

    expect(screen.getByText(SENT_TEXT)).toBeTruthy();
    expect(screen.getByText(/Report not accepted/)).toBeTruthy();
  });

  it('WF2P-S01 (G7): closing the banner early clears the timer', async () => {
    const rig = makeRig();
    rig.controller.syncPending.mockResolvedValue([SENT]);
    await renderApp(rig);
    await run(() => ui().onConnectivityRestored());
    const cleared = jest.spyOn(globalThis, 'clearTimeout');

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));

    expect(screen.queryByText(SENT_TEXT)).toBeNull();
    expect(cleared).toHaveBeenCalled();
    await advance(10_000);
    expect(ui().syncResults).toEqual([]);
  });

  it('WF2P-S01d (G7): "Draft discarded — nothing was sent" goes after 4 s', async () => {
    await renderApp(makeRig());
    await run(() => ui().discardDraft('draft-1'));
    expect(screen.getByText('Draft discarded — nothing was sent')).toBeTruthy();

    await advance(3_999);
    expect(screen.getByText('Draft discarded — nothing was sent')).toBeTruthy();
    await advance(1);

    expect(screen.queryByText('Draft discarded — nothing was sent')).toBeNull();
  });

  it('SC1 step 15 (G7): warning alerts never fade', async () => {
    await renderApp(makeRig());
    await run(() => ui().receiveAlert('W-1', 'LEVEL_3_RED', 'Evacuate low-lying areas now'));

    await advance(120_000);

    expect(screen.getByText('Evacuate low-lying areas now')).toBeTruthy();
  });

  it('SC2 step 30 (G7): the decision card never fades', async () => {
    await renderApp(makeRig());
    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-1',
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    await advance(120_000);

    expect(screen.getByText('Report decision')).toBeTruthy();
  });

  it('C1 (G7): the service-unavailable banner never fades', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockRejectedValue(new Error('down'));
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    await renderApp(rig);

    await advance(120_000);

    expect(screen.getByText('The service is unavailable and nothing was changed')).toBeTruthy();
  });
});

describe('with a screen reader on nothing fades (standards §10 item 12, P222)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('G7: the three fading notices stay while a screen reader is on', async () => {
    jest.spyOn(AccessibilityInfo, 'isScreenReaderEnabled').mockResolvedValue(true);
    const rig = makeRig();
    rig.controller.syncPending.mockResolvedValue([SENT]);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Profile/ })));
    await run(() => ui().onConnectivityRestored());
    await run(() => ui().discardDraft('draft-1'));

    await advance(60_000);

    expect(screen.getByText('Profile — not available in this version.')).toBeTruthy();
    expect(screen.getByText(SENT_TEXT)).toBeTruthy();
    expect(screen.getByText('Draft discarded — nothing was sent')).toBeTruthy();
  });

  it('G7: a screen reader switched on later stops the fading; switched off, it fades again', async () => {
    jest.spyOn(AccessibilityInfo, 'isScreenReaderEnabled').mockResolvedValue(false);
    const handlers: ((on: boolean) => void)[] = [];
    jest.spyOn(AccessibilityInfo, 'addEventListener').mockImplementation(((
      _event: string,
      handler: (on: boolean) => void,
    ) => {
      handlers.push(handler);
      return { remove: jest.fn() };
    }) as never);
    const rig = makeRig();
    rig.controller.syncPending.mockResolvedValue([SENT]);
    await renderApp(rig);
    await act(async () => handlers.forEach((handler) => handler(true)));
    await run(() => ui().onConnectivityRestored());
    await advance(60_000);
    expect(screen.getByText(SENT_TEXT)).toBeTruthy();

    await act(async () => handlers.forEach((handler) => handler(false)));
    await advance(6_000);

    expect(screen.queryByText(SENT_TEXT)).toBeNull();
  });
});
