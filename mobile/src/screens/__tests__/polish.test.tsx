import { act, fireEvent, screen } from '@testing-library/react-native';
import { Platform, StyleSheet } from 'react-native';
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

/** A promise that the test settles by hand. */
function deferred<T>(): { promise: Promise<T>; resolve: (value: T) => void } {
  let resolve: (value: T) => void = () => undefined;
  const promise = new Promise<T>((done) => (resolve = done));
  return { promise, resolve };
}

/** Home → Report Hazard → Flood → description → photo → Review (WF2P-S04). */
async function toReview(rig: Rig): Promise<void> {
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
  await run(() => fireEvent.changeText(screen.getByLabelText('Description *'), 'Water is rising'));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
}

const SENT = {
  submissionId: 'draft-1',
  outcome: 'ACCEPTED' as const,
  message: 'RPT-2026-COL-0905',
};

describe('the sync result on every screen and S05c (standards §10 item 6)', () => {
  it('WF2P-S05c / SC2 A2 18d-18f: the confirmation of a queued draft becomes "Report sent" when its draft is accepted', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));
    expect(screen.getByText('Saved on this phone — Pending synchronisation')).toBeTruthy();
    rig.controller.syncPending.mockResolvedValue([SENT]);

    await run(() => ui().onConnectivityRestored());

    expect(screen.getByText('Report sent')).toBeTruthy();
    expect(screen.getAllByText('RPT-2026-COL-0905').length).toBeGreaterThan(0);
    expect(screen.getByText('Pending verification')).toBeTruthy();
    expect(screen.queryByText('Saved on this phone — Pending synchronisation')).toBeNull();
    expect(screen.queryByText(/It will be sent automatically/)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Back to home' })).toBeNull();
  });

  it('WF2P-S05c: "Track report status" opens the report, and Back returns to the confirmation', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));
    rig.controller.syncPending.mockResolvedValue([SENT]);
    await run(() => ui().onConnectivityRestored());

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Track report status' })));
    expect(rig.controller.getReportStatus).toHaveBeenCalledWith('RPT-2026-COL-0905');
    expect(screen.getByText('Report status')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Back' })));

    expect(screen.getByText('Report sent')).toBeTruthy();
    expect(screen.getAllByText('RPT-2026-COL-0905').length).toBeGreaterThan(0);
    expect(screen.queryByText('Saved on this phone — Pending synchronisation')).toBeNull();
    expect(ui().sentReportId).toBe('RPT-2026-COL-0905');
  });

  it('WF2P-S05c: the Home tab resets the form, so the report ID of the sent draft is forgotten', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));
    rig.controller.syncPending.mockResolvedValue([SENT]);
    await run(() => ui().onConnectivityRestored());
    expect(ui().sentReportId).toBe('RPT-2026-COL-0905');

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /^Home/ })));

    expect(ui().sentReportId).toBeUndefined();
    expect(ui().result).toBeUndefined();
  });

  it('WF2P-S05c: a new submission replaces the stored result, and the old report ID with it', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));
    rig.controller.syncPending.mockResolvedValue([SENT]);
    await run(() => ui().onConnectivityRestored());

    await run(() => ui().startReport());

    expect(ui().sentReportId).toBeUndefined();
  });

  it('WF2P-S05c: the same screen also changes when the server answers DUPLICATE for its draft', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));
    rig.controller.syncPending.mockResolvedValue([{ ...SENT, outcome: 'DUPLICATE' }]);

    await run(() => ui().onConnectivityRestored());

    expect(screen.getByText('Report sent')).toBeTruthy();
  });

  it('WF2P-S05a: the confirmation does not change for another draft, or for a result with no report ID', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    rig.controller.syncPending.mockResolvedValue([{ ...SENT, submissionId: 'other' }]);
    await run(() => ui().onConnectivityRestored());
    rig.controller.syncPending.mockResolvedValue([
      { submissionId: 'draft-1', outcome: 'ACCEPTED' },
    ]);
    await run(() => ui().onConnectivityRestored());
    rig.controller.syncPending.mockResolvedValue([
      { submissionId: 'draft-1', outcome: 'REJECTED', message: 'x' },
    ]);
    await run(() => ui().onConnectivityRestored());

    expect(screen.getByText('Saved on this phone — Pending synchronisation')).toBeTruthy();
  });

  it('WF2P-S05c: a draft sent while another screen is open does not touch that screen', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await toReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Back to home' })));
    rig.controller.syncPending.mockResolvedValue([SENT]);

    await run(() => ui().onConnectivityRestored());

    expect(ui().sentReportId).toBeUndefined();
  });

  it('WF2P-S01b / SC2 A2 18f: the sync-result banner is drawn on every screen, not only on the home', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    rig.controller.syncPending.mockResolvedValue([SENT]);

    await run(() => ui().onConnectivityRestored());

    expect(screen.getByText('What did you see?')).toBeTruthy();
    expect(screen.getByText('Report sent — RPT-2026-COL-0905, Pending verification')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));
    expect(screen.queryByText(/Report sent —/)).toBeNull();
  });
});

describe('the keyboard (standards §10 item 3; iPhone only)', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('standards §10 item 3: the scroll view moves the typed field above the keyboard, stays scrollable and is not closed by a drag', async () => {
    await renderApp(makeRig());

    const scroll = screen.getByTestId('screen-scroll');

    expect(scroll.props.automaticallyAdjustKeyboardInsets).toBe(true);
    expect(scroll.props.keyboardDismissMode).toBe('none');
    expect(scroll.props.keyboardShouldPersistTaps).toBe('handled');
    expect(screen.queryByTestId('keyboard-avoiding')).toBeNull();
  });

  it('standards §10 item 3: on Android the scroll view is wrapped in a KeyboardAvoidingView (its behavior prop is not visible in a test)', async () => {
    jest.replaceProperty(Platform, 'OS', 'android');

    await renderApp(makeRig());

    expect(screen.getByTestId('keyboard-avoiding')).toBeTruthy();
    expect(screen.getByTestId('screen-scroll')).toBeTruthy();
  });
});

describe('progress and refresh (standards §10 items 4 and 5)', () => {
  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('standards §10 item 4: "Loading…" shows while the home loads and goes when it is done', async () => {
    const rig = makeRig();
    const pending = deferred<typeof ONLINE_HOME>();
    rig.controller.loadHome.mockReturnValue(pending.promise);

    await renderApp(rig);
    expect(ui().loading).toBe(true);
    expect(screen.getByText('Loading…')).toBeTruthy();
    await run(() => pending.resolve(ONLINE_HOME));

    expect(ui().loading).toBe(false);
    expect(screen.queryByText('Loading…')).toBeNull();
  });

  it('standards §10 item 4: overlapping actions keep the flag until the last one is done', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const first = deferred<undefined>();
    const second = deferred<undefined>();
    rig.controller.changeLanguage
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise);

    await act(async () => {
      void ui().selectLanguage('TAMIL');
      void ui().selectLanguage('SINHALA');
    });
    expect(ui().loading).toBe(true);
    await run(() => first.resolve(undefined));
    expect(ui().loading).toBe(true);
    expect(screen.getByText('Loading…')).toBeTruthy();
    await run(() => second.resolve(undefined));

    expect(ui().loading).toBe(false);
  });

  it('standards §10 item 4: a failed action clears the flag too', async () => {
    const rig = makeRig();
    await renderApp(rig);
    rig.controller.getReportStatus.mockRejectedValue(new Error('down'));

    await run(() => ui().openReportStatus('RPT-1'));

    expect(ui().loading).toBe(false);
  });

  it('standards §10 item 4: Submit Report reads "Sending…" and is disabled while the upload runs', async () => {
    const rig = makeRig();
    const upload = deferred<Awaited<ReturnType<typeof rig.controller.submit>>>();
    rig.controller.submit.mockReturnValue(upload.promise);
    await toReview(rig);

    await act(async () => {
      fireEvent.press(screen.getByRole('button', { name: 'Submit Report' }));
    });

    const sending = screen.getByRole('button', { name: 'Sending…' });
    expect(sending.props.accessibilityState.disabled).toBe(true);
    await run(() =>
      upload.resolve({ draftId: 'draft-1', state: 'SUBMITTED', report: REPORT, errors: [] }),
    );
    expect(screen.getByText('Report submitted')).toBeTruthy();
  });

  it('standards §10 item 5: pulling the home down reloads the home and the location and leaves the draft alone', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const pull = screen.getByTestId('screen-scroll').props.refreshControl.props;
    expect(pull.refreshing).toBe(false);

    await run(() => pull.onRefresh());

    expect(rig.controller.loadHome).toHaveBeenCalledTimes(2);
    expect(rig.controller.captureLocation).toHaveBeenCalledTimes(2);
    expect(rig.controller.startReport).not.toHaveBeenCalled();
    expect(ui().refreshing).toBe(false);
  });

  it('standards §10 item 5: the refresh indicator shows while the home reloads', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const pending = deferred<typeof ONLINE_HOME>();
    rig.controller.loadHome.mockReturnValue(pending.promise);

    await act(async () => {
      void ui().refreshHomeScreen();
    });
    expect(ui().refreshing).toBe(true);
    expect(screen.getByTestId('screen-scroll').props.refreshControl.props.refreshing).toBe(true);
    await run(() => pending.resolve(ONLINE_HOME));

    expect(ui().refreshing).toBe(false);
  });

  it('standards §10 item 5: pulling the report status down reads the report again', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus.mockResolvedValueOnce(REPORT).mockResolvedValueOnce({
      ...REPORT,
      status: 'VERIFIED',
      verificationTime: '2026-05-21T08:15:00+05:30',
    });
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    expect(screen.getByText('Pending verification')).toBeTruthy();

    await run(() => screen.getByTestId('screen-scroll').props.refreshControl.props.onRefresh());

    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(2);
    expect(screen.getByText('Verified')).toBeTruthy();
    expect(ui().refreshing).toBe(false);
  });

  it('standards §10 item 5 (N1): a pull on the status shows the pull indicator only, not "Loading…"', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));
    const again = deferred<typeof REPORT>();
    rig.controller.getReportStatus.mockReturnValue(again.promise);

    await act(async () => {
      void ui().refreshReportStatus();
    });
    expect(ui().refreshing).toBe(true);
    expect(ui().loading).toBe(false);
    expect(screen.queryByText('Loading…')).toBeNull();
    await run(() => again.resolve(REPORT));

    expect(ui().refreshing).toBe(false);
  });

  it('standards §10 item 4: opening a status still shows "Loading…" (only a pull does not)', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const first = deferred<typeof REPORT>();
    rig.controller.getReportStatus.mockReturnValue(first.promise);

    await act(async () => {
      void ui().openReportStatus('RPT-2026-KLT-0847');
    });
    expect(ui().loading).toBe(true);
    await run(() => first.resolve(REPORT));

    expect(ui().loading).toBe(false);
  });

  it('standards §10 item 5: refreshing the status on another screen does nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().refreshReportStatus());

    expect(rig.controller.getReportStatus).not.toHaveBeenCalled();
  });

  it('standards §10 item 5 / SC2 step 30: a decision for the open report reads it again; one for another report does not', async () => {
    const rig = makeRig();
    rig.controller.getReportStatus
      .mockResolvedValueOnce(REPORT)
      .mockResolvedValueOnce({ ...REPORT, status: 'REJECTED', rejectionReason: 'Duplicate' });
    await renderApp(rig);
    await run(() => ui().openReportStatus('RPT-2026-KLT-0847'));

    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-OTHER',
        status: 'VERIFIED',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );
    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(1);
    await run(() =>
      ui().receiveReportDecision({
        reportId: 'RPT-2026-KLT-0847',
        status: 'REJECTED',
        reason: 'Duplicate',
        decidedAt: '2026-05-21T08:15:00+05:30',
      }),
    );

    expect(rig.controller.getReportStatus).toHaveBeenCalledTimes(2);
    expect(screen.getByText('Rejected')).toBeTruthy();
    expect(screen.getByText('Duplicate')).toBeTruthy();
  });
});

describe('alerts (standards §10 item 7)', () => {
  it('standards §10 item 7: only the two newest alerts are drawn, with a line that counts the rest', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().receiveAlert('W-1', 'LEVEL_1_YELLOW', 'first'));
    await run(() => ui().receiveAlert('W-2', 'LEVEL_2_AMBER', 'second'));
    await run(() => ui().receiveAlert('W-3', 'LEVEL_3_RED', 'third'));

    expect(screen.getAllByText('Warning alert')).toHaveLength(2);
    expect(screen.queryByText('first')).toBeNull();
    expect(screen.getByText('second')).toBeTruthy();
    expect(screen.getByText('third')).toBeTruthy();
    expect(screen.getByText('1 more — see Active warnings on Home')).toBeTruthy();
    expect(ui().alerts).toHaveLength(3);
  });

  it('standards §10 item 7: closing one reveals the next, and the counting line goes', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().receiveAlert('W-1', 'LEVEL_1_YELLOW', 'first'));
    await run(() => ui().receiveAlert('W-2', 'LEVEL_2_AMBER', 'second'));
    await run(() => ui().receiveAlert('W-3', 'LEVEL_3_RED', 'third'));

    await run(() => fireEvent.press(screen.getAllByRole('button', { name: 'Close alert' })[1]));

    expect(screen.getByText('first')).toBeTruthy();
    expect(screen.getByText('second')).toBeTruthy();
    expect(screen.queryByText(/more — see Active warnings/)).toBeNull();
  });

  it('standards §10 item 7: the counting line goes to the home, through the discard question', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    await run(() => fireEvent.changeText(screen.getByLabelText('Description *'), 'Water'));
    await run(() => ui().receiveAlert('W-1', 'LEVEL_1_YELLOW', 'first'));
    await run(() => ui().receiveAlert('W-2', 'LEVEL_2_AMBER', 'second'));
    await run(() => ui().receiveAlert('W-3', 'LEVEL_3_RED', 'third'));

    await run(() => fireEvent.press(screen.getByText('1 more — see Active warnings on Home')));
    expect(screen.getByText('Discard this report?')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Discard' })));

    expect(screen.getByText('Not sent yet')).toBeTruthy();
  });
});

describe('the photo (standards §10 item 9)', () => {
  /** The form with a photo attached. */
  async function toPhoto(rig: Rig): Promise<void> {
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
  }

  it('WF2P-S03: the photo tile is full width and 180 high', async () => {
    const rig = makeRig();

    await toPhoto(rig);

    const style = StyleSheet.flatten(screen.getByLabelText('Photo of the hazard').props.style);
    expect(style.width).toBe('100%');
    expect(style.height).toBe(180);
  });

  it('WF2P-S03: the review screen keeps its smaller photo', async () => {
    const rig = makeRig();
    await toReview(rig);

    const style = StyleSheet.flatten(screen.getByLabelText('Photo of the hazard').props.style);

    expect(style.width).toBe(160);
    expect(screen.queryByRole('button', { name: 'Open photo' })).toBeNull();
  });

  it('WF2P-S03: a tap on the photo opens the full-size preview, and Close shuts it', async () => {
    const rig = makeRig();
    await toPhoto(rig);
    expect(screen.queryByLabelText('Photo preview')).toBeNull();

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open photo' })));
    expect(screen.getByLabelText('Photo preview').props.resizeMode).toBe('contain');
    expect(screen.getByRole('button', { name: 'Remove photo' })).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Close' })));

    expect(screen.queryByLabelText('Photo preview')).toBeNull();
    expect(ui().draft?.photoUri).toBe('file:///documents/draft-1.jpg');
  });

  it('WF2P-S03: "Remove photo" clears the photo and deletes the copy of an unsaved draft; Review Report then asks for a photo', async () => {
    const rig = makeRig();
    rig.controller.validateDraft.mockImplementation((draft) =>
      draft.photoUri
        ? { valid: true, errors: [] }
        : { valid: false, errors: ['photo: one photo is required'] },
    );
    await toPhoto(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open photo' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Remove photo' })));

    expect(ui().draft?.photoUri).toBeUndefined();
    expect(rig.photos.deletePhoto).toHaveBeenCalledWith('file:///documents/draft-1.jpg');
    expect(screen.getByText('0 of 1')).toBeTruthy();
    expect(screen.queryByLabelText('Photo preview')).toBeNull();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
    expect(screen.getByText('Add one photo of what you saw')).toBeTruthy();
  });

  it('WF2P-S03f: "Remove photo" on a stored rejected draft clears the photo but keeps the file', async () => {
    const rig = makeRig();
    const rejected = { ...FILLED_DRAFT, status: 'REJECTED' as const, rejectionReason: 'bad photo' };
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, pendingDrafts: [rejected] });
    rig.controller.getDraft.mockResolvedValue(rejected);
    await renderApp(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open' })));
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Open photo' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Remove photo' })));

    expect(ui().draft?.photoUri).toBeUndefined();
    expect(rig.photos.deletePhoto).not.toHaveBeenCalled();
  });

  it('WF2P-S03: removing a photo that is not there does nothing', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().removePhoto());

    expect(rig.photos.deletePhoto).not.toHaveBeenCalled();
  });
});
