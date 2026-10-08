import { screen } from '@testing-library/react-native';
import { ApiError, NetworkError } from '../../api/httpClient';
import type { HazardReport } from '../../api/reportApi';
import { makeRig, ONLINE_HOME, renderApp, REPORT, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const NOTE = '· showing the saved copy';
const LOADS_ONLINE = 'This report loads when you are online';
const BANNER = 'The service is unavailable and nothing was changed';

/** The same report after the officer's decision, as the server would answer it. */
const VERIFIED: HazardReport = { ...REPORT, status: 'VERIFIED' };

/** A home whose "My reports" holds the report, so the copy has it. */
const HOME_WITH_REPORT = { ...ONLINE_HOME, myReports: [REPORT] };

/** The saved copy of that home, as an offline load gives it. */
const SAVED_HOME = { ...HOME_WITH_REPORT, fromCache: true };

/** A promise that the test settles by hand. */
function deferred<T>(): { promise: Promise<T>; resolve: (value: T) => void } {
  let resolve: (value: T) => void = () => undefined;
  const promise = new Promise<T>((done) => (resolve = done));
  return { promise, resolve };
}

/** A rig whose home holds the report. */
function rigWithReport(): Rig {
  const rig = makeRig();
  rig.controller.loadHome.mockResolvedValue(HOME_WITH_REPORT);
  return rig;
}

/** Takes the phone offline on the home: the strip changes and the home reloads from the saved copy. */
async function goOffline(rig: Rig): Promise<void> {
  rig.controller.loadHome.mockResolvedValue(SAVED_HOME);
  await run(() => ui().onNetworkChange(false));
}

describe('the report status offline (standards §10 item 17, deviation D-UC2-6, P231; UC2-7k F3)', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('WF2P-S06 (F3): offline, opening a report from the home shows the copy with the strip note, makes no request and raises no banner', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await goOffline(rig);

    await run(() => ui().openReportStatus(REPORT.reportId));

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText('Report status')).toBeTruthy();
    expect(screen.getByText(REPORT.reportId)).toBeTruthy();
    expect(screen.getByText('Pending verification')).toBeTruthy();
    expect(screen.getByText(NOTE)).toBeTruthy();
    expect(screen.queryByText(BANNER)).toBeNull();
    expect(ui().reportFromCopy).toBe(true);
  });

  it('WF2P-S06 (F3): offline, a report that is not in the copy gives the title, the ID and "This report loads when you are online", with no banner and no Retry', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await goOffline(rig);

    await run(() => ui().openReportStatus('RPT-2026-XXX-0001'));

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText('Report status')).toBeTruthy();
    expect(screen.getByText('Report ID')).toBeTruthy();
    expect(screen.getByText('RPT-2026-XXX-0001')).toBeTruthy();
    expect(screen.getByText(LOADS_ONLINE)).toBeTruthy();
    expect(screen.queryByText(BANNER)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Retry' })).toBeNull();
    expect(screen.queryByText(NOTE)).toBeNull();
    expect(ui().reportMissing).toBe(true);
  });

  it("WF2P-S06 (F3): offline, from S05 the report is the submission's report", async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().startReport());
    await run(() => ui().submitReport());
    await run(() => ui().onNetworkChange(false));

    await run(() => ui().openReportStatusFrom(REPORT.reportId, 'submissionResult'));

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText(REPORT.reportId)).toBeTruthy();
    expect(screen.getByText(NOTE)).toBeTruthy();
    expect(screen.queryByText(LOADS_ONLINE)).toBeNull();
  });

  it('WF2P-S06 (F3): with the server not reachable the same happens, with no request', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    rig.controller.loadHome.mockResolvedValue(SAVED_HOME);
    await run(() => ui().onServerReachability(false));

    await run(() => ui().openReportStatus(REPORT.reportId));

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText(NOTE)).toBeTruthy();
    expect(screen.getByText('Server not reachable')).toBeTruthy();
    expect(screen.queryByText(BANNER)).toBeNull();
  });

  it('WF2P-S06 (F3): online with a NetworkError (unreachable or timeout) the copy is used, with the note and no banner', async () => {
    const rig = rigWithReport();
    rig.controller.getReportStatus.mockRejectedValue(new NetworkError('timeout'));
    await renderApp(rig);

    await run(() => ui().openReportStatus(REPORT.reportId));

    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(1);
    expect(screen.getByText(REPORT.reportId)).toBeTruthy();
    expect(screen.getByText(NOTE)).toBeTruthy();
    expect(screen.queryByText(BANNER)).toBeNull();
  });

  it('WF2P-S06 (C1, F3): online with a 5xx the C1 banner and Retry stay (unchanged)', async () => {
    const rig = rigWithReport();
    rig.controller.getReportStatus.mockRejectedValue(new ApiError(503, 'Service Unavailable'));
    await renderApp(rig);

    await run(() => ui().openReportStatus(REPORT.reportId));

    expect(screen.getByText(BANNER)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
    expect(screen.queryByText(NOTE)).toBeNull();
  });

  it('WF2P-S06 (F3): online a live read shows the report with no note', async () => {
    const rig = rigWithReport();
    await renderApp(rig);

    await run(() => ui().openReportStatus(REPORT.reportId));

    expect(screen.getByText(REPORT.reportId)).toBeTruthy();
    expect(screen.queryByText(NOTE)).toBeNull();
    expect(ui().reportFromCopy).toBe(false);
    expect(ui().reportMissing).toBe(false);
  });

  it('WF2P-S06 (F3): offline pull-to-refresh makes no request', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await goOffline(rig);
    await run(() => ui().openReportStatus(REPORT.reportId));

    await run(() => ui().refreshReportStatus());

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText(NOTE)).toBeTruthy();
    expect(ui().refreshing).toBe(false);
  });

  it('WF2P-S06 (F3): pull-to-refresh stays available on the "loads when you are online" state, and offline it makes no request', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await goOffline(rig);
    await run(() => ui().openReportStatus('RPT-2026-XXX-0001'));

    await run(() => screen.getByTestId('screen-scroll').props.refreshControl.props.onRefresh());

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText(LOADS_ONLINE)).toBeTruthy();
  });

  it('WF2P-S06 (F3): reconnecting with a status open reads it live once; the copy stays until the live report arrives, then the note goes', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await goOffline(rig);
    await run(() => ui().openReportStatus(REPORT.reportId));
    const live = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValue(live.promise);
    rig.controller.loadHome.mockResolvedValue(HOME_WITH_REPORT);

    // The connectivity trigger runs ahead of the strip, as it can.
    await run(() => {
      void ui().onConnectivityRestored();
    });
    await run(() => ui().onNetworkChange(true));

    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Loading…')).toBeTruthy();
    expect(screen.getByText('Pending verification')).toBeTruthy();
    expect(screen.getByText(NOTE)).toBeTruthy();

    await run(() => live.resolve(VERIFIED));

    expect(screen.getByText('Verified')).toBeTruthy();
    expect(screen.queryByText(NOTE)).toBeNull();
    expect(screen.queryByText('Loading…')).toBeNull();
  });

  it('WF2P-S06 (F3): the server coming back with a status open reads it live once', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    rig.controller.loadHome.mockResolvedValue(SAVED_HOME);
    await run(() => ui().onServerReachability(false));
    await run(() => ui().openReportStatus(REPORT.reportId));
    rig.controller.getReportStatus.mockResolvedValue(VERIFIED);
    rig.controller.loadHome.mockResolvedValue(HOME_WITH_REPORT);

    await run(() => ui().onServerReachability(true));

    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Verified')).toBeTruthy();
    expect(screen.queryByText(NOTE)).toBeNull();
  });

  it('WF2P-S06 (F3): the connectivity trigger while the server is not reachable reads the status from the copy, with no request', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    rig.controller.loadHome.mockResolvedValue(SAVED_HOME);
    await run(() => ui().onServerReachability(false));
    await run(() => ui().openReportStatus(REPORT.reportId));

    await run(() => ui().onConnectivityRestored());

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText(NOTE)).toBeTruthy();
  });

  it('WF2P-S06 (F3): losing the connection with a live report shown keeps it, with no note and no new request', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await run(() => ui().openReportStatus(REPORT.reportId));
    rig.controller.getReportStatus.mockClear();

    await run(() => ui().onNetworkChange(false));

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.getByText(REPORT.reportId)).toBeTruthy();
    expect(screen.queryByText(NOTE)).toBeNull();
    expect(screen.getByText('No internet connection')).toBeTruthy();
  });

  it('WF2P-S06 (F3): losing the connection with the C1 banner up replaces it with the copy, and the banner goes', async () => {
    const rig = rigWithReport();
    rig.controller.getReportStatus.mockRejectedValue(new ApiError(503, 'Service Unavailable'));
    await renderApp(rig);
    await run(() => ui().openReportStatus(REPORT.reportId));
    expect(screen.getByText(BANNER)).toBeTruthy();
    rig.controller.getReportStatus.mockClear();

    await run(() => ui().onNetworkChange(false));

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    expect(screen.queryByText(BANNER)).toBeNull();
    expect(screen.getByText(REPORT.reportId)).toBeTruthy();
    expect(screen.getByText(NOTE)).toBeTruthy();
  });

  it('WF2P-S06 (F3): losing the connection while the first read is still running shows the copy, and the late live reply is ignored', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    const live = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValue(live.promise);
    await run(() => {
      void ui().openReportStatus(REPORT.reportId);
    });

    await run(() => ui().onNetworkChange(false));
    expect(screen.getByText(NOTE)).toBeTruthy();
    await run(() => live.resolve(VERIFIED));

    expect(screen.getByText('Pending verification')).toBeTruthy();
    expect(screen.getByText(NOTE)).toBeTruthy();
  });

  it('WF2P-S06 (F3): the decision for the open report re-reads it online (unchanged)', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await run(() => ui().openReportStatus(REPORT.reportId));
    rig.controller.getReportStatus.mockResolvedValue(VERIFIED);

    await run(() =>
      ui().receiveReportDecision({
        reportId: REPORT.reportId,
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:00:00+05:30',
      }),
    );

    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(2);
    expect(screen.getByText('Verified')).toBeTruthy();
  });

  it('WF2P-S06 (F3): leaving the status screen and opening another one starts clean', async () => {
    const rig = rigWithReport();
    await renderApp(rig);
    await goOffline(rig);
    await run(() => ui().openReportStatus('RPT-2026-XXX-0001'));
    expect(screen.getByText(LOADS_ONLINE)).toBeTruthy();

    await run(() => ui().openReportStatus(REPORT.reportId));

    expect(screen.queryByText(LOADS_ONLINE)).toBeNull();
    expect(screen.getByText(NOTE)).toBeTruthy();
  });
});
