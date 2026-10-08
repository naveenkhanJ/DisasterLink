import { fireEvent, screen } from '@testing-library/react-native';
import { ApiError, NetworkError } from '../../api/httpClient';
import { makeRig, ONLINE_HOME, renderApp, REPORT, run, ui } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const VERIFIED = {
  ...REPORT,
  status: 'VERIFIED' as const,
  verificationTime: '2026-05-21T08:15:00+05:30',
  hazardId: 'HZ-2026-0142',
};

describe('report status', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  it('WF2P-S06 / SC2 steps 31-32: a verified report shows its ID, type, capture time, status and decision time', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, myReports: [VERIFIED] });
    rig.controller.getReportStatus.mockResolvedValue(VERIFIED);
    await renderApp(rig);

    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Open report RPT-2026-KLT-0847' })),
    );

    expect(screen.getByText('Report status')).toBeTruthy();
    expect(screen.getByText('RPT-2026-KLT-0847')).toBeTruthy();
    expect(screen.getByText('Flood')).toBeTruthy();
    expect(screen.getByText('21 May 2026, 07:40')).toBeTruthy();
    expect(screen.getByText('Verified')).toBeTruthy();
    expect(screen.getByText('Decision time')).toBeTruthy();
    expect(screen.getByText('21 May 2026, 08:15')).toBeTruthy();
    expect(screen.queryByText('Reason')).toBeNull();
  });

  it('WF2P-S06a / SC2 A4 25h: a rejected report shows Rejected with the reason as typed', async () => {
    const rig = makeRig();
    const reason = 'Duplicate of RPT-2026-KLT-0843 — please report only new changes.';
    rig.controller.getReportStatus.mockResolvedValue({
      ...VERIFIED,
      status: 'REJECTED',
      rejectionReason: reason,
    });
    await renderApp(rig);

    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));

    expect(screen.getByText('Rejected')).toBeTruthy();
    expect(screen.getByText('Reason')).toBeTruthy();
    expect(screen.getByText(reason)).toBeTruthy();
    expect(screen.getByText('21 May 2026, 08:15')).toBeTruthy();
  });

  it('WF2P-S06 / SC2 step 32: a report that is not decided yet shows Pending verification and no decision time', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));

    expect(screen.getByText('Pending verification')).toBeTruthy();
    expect(screen.queryByText('Decision time')).toBeNull();
  });

  it('WF2P-S06 (C1): a server fault shows the common error banner, and Retry reads the report again', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus
      .mockRejectedValueOnce(new ApiError(503, 'Service Unavailable'))
      .mockResolvedValueOnce(VERIFIED);
    await renderApp(rig);

    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    expect(screen.getByText('The service is unavailable and nothing was changed')).toBeTruthy();
    expect(screen.queryByText('Report status')).toBeNull();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));

    expect(screen.queryByText('The service is unavailable and nothing was changed')).toBeNull();
    expect(screen.getByText('Verified')).toBeTruthy();
    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(2);
  });

  it('WF2P-S06 (C1): an unreachable server shows the banner too', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus.mockRejectedValue(new NetworkError('offline'));
    await renderApp(rig);

    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));

    expect(screen.getByText('The service is unavailable and nothing was changed')).toBeTruthy();
  });

  it('WF2P-S06: the tab Home goes back to the home; the tab Report starts a new report', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));
    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();

    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Report/ })));
    expect(screen.getByText('What did you see?')).toBeTruthy();
  });
});
