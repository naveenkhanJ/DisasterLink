import { fireEvent, screen } from '@testing-library/react-native';
import { ApiError } from '../../api/httpClient';
import type { HazardReport } from '../../api/reportApi';
import type { HomeView } from '../../report/ReportDraft';
import { KALUTARA_BRIDGE, makeRig, ONLINE_HOME, renderApp, REPORT, run, ui } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const BANNER = 'The service is unavailable and nothing was changed';
const FINDING = 'Finding your location…';

/** A promise that the test settles by hand. */
function deferred<T>(): {
  promise: Promise<T>;
  resolve: (value: T) => void;
  reject: (error: unknown) => void;
} {
  let resolve: (value: T) => void = () => undefined;
  let reject: (error: unknown) => void = () => undefined;
  const promise = new Promise<T>((done, fail) => {
    resolve = done;
    reject = fail;
  });
  return { promise, resolve, reject };
}

describe('the loading row covers the server part of the home only (standards §10 item 4, P231; UC2-7k F2-a)', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('WF2P-S01 (F2-a): the row ends when the server part ends, while the fix is still being read', async () => {
    const rig = makeRig();
    const fix = deferred<typeof KALUTARA_BRIDGE | undefined>();
    rig.controller.captureLocation.mockReturnValue(fix.promise);

    await renderApp(rig);

    expect(ui().loading).toBe(false);
    expect(screen.queryByText('Loading…')).toBeNull();
    expect(ui().locating).toBe(true);
    await run(() => fix.resolve(KALUTARA_BRIDGE));
    expect(ui().locating).toBe(false);
  });

  it('WF2P-S01 (F2-a): the card reads "Finding your location…" while the first fix is pending, with no reason and no button, then the fix', async () => {
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('blocked');
    const fix = deferred<typeof KALUTARA_BRIDGE | undefined>();
    rig.controller.captureLocation.mockReturnValue(fix.promise);

    await renderApp(rig);

    expect(screen.getByText(FINDING)).toBeTruthy();
    expect(screen.queryByText('Current location unavailable')).toBeNull();
    expect(screen.queryByText(/^Allow location access/)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Allow location access' })).toBeNull();

    await run(() => fix.resolve(KALUTARA_BRIDGE));

    expect(screen.queryByText(FINDING)).toBeNull();
    expect(screen.getByText(/^Current location · /)).toBeTruthy();
  });

  it('WF2P-S01 (F2-a): a fix already shown stays during a new read', async () => {
    const rig = makeRig();
    await renderApp(rig);
    expect(screen.getByText(/^Current location · /)).toBeTruthy();
    const fix = deferred<typeof KALUTARA_BRIDGE | undefined>();
    rig.controller.captureLocation.mockReturnValue(fix.promise);

    await run(() => {
      void ui().openHome();
    });

    expect(screen.getByText(/^Current location · /)).toBeTruthy();
    expect(screen.queryByText(FINDING)).toBeNull();
    await run(() => fix.resolve(KALUTARA_BRIDGE));
  });

  it('WF2P-S01 (F2-a): when the read ends without a fix, the unavailable text and the button show', async () => {
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('blocked');
    const fix = deferred<typeof KALUTARA_BRIDGE | undefined>();
    rig.controller.captureLocation.mockReturnValue(fix.promise);
    await renderApp(rig);

    await run(() => fix.resolve(undefined));

    expect(screen.queryByText(FINDING)).toBeNull();
    expect(screen.getByText('Current location unavailable')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Allow location access' })).toBeTruthy();
  });

  it('WF2P-S01 (F2-a): pull-to-refresh is unchanged: it covers both parts with its own indicator and shows no row', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const home = deferred<HomeView>();
    rig.controller.loadHome.mockReturnValue(home.promise);

    await run(() => {
      void ui().refreshHomeScreen();
    });

    expect(ui().refreshing).toBe(true);
    expect(screen.queryByText('Loading…')).toBeNull();
    await run(() => home.resolve(ONLINE_HOME));
    expect(ui().refreshing).toBe(false);
  });
});

describe('the loading row shows only on the screen that started the load (standards §10 item 4, P231; UC2-7k F2-b)', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('WF2P-S02 (F2-b): a home load does not show the row on the hazard-type list, and shows it again on returning while it still runs', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const language = deferred<undefined>();
    rig.controller.changeLanguage.mockReturnValue(language.promise);
    await run(() => {
      void ui().selectLanguage('TAMIL');
    });
    expect(screen.getByText('Loading…')).toBeTruthy();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));

    expect(screen.getByText('What did you see?')).toBeTruthy();
    expect(screen.queryByText('Loading…')).toBeNull();
    expect(ui().loading).toBe(false);

    await run(() => {
      ui().goBack();
    });

    expect(screen.getByText('Active warnings')).toBeTruthy();
    expect(screen.getByText('Loading…')).toBeTruthy();
    await run(() => language.resolve(undefined));
    expect(screen.queryByText('Loading…')).toBeNull();
  });

  it('WF2P-S06 (F2-b): a status read does not show the row on the home', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const status = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValue(status.promise);
    await run(() => {
      void ui().openReportStatus(REPORT.reportId);
    });
    expect(screen.getByText('Loading…')).toBeTruthy();

    await run(() => {
      ui().goBack();
    });

    expect(screen.getByText('Active warnings')).toBeTruthy();
    expect(screen.queryByText('Loading…')).toBeNull();
    await run(() => status.resolve(REPORT));
  });

  it('WF2P-S06 (F2-b): the row of one report is not shown on the status of another', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const first = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValueOnce(first.promise);
    await run(() => {
      void ui().openReportStatus('RPT-A');
    });
    rig.controller.getReportStatus.mockResolvedValueOnce({ ...REPORT, reportId: 'RPT-B' });

    await run(() => ui().openReportStatus('RPT-B'));

    expect(screen.getByText('RPT-B')).toBeTruthy();
    expect(screen.queryByText('Loading…')).toBeNull();
    await run(() => first.resolve({ ...REPORT, reportId: 'RPT-A' }));
  });
});

describe('only the latest reply applies (standards §10 item 4, P231; UC2-7k F2-c)', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('WF2P-S01 (F2-c): an older home load that finishes last does not replace the newer view', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const older = deferred<HomeView>();
    const newer: HomeView = { ...ONLINE_HOME, myReports: [REPORT] };
    rig.controller.loadHome.mockReturnValueOnce(older.promise).mockResolvedValueOnce(newer);
    await run(() => {
      void ui().refreshHomeScreen();
    });

    await run(() => ui().openHome());
    await run(() => older.resolve({ ...ONLINE_HOME, myReports: [], fromCache: true }));

    expect(ui().home.myReports).toEqual([REPORT]);
    expect(ui().home.fromCache).toBe(false);
  });

  it("WF2P-S06 (F2-c): a late reply for report A does not appear on report B's status", async () => {
    const rig = makeRig();
    await renderApp(rig);
    const first = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValueOnce(first.promise);
    await run(() => {
      void ui().openReportStatus('RPT-A');
    });
    rig.controller.getReportStatus.mockResolvedValueOnce({ ...REPORT, reportId: 'RPT-B' });
    await run(() => ui().openReportStatus('RPT-B'));

    await run(() => first.resolve({ ...REPORT, reportId: 'RPT-A' }));

    expect(ui().report?.reportId).toBe('RPT-B');
    expect(screen.getByText('RPT-B')).toBeTruthy();
    expect(screen.queryByText('RPT-A')).toBeNull();
  });

  it('WF2P-S06 (F2-c): a reply for a report whose status was left does not come back', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const late = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValueOnce(late.promise);
    await run(() => {
      void ui().openReportStatus(REPORT.reportId);
    });
    await run(() => {
      ui().goBack();
    });

    await run(() => late.resolve(REPORT));

    expect(ui().report).toBeUndefined();
    expect(screen.getByText('Active warnings')).toBeTruthy();
  });

  it('C1 (F2-c): a superseded home load that fails raises no banner', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const older = deferred<HomeView>();
    rig.controller.loadHome.mockReturnValueOnce(older.promise).mockResolvedValueOnce(ONLINE_HOME);
    await run(() => {
      void ui().refreshHomeScreen();
    });
    await run(() => ui().openHome());

    await run(() => older.reject(new ApiError(500, 'The service is unavailable.')));

    expect(ui().serviceUnavailable).toBe(false);
    expect(screen.queryByText(BANNER)).toBeNull();
  });

  it('C1 (F2-c): a home load that is the latest and fails still raises the banner', async () => {
    const rig = makeRig();
    await renderApp(rig);
    rig.controller.loadHome.mockRejectedValueOnce(new ApiError(500, 'The service is unavailable.'));

    await run(() => ui().openHome());

    expect(screen.getByText(BANNER)).toBeTruthy();
  });

  it('C1 (F2-c): a superseded status read that fails raises no banner', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const first = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValueOnce(first.promise);
    await run(() => {
      void ui().openReportStatus('RPT-A');
    });
    rig.controller.getReportStatus.mockResolvedValueOnce({ ...REPORT, reportId: 'RPT-B' });
    await run(() => ui().openReportStatus('RPT-B'));

    await run(() => first.reject(new ApiError(503, 'Service Unavailable')));

    expect(screen.queryByText(BANNER)).toBeNull();
    expect(screen.getByText('RPT-B')).toBeTruthy();
  });

  it('C1 (F2-c): a status read that fails after the citizen left the screen raises no banner', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const late = deferred<HazardReport>();
    rig.controller.getReportStatus.mockReturnValueOnce(late.promise);
    await run(() => {
      void ui().openReportStatus(REPORT.reportId);
    });
    await run(() => {
      ui().goBack();
    });

    await run(() => late.reject(new ApiError(503, 'Service Unavailable')));

    expect(screen.queryByText(BANNER)).toBeNull();
  });
});
