import { fireEvent, screen } from '@testing-library/react-native';
import { ApiError } from '../../api/httpClient';
import type { HomeView } from '../../report/ReportDraft';
import { makeRig, ONLINE_HOME, renderApp, REPORT, run, ui, WARNING } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const BANNER = 'The service is unavailable and nothing was changed';

/** The warning after the officer escalated it to Level 3. */
const ESCALATED = { ...WARNING, warningLevel: 'LEVEL_3_RED' as const };

/** The decision notice for the phone's report. */
const VERIFIED_NOTICE = {
  reportId: REPORT.reportId,
  status: 'VERIFIED' as const,
  decidedAt: '2026-05-21T08:15:00+05:30',
};

/** A home with the warning at Level 2 and the report pending. */
const BEFORE: HomeView = { ...ONLINE_HOME, warnings: [WARNING], myReports: [REPORT] };

/** The same home after the escalation. */
const AFTER_ALERT: HomeView = { ...BEFORE, warnings: [ESCALATED] };

/** The same home after the decision. */
const AFTER_DECISION: HomeView = { ...BEFORE, myReports: [{ ...REPORT, status: 'VERIFIED' }] };

describe('the home follows alerts and decisions (standards §10 item 5, P237; UC2-7l R1)', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('SC2 step 2 (R1): on the home an alert reloads the home once, and Active warnings shows the new level without a pull', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(BEFORE);
    await renderApp(rig);
    expect(screen.getAllByText('Level 2 – Amber (Be Alert)').length).toBeGreaterThan(0);
    rig.controller.loadHome.mockClear();
    rig.controller.loadHome.mockResolvedValue(AFTER_ALERT);

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Evacuate now'));

    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
    expect(ui().home.warnings[0].warningLevel).toBe('LEVEL_3_RED');
    expect(ui().refreshing).toBe(false);
  });

  it('SC2 step 2 (R1): the reload shows no loading row', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(BEFORE);
    await renderApp(rig);
    let finish: (view: HomeView) => void = () => undefined;
    rig.controller.loadHome.mockReturnValue(new Promise<HomeView>((done) => (finish = done)));

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Evacuate now'));

    expect(ui().loading).toBe(false);
    expect(screen.queryByText('Loading…')).toBeNull();
    await run(() => finish(AFTER_ALERT));
  });

  it('SC2 step 30 (R1): on the home a decision reloads the home once, and My reports shows the new status', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(BEFORE);
    await renderApp(rig);
    expect(screen.getByText('Pending verification')).toBeTruthy();
    rig.controller.loadHome.mockClear();
    rig.controller.loadHome.mockResolvedValue(AFTER_DECISION);

    await run(() => ui().receiveReportDecision(VERIFIED_NOTICE));

    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Verified')).toBeTruthy();
    expect(screen.queryByText('Pending verification')).toBeNull();
  });

  it('SC1 step 15 (R1): on the report form an alert reloads nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    rig.controller.loadHome.mockClear();

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Evacuate now'));
    await run(() => ui().receiveReportDecision(VERIFIED_NOTICE));

    expect(rig.controller.loadHome).not.toHaveBeenCalled();
    expect(screen.getByText('Warning alert')).toBeTruthy();
  });

  it('SC1 step 15 (R1): on a status screen an alert reloads nothing, and the status is re-read only on its own decision', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(BEFORE);
    await renderApp(rig);
    await run(() => ui().openReportStatus(REPORT.reportId));
    rig.controller.loadHome.mockClear();
    rig.controller.getReportStatus.mockClear();

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Evacuate now'));
    expect(rig.controller.loadHome).not.toHaveBeenCalled();
    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();

    await run(() => ui().receiveReportDecision(VERIFIED_NOTICE));

    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(1);
    expect(rig.controller.loadHome).not.toHaveBeenCalled();
  });

  it('SC1 step 15 (R1): a decision for another report on a status screen reloads nothing either', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().openReportStatus(REPORT.reportId));
    rig.controller.loadHome.mockClear();
    rig.controller.getReportStatus.mockClear();

    await run(() => ui().receiveReportDecision({ ...VERIFIED_NOTICE, reportId: 'RPT-OTHER' }));

    expect(rig.controller.loadHome).not.toHaveBeenCalled();
    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
  });

  it('SC1 step 15 (R1): several alerts in one batch may reload more than once, and the home shows the latest view', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(BEFORE);
    await renderApp(rig);
    rig.controller.loadHome.mockClear();
    rig.controller.loadHome.mockResolvedValueOnce(BEFORE).mockResolvedValue(AFTER_ALERT);

    await run(() => {
      ui().receiveAlert(WARNING.warningId, 'LEVEL_2_AMBER', 'first');
      ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'second');
    });

    expect(rig.controller.loadHome.mock.calls.length).toBeGreaterThanOrEqual(1);
    expect(ui().home.warnings[0].warningLevel).toBe('LEVEL_3_RED');
  });

  it('C1 (R1): a reload that fails shows the C1 banner as any home load does', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(BEFORE);
    await renderApp(rig);
    rig.controller.loadHome.mockRejectedValue(new ApiError(500, 'The service is unavailable.'));

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Evacuate now'));

    expect(screen.getByText(BANNER)).toBeTruthy();
  });

  it('SC2 A8 (R1): offline the reload reads the saved copy as usual, with the strip note', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(BEFORE);
    await renderApp(rig);
    rig.controller.loadHome.mockResolvedValue({ ...BEFORE, fromCache: true });
    await run(() => ui().onNetworkChange(false));
    rig.controller.loadHome.mockClear();

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Evacuate now'));

    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
    expect(screen.getByText('· showing the saved copy')).toBeTruthy();
    expect(screen.queryByText(BANNER)).toBeNull();
  });
});
