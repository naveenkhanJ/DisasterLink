import { fireEvent, screen } from '@testing-library/react-native';
import { makeRig, ONLINE_HOME, renderApp, run, ui } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

describe('the connection strip (standards §10 item 11, P213; UC2-7c R7, R9, R11)', () => {
  it('WF2P-S01: the strip says "Online" at the start, and the home has no Online tag of its own', async () => {
    await renderApp(makeRig());

    expect(screen.getAllByText('Online')).toHaveLength(1);
    expect(ui().connection).toBe('online');
  });

  it('WF2P-S01a: the saved copy keeps its note on the home; the strip does not claim "Offline"', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, fromCache: true });

    await renderApp(rig);

    expect(screen.getByText('Showing the copy saved on this phone')).toBeTruthy();
    expect(screen.queryByText('Offline')).toBeNull();
    expect(screen.getByRole('radio', { name: 'Tamil' }).props.accessibilityState.disabled).toBe(
      true,
    );
  });

  it('SC2 A8: when the phone goes offline the strip says "No internet connection" at once, and "Online" again when it returns', async () => {
    await renderApp(makeRig());

    await run(() => ui().onNetworkChange(false));
    expect(screen.getByText('No internet connection')).toBeTruthy();
    expect(screen.queryByText('Online')).toBeNull();
    await run(() => ui().onNetworkChange(true));

    expect(screen.getByText('Online')).toBeTruthy();
    expect(screen.queryByText('No internet connection')).toBeNull();
  });

  it('SC2 E4: a failed poll shows "Server not reachable", a successful one "Online"', async () => {
    await renderApp(makeRig());

    await run(() => ui().onServerReachability(false));
    expect(screen.getByText('Server not reachable')).toBeTruthy();
    expect(ui().connection).toBe('serverDown');
    await run(() => ui().onServerReachability(true));

    expect(screen.getByText('Online')).toBeTruthy();
    expect(ui().connection).toBe('online');
  });

  it('standards §10 item 11: the strip is drawn on every screen', async () => {
    await renderApp(makeRig());
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => ui().onNetworkChange(false));

    expect(screen.getByText('What did you see?')).toBeTruthy();
    expect(screen.getByText('No internet connection')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    expect(screen.getByText('No internet connection')).toBeTruthy();
  });

  it('standards §10 item 11: a phone that is offline stays "No internet connection" when a poll fails', async () => {
    await renderApp(makeRig());
    await run(() => ui().onNetworkChange(false));

    await run(() => ui().onServerReachability(false));

    expect(screen.getByText('No internet connection')).toBeTruthy();
  });

  it('standards §10 item 11: when the network returns the server counts as reachable until the next poll, so the strip does not flash', async () => {
    await renderApp(makeRig());
    await run(() => ui().onNetworkChange(false));
    await run(() => ui().onServerReachability(false));

    await run(() => ui().onNetworkChange(true));

    expect(screen.getByText('Online')).toBeTruthy();
    expect(screen.queryByText('Server not reachable')).toBeNull();
    await run(() => ui().onServerReachability(false));
    expect(screen.getByText('Server not reachable')).toBeTruthy();
  });

  it('SC2 E4 18i: the server coming back (serverDown to online) raises onConnectivityRestored once', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onServerReachability(false));
    expect(rig.controller.syncPending).not.toHaveBeenCalled();

    await run(() => ui().onServerReachability(true));
    await run(() => ui().onServerReachability(true));

    expect(rig.controller.syncPending).toHaveBeenCalledTimes(1);
  });

  it('SC2 E4 18i: the home is reloaded when the server comes back', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onServerReachability(false));
    const loads = rig.controller.loadHome.mock.calls.length;

    await run(() => ui().onServerReachability(true));

    expect(rig.controller.loadHome.mock.calls.length).toBe(loads + 1);
  });

  it('SC2 A2 18d: the phone coming back online (noInternet to online) is left to the connectivity triggers, not raised by the hook', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onNetworkChange(false));

    await run(() => ui().onNetworkChange(true));

    expect(rig.controller.syncPending).not.toHaveBeenCalled();
  });

  it('SC2 E4: serverDown, then no internet, then online again raises nothing from the hook', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onServerReachability(false));
    await run(() => ui().onNetworkChange(false));

    await run(() => ui().onNetworkChange(true));

    expect(rig.controller.syncPending).not.toHaveBeenCalled();
    expect(ui().connection).toBe('online');
  });

  it('standards §10 item 11: an unchanged state raises nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().onNetworkChange(true));
    await run(() => ui().onServerReachability(true));

    expect(rig.controller.syncPending).not.toHaveBeenCalled();
    expect(ui().connection).toBe('online');
  });
});
