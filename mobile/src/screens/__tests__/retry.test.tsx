import { act, fireEvent, screen } from '@testing-library/react-native';
import { ApiError } from '../../api/httpClient';
import { makeRig, ONLINE_HOME, renderApp, REPORT, run, ui } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const BANNER = 'The service is unavailable and nothing was changed';
const STILL = /^Still unavailable — tried at \d\d:\d\d$/;
const FAILING_HOME = { ...ONLINE_HOME, error: true };

/** A promise that the test settles by hand. */
function deferred<T>(): { promise: Promise<T>; resolve: (value: T) => void } {
  let resolve: (value: T) => void = () => undefined;
  const promise = new Promise<T>((done) => (resolve = done));
  return { promise, resolve };
}

describe('Retry on the service-unavailable banner (standards §10 item 12, P222; UC2-7e E2)', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('C1 (E2): while Retry runs the loading row shows and the button is disabled and reads "Trying again…"', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    const pending = deferred<typeof FAILING_HOME>();
    rig.controller.loadHome.mockReturnValue(pending.promise);

    await act(async () => {
      fireEvent.press(screen.getByRole('button', { name: 'Retry' }));
    });

    const busy = screen.getByRole('button', { name: 'Trying again…' });
    expect(busy.props.accessibilityState.disabled).toBe(true);
    expect(screen.getByText('Loading…')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Retry' })).toBeNull();
    await run(() => pending.resolve(FAILING_HOME));
    expect(screen.queryByText('Loading…')).toBeNull();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
  });

  it('C1 (E2): a retry that finds the fault still there adds "Still unavailable — tried at HH:MM" and the banner stays', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    expect(screen.queryByText(STILL)).toBeNull();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));

    expect(screen.getByText(BANNER)).toBeTruthy();
    expect(screen.getByText(STILL)).toBeTruthy();
    expect(ui().retriedAt).toBeInstanceOf(Date);
  });

  it('C1 (E2): a successful retry removes the banner and the line', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    expect(screen.getByText(STILL)).toBeTruthy();
    rig.controller.loadHome.mockResolvedValue(ONLINE_HOME);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));

    expect(screen.queryByText(BANNER)).toBeNull();
    expect(screen.queryByText(STILL)).toBeNull();
    expect(ui().retriedAt).toBeUndefined();
  });

  it('C1 (E2): on the report status a failed retry says so too, and a successful one shows the report', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus.mockRejectedValue(new ApiError(503, 'Service Unavailable'));
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    expect(screen.getByText(BANNER)).toBeTruthy();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    expect(screen.getByText(STILL)).toBeTruthy();
    expect(screen.queryByText('Loading…')).toBeNull();
    rig.controller.getReportStatus.mockResolvedValue(REPORT);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));

    expect(screen.queryByText(BANNER)).toBeNull();
    expect(screen.queryByText(STILL)).toBeNull();
    expect(screen.getByText('Report status')).toBeTruthy();
  });

  it('C1 (E2): the loading row shows while a status retry runs', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus.mockRejectedValue(new ApiError(503, 'Service Unavailable'));
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    const pending = deferred<typeof REPORT>();
    rig.controller.getReportStatus.mockReturnValue(pending.promise);

    await act(async () => {
      fireEvent.press(screen.getByRole('button', { name: 'Retry' }));
    });

    expect(screen.getByText('Loading…')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Trying again…' })).toBeTruthy();
    await run(() => pending.resolve(REPORT));
    expect(screen.queryByText('Loading…')).toBeNull();
  });

  it('C1 (E2): the old time does not come back with a later banner', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    expect(screen.getByText(STILL)).toBeTruthy();

    await run(() => ui().refreshHomeScreen());

    expect(screen.getByText(BANNER)).toBeTruthy();
    expect(screen.queryByText(STILL)).toBeNull();
  });

  it('C1 (E2): the line is not shown once the banner is gone, even if the time is still held', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    rig.controller.loadHome.mockResolvedValue(ONLINE_HOME);

    await run(() => ui().openHome());

    expect(screen.queryByText(BANNER)).toBeNull();
    expect(ui().retriedAt).toBeUndefined();
  });
});
