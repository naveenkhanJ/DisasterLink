import { fireEvent, screen } from '@testing-library/react-native';
import { ApiError } from '../../api/httpClient';
import { makeRig, ONLINE_HOME, renderApp, REPORT, run, ui } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const BANNER = 'The service is unavailable and nothing was changed';
const STILL = /^Still unavailable — tried at \d\d:\d\d$/;
const FAILING_HOME = { ...ONLINE_HOME, error: true };

/** The least time "Trying again…" stays, as `MIN_RETRY_FEEDBACK_MS` says (standards §10 item 12, P225). */
const THREE_SECONDS = 3_000;

/** A promise that the test settles by hand. */
function deferred<T>(): { promise: Promise<T>; resolve: (value: T) => void } {
  let resolve: (value: T) => void = () => undefined;
  const promise = new Promise<T>((done) => (resolve = done));
  return { promise, resolve };
}

/** Lets the fake clock run on, with the promises that are waiting settled. */
async function wait(ms: number): Promise<void> {
  await run(() => jest.advanceTimersByTimeAsync(ms));
}

describe('Retry on the service-unavailable banner (standards §10 item 12, P222, P225; UC2-7e E2, UC2-7h R2)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date(2026, 9, 16, 14, 7, 0));
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('C1 (E2, R2): while Retry runs the loading row shows and the button is disabled and reads "Trying again…"', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    const pending = deferred<typeof FAILING_HOME>();
    rig.controller.loadHome.mockReturnValue(pending.promise);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));

    const busy = screen.getByRole('button', { name: 'Trying again…' });
    expect(busy.props.accessibilityState.disabled).toBe(true);
    expect(screen.getByText('Loading…')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Retry' })).toBeNull();
    expect(ui().retrying).toBe(true);
  });

  it('C1 (R2): a retry whose fault remains keeps "Trying again…" for 3 seconds, then shows the line with the time of that moment', async () => {
    jest.setSystemTime(new Date(2026, 9, 16, 14, 7, 58));
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    await wait(THREE_SECONDS - 1);

    expect(screen.getByRole('button', { name: 'Trying again…' })).toBeTruthy();
    expect(screen.getByText('Loading…')).toBeTruthy();
    expect(screen.queryByText(STILL)).toBeNull();

    await wait(1);

    expect(screen.getByText('Still unavailable — tried at 14:08')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
    expect(screen.queryByText('Loading…')).toBeNull();
    expect(screen.getByText(BANNER)).toBeTruthy();
    expect(ui().retriedAt).toBeInstanceOf(Date);
    expect(ui().retrying).toBe(false);
  });

  it('C1 (R2): a retry that works removes the banner as soon as the call ends, without waiting for the 3 seconds', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    rig.controller.loadHome.mockResolvedValue(ONLINE_HOME);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));

    expect(screen.queryByText(BANNER)).toBeNull();
    expect(screen.getByText('Loading…')).toBeTruthy();
    await wait(THREE_SECONDS);
    expect(screen.queryByText('Loading…')).toBeNull();
    expect(screen.queryByText(STILL)).toBeNull();
    expect(ui().retriedAt).toBeUndefined();
    expect(ui().retrying).toBe(false);
  });

  it('C1 (R2): a failing call slower than 3 seconds shows the line when the call ends', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    const pending = deferred<typeof FAILING_HOME>();
    rig.controller.loadHome.mockReturnValue(pending.promise);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    await wait(THREE_SECONDS + 4_000);

    expect(screen.getByRole('button', { name: 'Trying again…' })).toBeTruthy();
    expect(screen.queryByText(STILL)).toBeNull();

    await run(() => pending.resolve(FAILING_HOME));

    expect(screen.getByText('Still unavailable — tried at 14:07')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
  });

  it('C1 (R2): a second press while Retry runs does nothing', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    rig.controller.loadHome.mockClear();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    await run(() => ui().retry());
    await wait(THREE_SECONDS);

    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
  });

  it('C1 (R2): the banner button is busy only for Retry, not for other loads', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    const pending = deferred<undefined>();
    rig.controller.changeLanguage.mockReturnValue(pending.promise);

    await run(() => {
      void ui().selectLanguage('SINHALA');
    });

    expect(ui().loading).toBe(true);
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Trying again…' })).toBeNull();
    await run(() => pending.resolve(undefined));
  });

  it('C1 (E2, R2): a retry that finds the fault still there adds "Still unavailable — tried at HH:MM" and the banner stays', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    expect(screen.queryByText(STILL)).toBeNull();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    await wait(THREE_SECONDS);

    expect(screen.getByText(BANNER)).toBeTruthy();
    expect(screen.getByText(STILL)).toBeTruthy();
  });

  it('C1 (E2, R2): on the report status a failed retry says so too, and a successful one shows the report', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus.mockRejectedValue(new ApiError(503, 'Service Unavailable'));
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    expect(screen.getByText(BANNER)).toBeTruthy();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    expect(screen.getByRole('button', { name: 'Trying again…' })).toBeTruthy();
    await wait(THREE_SECONDS);
    expect(screen.getByText(STILL)).toBeTruthy();
    expect(screen.queryByText('Loading…')).toBeNull();
    rig.controller.getReportStatus.mockResolvedValue(REPORT);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    await wait(THREE_SECONDS);

    expect(screen.queryByText(BANNER)).toBeNull();
    expect(screen.queryByText(STILL)).toBeNull();
    expect(screen.getByText('Report status')).toBeTruthy();
  });

  it('C1 (E2, R2): the loading row shows while a status retry runs', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus.mockRejectedValue(new ApiError(503, 'Service Unavailable'));
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    const pending = deferred<typeof REPORT>();
    rig.controller.getReportStatus.mockReturnValue(pending.promise);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));

    expect(screen.getByText('Loading…')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Trying again…' })).toBeTruthy();
    await run(() => pending.resolve(REPORT));
    await wait(THREE_SECONDS);
    expect(screen.queryByText('Loading…')).toBeNull();
  });

  it('C1 (E2): the old time does not come back with a later banner', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue(FAILING_HOME);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retry' })));
    await wait(THREE_SECONDS);
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
    await wait(THREE_SECONDS);
    rig.controller.loadHome.mockResolvedValue(ONLINE_HOME);

    await run(() => ui().openHome());

    expect(screen.queryByText(BANNER)).toBeNull();
    expect(ui().retriedAt).toBeUndefined();
  });
});
