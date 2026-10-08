import { act, fireEvent, screen } from '@testing-library/react-native';
import { Linking } from 'react-native';
import { searchPlaces } from '../../location/placeIndex';
import { KALUTARA_BRIDGE, makeRig, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const OUTSIDE_PIN = 'This location is outside Sri Lanka — move the pin to the place you saw';

/** The map's region, as the mock exposes it. */
const region = () => screen.getByTestId('map').props.region;

/** A map move, as the user's drag ends. */
async function moveMapTo(latitude: number, longitude: number): Promise<void> {
  await act(async () => {
    fireEvent(screen.getByTestId('map'), 'regionChangeComplete', {
      latitude,
      longitude,
      latitudeDelta: 0.01,
      longitudeDelta: 0.01,
    });
  });
}

/** The form with no GPS fix, the picker closed. */
async function toUnavailable(rig: Rig): Promise<void> {
  rig.controller.captureLocation.mockResolvedValue(undefined);
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

/** The form with no GPS fix and the picker open. */
async function toPicker(rig: Rig): Promise<void> {
  await toUnavailable(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })));
}

const typeInPlaceField = (text: string) =>
  run(() => fireEvent.changeText(screen.getByLabelText('Type a town or village'), text));

describe('the manual location: place list and map pin (standards §10, P213)', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('WF2P-S03a / SC2 A1 12b: no fix shows "GPS location is unavailable" and "Enter location manually"', async () => {
    await toUnavailable(makeRig());

    expect(screen.getByText('GPS location is unavailable')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Enter location manually' })).toBeTruthy();
    expect(screen.queryByTestId('map')).toBeNull();
  });

  it('WF2P-S03a / SC2 A1 12c: the picker opens with the place field, the map on the registered district, the caption and the credit; no coordinate fields, no search button', async () => {
    const rig = makeRig();

    await toPicker(rig);

    expect(screen.getByLabelText('Type a town or village').props.returnKeyType).toBe('search');
    expect(screen.getByText('Drag the map to put the pin on the spot you saw')).toBeTruthy();
    expect(screen.getByText('Place names: GeoNames (CC BY 4.0)')).toBeTruthy();
    expect(region().latitudeDelta).toBe(0.3);
    expect(region().latitude).toBeCloseTo(ui().mapStart.latitude, 5);
    expect(screen.queryByLabelText('Latitude (°N)')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Enter coordinates' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Search' })).toBeNull();
    expect(screen.queryByLabelText('Search for a place')).toBeNull();
  });

  it('WF2P-S03a (P213): typing two letters or more lists up to eight places "<place> — <District>", each at least 44 points high', async () => {
    await toPicker(makeRig());

    await typeInPlaceField('ka');

    const rows = screen.getAllByRole('button', { name: /^.+ — .+$/ });
    expect(rows.length).toBeGreaterThan(0);
    expect(rows.length).toBeLessThanOrEqual(8);
    rows.forEach((row) => expect(row.props.style.minHeight).toBe(44));
  });

  it('WF2P-S03a (P213): "kalu" lists Kalutara; one letter, an empty field or nonsense list nothing', async () => {
    await toPicker(makeRig());

    await typeInPlaceField('kalu');
    expect(screen.getByRole('button', { name: 'Kalutara — Kalutara' })).toBeTruthy();
    await typeInPlaceField('k');
    expect(screen.queryByRole('button', { name: /^.+ — .+$/ })).toBeNull();
    await typeInPlaceField('');
    expect(screen.queryByRole('button', { name: /^.+ — .+$/ })).toBeNull();
    await typeInPlaceField('xqzqxq');
    expect(screen.queryByRole('button', { name: /^.+ — .+$/ })).toBeNull();
  });

  it('WF2P-S03a (P213): choosing a place fills the field, moves the map there (0.02) and hides the list', async () => {
    await toPicker(makeRig());
    await typeInPlaceField('kalu');
    const kalutara = searchPlaces('kalu').find((match) => match.name === 'Kalutara');

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Kalutara — Kalutara' })));

    expect(screen.getByLabelText('Type a town or village').props.value).toBe('Kalutara');
    expect(region()).toEqual({
      latitude: kalutara?.point.latitude,
      longitude: kalutara?.point.longitude,
      latitudeDelta: 0.02,
      longitudeDelta: 0.02,
    });
    expect(screen.queryByRole('button', { name: /^.+ — .+$/ })).toBeNull();
  });

  it('WF2P-S03a (P213): typing again after a choice shows the list again', async () => {
    await toPicker(makeRig());
    await typeInPlaceField('kalu');
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Kalutara — Kalutara' })));

    await typeInPlaceField('Kalutar');

    expect(screen.getByRole('button', { name: 'Kalutara — Kalutara' })).toBeTruthy();
  });

  it('WF2P-S03a / SC2 A1 12c-12d: "Use this location" sends the centre of the map; the block reads "Entered manually"', async () => {
    const rig = makeRig();
    await toPicker(rig);
    await moveMapTo(6.5888, 79.964);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));

    expect(ui().draft?.location).toEqual({ latitude: 6.5888, longitude: 79.964 });
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText('Entered manually')).toBeTruthy();
    expect(screen.queryByTestId('map')).toBeNull();
    expect(ui().draft?.locationFromGps).toBe(false);
  });

  it('WF2P-S03a (P213): a pin within 5 km of the chosen place names the location after it', async () => {
    await toPicker(makeRig());
    await typeInPlaceField('kalu');
    const kalutara = searchPlaces('kalu').find((match) => match.name === 'Kalutara');
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Kalutara — Kalutara' })));
    await moveMapTo((kalutara?.point.latitude ?? 0) + 0.01, kalutara?.point.longitude ?? 0);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));

    expect(ui().manualPlaceName).toBe('Kalutara');
    expect(screen.getByText(/^near Kalutara, .* N, .* E — Entered manually$/)).toBeTruthy();
  });

  it('WF2P-S03a (P213): a pin farther than 5 km from the chosen place gives a location with no place name', async () => {
    await toPicker(makeRig());
    await typeInPlaceField('kalu');
    const kalutara = searchPlaces('kalu').find((match) => match.name === 'Kalutara');
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Kalutara — Kalutara' })));
    await moveMapTo((kalutara?.point.latitude ?? 0) + 0.2, kalutara?.point.longitude ?? 0);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));

    expect(ui().manualPlaceName).toBeUndefined();
    expect(screen.queryByText(/^near /)).toBeNull();
    expect(screen.getByText('Entered manually')).toBeTruthy();
  });

  it('WF2P-S03a (P213): a location entered without choosing a place has no place name', async () => {
    await toPicker(makeRig());
    await moveMapTo(6.5888, 79.964);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));

    expect(ui().manualPlaceName).toBeUndefined();
  });

  it('WF2P-S03b / SC2 E3 12e-12f: a pin outside Sri Lanka is refused under the map and the map stays', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['location: outside Sri Lanka'],
    });
    await toPicker(rig);
    await moveMapTo(65.888, 79.964);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));

    expect(screen.getByText(OUTSIDE_PIN)).toBeTruthy();
    expect(ui().draft?.location).toBeUndefined();
    expect(region().latitude).toBe(65.888);
  });

  it('WF2P-S03b / SC2 A1, E3: a GPS fix outside Sri Lanka offers the picker under the location, and a valid place replaces it', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue({ latitude: 51.5, longitude: -0.12 });
    rig.controller.validateDraft.mockImplementation((draft) =>
      draft.location && draft.location.latitude > 9.9
        ? { valid: false, errors: ['location: outside Sri Lanka'] }
        : { valid: true, errors: [] },
    );
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    expect(screen.queryByTestId('map')).toBeNull();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('51.5000 N, -0.1200 E')).toBeTruthy();
    expect(screen.getByText(OUTSIDE_PIN)).toBeTruthy();
    expect(screen.queryByText('GPS location is unavailable')).toBeNull();
    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );
    await moveMapTo(6.5888, 79.964);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));

    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(ui().draft?.locationFromGps).toBe(false);
  });
});

describe('"Change" on a manual location (standards §10 item 10, P213; UC2-7c R5)', () => {
  /** A form whose location was entered by hand at the Kalutara bridge. */
  async function toManual(rig: Rig): Promise<void> {
    await toPicker(rig);
    await moveMapTo(6.5888, 79.964);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));
  }

  it('WF2P-S03a: a GPS location has no "Change" link', async () => {
    await renderApp(makeRig());
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));

    expect(screen.queryByRole('button', { name: 'Change location' })).toBeNull();
  });

  it('WF2P-S03a: "Change" opens the picker at once, at the current point, with "Keep this location"', async () => {
    const rig = makeRig();
    await toManual(rig);
    expect(screen.queryByTestId('map')).toBeNull();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change location' })));

    expect(screen.getByTestId('map')).toBeTruthy();
    expect(region().latitude).toBe(6.5888);
    expect(region().longitude).toBe(79.964);
    expect(screen.getByRole('button', { name: 'Keep this location' })).toBeTruthy();
    expect(ui().draft?.location).toEqual({ latitude: 6.5888, longitude: 79.964 });
    expect(ui().changingLocation).toBe(true);
  });

  it('WF2P-S03a: a new point replaces the old one and closes the picker', async () => {
    const rig = makeRig();
    await toManual(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change location' })));
    await moveMapTo(6.9356, 79.9843);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));

    expect(ui().draft?.location).toEqual({ latitude: 6.9356, longitude: 79.9843 });
    expect(screen.getByText('6.9356 N, 79.9843 E')).toBeTruthy();
    expect(screen.queryByTestId('map')).toBeNull();
    expect(ui().changingLocation).toBe(false);
  });

  it('WF2P-S03a: "Keep this location" closes the picker with nothing changed', async () => {
    const rig = makeRig();
    await toManual(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change location' })));
    await moveMapTo(6.9356, 79.9843);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Keep this location' })));

    expect(screen.queryByTestId('map')).toBeNull();
    expect(ui().draft?.location).toEqual({ latitude: 6.5888, longitude: 79.964 });
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(ui().changingLocation).toBe(false);
  });
});

describe('the location access notice (standards §10 item 8, WF2P-S03g)', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('WF2P-S03g: a refused permission that cannot be asked again shows the notice; "Allow location access" opens Settings', async () => {
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('blocked');
    const openSettings = jest.spyOn(Linking, 'openSettings').mockResolvedValue(undefined);

    await toUnavailable(rig);

    expect(screen.getByText('Allow location access to use your current location')).toBeTruthy();
    expect(screen.queryByText('GPS location is unavailable')).toBeNull();
    expect(screen.getByRole('button', { name: 'Enter location manually' })).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Allow location access' })));
    expect(openSettings).toHaveBeenCalledTimes(1);
    expect(rig.locationAccess.request).not.toHaveBeenCalled();
  });

  it('WF2P-S03g: an askable permission asks again, and a grant captures the location again for the draft and the home', async () => {
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('askable');
    rig.locationAccess.request.mockResolvedValue(true);
    await toUnavailable(rig);
    expect(screen.getByText('Allow location access to use your current location')).toBeTruthy();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA_BRIDGE);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Allow location access' })));

    expect(rig.locationAccess.request).toHaveBeenCalledTimes(1);
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.queryByText('Allow location access to use your current location')).toBeNull();
    expect(ui().currentLocation).toEqual(KALUTARA_BRIDGE);
  });

  it('WF2P-S03g: an askable permission that is refused again is read anew', async () => {
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValueOnce('askable').mockResolvedValueOnce('askable');
    rig.locationAccess.request.mockResolvedValue(false);
    await toUnavailable(rig);
    rig.locationAccess.check.mockResolvedValue('blocked');

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Allow location access' })));

    expect(ui().locationAccess).toBe('blocked');
    expect(screen.getByText('Allow location access to use your current location')).toBeTruthy();
  });

  it('WF2P-S03g: Location Services off shows its sentence and no button', async () => {
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('servicesOff');

    await toUnavailable(rig);

    expect(screen.getByText('Turn on Location Services to use your current location')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Allow location access' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Enter location manually' })).toBeTruthy();
    await run(() => ui().allowLocationAccess());
    expect(rig.locationAccess.request).not.toHaveBeenCalled();
  });

  it('WF2P-S03g: a granted state with no fix keeps the plain "GPS location is unavailable"', async () => {
    const rig = makeRig();

    await toUnavailable(rig);

    expect(screen.getByText('GPS location is unavailable')).toBeTruthy();
    expect(screen.queryByText('Allow location access to use your current location')).toBeNull();
  });

  it('WF2P-S01 / S03g: the home says why the current location is not shown, without a button', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('blocked');

    await renderApp(rig);

    expect(
      screen.getByText('Current location · Allow location access to use your current location'),
    ).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Allow location access' })).toBeNull();
  });

  it('WF2P-S01 / S03g: the home names Location Services when they are off, and "not available" when it does not know', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('servicesOff');
    await renderApp(rig);
    expect(
      screen.getByText('Current location · Turn on Location Services to use your current location'),
    ).toBeTruthy();

    rig.locationAccess.check.mockResolvedValue('granted');
    await run(() => ui().refreshHomeScreen());
    expect(screen.getByText('Current location · not available')).toBeTruthy();
  });

  it('WF2P-S03g: "Allow location access" with no state read does nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().allowLocationAccess());

    expect(rig.locationAccess.request).not.toHaveBeenCalled();
  });
});
