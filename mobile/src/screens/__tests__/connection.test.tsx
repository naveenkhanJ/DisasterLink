import { fireEvent, render, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { ConnectionStrip } from '../ConnectionStrip';
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

  it('WF2P-S01a (R9): the saved copy is noted in the strip, not on the home; the strip does not claim "Offline"', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, fromCache: true });

    await renderApp(rig);

    expect(screen.getByText('· showing the saved copy')).toBeTruthy();
    expect(screen.queryByText('Showing the copy saved on this phone')).toBeNull();
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

const STATES = [
  ['online', 'Online'],
  ['noInternet', 'No internet connection'],
  ['serverDown', 'Server not reachable'],
] as const;

describe('the saved-copy note in the strip (standards §10 item 11, P229; UC2-7j R9)', () => {
  it.each(STATES)(
    'WF2P-S01a (R9): in the state %s the strip shows the note after the state when it is on',
    async (state, label) => {
      await render(<ConnectionStrip state={state} savedCopy />);

      expect(screen.getByText(label)).toBeTruthy();
      expect(screen.getByText('· showing the saved copy')).toBeTruthy();
      expect(screen.getByLabelText(`${label} · showing the saved copy`)).toBeTruthy();
    },
  );

  it.each(STATES)(
    'WF2P-S01a (R9): in the state %s the strip has no note when it is off',
    async (state, label) => {
      await render(<ConnectionStrip state={state} savedCopy={false} />);

      expect(screen.getByText(label)).toBeTruthy();
      expect(screen.queryByText('· showing the saved copy')).toBeNull();
      expect(screen.getByLabelText(label)).toBeTruthy();
    },
  );

  it('WF2P-S01a (R9): the note can shrink and wrap inside the strip', async () => {
    await render(<ConnectionStrip state="noInternet" savedCopy />);

    const note = screen.getByText('· showing the saved copy');

    expect(StyleSheet.flatten(note.props.style).flexShrink).toBe(1);
  });

  it('WF2P-S01a (R9): the note shows only on the home, and only while the home is the saved copy', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, fromCache: true });
    await renderApp(rig);
    expect(screen.getByText('· showing the saved copy')).toBeTruthy();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));

    expect(screen.queryByText('· showing the saved copy')).toBeNull();
  });

  it('WF2P-S01a (R9): the live home has no note', async () => {
    await renderApp(makeRig());

    expect(screen.queryByText('· showing the saved copy')).toBeNull();
  });
});

describe('the home reloads when the connection is lost (standards §10 item 11, P229; UC2-7j R8)', () => {
  const SAVED = { ...ONLINE_HOME, fromCache: true };

  it('SC2 A8 (R8): on the home Online to No internet reloads the home once, shows the saved copy and the note, and the C1 banner goes', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, error: true });
    await renderApp(rig);
    expect(screen.getByText('The service is unavailable and nothing was changed')).toBeTruthy();
    rig.controller.loadHome.mockClear();
    rig.controller.loadHome.mockResolvedValue(SAVED);

    await run(() => ui().onNetworkChange(false));

    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
    expect(screen.getByText('No internet connection')).toBeTruthy();
    expect(screen.getByText('· showing the saved copy')).toBeTruthy();
    expect(screen.queryByText('The service is unavailable and nothing was changed')).toBeNull();
  });

  it('SC2 E4 (R8): on the home Online to Server not reachable reloads the home once', async () => {
    const rig = makeRig();
    await renderApp(rig);
    rig.controller.loadHome.mockClear();
    rig.controller.loadHome.mockResolvedValue(SAVED);

    await run(() => ui().onServerReachability(false));

    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Server not reachable')).toBeTruthy();
    expect(screen.getByText('· showing the saved copy')).toBeTruthy();
  });

  it('SC2 A8 (R8): on the form nothing reloads when the connection is lost', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    rig.controller.loadHome.mockClear();

    await run(() => ui().onNetworkChange(false));

    expect(rig.controller.loadHome).not.toHaveBeenCalled();
  });

  it('SC2 A8 (R8): No internet to Server not reachable and back reload nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onNetworkChange(false));
    rig.controller.loadHome.mockClear();

    // While the phone is offline a failed or a successful poll leaves the strip at No internet.
    await run(() => ui().onServerReachability(false));
    await run(() => ui().onServerReachability(true));

    expect(ui().connection).toBe('noInternet');
    expect(rig.controller.loadHome).not.toHaveBeenCalled();
  });

  it('SC2 E4 18i (R8): the reconnect path is unchanged: serverDown to online syncs and reloads the home once', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onServerReachability(false));
    rig.controller.loadHome.mockClear();

    await run(() => ui().onServerReachability(true));

    expect(rig.controller.syncPending).toHaveBeenCalledTimes(1);
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
  });
});

describe('the strip sets the server holder before it syncs or reloads (standards §10 item 11, P231; UC2-7k F1)', () => {
  /** The call order of the holder's `setDown` calls, with their arguments. */
  const downCalls = (rig: ReturnType<typeof makeRig>) =>
    rig.serverState.setDown.mock.calls.map((call, index) => ({
      down: call[0],
      order: rig.serverState.setDown.mock.invocationCallOrder[index],
    }));

  it('SC2 E4 (F1): Server not reachable sets the holder down, and the return to Online clears it before the sync and the reload', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onServerReachability(false));
    expect(downCalls(rig).at(-1)?.down).toBe(true);
    rig.controller.loadHome.mockClear();

    await run(() => ui().onServerReachability(true));

    const cleared = downCalls(rig).at(-1);
    expect(cleared?.down).toBe(false);
    expect(cleared?.order).toBeLessThan(rig.controller.syncPending.mock.invocationCallOrder[0]);
    expect(cleared?.order).toBeLessThan(rig.controller.loadHome.mock.invocationCallOrder[0]);
  });

  it('SC2 E4 (F1): the holder is set down before the reload on the loss of the server', async () => {
    const rig = makeRig();
    await renderApp(rig);
    rig.controller.loadHome.mockClear();

    await run(() => ui().onServerReachability(false));

    expect(downCalls(rig).at(-1)?.down).toBe(true);
    expect(downCalls(rig).at(-1)?.order).toBeLessThan(
      rig.controller.loadHome.mock.invocationCallOrder[0],
    );
  });

  it('SC2 A2 (F1): No internet is not "down": the holder is clear, so the sync works whichever listener runs first', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onServerReachability(false));
    await run(() => ui().onNetworkChange(false));
    expect(downCalls(rig).at(-1)?.down).toBe(false);
    rig.controller.syncPending.mockClear();

    // The connectivity trigger's callback runs before the network listener reaches the hook.
    await run(() => ui().onConnectivityRestored());
    await run(() => ui().onNetworkChange(true));

    expect(rig.controller.syncPending).toHaveBeenCalledTimes(1);
    expect(downCalls(rig).at(-1)?.down).toBe(false);
  });

  it('SC2 A2 (F1): a poll that failed during No internet does not leave the holder down after the internet returns', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().onNetworkChange(false));
    await run(() => ui().onServerReachability(false));

    await run(() => ui().onNetworkChange(true));

    expect(ui().connection).toBe('online');
    expect(downCalls(rig).at(-1)?.down).toBe(false);
  });
});
