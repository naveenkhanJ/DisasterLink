import { fireEvent, screen } from '@testing-library/react-native';
import { KALUTARA_BRIDGE, makeRig, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const ALLOW = 'Allow location access to use your current location';

/** Home → Report Hazard (WF2P-S02). */
async function toList(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
}

/** Home → Report Hazard → Flood (WF2P-S03). */
async function toForm(rig: Rig): Promise<void> {
  await toList(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

describe('returning to the foreground re-reads the location (standards §10 item 8; UC2-7b G1)', () => {
  it('WF2P-S03g: back from Settings with the permission granted, the form fills the location and drops the notice', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('blocked');
    await toForm(rig);
    expect(screen.getByText(ALLOW)).toBeTruthy();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA_BRIDGE);
    rig.locationAccess.check.mockResolvedValue('granted');

    await run(() => ui().onAppForeground());

    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText('GPS · accuracy ±8 m')).toBeTruthy();
    expect(screen.queryByText(ALLOW)).toBeNull();
    expect(ui().draft?.locationFromGps).toBe(true);
    expect(ui().locationUnavailable).toBe(false);
  });

  it('WF2P-S03g: back from Settings with Location Services still off, the notice stays and is read again', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('blocked');
    await toForm(rig);
    rig.locationAccess.check.mockResolvedValue('servicesOff');

    await run(() => ui().onAppForeground());

    expect(screen.getByText('Turn on Location Services to use your current location')).toBeTruthy();
    expect(screen.queryByText(ALLOW)).toBeNull();
  });

  it('WF2P-S01: on the home, the current location appears after the permission was granted in Settings', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('blocked');
    await renderApp(rig);
    expect(screen.getByText(`Current location · ${ALLOW}`)).toBeTruthy();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA_BRIDGE);

    await run(() => ui().onAppForeground());

    expect(screen.getByText('Current location · 6.5888 N, 79.9640 E')).toBeTruthy();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
  });

  it.each([
    ['the hazard-type list', toList],
    ['the review screen', reachReview],
    ['a report status', reachStatus],
  ])(
    'standards §10 item 8: on %s, returning to the foreground makes no capture',
    async (_name, reach) => {
      const rig = makeRig();
      await reach(rig);
      const reads = rig.controller.captureLocation.mock.calls.length;

      await run(() => ui().onAppForeground());

      expect(rig.controller.captureLocation.mock.calls.length).toBe(reads);
    },
  );

  it('standards §10 item 8: a draft that already has a location makes no capture', async () => {
    const rig = makeRig();
    await toForm(rig);
    expect(ui().draft?.location).toEqual(KALUTARA_BRIDGE);
    const reads = rig.controller.captureLocation.mock.calls.length;

    await run(() => ui().onAppForeground());

    expect(rig.controller.captureLocation.mock.calls.length).toBe(reads);
  });

  it('standards §10 item 8: a form that is still waiting for its first fix makes no second capture', async () => {
    const rig = makeRig();
    await toList(rig);
    rig.controller.captureLocation.mockImplementation(() => new Promise(() => undefined));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    expect(screen.getByText('Finding your location…')).toBeTruthy();
    const reads = rig.controller.captureLocation.mock.calls.length;

    await run(() => ui().onAppForeground());

    expect(rig.controller.captureLocation.mock.calls.length).toBe(reads);
  });
});

async function reachReview(rig: Rig): Promise<void> {
  await toForm(rig);
  await run(() => fireEvent.changeText(screen.getByLabelText('Description *'), 'Water is rising'));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
  expect(screen.getByText('Review your report')).toBeTruthy();
}

async function reachStatus(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
  expect(screen.getByText('Report status')).toBeTruthy();
}
