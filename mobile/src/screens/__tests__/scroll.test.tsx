import { render, fireEvent, screen } from '@testing-library/react-native';
import React, { useEffect, useRef } from 'react';
import { ScrollView, TextInput, View } from 'react-native';
import { useScrollIntoView } from '../Screen';
import { FILLED_DRAFT, makeRig, ONLINE_HOME, renderApp, run, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

// The Jest React Native mocks give View.measureLayout, ScrollView.scrollTo and TextInput.focus as shared
// jest.fn()s whose `this` is the host instance, so the tests read which element was scrolled to.
const measureLayout = View.prototype.measureLayout as unknown as jest.Mock;
const scrollTo = ScrollView.prototype.scrollTo as unknown as jest.Mock;
const focus = TextInput.prototype.focus as unknown as jest.Mock;

/** The testIDs of the elements that were scrolled into view, in order. */
const scrolledTo = () =>
  measureLayout.mock.contexts.map(
    (context: { props: { testID?: string } }) => context.props.testID,
  );

beforeEach(() => {
  measureLayout.mockReset();
  scrollTo.mockReset();
  focus.mockReset();
  measureLayout.mockImplementation((_relativeTo: unknown, onSuccess: (...args: number[]) => void) =>
    onSuccess(0, 300, 358, 60),
  );
});

async function toForm(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

/** The form with a description typed, so only the errors a test sets can come up. */
async function toFormWithDescription(rig: Rig): Promise<void> {
  await toForm(rig);
  await run(() => fireEvent.changeText(screen.getByLabelText('Description *'), 'Water is rising'));
}

async function toFormWithoutFix(rig: Rig): Promise<void> {
  rig.controller.captureLocation.mockResolvedValue(undefined);
  await toForm(rig);
}

describe('content that appears is scrolled into view (standards §10 item 13, P222; UC2-7e G8)', () => {
  it('WF2P-S03a (G8): opening the picker scrolls the place field to 16 points below the top and focuses it', async () => {
    await toFormWithoutFix(makeRig());
    measureLayout.mockClear();
    scrollTo.mockClear();

    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );

    expect(scrolledTo()).toContain('place-field');
    expect(scrollTo).toHaveBeenCalledWith({ y: 284, animated: true });
    expect(focus).toHaveBeenCalled();
    expect(focus.mock.contexts[0].props.accessibilityLabel).toBe('Type a town or village');
  });

  it('WF2P-S03a (G8): never above the top of the page: a target near the top scrolls to 0', async () => {
    measureLayout.mockImplementation(
      (_relativeTo: unknown, onSuccess: (...args: number[]) => void) => onSuccess(0, 5, 358, 60),
    );
    await toFormWithoutFix(makeRig());
    scrollTo.mockClear();

    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );

    expect(scrollTo).toHaveBeenCalledWith({ y: 0, animated: true });
  });

  it('WF2P-S03a (G8): "Change" opens the picker at once and scrolls to and focuses the place field', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );
    await run(() => rig.controller.captureLocation.mockResolvedValue(undefined));
    await run(() =>
      fireEvent(screen.getByTestId('map'), 'regionChangeComplete', {
        latitude: 6.5888,
        longitude: 79.964,
        latitudeDelta: 0.01,
        longitudeDelta: 0.01,
      }),
    );
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Use this location' })));
    measureLayout.mockClear();
    focus.mockClear();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change location' })));

    expect(scrolledTo()).toContain('place-field');
    expect(focus).toHaveBeenCalled();
  });

  it('WF2P-S03a (G8): the place list appearing while typing scrolls the field so the list stays above the keyboard', async () => {
    await toFormWithoutFix(makeRig());
    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );
    measureLayout.mockClear();

    await run(() => fireEvent.changeText(screen.getByLabelText('Type a town or village'), 'kalu'));

    expect(scrolledTo()).toEqual(['place-field']);
  });

  it('WF2P-S03a (G8): the address field appearing scrolls to it, but a field that is there from the start does not move the page', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    measureLayout.mockClear();

    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );

    expect(scrolledTo()).toContain('address-field');

    const reopened = makeRig();
    const stored = {
      ...FILLED_DRAFT,
      description: 'Water\nAddress: Near the bridge',
      status: 'REJECTED' as const,
      rejectionReason: 'x',
    };
    reopened.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, pendingDrafts: [stored] });
    reopened.controller.getDraft.mockResolvedValue(stored);
    measureLayout.mockClear();
    await renderApp(reopened);
    await run(() => fireEvent.press(screen.getAllByRole('button', { name: 'Open' })[0]));
    expect(screen.getAllByLabelText('Address or landmark').length).toBeGreaterThan(0);
    expect(scrolledTo()).not.toContain('address-field');
  });

  it('WF2P-S03e (G8): after "Review Report" the first field with an error is scrolled to (the hazard type before the description)', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['hazardType: a hazard type is required', 'description: a description is required'],
    });
    await toForm(rig);
    measureLayout.mockClear();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(scrolledTo()).toEqual(['hazard-row']);
  });

  it.each([
    ['description', ['description: a description is required'], 'description-field'],
    ['photo', ['photo: one photo is required'], 'photo-block'],
    ['location', ['location: a location is required'], 'location-block'],
  ])(
    'WF2P-S03e / S03c (G8): an error in the %s alone scrolls to it',
    async (_name, errors, target) => {
      const rig = makeRig();
      rig.controller.validateDraft.mockReturnValue({ valid: false, errors });
      if (target === 'description-field') {
        await toForm(rig);
      } else {
        await toFormWithDescription(rig);
      }
      measureLayout.mockClear();

      await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

      expect(scrolledTo()).toEqual([target]);
    },
  );

  it('WF2P-S03e (G8): a second review scrolls again, but clearing the photo error by attaching a photo does not', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['description: a description is required', 'photo: one photo is required'],
    });
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
    measureLayout.mockClear();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    expect(scrolledTo()).toEqual([]);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(scrolledTo()).toEqual(['description-field']);
  });

  it('WF2P-S03e (G8): a review that passes scrolls nowhere', async () => {
    const rig = makeRig();
    await toFormWithDescription(rig);
    measureLayout.mockClear();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(scrolledTo()).toEqual([]);
  });

  it('WF2P-S03g (G8): the location notice appearing on the form scrolls to the location block', async () => {
    const rig = makeRig();
    rig.locationAccess.check.mockResolvedValue('blocked');
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    // The fix is still being looked for when the form opens; the notice comes when it is not found.
    let noFix: (fix: undefined) => void = () => undefined;
    rig.controller.captureLocation.mockImplementation(
      () => new Promise((resolve) => (noFix = resolve)),
    );
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    expect(screen.getByText('Finding your location…')).toBeTruthy();
    measureLayout.mockClear();

    await run(() => noFix(undefined));

    expect(
      screen.getAllByText('Allow location access to use your current location').length,
    ).toBeGreaterThan(0);
    expect(scrolledTo()).toContain('location-block');
  });

  it('WF2P-S03 (G8): the plain "GPS location is unavailable" does not scroll (only the S03g notice does)', async () => {
    await toFormWithoutFix(makeRig());

    expect(scrolledTo()).not.toContain('location-block');
  });

  it('standards §10 item 13: outside a Screen scrollIntoView does nothing', async () => {
    function Probe() {
      const scrollIntoView = useScrollIntoView();
      const ref = useRef<View>(null);
      useEffect(() => scrollIntoView(ref), [scrollIntoView]);
      return <View ref={ref} testID="probe" />;
    }

    await render(<Probe />);

    expect(measureLayout).not.toHaveBeenCalled();
    expect(scrollTo).not.toHaveBeenCalled();
  });

  it('standards §10 item 13: a failed measurement scrolls nowhere and does not throw', async () => {
    measureLayout.mockImplementation(
      (_relativeTo: unknown, _onSuccess: unknown, onFail: () => void) => onFail(),
    );
    await toFormWithoutFix(makeRig());
    scrollTo.mockClear();

    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );

    expect(scrollTo).not.toHaveBeenCalled();
  });
});
