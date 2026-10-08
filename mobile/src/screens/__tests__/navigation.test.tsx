import { act, fireEvent, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { withStatus } from '../../report/ReportDraft';
import {
  FILLED_DRAFT,
  makeRig,
  ONLINE_HOME,
  renderApp,
  REPORT,
  run,
  ui,
  type Rig,
} from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const back = () => screen.getByRole('button', { name: 'Back' });

/** Home → Report Hazard → Flood: the form (WF2P-S03). */
async function toForm(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
}

/** The form with a description typed (the draft has content). */
async function toFormWithContent(rig: Rig): Promise<void> {
  await toForm(rig);
  await run(() => fireEvent.changeText(screen.getByLabelText('Description *'), 'Water is rising'));
}

/** Runs `goBack` the way the Android Back button does and gives its answer. */
async function pressBack(): Promise<boolean> {
  let handled = false;
  await act(async () => {
    handled = ui().goBack();
  });
  return handled;
}

describe('the Back link, the discard question and Android Back (standards §10 items 1 and 2)', () => {
  it('standards §10 item 1: the home has no Back icon and does not handle Back', async () => {
    const rig = makeRig();
    await renderApp(rig);

    expect(screen.queryByRole('button', { name: 'Back' })).toBeNull();
    expect(await pressBack()).toBe(false);
  });

  it('standards §10 item 1: the confirmation screen has no Back link and does not handle Back', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(screen.getByText('Saved on this phone — Pending synchronisation')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Back' })).toBeNull();
    expect(await pressBack()).toBe(false);
  });

  it('WF2P-S02 (R2): the hazard-type list is the first screen of the Report tab and has no Back icon', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));

    expect(screen.getByText('What did you see?')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Back' })).toBeNull();
  });

  it('WF2P-S02: Android Back goes to the home, and an empty draft is dropped without a question', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));

    expect(await pressBack()).toBe(true);

    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
    expect(ui().draft).toBeUndefined();
  });

  it('WF2P-S02: Android Back asks first when the draft has content, and Discard goes home', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change' })));

    expect(await pressBack()).toBe(true);

    expect(screen.getByText('Discard this report?')).toBeTruthy();
    expect(screen.getByText('What you entered on this report will be lost.')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Discard' })));
    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
    expect(screen.queryByText('Discard this report?')).toBeNull();
  });

  it('WF2P-S03 (R2): the Back control is an icon with no word: a mirrored chevron of size 28 in a 44 × 44 touch area, labelled "Back"', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);

    const style = StyleSheet.flatten(back().props.style);

    expect(style.minWidth).toBe(44);
    expect(style.minHeight).toBe(44);
    expect(screen.queryByText(/Back/)).toBeNull();
    const tree = JSON.stringify(screen.toJSON());
    expect(tree).toContain('"scaleX":-1');
    expect(tree).toContain('"width":28');
  });

  it('WF2P-S05 / S05a / S05b / S05c (P214): no confirmation has a Back icon; each keeps its own button', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({
      draftId: 'draft-1',
      state: 'SUBMITTED',
      report: REPORT,
      errors: [],
    });
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(screen.getByText('Report submitted')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Back' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Track report status' })).toBeTruthy();
    expect(ui().route.name).toBe('submissionResult');
  });

  it('WF2P-S03: Back goes to the hazard-type list with the values kept, with no question', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);

    await run(() => fireEvent.press(back()));

    expect(screen.getByText('What did you see?')).toBeTruthy();
    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(ui().draft?.description).toBe('Water is rising');
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    expect(screen.getByDisplayValue('Water is rising')).toBeTruthy();
  });

  it('WF2P-S04: Back goes to the form', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
    expect(screen.getByText('Review your report')).toBeTruthy();

    await run(() => fireEvent.press(back()));

    expect(screen.getByText('Report a hazard')).toBeTruthy();
    expect(screen.getByDisplayValue('Water is rising')).toBeTruthy();
  });

  it('WF2P-S06: Back goes to the home when the status was opened from the home', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, myReports: [REPORT] });
    await renderApp(rig);
    await run(() =>
      fireEvent.press(screen.getByRole('button', { name: 'Open report RPT-2026-KLT-0847' })),
    );
    expect(screen.getByText('Report status')).toBeTruthy();

    await run(() => fireEvent.press(back()));

    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
  });

  it('WF2P-S06: Back goes to the confirmation when the status was opened from "Track report status"', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Track report status' })));
    expect(screen.getByText('Report status')).toBeTruthy();

    await run(() => fireEvent.press(back()));

    expect(screen.getByText('Report submitted')).toBeTruthy();
    expect(await pressBack()).toBe(false);
  });

  it('WF2P-S03f: Back from a reopened REJECTED draft goes home with no question, because it is stored', async () => {
    const rig = makeRig();
    const rejected = { ...FILLED_DRAFT, status: 'REJECTED' as const, rejectionReason: 'bad photo' };
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, pendingDrafts: [rejected] });
    rig.controller.getDraft.mockResolvedValue(rejected);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open' })));

    await run(() => fireEvent.press(back()));

    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
  });

  it('standards §10 item 2: no question for a draft that has only a GPS location', async () => {
    const rig = makeRig();
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Change' })));

    expect(await pressBack()).toBe(true);

    expect(screen.queryByText('Discard this report?')).toBeNull();
  });

  it('standards §10 item 2: a photo alone makes the draft worth asking about; Discard deletes its copy', async () => {
    const rig = makeRig();
    await toForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));
    expect(screen.getByText('Discard this report?')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Discard' })));

    expect(rig.photos.deletePhoto).toHaveBeenCalledWith('file:///documents/draft-1.jpg');
    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
  });

  it('standards §10 item 2: a manual location alone makes the draft worth asking about', async () => {
    const rig = makeRig();
    rig.controller.captureLocation.mockResolvedValue(undefined);
    await toForm(rig);
    await run(() => ui().enterLocationManually({ latitude: 6.5888, longitude: 79.964 }));

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Report/ })));

    expect(screen.getByText('Discard this report?')).toBeTruthy();
    expect(ui().draft?.locationFromGps).toBe(false);
  });

  it('standards §10 item 2: a blank description does not count as content', async () => {
    const rig = makeRig();
    await toForm(rig);
    await run(() => fireEvent.changeText(screen.getByLabelText('Description *'), '   '));

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));

    expect(screen.queryByText('Discard this report?')).toBeNull();
  });

  it('standards §10 item 2: Keep editing closes the question and stays; nothing is deleted', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Keep editing' })));

    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(screen.getByDisplayValue('Water is rising')).toBeTruthy();
    expect(rig.photos.deletePhoto).not.toHaveBeenCalled();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(1);
  });

  it('standards §10 item 2: the tab Report goes through the question too, then starts a new draft', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Report/ })));
    expect(rig.controller.startReport).toHaveBeenCalledTimes(1);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Discard' })));

    expect(rig.controller.startReport).toHaveBeenCalledTimes(2);
    expect(screen.getByText('What did you see?')).toBeTruthy();
  });

  it('standards §10 item 2: the tabs from a stored draft or from the home ask nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Report/ })));

    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(screen.getByText('What did you see?')).toBeTruthy();
  });

  it('standards §10 item 1: Android Back while the question is open means Keep editing', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));
    expect(screen.getByText('Discard this report?')).toBeTruthy();

    expect(await pressBack()).toBe(true);

    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(screen.getByDisplayValue('Water is rising')).toBeTruthy();
  });

  it('standards §10 item 1: Android Back follows the same history as the Back link', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);

    expect(await pressBack()).toBe(true);
    expect(screen.getByText('What did you see?')).toBeTruthy();
    expect(await pressBack()).toBe(true);
    expect(screen.getByText('Discard this report?')).toBeTruthy();
  });

  it('standards §10 item 2: a discard without a photo deletes nothing', async () => {
    const rig = makeRig();
    await toFormWithContent(rig);
    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Discard' })));

    expect(rig.photos.deletePhoto).not.toHaveBeenCalled();
  });

  it('standards §10 item 2: confirming with nothing waiting does nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().confirmDiscard());

    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
  });

  it('standards §10 item 2: a draft that was queued keeps its photo copy when its form is left', async () => {
    const rig = makeRig();
    const queued = withStatus(FILLED_DRAFT, 'QUEUED');
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, pendingDrafts: [queued] });
    await renderApp(rig);

    await run(() => ui().requestLeave(() => undefined));

    expect(rig.photos.deletePhoto).not.toHaveBeenCalled();
  });
});

describe('the decision card and the discard question (UC2-7b G2)', () => {
  const notice = {
    reportId: 'RPT-2026-KLT-0847',
    status: 'VERIFIED' as const,
    decidedAt: '2026-05-21T08:15:00+05:30',
  };

  /** The form with a description typed and a decision card on top of it. */
  async function toCardOverForm(rig: Rig): Promise<void> {
    await toFormWithContent(rig);
    await run(() => ui().receiveReportDecision(notice));
  }

  it('SC2 step 31 (G2): "Open report" over an unsaved draft with content asks first; Keep editing stays on the form', async () => {
    const rig = makeRig();
    await toCardOverForm(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open report' })));

    expect(screen.getByText('Discard this report?')).toBeTruthy();
    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Keep editing' })));
    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(screen.getByDisplayValue('Water is rising')).toBeTruthy();
    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
  });

  it('SC2 step 31 (G2): Discard opens the report status', async () => {
    const rig = makeRig();
    await toCardOverForm(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open report' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Discard' })));

    expect(rig.controller.getReportStatus).toHaveBeenCalledWith('RPT-2026-KLT-0847');
    expect(screen.getByText('Report status')).toBeTruthy();
    expect(screen.queryByText('Report decision')).toBeNull();
  });

  it('SC2 step 31: "Open report" on the home, or over a draft with no content, asks nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().receiveReportDecision(notice));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open report' })));

    expect(screen.queryByText('Discard this report?')).toBeNull();
    expect(screen.getByText('Report status')).toBeTruthy();
  });
});
