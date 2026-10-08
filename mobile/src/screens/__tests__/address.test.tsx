import { act, fireEvent, screen } from '@testing-library/react-native';
import { FILLED_DRAFT, makeRig, ONLINE_HOME, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const ADDRESS_FIELD = 'Address or landmark';

/** The form of a new report with a GPS fix (WF2P-S03). */
async function toForm(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

/** The form with no GPS fix. */
async function toFormWithoutFix(rig: Rig): Promise<void> {
  rig.controller.captureLocation.mockResolvedValue(undefined);
  await toForm(rig);
}

const type = (label: string, text: string) =>
  run(() => fireEvent.changeText(screen.getByLabelText(label), text));

describe('the "Address or landmark" field (standards §10 "The address", P214; D-UC2-4)', () => {
  it('WF2P-S03: the field is hidden for a GPS location with no address text', async () => {
    await toForm(makeRig());

    expect(screen.queryByLabelText(ADDRESS_FIELD)).toBeNull();
  });

  it('WF2P-S03a: the field appears when the picker is open', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    expect(screen.queryByLabelText(ADDRESS_FIELD)).toBeNull();

    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Enter location manually' })),
    );

    expect(screen.getByLabelText(ADDRESS_FIELD)).toBeTruthy();
  });

  it('WF2P-S03a: the field stays for a manual location after the picker closes', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));

    expect(screen.getByLabelText(ADDRESS_FIELD)).toBeTruthy();
  });

  it('WF2P-S03: the field stays visible while it has text, even for a GPS location', async () => {
    const rig = makeRig();
    const stored = { ...FILLED_DRAFT, description: 'Water\nAddress: Near the bridge' };
    rig.controller.loadHome.mockResolvedValue({
      ...ONLINE_HOME,
      pendingDrafts: [{ ...stored, status: 'REJECTED', rejectionReason: 'x' }],
    });
    rig.controller.getDraft.mockResolvedValue({
      ...stored,
      status: 'REJECTED',
      rejectionReason: 'x',
    });
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open' })));

    expect(ui().draft?.locationFromGps).toBe(true);
    expect(screen.getByLabelText(ADDRESS_FIELD).props.value).toBe('Near the bridge');
  });

  it('WF2P-S03: the description is stored as the description text, a new line and "Address: " with the address', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));

    await type('Description *', 'Water is rising');
    await type(ADDRESS_FIELD, 'Near the old bridge');

    expect(ui().draft?.description).toBe('Water is rising\nAddress: Near the old bridge');
    expect(screen.getByLabelText('Description *').props.value).toBe('Water is rising');
    expect(screen.getByLabelText(ADDRESS_FIELD).props.value).toBe('Near the old bridge');
  });

  it('WF2P-S03: a blank address leaves the description alone, whatever the order of typing', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await type(ADDRESS_FIELD, 'Near the old bridge');
    await type('Description *', 'Water is rising');
    expect(ui().draft?.description).toBe('Water is rising\nAddress: Near the old bridge');

    await type(ADDRESS_FIELD, '   ');

    expect(ui().draft?.description).toBe('Water is rising');
  });

  it('WF2P-S03: the counter shows the combined length', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await type('Description *', 'Water');
    expect(screen.getByText('5 / 500')).toBeTruthy();

    await type(ADDRESS_FIELD, 'Bridge');

    const composed = 'Water\nAddress: Bridge';
    expect(screen.getByText(`${composed.length} / 500`)).toBeTruthy();
  });

  it('WF2P-S03e (P214): a blank description with an address is refused, with the existing wording, and the address stays', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await type(ADDRESS_FIELD, 'Near the old bridge');

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(
      screen.getByText('Enter a description of what you saw (at most 500 characters)'),
    ).toBeTruthy();
    expect(screen.queryByText('Review your report')).toBeNull();
    expect(screen.getByLabelText(ADDRESS_FIELD).props.value).toBe('Near the old bridge');
  });

  it('WF2P-S03e (P214): a description of spaces with an address is refused too', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await type('Description *', '   ');
    await type(ADDRESS_FIELD, 'Near the old bridge');

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.queryByText('Review your report')).toBeNull();
    expect(
      screen.getByText('Enter a description of what you saw (at most 500 characters)'),
    ).toBeTruthy();
  });

  it('WF2P-S03e: with a description and an address the review is reached, and shows the stored text with its "Address:" line', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await type('Description *', 'Water is rising');
    await type(ADDRESS_FIELD, 'Near the old bridge');

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('Review your report')).toBeTruthy();
    expect(screen.getByText('Water is rising\nAddress: Near the old bridge')).toBeTruthy();
  });

  it('WF2P-S04 / A3 17b: Edit from the review brings both parts back into their fields', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await type('Description *', 'Water is rising');
    await type(ADDRESS_FIELD, 'Near the old bridge');
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Edit' })));

    expect(screen.getByLabelText('Description *').props.value).toBe('Water is rising');
    expect(screen.getByLabelText(ADDRESS_FIELD).props.value).toBe('Near the old bridge');
  });

  it('WF2P-S03f / SC2 E8 18l: a reopened REJECTED draft shows its two parts in their fields', async () => {
    const rig = makeRig();
    const rejected = {
      ...FILLED_DRAFT,
      description: 'Line one\nLine two\nAddress: Near the old bridge',
      locationFromGps: false,
      status: 'REJECTED' as const,
      rejectionReason: 'bad photo',
    };
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, pendingDrafts: [rejected] });
    rig.controller.getDraft.mockResolvedValue(rejected);
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open' })));

    expect(screen.getByLabelText('Description *').props.value).toBe('Line one\nLine two');
    expect(screen.getByLabelText(ADDRESS_FIELD).props.value).toBe('Near the old bridge');
    expect(ui().draft?.description).toBe(rejected.description);
  });

  it('WF2P-S03: the address field is limited to 200 characters', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    expect(screen.getByLabelText(ADDRESS_FIELD).props.maxLength).toBe(200);

    await type(ADDRESS_FIELD, 'x'.repeat(250));

    expect(ui().addressText).toHaveLength(200);
    expect(ui().draft?.description).toBe(`\nAddress: ${'x'.repeat(200)}`);
  });

  it('WF2P-S03e: a description and an address that together pass 500 characters are refused by the existing rule', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockImplementation((draft) =>
      (draft.description ?? '').length > 500
        ? { valid: false, errors: ['description: at most 500 characters'] }
        : { valid: true, errors: [] },
    );
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await type('Description *', 'x'.repeat(480));
    await type(ADDRESS_FIELD, 'y'.repeat(100));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('590 / 500')).toBeTruthy();
    expect(
      screen.getByText('Enter a description of what you saw (at most 500 characters)'),
    ).toBeTruthy();
  });

  it('WF2P-S05 / SC2 step 18: the submitted draft carries the composed description', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await type('Description *', 'Water is rising');
    await type(ADDRESS_FIELD, 'Near the old bridge');
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(rig.controller.submit).toHaveBeenCalledWith(
      expect.objectContaining({ description: 'Water is rising\nAddress: Near the old bridge' }),
    );
  });

  it('WF2P-S02: a new report starts with both fields empty', async () => {
    const rig = makeRig();
    await toFormWithoutFix(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));
    await type('Description *', 'Water');
    await type(ADDRESS_FIELD, 'Bridge');

    await act(async () => {
      ui().startReport();
    });

    expect(ui().descriptionText).toBe('');
    expect(ui().addressText).toBe('');
  });
});
