import { fireEvent, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { FILLED_DRAFT, makeRig, ONLINE_HOME, renderApp, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const FIRST = 'file:///documents/draft-1-1.jpg';
const SECOND = 'file:///documents/draft-1-2.jpg';

/** The form of a new report (WF2P-S03). */
async function toForm(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

/** The element types drawn inside a host element, to tell which icon a button has. */
function typesIn(element: { type?: unknown; children?: unknown[] }): string[] {
  const own = typeof element.type === 'string' ? [element.type] : [];
  const children = (element.children ?? []).filter(
    (child): child is { type?: unknown; children?: unknown[] } => typeof child === 'object',
  );
  return own.concat(...children.map(typesIn));
}

const shownUri = () => screen.getByLabelText('Photo of the hazard').props.source.uri;

describe('photo replace with visible controls (standards §10 item 9, P213; UC2-7c R4)', () => {
  it('WF2P-S03: without a photo the buttons read "Take photo" and "Choose from gallery", with no View, Remove or note', async () => {
    await toForm(makeRig());

    expect(screen.getByRole('button', { name: 'Take photo' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Choose from gallery' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'View' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Remove' })).toBeNull();
    expect(screen.queryByText('A new photo replaces this one')).toBeNull();
  });

  it('WF2P-S03: with a photo the buttons read "Retake photo" and "Choose another", and View and Remove are shown with the note', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'photo', uri: FIRST });
    await toForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    expect(screen.getByRole('button', { name: 'Retake photo' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Choose another' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'View' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Remove' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Open photo' })).toBeTruthy();
    expect(screen.getByText('A new photo replaces this one')).toBeTruthy();
  });

  it('WF2P-S03: a second photo replaces the first on screen and deletes the old copy of an unsaved draft', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValueOnce({ kind: 'photo', uri: FIRST });
    rig.photos.choosePhoto.mockResolvedValueOnce({ kind: 'photo', uri: SECOND });
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    expect(shownUri()).toBe(FIRST);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Choose another' })));

    expect(shownUri()).toBe(SECOND);
    expect(ui().draft?.photoUri).toBe(SECOND);
    expect(rig.photos.deletePhoto).toHaveBeenCalledTimes(1);
    expect(rig.photos.deletePhoto).toHaveBeenCalledWith(FIRST);
  });

  it('WF2P-S03: retaking with the camera replaces the photo the same way', async () => {
    const rig = makeRig();
    rig.photos.takePhoto
      .mockResolvedValueOnce({ kind: 'photo', uri: FIRST })
      .mockResolvedValueOnce({ kind: 'photo', uri: SECOND });
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retake photo' })));

    expect(shownUri()).toBe(SECOND);
    expect(rig.photos.deletePhoto).toHaveBeenCalledWith(FIRST);
  });

  it('WF2P-S03: a pick that gives the same file does not delete it', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'photo', uri: FIRST });
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Retake photo' })));

    expect(rig.photos.deletePhoto).not.toHaveBeenCalled();
  });

  it('WF2P-S03f: a stored rejected draft keeps its old file when the photo is replaced', async () => {
    const rig = makeRig();
    const rejected = { ...FILLED_DRAFT, status: 'REJECTED' as const, rejectionReason: 'bad photo' };
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, pendingDrafts: [rejected] });
    rig.controller.getDraft.mockResolvedValue(rejected);
    rig.photos.choosePhoto.mockResolvedValue({ kind: 'photo', uri: SECOND });
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Choose another' })));

    expect(ui().draft?.photoUri).toBe(SECOND);
    expect(rig.photos.deletePhoto).not.toHaveBeenCalled();
  });

  it('WF2P-S03: "View" opens the full-size preview', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'photo', uri: FIRST });
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'View' })));

    expect(screen.getByLabelText('Photo preview').props.source.uri).toBe(FIRST);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));
    expect(screen.queryByLabelText('Photo preview')).toBeNull();
  });

  it('WF2P-S03: "Remove" clears the photo at once, deletes the copy of an unsaved draft and shows "0 of 1"', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'photo', uri: FIRST });
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Remove' })));

    expect(ui().draft?.photoUri).toBeUndefined();
    expect(rig.photos.deletePhoto).toHaveBeenCalledWith(FIRST);
    expect(screen.getByText('0 of 1')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'View' })).toBeNull();
    expect(screen.queryByText('A new photo replaces this one')).toBeNull();
    expect(screen.getByRole('button', { name: 'Take photo' })).toBeTruthy();
  });

  it('WF2P-S03 (G1): the gallery button has the image icon (a rounded rectangle), the camera button its camera icon, and both buttons have the same width', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'photo', uri: FIRST });
    await toForm(rig);

    const gallery = screen.getByRole('button', { name: 'Choose from gallery' });
    const camera = screen.getByRole('button', { name: 'Take photo' });

    expect(typesIn(gallery).join(' ')).toContain('RNSVGRect');
    expect(typesIn(camera).join(' ')).not.toContain('RNSVGRect');
    expect(StyleSheet.flatten(gallery.props.style).flex).toBe(1);
    expect(StyleSheet.flatten(camera.props.style).flex).toBe(1);
    await run(() => fireEvent.press(camera));
    const another = screen.getByRole('button', { name: 'Choose another' });
    expect(typesIn(another).join(' ')).toContain('RNSVGRect');
    expect(StyleSheet.flatten(another.props.style).flex).toBe(1);
  });

  it('WF2P-S03 (G2): "Remove" under the photo is a danger button (red border, red text); "View" stays secondary', async () => {
    const rig = makeRig();
    rig.photos.takePhoto.mockResolvedValue({ kind: 'photo', uri: FIRST });
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    const remove = StyleSheet.flatten(screen.getByRole('button', { name: 'Remove' }).props.style);
    const view = StyleSheet.flatten(screen.getByRole('button', { name: 'View' }).props.style);

    expect(remove.borderColor).toBe('#EF4444');
    expect(remove.borderWidth).toBe(2);
    expect(view.borderColor).not.toBe('#EF4444');
  });
});
