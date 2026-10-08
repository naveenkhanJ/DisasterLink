import { fireEvent, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import type { ReportDecisionNotice } from '../../api/reportApi';
import { presentLocalAlert } from '../../notifications/notifications';
import { decisionNoticeText } from '../texts';
import { makeRig, ONLINE_HOME, renderApp, run, ui, WARNING, type Rig } from './support';

jest.mock('../../notifications/notifications', () => ({
  presentLocalAlert: jest.fn(async () => undefined),
}));

const SINHALA_FONT = 'NotoSansSinhala';
const TAMIL_FONT = 'NotoSansTamil';

/** The decision notice used by the card tests. */
const NOTICE: ReportDecisionNotice = {
  reportId: 'RPT-2026-KLT-0847',
  status: 'VERIFIED',
  decidedAt: '2026-05-21T08:15:00+05:30',
};

/** The home with the warning, in the language given. */
const homeIn = (language: 'SINHALA' | 'TAMIL' | 'ENGLISH') => ({ ...ONLINE_HOME, language });

/** The font family of the text with the given content (all matches must agree). */
function fontsOf(text: string): (string | undefined)[] {
  return screen
    .getAllByText(text)
    .map((element) => StyleSheet.flatten(element.props.style).fontFamily);
}

/** Changes the selector to a language, as the citizen does: the server answers with that home. */
async function chooseLanguage(
  rig: Rig,
  language: 'SINHALA' | 'TAMIL' | 'ENGLISH',
  name: string,
): Promise<void> {
  rig.controller.loadHome.mockResolvedValue(homeIn(language));
  await run(() => fireEvent.press(screen.getByRole('radio', { name })));
}

describe('an alert banner follows the selected language (standards §10 item 18, D-UC2-8, P240)', () => {
  it('SC1 step 15 (P240): an alert for a warning on the home shows that warning in the selected language and follows the selector', async () => {
    const rig = makeRig();
    await renderApp(rig);
    expect(screen.getAllByText(WARNING.message.english)).toHaveLength(1);

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_2_AMBER', 'text as received'));

    expect(screen.queryByText('text as received')).toBeNull();
    expect(screen.getAllByText(WARNING.message.english)).toHaveLength(2);

    await chooseLanguage(rig, 'SINHALA', 'Sinhala');

    expect(screen.queryAllByText(WARNING.message.english)).toHaveLength(0);
    expect(screen.getAllByText(WARNING.message.sinhala)).toHaveLength(2);
    expect(fontsOf(WARNING.message.sinhala)).toEqual([SINHALA_FONT, SINHALA_FONT]);

    await chooseLanguage(rig, 'ENGLISH', 'English');

    expect(screen.getAllByText(WARNING.message.english)).toHaveLength(2);
    expect(screen.queryAllByText(WARNING.message.sinhala)).toHaveLength(0);
  });

  it('SC1 step 15 (P240): in Tamil the banner uses the Tamil text and face', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_2_AMBER', 'text as received'));

    await chooseLanguage(rig, 'TAMIL', 'Tamil');

    expect(fontsOf(WARNING.message.tamil)).toEqual([TAMIL_FONT, TAMIL_FONT]);
  });

  it('SC1 step 15 (P240): an alert for a warning that is not on the home keeps the text as received, whatever the language', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().receiveAlert('WRN-2026-0199', 'LEVEL_3_RED', 'text as received'));
    expect(screen.getByText('text as received')).toBeTruthy();

    await chooseLanguage(rig, 'SINHALA', 'Sinhala');

    expect(screen.getByText('text as received')).toBeTruthy();
    expect(screen.getAllByText('Level 3 – Red (Evacuate)').length).toBeGreaterThan(0);
  });

  it('SC1 step 15 (P241): an escalation alert at Level 3 while the home still holds the warning at Level 2 shows the text as received', async () => {
    const rig = makeRig();
    await renderApp(rig);
    // The reload after the alert has not happened (or failed): the home still holds Level 2.
    rig.controller.loadHome.mockReturnValue(new Promise(() => undefined));

    await run(() =>
      ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Level 3 text as received'),
    );

    expect(screen.getByText('Level 3 text as received')).toBeTruthy();
    expect(screen.getAllByText(WARNING.message.english)).toHaveLength(1);
  });

  it('SC1 step 15 (P241): after the home reloads with the warning at Level 3 the banner follows the selector', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const escalated = { ...WARNING, warningLevel: 'LEVEL_3_RED' as const };
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, warnings: [escalated] });

    // On the home the alert reloads the home, which now holds the warning at Level 3.
    await run(() =>
      ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'Level 3 text as received'),
    );

    expect(screen.queryByText('Level 3 text as received')).toBeNull();
    expect(screen.getAllByText(WARNING.message.english)).toHaveLength(2);

    rig.controller.loadHome.mockResolvedValue({
      ...ONLINE_HOME,
      language: 'SINHALA',
      warnings: [escalated],
    });
    await run(() => fireEvent.press(screen.getByRole('radio', { name: 'Sinhala' })));

    expect(screen.getAllByText(WARNING.message.sinhala)).toHaveLength(2);
    expect(fontsOf(WARNING.message.sinhala)).toEqual([SINHALA_FONT, SINHALA_FONT]);
  });

  it('SC1 step 15 (P241): an alert whose level matches the warning on the home follows the selector as before', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await run(() => ui().receiveAlert(WARNING.warningId, WARNING.warningLevel, 'text as received'));
    await chooseLanguage(rig, 'TAMIL', 'Tamil');

    expect(screen.queryByText('text as received')).toBeNull();
    expect(screen.getAllByText(WARNING.message.tamil)).toHaveLength(2);
  });

  it('SC1 step 15 (P240): the local notification is presented once, with the text it arrived with', async () => {
    const rig = makeRig();
    await renderApp(rig);
    jest.mocked(presentLocalAlert).mockClear();

    await run(() => ui().receiveAlert(WARNING.warningId, 'LEVEL_3_RED', 'text as received'));
    await chooseLanguage(rig, 'SINHALA', 'Sinhala');

    expect(presentLocalAlert).toHaveBeenCalledTimes(1);
    expect(presentLocalAlert).toHaveBeenCalledWith('Level 3 – Red (Evacuate)', 'text as received');
  });
});

describe('the decision card follows the selected language (standards §10 item 18, D-UC2-8, P240)', () => {
  it('WF2P-S01e (P240): the card switches between the three languages with the selector', async () => {
    const rig = makeRig();
    await renderApp(rig);
    await run(() => ui().receiveReportDecision(NOTICE));
    const english = decisionNoticeText(NOTICE, 'ENGLISH');
    const sinhala = decisionNoticeText(NOTICE, 'SINHALA');
    const tamil = decisionNoticeText(NOTICE, 'TAMIL');
    expect(screen.getByText(english)).toBeTruthy();

    await chooseLanguage(rig, 'SINHALA', 'Sinhala');
    expect(screen.queryByText(english)).toBeNull();
    expect(screen.getByText(sinhala)).toBeTruthy();
    expect(fontsOf(sinhala)).toEqual([SINHALA_FONT]);

    await chooseLanguage(rig, 'TAMIL', 'Tamil');
    expect(screen.queryByText(sinhala)).toBeNull();
    expect(screen.getByText(tamil)).toBeTruthy();
    expect(fontsOf(tamil)).toEqual([TAMIL_FONT]);

    await chooseLanguage(rig, 'ENGLISH', 'English');
    expect(screen.getByText(english)).toBeTruthy();
  });

  it("WF2P-S01f (P240): a rejection's reason stays as the officer typed it in every language", async () => {
    const rig = makeRig();
    await renderApp(rig);
    const rejected: ReportDecisionNotice = {
      ...NOTICE,
      status: 'REJECTED',
      reason: 'Duplicate of RPT-2026-KLT-0843',
    };
    await run(() => ui().receiveReportDecision(rejected));

    await chooseLanguage(rig, 'SINHALA', 'Sinhala');

    expect(
      screen.getByText(decisionNoticeText(rejected, 'SINHALA'), { exact: false }),
    ).toBeTruthy();
    expect(screen.getByText('Duplicate of RPT-2026-KLT-0843', { exact: false })).toBeTruthy();
  });

  it('WF2P-S01e (P240): the local notification is presented once, with the arrival text', async () => {
    const rig = makeRig();
    await renderApp(rig);
    jest.mocked(presentLocalAlert).mockClear();

    await run(() => ui().receiveReportDecision(NOTICE));
    await chooseLanguage(rig, 'SINHALA', 'Sinhala');

    expect(presentLocalAlert).toHaveBeenCalledTimes(1);
    expect(presentLocalAlert).toHaveBeenCalledWith(
      'Report decision',
      decisionNoticeText(NOTICE, 'ENGLISH'),
    );
  });
});

describe('the language segments are written in their own script (standards §10 item 18, D-UC2-8, P240)', () => {
  it('WF2P-S01 (D-UC2-8): the three labels and their accessibility names', async () => {
    await renderApp(makeRig());

    expect(screen.getByText('සිංහල')).toBeTruthy();
    expect(screen.getByText('தமிழ்')).toBeTruthy();
    expect(screen.getByText('English')).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'Sinhala' })).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'Tamil' })).toBeTruthy();
    expect(screen.getByRole('radio', { name: 'English' })).toBeTruthy();
    expect(screen.queryByText('Sinhala')).toBeNull();
    expect(screen.queryByText('Tamil')).toBeNull();
  });

  it('WF2P-S01 (D-UC2-8): each segment is drawn in the face of its script', async () => {
    await renderApp(makeRig());

    expect(fontsOf('සිංහල')).toEqual([SINHALA_FONT]);
    expect(fontsOf('தமிழ்')).toEqual([TAMIL_FONT]);
    expect(fontsOf('English')).toEqual(['Manrope']);
  });

  it('WF2P-S01 (D-UC2-8): the Sinhala and Tamil segments set no bold weight, selected or not; English keeps its weights', async () => {
    const rig = makeRig();
    await renderApp(rig);
    const weight = (text: string) =>
      StyleSheet.flatten(screen.getByText(text).props.style).fontWeight;
    expect(weight('English')).toBe('800');
    expect(weight('සිංහල')).toBe('normal');

    await chooseLanguage(rig, 'SINHALA', 'Sinhala');
    expect(weight('සිංහල')).toBe('normal');
    expect(weight('English')).toBe('600');

    await chooseLanguage(rig, 'TAMIL', 'Tamil');
    expect(weight('தமிழ்')).toBe('normal');
  });

  it('WF2P-S01 (D-UC2-8): the selector still changes the language and shows the selection', async () => {
    const rig = makeRig();
    await renderApp(rig);

    await chooseLanguage(rig, 'TAMIL', 'Tamil');

    expect(screen.getByRole('radio', { name: 'Tamil' }).props.accessibilityState.selected).toBe(
      true,
    );
    expect(screen.getByRole('radio', { name: 'English' }).props.accessibilityState.selected).toBe(
      false,
    );
    expect(rig.controller.changeLanguage).toHaveBeenCalledWith('TAMIL');
  });

  it('WF2P-S01a (D-UC2-8): on the saved copy every segment is disabled', async () => {
    const rig = makeRig();
    rig.controller.loadHome.mockResolvedValue({ ...ONLINE_HOME, fromCache: true });

    await renderApp(rig);

    for (const name of ['Sinhala', 'Tamil', 'English']) {
      expect(screen.getByRole('radio', { name }).props.accessibilityState.disabled).toBe(true);
    }
    expect(screen.getByText('Language can be changed when you are online')).toBeTruthy();
  });
});
