import { act, fireEvent, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { ApiError, NetworkError } from '../../api/httpClient';
import { MIN_RETRY_FEEDBACK_MS } from '../../constants';
import { withStatus } from '../../report/ReportDraft';
import { FILLED_DRAFT, makeRig, ONLINE_HOME, renderApp, REPORT, run, ui, WARNING } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const SERVER_FAULT = new ApiError(500, 'Internal Server Error');

describe('home screen', () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  it('WF2P-S01 / SC2 steps 1-2: shows the connectivity, the warning, the location and the empty lists', async () => {
    const rig = makeRig();

    await renderApp(rig);

    expect(screen.getByText('Online')).toBeTruthy();
    expect(screen.getByText('Level 2 – Amber (Be Alert)')).toBeTruthy();
    expect(screen.getByText('Flood — Kalu Ganga at Ratnapura')).toBeTruthy();
    expect(screen.getByText('Ratnapura, Kalutara · Valid until 21 May 2026, 10:45')).toBeTruthy();
    expect(screen.getByText(WARNING.message.english)).toBeTruthy();
    expect(screen.getByText(WARNING.instructions.english)).toBeTruthy();
    expect(screen.getByText('Current location · 6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText(/^GPS · ±8 m · near /)).toBeTruthy();
    expect(screen.getByText('No reports yet')).toBeTruthy();
    expect(screen.getByText('Nothing waiting')).toBeTruthy();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
  });

  it('WF2P-S01 / SC2 steps 3-4: selecting Sinhala changes the language, then the warning is shown in Sinhala', async () => {
    const rig = makeRig();
    rig.controller.loadHome
      .mockResolvedValueOnce(ONLINE_HOME)
      .mockResolvedValueOnce({ ...ONLINE_HOME, language: 'SINHALA' });
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('radio', { name: 'Sinhala' })));

    expect(rig.controller.changeLanguage).toHaveBeenCalledWith('SINHALA');
    expect(screen.getByText(WARNING.message.sinhala)).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'Sinhala' }).props.accessibilityState.selected).toBe(
      true,
    );
  });

  it('WF2P-S01 shows the Tamil text when the home is in Tamil', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, language: 'TAMIL' });

    await renderApp(rig);

    expect(screen.getByText(WARNING.message.tamil)).toBeTruthy();
    expect(screen.getByText(WARNING.instructions.tamil)).toBeTruthy();
  });

  it.each([
    ['LEVEL_1_YELLOW', 'Level 1 – Yellow (Remain Vigilant)', '#FEF9C3', '#FACC15'],
    ['LEVEL_2_AMBER', 'Level 2 – Amber (Be Alert)', '#FEF3C7', '#F59E0B'],
    ['LEVEL_3_RED', 'Level 3 – Red (Evacuate)', '#FEE2E2', '#DC2626'],
  ] as const)(
    'WF2P-S01: a %s warning is drawn in its own colour together with its level text',
    async (level, label, background, border) => {
      const rig = makeRig();
      rig.controller.loadHome.mockResolvedValue({
        ...ONLINE_HOME,
        warnings: [{ ...WARNING, warningLevel: level }],
      });

      await renderApp(rig);

      const tag = StyleSheet.flatten(screen.getByText(label).parent?.props.style);
      expect(tag.backgroundColor).toBe(background);
      expect(tag.borderColor).toBe(border);
    },
  );

  it('WF2P-S01a / SC2 A8 2a-2d: the saved copy is marked Offline and the language selector is disabled', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({
      ...ONLINE_HOME,
      fromCache: true,
      pendingDrafts: [withStatus(FILLED_DRAFT, 'QUEUED')],
    });

    await renderApp(rig);

    expect(screen.queryByText('Showing the copy saved on this phone')).toBeNull();
    expect(screen.getByText('· showing the saved copy')).toBeTruthy();
    expect(screen.getByText('Language can be changed when you are online')).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'Tamil' }).props.accessibilityState.disabled).toBe(
      true,
    );
    expect(screen.getByText('Pending synchronisation')).toBeTruthy();
    expect(screen.getByText('Captured 21 May 2026, 07:40')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Report Hazard' })).toBeTruthy();
  });

  it('WF2P-S01a: the language input does nothing while the home is the saved copy (disabled while fromCache)', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, fromCache: true });
    await renderApp(rig);

    await run(() => ui().selectLanguage('TAMIL'));
    await run(() => fireEvent.press(screen.getByRole('radio', { name: 'Tamil' })));

    expect(rig.controller.changeLanguage).not.toHaveBeenCalled();
  });

  it('SC2 A8 2b: a first launch offline shows only the offline mark and the empty lists', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({
      warnings: [],
      myReports: [],
      pendingDrafts: [],
      language: 'ENGLISH',
      fromCache: true,
    });
    rig.controller.captureLocation.mockResolvedValue(undefined);

    await renderApp(rig);

    expect(screen.getByText('Warnings load when you are online')).toBeTruthy();
    expect(screen.getByText('Reports load when you are online')).toBeTruthy();
    expect(screen.getByText('Current location unavailable')).toBeTruthy();
    expect(screen.getByText('The phone has not found its position yet')).toBeTruthy();
  });

  it('SC2 A8 (R6): offline with only one list empty, only that list gives the offline line', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({
      warnings: [],
      myReports: [REPORT],
      pendingDrafts: [],
      language: 'ENGLISH',
      fromCache: true,
    });

    await renderApp(rig);

    expect(screen.getByText('Warnings load when you are online')).toBeTruthy();
    expect(screen.queryByText('Reports load when you are online')).toBeNull();
    expect(screen.queryByText('No reports yet')).toBeNull();
  });

  it('SC2 step 2 (R6): online the empty lists keep their own texts', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, warnings: [], myReports: [] });

    await renderApp(rig);

    expect(screen.getByText('No active warnings')).toBeTruthy();
    expect(screen.getByText('No reports yet')).toBeTruthy();
    expect(screen.queryByText('Warnings load when you are online')).toBeNull();
    expect(screen.queryByText('Reports load when you are online')).toBeNull();
  });

  it('SC2 step 2: "My reports" lists the reports with their status and opens one', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({
      ...ONLINE_HOME,
      myReports: [
        REPORT,
        { ...REPORT, reportId: 'RPT-2', status: 'VERIFIED' },
        { ...REPORT, reportId: 'RPT-3', status: 'REJECTED' },
      ],
    });
    await renderApp(rig);

    expect(screen.getByText('RPT-2026-KLT-0847 · Flood')).toBeTruthy();
    expect(screen.getByText('Pending verification')).toBeTruthy();
    expect(screen.getByText('Verified')).toBeTruthy();
    expect(screen.getByText('Rejected')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open report RPT-2' })));

    expect(rig.controller.getReportStatus).toHaveBeenCalledWith('RPT-2');
  });

  it('SC2 step 5: Report Hazard starts a draft and shows the hazard-type list', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));

    expect(rig.controller.startReport).toHaveBeenCalledTimes(1);
    expect(screen.getByText('What did you see?')).toBeTruthy();
  });

  it('SC2 steps 1-2 (C1): a server fault on the home shows the common error banner and keeps the rest', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, warnings: [], error: true });

    await renderApp(rig);

    expect(screen.getByText('The service is unavailable and nothing was changed')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Report Hazard' })).toBeTruthy();
  });

  it('SC2 steps 3-4 (C1): a failed language change shows the banner and leaves the language unchanged', async () => {
    const rig = makeRig();
    rig.controller.changeLanguage.mockRejectedValue(new NetworkError('offline'));
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('radio', { name: 'Tamil' })));

    expect(screen.getByText('The service is unavailable and nothing was changed')).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'English' }).props.accessibilityState.selected).toBe(
      true,
    );
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
  });

  it('C1: Retry loads the home again and the banner goes away', async () => {
    jest.useFakeTimers();
    const rig = makeRig();
    rig.controller.loadHome
      .mockResolvedValueOnce({ ...ONLINE_HOME, error: true })
      .mockResolvedValueOnce(ONLINE_HOME);
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    await run(() => jest.advanceTimersByTimeAsync(MIN_RETRY_FEEDBACK_MS));

    expect(screen.queryByText('The service is unavailable and nothing was changed')).toBeNull();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(2);
  });

  it('C1: a home that cannot be loaded at all still offers Report Hazard and shows the banner', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockRejectedValue(SERVER_FAULT);
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);

    await renderApp(rig);

    expect(screen.getByText('The service is unavailable and nothing was changed')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Report Hazard' })).toBeTruthy();
  });

  it('tab bar: Home reloads the home; Profile is signposted as not in this version', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(2);

    await act(async () => fireEvent.press(screen.getByRole('tab', { name: /Profile/ })));
    expect(screen.getByText('Profile — not available in this version.')).toBeTruthy();
  });
});
