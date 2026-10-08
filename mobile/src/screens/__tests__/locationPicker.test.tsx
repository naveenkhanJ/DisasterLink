import { act, fireEvent, screen } from '@testing-library/react-native';
import { Linking, Platform } from 'react-native';
import { searchPlace } from '../../location/placeSearch';
import { isOnline } from '../../sync/connectivityTriggers';
import { KALUTARA_BRIDGE, makeRig, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));
jest.mock('../../location/placeSearch', () => ({ searchPlace: jest.fn() }));
jest.mock('../../sync/connectivityTriggers', () => ({ isOnline: jest.fn() }));

const search = searchPlace as jest.Mock;
const online = isOnline as jest.Mock;
const OUTSIDE_PIN = 'This location is outside Sri Lanka — move the pin to the place you saw';
const OUTSIDE_FIELDS = 'This location is outside Sri Lanka — check the latitude and longitude';
const NUMBERS = 'Enter the latitude and longitude as numbers';

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

const useMapPin = () => screen.getAllByRole('button', { name: 'Use this location' })[0];
const useFields = () => screen.getAllByRole('button', { name: 'Use this location' })[1];

describe('the manual location: map pin and place search (standards §10, P206)', () => {
  beforeEach(() => {
    search.mockReset();
    online.mockReset();
    online.mockResolvedValue(true);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('WF2P-S03a / SC2 A1 12b: no fix shows "GPS location is unavailable" and "Enter location manually"', async () => {
    const rig = makeRig();

    await toUnavailable(rig);

    expect(screen.getByText('GPS location is unavailable')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Enter location manually' })).toBeTruthy();
    expect(screen.queryByTestId('map')).toBeNull();
  });

  it('WF2P-S03a / SC2 A1 12c: the picker opens with the search, the map on the registered district and the caption', async () => {
    const rig = makeRig();

    await toPicker(rig);

    expect(screen.getByLabelText('Search for a place')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Search' })).toBeTruthy();
    expect(screen.getByText('Drag the map to put the pin on the spot you saw')).toBeTruthy();
    expect(region().latitudeDelta).toBe(0.3);
    expect(region().latitude).toBeCloseTo(ui().mapStart.latitude, 5);
    expect(region().longitude).toBeCloseTo(ui().mapStart.longitude, 5);
    expect(screen.queryByText('Enter coordinates')).toBeNull();
  });

  it('WF2P-S03a / SC2 A1 12c: a search result moves the map to the place', async () => {
    search.mockResolvedValue(KALUTARA_BRIDGE);
    const rig = makeRig();
    await toPicker(rig);
    await run(() =>
      fireEvent.changeText(screen.getByLabelText('Search for a place'), 'Kalutara bridge'),
    );

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Search' })));

    expect(search).toHaveBeenCalledWith('Kalutara bridge');
    expect(region()).toEqual({
      latitude: 6.5888,
      longitude: 79.964,
      latitudeDelta: 0.01,
      longitudeDelta: 0.01,
    });
  });

  it('WF2P-S03a: no result shows "No place found" and does not offer the coordinates', async () => {
    search.mockResolvedValue(undefined);
    const rig = makeRig();
    await toPicker(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Search' })));

    expect(screen.getByText('No place found — try another name or move the pin')).toBeTruthy();
    expect(screen.queryByText('Enter coordinates')).toBeNull();
  });

  it('WF2P-S03a (N5): the search field has a "search" return key, and submitting it searches like the button', async () => {
    search.mockResolvedValue(KALUTARA_BRIDGE);
    const rig = makeRig();
    await toPicker(rig);
    const field = screen.getByLabelText('Search for a place');
    expect(field.props.returnKeyType).toBe('search');
    await run(() => fireEvent.changeText(field, 'Kalutara bridge'));

    await run(() => fireEvent(field, 'submitEditing'));

    expect(search).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledWith('Kalutara bridge');
    expect(region().latitude).toBe(6.5888);
  });

  it('WF2P-S03a: a search result clears the "No place found" message', async () => {
    search.mockResolvedValueOnce(undefined).mockResolvedValueOnce(KALUTARA_BRIDGE);
    const rig = makeRig();
    await toPicker(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Search' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Search' })));

    expect(screen.queryByText('No place found — try another name or move the pin')).toBeNull();
  });

  it('WF2P-S03a: a search error shows its message and the "Enter coordinates" link', async () => {
    search.mockRejectedValue(new Error('no network'));
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    const rig = makeRig();
    await toPicker(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Search' })));

    expect(
      screen.getByText('The place search is not available — move the pin, or enter coordinates'),
    ).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Enter coordinates' })).toBeTruthy();
  });

  it('WF2P-S03a: offline, the "Enter coordinates" link is shown at once', async () => {
    online.mockResolvedValue(false);
    const rig = makeRig();

    await toPicker(rig);

    expect(screen.getByRole('button', { name: 'Enter coordinates' })).toBeTruthy();
  });

  it('WF2P-S03a: online with the search allowed, the link stays hidden', async () => {
    const rig = makeRig();

    await toPicker(rig);

    expect(screen.queryByRole('button', { name: 'Enter coordinates' })).toBeNull();
  });

  it('WF2P-S03a: on Android without the location permission the search is hidden and the link is shown', async () => {
    jest.replaceProperty(Platform, 'OS', 'android');
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('blocked');

    await toPicker(rig);

    expect(screen.queryByLabelText('Search for a place')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Search' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Enter coordinates' })).toBeTruthy();
  });

  it('WF2P-S03a: on Android with the permission the search is shown', async () => {
    jest.replaceProperty(Platform, 'OS', 'android');
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('granted');

    await toPicker(rig);

    expect(screen.getByLabelText('Search for a place')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Enter coordinates' })).toBeNull();
  });

  it('WF2P-S03a / SC2 A1 12c-12d: "Use this location" sends the centre of the map; the block reads "Entered manually"', async () => {
    const rig = makeRig();
    await toPicker(rig);
    await moveMapTo(6.5888, 79.964);

    await run(() => fireEvent.press(useMapPin()));

    expect(ui().draft?.location).toEqual({ latitude: 6.5888, longitude: 79.964 });
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText('Entered manually')).toBeTruthy();
    expect(screen.queryByTestId('map')).toBeNull();
  });

  it('WF2P-S03b / SC2 E3 12e-12f: a pin outside Sri Lanka is refused with the pin wording under the map, and the map stays', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['location: outside Sri Lanka'],
    });
    await toPicker(rig);
    await moveMapTo(65.888, 79.964);

    await run(() => fireEvent.press(useMapPin()));

    expect(screen.getByText(OUTSIDE_PIN)).toBeTruthy();
    expect(screen.queryByText(OUTSIDE_FIELDS)).toBeNull();
    expect(ui().draft?.location).toBeUndefined();
    expect(region().latitude).toBe(65.888);
  });

  it('WF2P-S03b / SC2 E3: coordinates outside Sri Lanka are refused with the fields wording and the typed values stay', async () => {
    online.mockResolvedValue(false);
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['location: outside Sri Lanka'],
    });
    await toPicker(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Enter coordinates' })));
    await run(() => fireEvent.changeText(screen.getByLabelText('Latitude (°N)'), '65.888'));
    await run(() => fireEvent.changeText(screen.getByLabelText('Longitude (°E)'), '79.9640'));

    await run(() => fireEvent.press(useFields()));

    expect(screen.getByText(OUTSIDE_FIELDS)).toBeTruthy();
    expect(screen.queryByText(OUTSIDE_PIN)).toBeNull();
    expect(screen.getByLabelText('Latitude (°N)').props.value).toBe('65.888');
    expect(screen.getByLabelText('Longitude (°E)').props.value).toBe('79.9640');
  });

  it.each([
    ['blank latitude', '', '79.9640'],
    ['blank longitude', '6.5888', ''],
    ['blank fields', '', ''],
    ['non-numeric latitude', 'six', '79.9640'],
    ['non-numeric longitude', '6.5888', '79.9640 E'],
  ])(
    'WF2P-S03b / SC2 E3 12e: %s asks for numbers, sends nothing and keeps the typed values',
    async (_name, latitude, longitude) => {
      online.mockResolvedValue(false);
      const rig = makeRig();
      await toPicker(rig);
      await run(() => fireEvent.press(screen.getByRole('button', { name: 'Enter coordinates' })));
      await run(() => fireEvent.changeText(screen.getByLabelText('Latitude (°N)'), latitude));
      await run(() => fireEvent.changeText(screen.getByLabelText('Longitude (°E)'), longitude));
      rig.controller.validateDraft.mockClear();

      await run(() => fireEvent.press(useFields()));

      expect(screen.getByText(NUMBERS)).toBeTruthy();
      expect(rig.controller.validateDraft).not.toHaveBeenCalled();
      expect(ui().draft?.location).toBeUndefined();
      expect(screen.getByLabelText('Latitude (°N)').props.value).toBe(latitude);
      expect(screen.getByLabelText('Longitude (°E)').props.value).toBe(longitude);
    },
  );

  it('WF2P-S03a / SC2 A1 12c: valid typed coordinates are sent as a manual location, and the numbers error goes', async () => {
    online.mockResolvedValue(false);
    const rig = makeRig();
    await toPicker(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Enter coordinates' })));
    await run(() => fireEvent.press(useFields()));
    expect(screen.getByText(NUMBERS)).toBeTruthy();
    await run(() => fireEvent.changeText(screen.getByLabelText('Latitude (°N)'), '6.5888'));
    await run(() => fireEvent.changeText(screen.getByLabelText('Longitude (°E)'), '79.9640'));

    await run(() => fireEvent.press(useFields()));

    expect(screen.queryByText(NUMBERS)).toBeNull();
    expect(screen.getByText('Entered manually')).toBeTruthy();
    expect(ui().draft?.location).toEqual({ latitude: 6.5888, longitude: 79.964 });
  });

  it('WF2P-S03b / SC2 A1, E3: a GPS fix outside Sri Lanka offers the picker under the location, with the pin wording', async () => {
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
    await run(() => fireEvent.press(useMapPin()));

    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText('Entered manually')).toBeTruthy();
    expect(ui().draft?.locationFromGps).toBe(false);
  });
});

describe('the location access notice (standards §10 item 8, WF2P-S03g)', () => {
  beforeEach(() => {
    online.mockResolvedValue(true);
  });

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
