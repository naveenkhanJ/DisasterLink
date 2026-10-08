import { fireEvent, screen } from '@testing-library/react-native';
import { Linking } from 'react-native';
import { withStatus } from '../../report/ReportDraft';
import { FILLED_DRAFT, KALUTARA_BRIDGE, makeRig, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const NOW = '2026-05-21T02:10:00.000Z';
const SINHALA_REASON = 'photo: not a readable JPEG or PNG image';

async function openForm(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

describe('hazard-type list and report form', () => {
  it('WF2P-S02 / SC2 steps 6-7: lists the seven hazard types with the capture time recorded automatically', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));

    expect(
      screen.getByText('Capture time 21 May 2026, 07:40 — recorded automatically'),
    ).toBeTruthy();
    for (const label of [
      'Flood',
      'Landslide',
      'Cyclone',
      'Tsunami',
      'Drought',
      'Blocked road',
      'Other',
    ]) {
      expect(screen.getByRole('button', { name: label })).toBeTruthy();
    }
  });

  it('WF2P-S03 / SC2 steps 7-8, 12-14: the form opens with the hazard type and the GPS location as text', async () => {
    const rig = makeRig();

    await openForm(rig);

    expect(screen.getByText('Report a hazard')).toBeTruthy();
    expect(screen.getByText('Flood')).toBeTruthy();
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText('GPS · accuracy ±8 m')).toBeTruthy();
    expect(screen.getByText('0 / 500')).toBeTruthy();
    expect(rig.controller.captureLocation).toHaveBeenCalledTimes(2); // the home, then the form
  });

  it('WF2P-S03 / SC2 step 9: typing the description updates the counter and the draft', async () => {
    const rig = makeRig();
    await openForm(rig);

    await run(() =>
      fireEvent.changeText(screen.getByLabelText('Description *'), 'Water is rising'),
    );

    expect(screen.getByText('15 / 500')).toBeTruthy();
    expect(ui().draft?.description).toBe('Water is rising');
  });

  it('WF2P-S03 / SC2 steps 10-11: Take photo attaches the copy and shows "1 of 1"', async () => {
    const rig = makeRig();
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    expect(rig.photos.takePhoto).toHaveBeenCalledWith('draft-1');
    expect(screen.getByText('1 of 1')).toBeTruthy();
    expect(ui().draft?.photoUri).toBe('file:///documents/draft-1.jpg');
  });

  it('WF2P-S03 / SC2 step 10: Choose from gallery attaches the picked copy', async () => {
    const rig = makeRig();
    rig.photos.choosePhoto.mockResolvedValue({ kind: 'photo', uri: 'file:///documents/g.jpg' });
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Choose from gallery' })));

    expect(ui().draft?.photoUri).toBe('file:///documents/g.jpg');
  });

  it('WF2P-S03 / SC2 step 10: a cancelled picker changes nothing', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'cancelled' });
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    expect(ui().draft?.photoUri).toBeUndefined();
    expect(screen.getByText('0 of 1')).toBeTruthy();
    expect(screen.queryByText('Allow camera access to take the required photo')).toBeNull();
  });

  it('WF2P-S03d / SC2 E2 11d: a refused camera permission shows the allow-access notice and the gallery still works', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'permissionDenied' });
    const openSettings = jest.spyOn(Linking, 'openSettings').mockResolvedValue(undefined);
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    expect(screen.getByText('Allow camera access to take the required photo')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Allow camera access' })));
    expect(openSettings).toHaveBeenCalledTimes(1);
    await run(() =>
      fireEvent.press(screen.getAllByRole('button', { name: 'Choose from gallery' })[0]),
    );
    expect(screen.queryByText('Allow camera access to take the required photo')).toBeNull();
    expect(ui().draft?.photoUri).toBe('file:///documents/draft-1.jpg');
  });

  it('WF2P-S03d: a refused gallery permission shows its own notice without the gallery button', async () => {
    const rig = makeRig();
    rig.photos.choosePhoto.mockResolvedValue({ kind: 'permissionDenied' });
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Choose from gallery' })));

    expect(screen.getByText('Allow photo access to choose the required photo')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Allow photo access' })).toBeTruthy();
  });

  it('WF2P-S03 / SC2 A3 17c: Change goes back to the list and a new type keeps the form values', async () => {
    const rig = makeRig();
    await openForm(rig);
    await run(() =>
      fireEvent.changeText(screen.getByLabelText('Description *'), 'Water is rising'),
    );

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Landslide' })));

    expect(screen.getByText('Landslide')).toBeTruthy();
    expect(ui().draft?.description).toBe('Water is rising');
    expect(ui().draft?.location).toEqual(KALUTARA_BRIDGE);
    expect(rig.controller.captureLocation).toHaveBeenCalledTimes(2);
  });

  it('WF2P-S03e / SC2 E1 16a-16c: an empty description is shown under its field, with the other values kept', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['description: a description is required'],
    });
    await openForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(
      screen.getByText('Enter a description of what you saw (at most 500 characters)'),
    ).toBeTruthy();
    expect(screen.getByText('Please correct the field marked above')).toBeTruthy();
    expect(screen.getByText('1 of 1')).toBeTruthy();
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText('Flood')).toBeTruthy();
  });

  it('WF2P-S03e: a missing hazard type and location are worded under their fields', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: [
        'hazardType: a hazard type is required',
        'location: a location is required',
        'other: x',
      ],
    });
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('Choose what you saw')).toBeTruthy();
    expect(screen.getByText('GPS location is unavailable')).toBeTruthy();
    expect(screen.getByText('other: x')).toBeTruthy();
  });

  it('WF2P-S03e: a location error without a manual-entry block shows under the location', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockImplementation(() => new Promise(() => undefined));
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['location: a location is required'],
    });
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('Finding your location…')).toBeTruthy();
    expect(screen.getByText('Enter the location of what you saw')).toBeTruthy();
  });

  it('WF2P-S03c / SC2 E2 11a-11c: an unreadable photo shows the photo error and the buttons stay enabled', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({ valid: false, errors: [SINHALA_REASON] });
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('This image cannot be used — choose a JPEG or PNG photo')).toBeTruthy();
    expect(screen.getByText('0 of 1')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    expect(screen.queryByText('This image cannot be used — choose a JPEG or PNG photo')).toBeNull();
  });

  it('WF2P-S03c: a missing photo is worded as the photo error', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockReturnValue({
      valid: false,
      errors: ['photo: one photo is required', 'photo: another reason'],
    });
    await openForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));

    expect(screen.getByText('another reason')).toBeTruthy();
  });

  it('WF2P-S03f / SC2 E8 18l-18n: a rejected draft opens with its reason, its values and Choose another', async () => {
    const rig = makeRig();
    const rejected = {
      ...withStatus(FILLED_DRAFT, 'QUEUED'),
      status: 'REJECTED' as const,
      rejectionReason: SINHALA_REASON,
    };
    rig.controller.loadHome.mockResolvedValue({
      warnings: [],
      myReports: [],
      pendingDrafts: [rejected],
      language: 'ENGLISH',
      fromCache: false,
    });
    rig.controller.getDraft.mockResolvedValue(rejected);
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open' })));

    expect(rig.controller.getDraft).toHaveBeenCalledWith('draft-1');
    expect(
      screen.getByText(`Not accepted by the DMC: Validation failed — ${SINHALA_REASON}`),
    ).toBeTruthy();
    expect(screen.getByDisplayValue(FILLED_DRAFT.description as string)).toBeTruthy();
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Choose another' })));
    expect(rig.photos.choosePhoto).toHaveBeenCalledWith('draft-1');
    expect(ui().draft?.draftId).toBe('draft-1');
  });

  it('WF2P-S01d / SC2 E8 18o: Discard draft removes the draft and the home confirms that nothing was sent', async () => {
    const rig = makeRig();
    const rejected = { ...FILLED_DRAFT, status: 'REJECTED' as const, rejectionReason: 'bad photo' };
    rig.controller.loadHome.mockResolvedValueOnce({
      warnings: [],
      myReports: [],
      pendingDrafts: [rejected],
      language: 'ENGLISH',
      fromCache: false,
    });
    rig.controller.getDraft.mockResolvedValue(rejected);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Discard draft' })));

    expect(rig.controller.discardDraft).toHaveBeenCalledWith('draft-1');
    expect(screen.getByText('Draft discarded — nothing was sent')).toBeTruthy();
    expect(screen.getByText('Nothing waiting')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));
    expect(screen.queryByText('Draft discarded — nothing was sent')).toBeNull();
  });

  it('SC2 E8 18l: opening a draft that is gone returns to the home', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().openDraft('missing'));

    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
  });

  it('SC2 steps 7-14: inputs without a draft change nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().selectHazardType('FLOOD'));
    await run(() => ui().pickPhoto('camera'));
    await run(() => ui().enterDescription('x'));
    await run(() => ui().attachPhoto('file:///x.jpg'));
    await run(() => ui().enterLocationManually(KALUTARA_BRIDGE));
    await run(() => ui().reviewReport());
    await run(() => ui().submitReport());

    expect(ui().draft).toBeUndefined();
    expect(rig.controller.submit).not.toHaveBeenCalled();
  });

  it('SC2 step 13: a fix that arrives after the draft was abandoned is ignored', async () => {
    const rig = makeRig();
    let deliver: (fix: typeof KALUTARA_BRIDGE) => void = () => undefined;
    rig.controller.captureLocation
      .mockResolvedValueOnce(undefined)
      .mockImplementationOnce(() => new Promise((resolve) => (deliver = resolve)));
    rig.controller.startReport
      .mockReturnValueOnce({ draftId: 'first', locationFromGps: false, capturedAt: NOW })
      .mockReturnValueOnce({ draftId: 'second', locationFromGps: false, capturedAt: NOW });
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));

    await run(() => ui().startReport());
    await run(() => deliver(KALUTARA_BRIDGE));

    expect(ui().draft?.location).toBeUndefined();
  });
});
