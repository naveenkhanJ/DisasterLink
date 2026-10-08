import { act, fireEvent, screen } from '@testing-library/react-native';
import { RETURN_TO_REVIEW_MS } from '../../app/useCitizenAppUI';
import { FILLED_DRAFT, makeRig, renderApp, REPORT, run, ui, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

/** Opens the form, fills it as SC2 steps 7-15 do and reviews the report (WF2P-S04). */
async function reachReview(rig: Rig): Promise<void> {
  rig.controller.startReport.mockReturnValue({
    draftId: FILLED_DRAFT.draftId,
    locationFromGps: false,
    capturedAt: FILLED_DRAFT.capturedAt,
  });
  await renderApp(rig);
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Report Hazard' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Flood' })));
  await run(() =>
    fireEvent.changeText(
      screen.getByLabelText('Description *'),
      FILLED_DRAFT.description as string,
    ),
  );
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Take photo' })));
  await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
}

describe('review and submission', () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  it('WF2P-S04 / SC2 steps 15-16: a valid draft reaches the review screen with every value', async () => {
    const rig = makeRig();

    await reachReview(rig);

    expect(screen.getByText('Review your report')).toBeTruthy();
    expect(screen.getByText(FILLED_DRAFT.description as string)).toBeTruthy();
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    expect(screen.getByText('GPS · accuracy ±8 m')).toBeTruthy();
    expect(screen.getByText('21 May 2026, 07:40')).toBeTruthy();
    expect(screen.getByText('1 of 1')).toBeTruthy();
    expect(rig.controller.validateDraft).toHaveBeenCalledWith(
      expect.objectContaining({ draftId: 'draft-1', hazardType: 'FLOOD' }),
    );
  });

  it('WF2P-S04 / SC2 A3 17a-17d: Edit returns to the form with every value kept', async () => {
    const rig = makeRig();
    await reachReview(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Edit' })));

    expect(screen.getByDisplayValue(FILLED_DRAFT.description as string)).toBeTruthy();
    expect(screen.getByText('1 of 1')).toBeTruthy();
    expect(screen.getByText('6.5888 N, 79.9640 E')).toBeTruthy();
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Review Report' })));
    expect(screen.getByText('Review your report')).toBeTruthy();
  });

  it('WF2P-S05 / SC2 steps 17-20: an online submission shows the report ID and Pending verification', async () => {
    const rig = makeRig();
    await reachReview(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(rig.controller.submit).toHaveBeenCalledWith(
      expect.objectContaining({ draftId: 'draft-1', photoUri: FILLED_DRAFT.photoUri }),
    );
    expect(screen.getByText('Report submitted')).toBeTruthy();
    expect(screen.getByText('RPT-2026-KLT-0847')).toBeTruthy();
    expect(screen.getByText('Pending verification')).toBeTruthy();
  });

  it('WF2P-S05 / SC2 step 31: "Track report status" opens the report-status screen', async () => {
    const rig = makeRig();
    await reachReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Track report status' })));

    expect(rig.controller.getReportStatus).toHaveBeenCalledWith('RPT-2026-KLT-0847');
    expect(screen.getByText('Report status')).toBeTruthy();
  });

  it('WF2P-S05 / SC2 step 20: a submitted result with no report keeps the card and offers no tracking', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'SUBMITTED', errors: [] });
    await reachReview(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(screen.getByText('Report submitted')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Track report status' })).toBeNull();
  });

  it('WF2P-S05a / SC2 A2 18a-18c: offline, the report is saved on the phone as Pending synchronisation, with no report ID', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await reachReview(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(screen.getByText('Saved on this phone — Pending synchronisation')).toBeTruthy();
    expect(screen.getByText('21 May 2026, 07:40')).toBeTruthy();
    expect(
      screen.getByText(
        'It will be sent automatically when the app is open and the connection returns',
      ),
    ).toBeTruthy();
    expect(screen.queryByText('Report ID')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Track report status' })).toBeNull();
    expect(screen.queryByText('The service is unavailable and nothing was changed')).toBeNull();
  });

  it('WF2P-S05a / SC2 A2 18c: Back to home shows the home again', async () => {
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({ draftId: 'draft-1', state: 'QUEUED', errors: [] });
    await reachReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Back to home' })));

    expect(screen.getByText('My reports — waiting on this phone')).toBeTruthy();
    expect(rig.controller.loadHome).toHaveBeenCalledTimes(2);
  });

  it('WF2P-S05b / SC2 E6 19a-19c: a refused report shows the errors and returns to the review screen by itself', async () => {
    jest.useFakeTimers();
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({
      draftId: 'draft-1',
      state: 'INVALID',
      errors: ['photo: not a readable JPEG or PNG image'],
    });
    await reachReview(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(screen.getByText('Report not accepted')).toBeTruthy();
    expect(screen.getByText('photo: not a readable JPEG or PNG image')).toBeTruthy();
    expect(screen.getByText('Returning to your report…')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Back to home' })).toBeNull();
    await act(async () => {
      jest.advanceTimersByTime(RETURN_TO_REVIEW_MS);
    });
    expect(screen.getByText('Review your report')).toBeTruthy();
    expect(screen.getByText(FILLED_DRAFT.description as string)).toBeTruthy();
  });

  it('WF2P-S05b / SC2 E6 19c: the screen does not go back before the time is up', async () => {
    jest.useFakeTimers();
    const rig = makeRig();
    rig.controller.submit.mockResolvedValue({
      draftId: 'draft-1',
      state: 'INVALID',
      errors: ['x: y'],
    });
    await reachReview(rig);
    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    await act(async () => {
      jest.advanceTimersByTime(RETURN_TO_REVIEW_MS - 1);
    });

    expect(screen.getByText('Report not accepted')).toBeTruthy();
  });

  it('WF2P-S05b: an unexpected fault while sending is shown as a refusal, not as the common error banner', async () => {
    jest.useFakeTimers();
    const rig = makeRig();
    rig.controller.submit.mockRejectedValue(new TypeError('boom'));
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    await reachReview(rig);

    await run(() => fireEvent.press(screen.getByRole('button', { name: 'Submit Report' })));

    expect(screen.getByText('Report not accepted')).toBeTruthy();
    expect(screen.getByText('The report could not be prepared — try again')).toBeTruthy();
    expect(screen.queryByText('The service is unavailable and nothing was changed')).toBeNull();
  });

  it('SC2 step 17: a second press while the submission is running sends nothing more', async () => {
    const rig = makeRig();
    let finish: (value: Awaited<ReturnType<typeof rig.controller.submit>>) => void = () =>
      undefined;
    rig.controller.submit.mockImplementation(() => new Promise((resolve) => (finish = resolve)));
    await reachReview(rig);

    await act(async () => {
      fireEvent.press(screen.getByRole('button', { name: 'Submit Report' }));
    });
    await act(async () => {
      void ui().submitReport();
    });
    expect(ui().submitting).toBe(true);
    await run(() => finish({ draftId: 'draft-1', state: 'SUBMITTED', report: REPORT, errors: [] }));

    expect(rig.controller.submit).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Report submitted')).toBeTruthy();
  });

  it('SC2 steps 16-17: the tab Report starts a new draft from any screen', async () => {
    const rig = makeRig();
    await reachReview(rig);

    await run(() => fireEvent.press(screen.getByRole('tab', { name: /Report/ })));

    expect(screen.getByText('What did you see?')).toBeTruthy();
    expect(rig.controller.startReport).toHaveBeenCalledTimes(2);
  });
});
