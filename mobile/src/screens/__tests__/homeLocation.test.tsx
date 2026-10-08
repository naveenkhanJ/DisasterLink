import { fireEvent, render, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { ShowHome } from '../ShowHome';
import { makeRig, ONLINE_HOME, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

/** A GPS fix in Kalutara with an accuracy: a place lies within 10 km. */
const KALUTARA = { latitude: 6.5888, longitude: 79.964, accuracyMetres: 8 };

/** A GPS fix in Kalutara with no accuracy. */
const KALUTARA_NO_ACCURACY = { latitude: 6.5888, longitude: 79.964 };

/** A point inside Hambantota with no place within 10 km (found on the real data). */
const REMOTE = { latitude: 6.45, longitude: 81.6, accuracyMetres: 5 };

const styleOf = (element: { props: { style?: unknown } } | null | undefined) =>
  StyleSheet.flatten(element?.props.style as never) as Record<string, unknown>;

async function toForm(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

describe('the nearest place under a GPS fix (standards §10, Nearest-place label, P222; UC2-7e G10)', () => {
  it('WF2P-S01 (G10): the home card says "GPS · ±8 m · near <place>" when a place is within 10 km', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA);

    await renderApp(rig);

    expect(screen.getByText('Current location · 6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText(/^GPS · ±8 m · near .+$/)).toBeTruthy();
  });

  it('WF2P-S01 (G10): without an accuracy the home line is "GPS · near <place>"', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA_NO_ACCURACY);

    await renderApp(rig);

    expect(screen.getByText(/^GPS · near .+$/)).toBeTruthy();
  });

  it('WF2P-S01 (G10): with no place within 10 km the home line has no "near" part', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(REMOTE);

    await renderApp(rig);

    expect(screen.getByText('GPS · accuracy ±5 m')).toBeTruthy();
    expect(screen.queryByText(/ near /)).toBeNull();
  });

  it('WF2P-S01 (G10): with no place and no accuracy the home line is just "GPS"', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue({ latitude: 6.45, longitude: 81.6 });

    await renderApp(rig);

    expect(screen.getByText('GPS')).toBeTruthy();
  });

  it('WF2P-S03 (G10): the form says "GPS · ±8 m · near <place>" under a GPS fix', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA);

    await toForm(rig);

    expect(screen.getByText(/^GPS · ±8 m · near .+$/)).toBeTruthy();
    expect(ui().draft?.locationFromGps).toBe(true);
  });

  it('WF2P-S03 (G10): the form says "GPS · near <place>" without an accuracy', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA_NO_ACCURACY);

    await toForm(rig);

    expect(screen.getByText(/^GPS · near .+$/)).toBeTruthy();
  });

  it('WF2P-S03 (G10): the form has no "near" part when no place is within 10 km', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(REMOTE);

    await toForm(rig);

    expect(screen.getByText('GPS · accuracy ±5 m')).toBeTruthy();
    expect(screen.queryByText(/ near /)).toBeNull();
  });

  it('SC2 step 14: the review screen keeps the plain "GPS · accuracy ±8 m" (the nearest place is for the form and the home)', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA);
    await toForm(rig);
    await run(() => fireEvent.changeText(screen.getByLabelText('Description *'), 'Water'));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('GPS · accuracy ±8 m')).toBeTruthy();
  });
});

/** Draws the home with no fix and a blocked permission, for the app name given. */
async function renderBlockedHome(appName: string): Promise<void> {
  await render(
    <ShowHome
      view={ONLINE_HOME}
      currentLocation={undefined}
      locationAccess="blocked"
      appName={appName}
      locating={false}
      draftDiscarded={false}
      onSelectLanguage={jest.fn()}
      onAllowLocationAccess={jest.fn()}
      onReportHazard={jest.fn()}
      onOpenReport={jest.fn()}
      onOpenDraft={jest.fn()}
      onDismissDiscarded={jest.fn()}
    />,
  );
}

describe('the Settings line names the host app (standards §10 item 14, P225; UC2-7h R1)', () => {
  it('WF2P-S01 (R1): in Expo Go the blocked line says "Expo Go"', async () => {
    await renderBlockedHome('Expo Go');

    expect(screen.getByText('Allow location access for Expo Go in Settings')).toBeTruthy();
  });

  it('WF2P-S01 (R1): in the app itself the blocked line says "DisasterLink"', async () => {
    await renderBlockedHome('DisasterLink');

    expect(screen.getByText('Allow location access for DisasterLink in Settings')).toBeTruthy();
    expect(screen.queryByText(/Expo Go/)).toBeNull();
  });

  it('WF2P-S01 (R1): the app passes the name that hostAppName gives (under jest-expo, not Expo Go)', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('blocked');

    await renderApp(rig);

    expect(screen.getByText(/^Allow location access for .+ in Settings$/)).toBeTruthy();
  });
});

describe('the home (standards §10 items 14 and 15, P222; UC2-7e E1, G6, G9)', () => {
  it('WF2P-S01 (E1): the text column of the location card can shrink, so a long sentence wraps inside the card', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    await renderApp(rig);

    expect(styleOf(screen.getByText('Current location unavailable').parent).flex).toBe(1);
  });

  it('WF2P-S01 (E1): the same holds with a fix', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA);
    await renderApp(rig);

    expect(styleOf(screen.getByText('Current location · 6.5888 N, 79.9640 E').parent).flex).toBe(1);
  });

  it("WF2P-S03 (E1): the text column of the form's location block can shrink too", async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA);
    await toForm(rig);

    expect(styleOf(screen.getByText('Location *').parent).flex).toBe(1);
  });

  it('C1 (E1): the content column of a banner (and so a sentence next to its icon) can shrink', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, error: true });
    await renderApp(rig);

    expect(
      styleOf(screen.getByText('The service is unavailable and nothing was changed').parent).flex,
    ).toBe(1);
  });

  it('WF2P-S01 (G9, D-UC2-5): the second list is "Not sent yet" with its explaining line, and the old title is gone', async () => {
    await renderApp(makeRig());

    expect(screen.getByText('Not sent yet')).toBeTruthy();
    expect(
      screen.getByText('Saved on this phone — sent automatically when you are online'),
    ).toBeTruthy();
    expect(screen.getByText('Nothing waiting')).toBeTruthy();
    expect(screen.queryByText(/waiting on this phone/)).toBeNull();
  });
});
