import { act, fireEvent, screen } from '@testing-library/react-native';
import { Linking } from 'react-native';
import { nearestPlace, searchPlaces } from '../../location/placeIndex';
import { KALUTARA_BRIDGE, makeRig, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const OUTSIDE_PIN = 'This location is outside Sri Lanka — move the pin to the place you saw';
const OUTSIDE_ISLAND = 'Outside Sri Lanka — move the pin onto the island';

/** A point inside Hambantota with no place within 10 km (found on the real data). */
const REMOTE = { latitude: 6.45, longitude: 81.6 };

/** A point whose nearest place is 5.7 km away (Palugaswala): a label, but no name after entry. */
const BETWEEN = { latitude: 6.25, longitude: 81.05 };

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

const useButton = () => screen.getByRole('button', { name: 'Use this location' });

/** Types "kalu" and chooses Kalutara; gives its point. */
async function chooseKalutara(): Promise<{ latitude: number; longitude: number }> {
  await typeInPlaceField('kalu');
  const kalutara = searchPlaces('kalu').find((match) => match.name === 'Kalutara');
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Kalutara — Kalutara' })));
  return kalutara?.point ?? { latitude: 0, longitude: 0 };
}

describe('the manual location: place list and map pin (standards §10, P213, P222)', () => {
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

    const point = await chooseKalutara();

    expect(screen.getByLabelText('Type a town or village').props.value).toBe('Kalutara');
    expect(region()).toEqual({ ...point, latitudeDelta: 0.02, longitudeDelta: 0.02 });
    expect(screen.queryByRole('button', { name: /^.+ — .+$/ })).toBeNull();
  });

  it('WF2P-S03a (P213): typing again after a choice shows the list again', async () => {
    await toPicker(makeRig());
    await chooseKalutara();

    await typeInPlaceField('Kalutar');

    expect(screen.getByRole('button', { name: 'Kalutara — Kalutara' })).toBeTruthy();
  });

  it('WF2P-S03a / SC2 A1 12c-12d: "Use this location" sends the centre of the map; the block reads "Entered manually"', async () => {
    const rig = makeRig();
    await toPicker(rig);
    await moveMapTo(6.5888, 79.964);

    await run(() => fireEvent.press(useButton()));

    expect(ui().draft?.location).toEqual({ latitude: 6.5888, longitude: 79.964 });
    expect(screen.getByText(/6\.5888 N, 79\.9640 E/)).toBeTruthy();
    expect(screen.getByText(/Entered manually/)).toBeTruthy();
    expect(screen.queryByTestId('map')).toBeNull();
    expect(ui().draft?.locationFromGps).toBe(false);
  });
});

describe('the nearest-place label under the map (standards §10, Nearest-place label, P222; UC2-7e G10)', () => {
  it('WF2P-S03a (D1): a pin near a town says "Near <place> — <District of the pin> · <d> km", one decimal under 10 km', async () => {
    await toPicker(makeRig());
    await moveMapTo(BETWEEN.latitude, BETWEEN.longitude);

    const near = nearestPlace(BETWEEN);
    expect(near?.name).toBe('Palugaswala');

    expect(screen.getByText(/^Near Palugaswala — .+ · 5\.7 km$/)).toBeTruthy();
  });

  it('WF2P-S03a (D1): the label is announced politely to a screen reader', async () => {
    await toPicker(makeRig());

    expect(screen.getByTestId('centre-label').props.accessibilityLiveRegion).toBe('polite');
  });

  it('WF2P-S03a (D1): the label follows the map only when it stops, from the starting district centre onwards', async () => {
    await toPicker(makeRig());
    expect(screen.getByText(/^Near .+ · .+ km$/)).toBeTruthy();

    await moveMapTo(REMOTE.latitude, REMOTE.longitude);

    expect(screen.getByText('No town nearby — the point is still used')).toBeTruthy();
    expect(screen.queryByText(/^Near /)).toBeNull();
  });

  it('WF2P-S03a (D1): a pin outside Sri Lanka says so in the error style and "Use this location" is disabled', async () => {
    const rig = makeRig();
    await toPicker(rig);

    await moveMapTo(65.888, 79.964);

    expect(screen.getByText(OUTSIDE_ISLAND)).toBeTruthy();
    expect(useButton().props.accessibilityState.disabled).toBe(true);
    await run(() => fireEvent.press(useButton()));
    expect(ui().draft?.location).toBeUndefined();
    await moveMapTo(6.5888, 79.964);
    expect(useButton().props.accessibilityState.disabled).toBe(false);
  });

  it('WF2P-S03a (D1): a drag of more than 1 km from the chosen place clears the choice and empties the field', async () => {
    await toPicker(makeRig());
    const point = await chooseKalutara();

    await moveMapTo(point.latitude + 0.02, point.longitude);

    expect(screen.getByLabelText('Type a town or village').props.value).toBe('');
    expect(screen.queryByRole('button', { name: /^.+ — .+$/ })).toBeNull();
  });

  it('WF2P-S03a (D1): a drag of less than 1 km keeps the choice', async () => {
    await toPicker(makeRig());
    const point = await chooseKalutara();

    await moveMapTo(point.latitude + 0.005, point.longitude);

    expect(screen.getByLabelText('Type a town or village').props.value).toBe('Kalutara');
  });

  it('WF2P-S03a (D1): choosing a row does not clear itself when the map settles on the place', async () => {
    await toPicker(makeRig());
    const point = await chooseKalutara();

    await moveMapTo(point.latitude, point.longitude);

    expect(screen.getByLabelText('Type a town or village').props.value).toBe('Kalutara');
  });
});

describe('the name after "Use this location" (standards §10, P222)', () => {
  it('WF2P-S03a (D1): a place that is still chosen names the location, even a few hundred metres from the pin', async () => {
    await toPicker(makeRig());
    const point = await chooseKalutara();
    await moveMapTo(point.latitude + 0.005, point.longitude);

    await run(() => fireEvent.press(useButton()));

    expect(ui().manualPlaceName).toBe('Kalutara');
    expect(screen.getByText(/^near Kalutara, .* N, .* E — Entered manually$/)).toBeTruthy();
  });

  it('WF2P-S03a (D1): when the choice was cleared, the nearest place within 5 km names it', async () => {
    await toPicker(makeRig());
    const point = await chooseKalutara();
    const away = { latitude: point.latitude + 0.03, longitude: point.longitude };
    await moveMapTo(away.latitude, away.longitude);
    const expected = nearestPlace(away, 5)?.name;

    await run(() => fireEvent.press(useButton()));

    expect(ui().manualPlaceName).toBe(expected);
    expect(expected).toBeDefined();
  });

  it('WF2P-S03a (D1): a pin whose nearest place is farther than 5 km gives a location with no place name', async () => {
    await toPicker(makeRig());
    await moveMapTo(BETWEEN.latitude, BETWEEN.longitude);

    await run(() => fireEvent.press(useButton()));

    expect(ui().manualPlaceName).toBeUndefined();
    expect(screen.queryByText(/^near /)).toBeNull();
    expect(screen.getByText('Entered manually')).toBeTruthy();
  });

  it('WF2P-S03a (D1): a pin with no town nearby gives a location with no place name', async () => {
    await toPicker(makeRig());
    await moveMapTo(REMOTE.latitude, REMOTE.longitude);

    await run(() => fireEvent.press(useButton()));

    expect(ui().manualPlaceName).toBeUndefined();
  });

  it('WF2P-S03b / SC2 E3 12e-12f: the dictionary input still refuses a point outside Sri Lanka, as the button now cannot send one', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['location: outside Sri Lanka'],
    });
    await toUnavailable(rig);

    await run(() => ui().enterLocationManually({ latitude: 65.888, longitude: 79.964 }));

    expect(ui().draft?.location).toBeUndefined();
    expect(screen.getByText(OUTSIDE_PIN)).toBeTruthy();
  });

  it('WF2P-S03b / SC2 E3 12e-12f: the error from the review shows under the map and the map stays', async () => {
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

    expect(screen.getByText(/^51\.5000 N, -0\.1200 E/)).toBeTruthy();
    expect(screen.getByText(OUTSIDE_PIN)).toBeTruthy();
    expect(screen.queryByText('GPS location is unavailable')).toBeNull();
    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );
    await moveMapTo(6.5888, 79.964);
    await run(() => fireEvent.press(useButton()));

    expect(screen.getByText(/6\.5888 N, 79\.9640 E/)).toBeTruthy();
    expect(ui().draft?.locationFromGps).toBe(false);
  });
});

describe('"Change" on a manual location (standards §10 item 10, P213; UC2-7c R5)', () => {
  /** A form whose location was entered by hand at the Kalutara bridge. */
  async function toManual(rig: Rig): Promise<void> {
    await toPicker(rig);
    await moveMapTo(6.5888, 79.964);
    await run(() => fireEvent.press(useButton()));
  }

  it('WF2P-S03a: a GPS location has no "Change" link', async () => {
    await renderApp(makeRig());
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));

    expect(screen.queryByRole('button', { name: 'Change location' })).toBeNull();
  });

  it('WF2P-S03a: "Change" opens the picker at once, at the current point, with the button "Keep this location"', async () => {
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

  it('WF2P-S03a (G3): "Keep this location" is a secondary button under "Use this location", not a link', async () => {
    const rig = makeRig();
    await toManual(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change location' })));

    const keep = screen.getByRole('button', { name: 'Keep this location' });

    expect(
      keep.props.style.some?.((item: { borderWidth?: number }) => item?.borderWidth === 1),
    ).toBe(true);
    expect(keep.props.accessibilityState.disabled).toBe(false);
  });

  it('WF2P-S03a: a new point replaces the old one and closes the picker', async () => {
    const rig = makeRig();
    await toManual(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change location' })));
    await moveMapTo(6.9356, 79.9843);

    await run(() => fireEvent.press(useButton()));

    expect(ui().draft?.location).toEqual({ latitude: 6.9356, longitude: 79.9843 });
    expect(screen.getByText(/6\.9356 N, 79\.9843 E/)).toBeTruthy();
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
    expect(screen.getByText(/6\.5888 N, 79\.9640 E/)).toBeTruthy();
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

  it('WF2P-S01 / S03g (E1, G6): a blocked permission gives a title, the reason on a second line and the button "Allow location access" that opens Settings', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('blocked');
    const openSettings = jest.spyOn(Linking, 'openSettings').mockResolvedValue(undefined);
    openSettings.mockClear();

    await renderApp(rig);

    expect(screen.getByText('Current location unavailable')).toBeTruthy();
    expect(screen.getByText('Allow location access for Expo Go in Settings')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Allow location access' })));
    expect(openSettings).toHaveBeenCalledTimes(1);
  });

  it('WF2P-S01 / S03g (E1, G6): an askable permission says so and its button asks the permission again', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('askable');
    rig.locationAccess.request.mockResolvedValue(true);
    await renderApp(rig);
    expect(screen.getByText('Allow location access to show it here')).toBeTruthy();
    rig.controller.captureLocation.mockResolvedValue(KALUTARA_BRIDGE);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Allow location access' })));

    expect(rig.locationAccess.request).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Current location · 6.5888 N, 79.9640 E')).toBeTruthy();
  });

  it('WF2P-S01 / S03g (E1, G6): Location Services off says so and has no button; an unknown cause says the phone has not found its position', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.locationAccess.check.mockResolvedValue('servicesOff');
    await renderApp(rig);
    expect(screen.getByText('Turn on Location Services in Settings')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Allow location access' })).toBeNull();

    rig.locationAccess.check.mockResolvedValue('granted');
    await run(() => ui().refreshHomeScreen());

    expect(screen.getByText('The phone has not found its position yet')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Allow location access' })).toBeNull();
  });

  it('WF2P-S01 (E1): the location card has no button while there is a fix', async () => {
    await renderApp(makeRig());

    expect(screen.getByText('Current location · 6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Allow location access' })).toBeNull();
  });

  it('WF2P-S03g: "Allow location access" with no state read does nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().allowLocationAccess());

    expect(rig.locationAccess.request).not.toHaveBeenCalled();
  });
});
