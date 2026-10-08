import { fireEvent, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { presentLocalAlert } from '../../notifications/notifications';
import { withStatus } from '../../report/ReportDraft';
import { FILLED_DRAFT, makeRig, ONLINE_HOME, renderApp, REPORT, run, ui } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const presentAlert = presentLocalAlert as jest.Mock;

describe('found messages: synchronisation, alerts and decisions', () => {
  beforeEach(() => {
    presentAlert.mockClear();
    presentAlert.mockResolvedValue(undefined);
  });

  it('WF2P-S01b / SC2 A2 18d-18f: a restored connection sends the queued draft, shows "Report sent" and refreshes the home', async () => {
    const rig = makeRig();
    rig.controller.loadHome
      .mockResolvedValueOnce({
        ...ONLINE_HOME,
        fromCache: true,
        pendingDrafts: [withStatus(FILLED_DRAFT, 'QUEUED')],
      })
      .mockResolvedValueOnce({ ...ONLINE_HOME, myReports: [REPORT] });
    rig.controller.syncPending.mockResolvedValue([
      { submissionId: 'draft-1', outcome: 'ACCEPTED', message: 'RPT-2026-KLT-0847' },
    ]);
    await renderApp(rig);

    await run(() => ui().onConnectivityRestored());

    expect(rig.controller.syncPending).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Report sent — RPT-2026-KLT-0847, Pending verification')).toBeTruthy();
    expect(screen.getByText('Online')).toBeTruthy();
    expect(screen.getByText('Nothing waiting')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Open report RPT-2026-KLT-0847' })).toBeTruthy();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(2);
  });

  it('WF2P-S01b: the language selector is enabled again after the refresh', async () => {
    const rig = makeRig();
    rig.controller.loadHome
      .mockResolvedValueOnce({ ...ONLINE_HOME, fromCache: true })
      .mockResolvedValueOnce(ONLINE_HOME);
    await renderApp(rig);
    expect(screen.getByRole('radio', { name: 'Tamil' }).props.accessibilityState.disabled).toBe(
      true,
    );

    await run(() => ui().onConnectivityRestored());

    expect(screen.getByRole('radio', { name: 'Tamil' }).props.accessibilityState.disabled).toBe(
      false,
    );
  });

  it('WF2P-S01c / SC2 E8 18j-18k: a rejected replay shows "Report not accepted: <reason>" and the draft stays as Rejected with its reason', async () => {
    const rig = makeRig();
    const rejected = {
      ...FILLED_DRAFT,
      status: 'REJECTED' as const,
      rejectionReason: 'photo: not a readable JPEG or PNG image',
    };
    rig.controller.loadHome
      .mockResolvedValueOnce({
        ...ONLINE_HOME,
        pendingDrafts: [withStatus(FILLED_DRAFT, 'QUEUED')],
      })
      .mockResolvedValueOnce({ ...ONLINE_HOME, pendingDrafts: [rejected] });
    rig.controller.syncPending.mockResolvedValue([
      {
        submissionId: 'draft-1',
        outcome: 'REJECTED',
        reason: 'VALIDATION_FAILED',
        message: 'photo: not a readable JPEG or PNG image',
      },
    ]);
    await renderApp(rig);

    await run(() => ui().onConnectivityRestored());

    expect(
      screen.getByText(
        'Report not accepted: Validation failed — photo: not a readable JPEG or PNG image',
      ),
    ).toBeTruthy();
    expect(screen.getByText('Rejected')).toBeTruthy();
    expect(
      screen.getByText('Validation failed — photo: not a readable JPEG or PNG image'),
    ).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Open' })).toBeTruthy();
  });

  it('WF2P-S01c: a refusal without a reason code shows its message, and the result banner can be closed', async () => {
    const rig = makeRig();
    rig.controller.syncPending.mockResolvedValue([
      { submissionId: 'a', outcome: 'REJECTED', message: 'The photo is too large to send' },
      { submissionId: 'b', outcome: 'REJECTED', reason: 'INVALID_VALUE' },
      { submissionId: 'c', outcome: 'REJECTED' },
      { submissionId: 'd', outcome: 'DUPLICATE' },
    ]);
    await renderApp(rig);

    await run(() => ui().onConnectivityRestored());

    expect(screen.getByText('Report not accepted: The photo is too large to send')).toBeTruthy();
    expect(screen.getByText('Report not accepted: Invalid value')).toBeTruthy();
    expect(screen.getByText('Report not accepted')).toBeTruthy();
    expect(screen.getByText('Report sent — Pending verification')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));
    expect(screen.queryByText('Report sent — Pending verification')).toBeNull();
  });

  it('SC2 A2 18d: a restored connection with nothing queued shows no result banner but still refreshes the home', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().onConnectivityRestored());

    expect(screen.queryByText(/Report sent/)).toBeNull();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(2);
  });

  it('SC2 E4 18h-18i: a synchronisation fault is logged, nothing is shown and the home is still refreshed', async () => {
    const rig = makeRig();
    rig.controller.syncPending.mockRejectedValue(new TypeError('boom'));
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    await renderApp(rig);

    await run(() => ui().onConnectivityRestored());

    expect(warn).toHaveBeenCalled();
    expect(screen.queryByText(/Report sent|Report not accepted/)).toBeNull();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(2);
  });

  it('SC1 step 15 / showAlert: an alert shows an in-app banner and a local notification with the warning text', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() =>
      ui().receiveAlert('WRN-2026-0199', 'LEVEL_3_RED', 'Evacuate low-lying areas now'),
    );

    expect(screen.getByText('Warning alert')).toBeTruthy();
    expect(screen.getByText('Evacuate low-lying areas now')).toBeTruthy();
    expect(screen.getAllByText('Level 3 – Red (Evacuate)').length).toBeGreaterThan(0);
    expect(presentAlert).toHaveBeenCalledWith(
      'Level 3 – Red (Evacuate)',
      'Evacuate low-lying areas now',
    );
  });

  it.each([
    ['LEVEL_1_YELLOW', 'Level 1 – Yellow (Remain Vigilant)', '#FEF9C3', '#FACC15'],
    ['LEVEL_2_AMBER', 'Level 2 – Amber (Be Alert)', '#FEF3C7', '#F59E0B'],
    ['LEVEL_3_RED', 'Level 3 – Red (Evacuate)', '#FEE2E2', '#DC2626'],
  ] as const)(
    'SC1 step 15 / showAlert: a %s alert has its own tag colour and card border with its level text',
    async (level, label, background, border) => {
      const rig = makeRig();
      await renderApp(rig);

      await run(() => ui().receiveAlert('WRN-1', level, 'alert text'));

      const tag = StyleSheet.flatten(screen.getAllByText(label)[0].parent?.props.style);
      expect(tag.backgroundColor).toBe(background);
      expect(tag.borderColor).toBe(border);
      const card = StyleSheet.flatten(screen.getByText('alert text').parent?.props.style);
      expect(card.borderColor).toBe(border);
    },
  );

  it('SC1 step 15 / showAlert: each merged alert is its own banner and each can be closed', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().receiveAlert('WRN-1', 'LEVEL_1_YELLOW', 'first'));
    await run(() => ui().receiveAlert('WRN-1', 'LEVEL_2_AMBER', 'second'));

    expect(screen.getAllByText('Warning alert')).toHaveLength(2);
    await run(() => fireEvent.press(screen.getAllByRole('button', { name: 'Close alert' })[0]));
    expect(screen.queryByText('first')).toBeNull();
    expect(screen.getByText('second')).toBeTruthy();
    expect(presentAlert).toHaveBeenCalledTimes(2);
  });

  it('SC1 step 15 / showAlert: a notification that cannot be shown is logged and the banner stays', async () => {
    const rig = makeRig();
    presentAlert.mockRejectedValue(new Error('no permission'));
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    await renderApp(rig);

    await run(() => ui().receiveAlert('WRN-1', 'LEVEL_1_YELLOW', 'text'));

    expect(screen.getByText('text')).toBeTruthy();
    expect(warn).toHaveBeenCalled();
  });

  it('WF2P-S01e / SC2 step 30: a verified decision shows the card in English and a local notification', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-2026-KLT-0847',
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    expect(screen.getByText('Report decision')).toBeTruthy();
    expect(
      screen.getByText(
        'Your report RPT-2026-KLT-0847 was verified by the DMC (21 May 2026, 08:15)',
      ),
    ).toBeTruthy();
    expect(presentAlert).toHaveBeenCalledWith(
      'Report decision',
      'Your report RPT-2026-KLT-0847 was verified by the DMC (21 May 2026, 08:15)',
    );
  });

  it('WF2P-S01e / SC2 step 30: the Sinhala citizen reads the notice in Sinhala', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, language: 'SINHALA' });
    await renderApp(rig);

    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-2026-KLT-0847',
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    expect(
      screen.getByText(
        'ඔබේ වාර්තාව RPT-2026-KLT-0847 ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් තහවුරු කරන ලදී (2026 මැයි 21, 08:15)',
      ),
    ).toBeTruthy();
  });

  it("WF2P-S01f / SC2 A4 25f, 25h: a rejected decision shows the officer's reason as typed", async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-2026-KLT-0847',
        status: 'REJECTED',
        reason: 'Duplicate of RPT-2026-KLT-0843',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    expect(
      screen.getByText(
        'Your report RPT-2026-KLT-0847 was rejected by the DMC (21 May 2026, 08:15). Reason: Duplicate of RPT-2026-KLT-0843',
      ),
    ).toBeTruthy();
  });

  it('WF2P-S01e: the Tamil citizen reads the notice in Tamil', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, language: 'TAMIL' });
    await renderApp(rig);

    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-1',
        status: 'REJECTED',
        reason: 'காரணம்',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    expect(screen.getByText(/உங்கள் அபாய அறிக்கை RPT-1 ஏற்றுக்கொள்ளப்படவில்லை/)).toBeTruthy();
  });

  it('WF2P-S01e / SC2 step 31: "Open report" closes the card and opens the report-status screen', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-2026-KLT-0847',
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open report' })));

    expect(rig.controller.getReportStatus).toHaveBeenCalledWith('RPT-2026-KLT-0847');
    expect(screen.queryByText('Report decision')).toBeNull();
    expect(screen.getByText('Report status')).toBeTruthy();
  });

  it('WF2P-S01e: the card can be closed and the home underneath is unchanged', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-2026-KLT-0847',
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));

    expect(screen.queryByText('Report decision')).toBeNull();
    expect(screen.getByText('No reports yet')).toBeTruthy();
  });

  it('WF2P-S01e: a notification that cannot be shown is logged and the card stays', async () => {
    const rig = makeRig();
    presentAlert.mockRejectedValue(new Error('no permission'));
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    await renderApp(rig);

    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-1',
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    expect(screen.getByText('Report decision')).toBeTruthy();
    expect(warn).toHaveBeenCalled();
  });
});
