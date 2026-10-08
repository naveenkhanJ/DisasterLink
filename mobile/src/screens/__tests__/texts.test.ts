import {
  decisionNoticeText,
  districtLabel,
  formatCoordinates,
  formatDateTime,
  hasErrors,
  hazardTypeIcon,
  hazardTypeLabel,
  LEVEL_TONES,
  locationSource,
  LOCATION_OUTSIDE_TEXT,
  namedLocationText,
  placeRowText,
  CONNECTION_TEXTS,
  homeLocationReason,
  nearestPlaceLabel,
  stillUnavailableText,
  NO_ERRORS,
  rejectionText,
  syncResultText,
  toFieldErrors,
} from '../texts';

describe('formats and wording of the screens', () => {
  it('WF2P-S01 / wireframe-spec 2.1: a date-time is written in Sri Lanka time, "21 May 2026, 07:40"', () => {
    expect(formatDateTime('2026-05-21T07:40:00+05:30')).toBe('21 May 2026, 07:40');
    expect(formatDateTime('2026-05-21T02:10:00.000Z')).toBe('21 May 2026, 07:40');
    expect(formatDateTime('2026-10-09T18:30:00Z')).toBe('10 Oct 2026, 00:00');
  });

  it('WF2P-S03: coordinates are written with four decimals, and the source says GPS, accuracy or manual', () => {
    expect(formatCoordinates({ latitude: 6.5888, longitude: 79.964 })).toBe('6.5888 N, 79.9640 E');
    expect(locationSource({ latitude: 1, longitude: 1, accuracyMetres: 8.4 }, true)).toBe(
      'GPS · accuracy ±8 m',
    );
    expect(locationSource({ latitude: 1, longitude: 1 }, true)).toBe('GPS');
    expect(locationSource({ latitude: 1, longitude: 1, accuracyMetres: null }, true)).toBe('GPS');
    expect(locationSource({ latitude: 1, longitude: 1, accuracyMetres: 8 }, false)).toBe(
      'Entered manually',
    );
  });

  it('WF2P-S01 / S02: districts and hazard types are written as the frames do', () => {
    expect(districtLabel('NUWARA_ELIYA')).toBe('Nuwara Eliya');
    expect(districtLabel('KALUTARA')).toBe('Kalutara');
    expect(hazardTypeLabel('BLOCKED_ROAD')).toBe('Blocked road');
    expect(hazardTypeLabel(undefined)).toBe('Not chosen');
    expect(hazardTypeIcon('BLOCKED_ROAD')).toBe('road-blocked');
    expect(hazardTypeIcon(undefined)).toBe('other');
  });

  it('SC2 E1-E3: errors are sorted under their fields and worded', () => {
    const errors = toFieldErrors([
      'hazardType: a hazard type is required',
      'description: at most 500 characters',
      'photo: one photo is required',
      'location: outside Sri Lanka',
      'location: a location is required',
      'photo: something else',
      'unknown',
      'nofield: x',
    ]);

    expect(errors.hazardType).toBe('Choose what you saw');
    expect(errors.description).toBe('Enter a description of what you saw (at most 500 characters)');
    expect(errors.photo).toBe('something else');
    expect(errors.location).toBe('Enter the location of what you saw');
    expect(errors.other).toEqual(['unknown', 'nofield: x']);
    expect(hasErrors(errors)).toBe(true);
    expect(hasErrors(NO_ERRORS)).toBe(false);
    expect(hasErrors({ other: ['x'] })).toBe(true);
  });

  it('SC2 E8 18k: a rejection reason and a synchronisation result are worded', () => {
    expect(rejectionText('photo: bad')).toBe('Validation failed — photo: bad');
    expect(rejectionText(undefined)).toBe('Validation failed');
    expect(syncResultText({ submissionId: 'a', outcome: 'ACCEPTED', message: 'RPT-1' })).toBe(
      'Report sent — RPT-1, Pending verification',
    );
    expect(
      syncResultText({
        submissionId: 'a',
        outcome: 'REJECTED',
        reason: 'OVER_CAPACITY',
        message: 'full',
      }),
    ).toBe('Report not accepted: Over capacity — full');
    expect(
      syncResultText({ submissionId: 'a', outcome: 'REJECTED', reason: 'VALIDATION_FAILED' }),
    ).toBe('Report not accepted: Validation failed');
  });

  it('SC2 step 30: a decision notice has a Sinhala month name and no reason when verified', () => {
    const notice = {
      reportId: 'RPT-1',
      status: 'VERIFIED' as const,
      decidedAt: '2026-10-09T15:11:00Z',
    };

    expect(decisionNoticeText(notice, 'SINHALA')).toContain('(2026 ඔක්තෝබර් 9, 20:41)');
    expect(decisionNoticeText({ ...notice, status: 'REJECTED' }, 'ENGLISH')).toBe(
      'Your report RPT-1 was rejected by the DMC (9 Oct 2026, 20:41). Reason: ',
    );
    expect(decisionNoticeText(notice, 'TAMIL')).toContain('RPT-1');
  });

  it('SC1 step 15: each warning level has its own tone', () => {
    expect(LEVEL_TONES).toEqual({
      LEVEL_1_YELLOW: 'l1',
      LEVEL_2_AMBER: 'l2',
      LEVEL_3_RED: 'l3',
    });
  });

  it('SC2 A1 12c (P213): a suggestion is "<place> — <District>", or the name alone without a district', () => {
    expect(placeRowText('Kalutara', 'KALUTARA')).toBe('Kalutara — Kalutara');
    expect(placeRowText('Polgahawela', 'KURUNEGALA')).toBe('Polgahawela — Kurunegala');
    expect(placeRowText('Somewhere', undefined)).toBe('Somewhere');
  });

  it('SC2 A1 12d (P213): a manual location named after a place reads "near <place>, <lat> N, <lon> E — Entered manually"', () => {
    expect(namedLocationText('Kalutara', { latitude: 6.5888, longitude: 79.964 })).toBe(
      'near Kalutara, 6.5888 N, 79.9640 E — Entered manually',
    );
  });

  it('SC2 E3 (P213): the outside-Sri-Lanka error has the pin wording only, and a location error is worded with it', () => {
    expect(LOCATION_OUTSIDE_TEXT).toBe(
      'This location is outside Sri Lanka — move the pin to the place you saw',
    );
    expect(toFieldErrors(['location: outside Sri Lanka']).location).toBe(LOCATION_OUTSIDE_TEXT);
  });

  it('standards §10 item 11: the strip has words, an icon and a tone for each connection state', () => {
    expect(CONNECTION_TEXTS.online).toEqual({ label: 'Online', icon: 'wifi', tone: 'ok' });
    expect(CONNECTION_TEXTS.noInternet).toEqual({
      label: 'No internet connection',
      icon: 'offline',
      tone: 'neutral',
    });
    expect(CONNECTION_TEXTS.serverDown).toEqual({
      label: 'Server not reachable',
      icon: 'error',
      tone: 'warn',
    });
  });

  it('standards §10 (P222): the nearest-place label has one decimal under 10 km and whole kilometres from 10', () => {
    expect(nearestPlaceLabel('Kaduwela', 'COLOMBO', 0.84)).toBe('Near Kaduwela — Colombo · 0.8 km');
    expect(nearestPlaceLabel('Palugaswala', 'HAMBANTOTA', 5.66)).toBe(
      'Near Palugaswala — Hambantota · 5.7 km',
    );
    expect(nearestPlaceLabel('Galle', 'GALLE', 9.96)).toBe('Near Galle — Galle · 10.0 km');
    expect(nearestPlaceLabel('Galle', 'GALLE', 10)).toBe('Near Galle — Galle · 10 km');
    expect(nearestPlaceLabel('Somewhere', undefined, 3)).toBe('Near Somewhere · 3.0 km');
  });

  it('standards §10 item 14 (P222): the home location card gives the reason and the action on its second line', () => {
    expect(homeLocationReason('blocked', 'Expo Go')).toBe(
      'Allow location access for Expo Go in Settings',
    );
    expect(homeLocationReason('blocked', 'DisasterLink')).toBe(
      'Allow location access for DisasterLink in Settings',
    );
    expect(homeLocationReason('askable', 'Expo Go')).toBe('Allow location access to show it here');
    expect(homeLocationReason('servicesOff', 'Expo Go')).toBe(
      'Turn on Location Services in Settings',
    );
    expect(homeLocationReason('granted', 'Expo Go')).toBe(
      'The phone has not found its position yet',
    );
    expect(homeLocationReason(undefined, 'Expo Go')).toBe(
      'The phone has not found its position yet',
    );
  });

  it('standards §10 item 12 (P222): the retry line gives the local time of the phone, 24-hour, with leading zeros', () => {
    expect(stillUnavailableText(new Date(2026, 9, 16, 14, 7))).toBe(
      'Still unavailable — tried at 14:07',
    );
    expect(stillUnavailableText(new Date(2026, 9, 16, 0, 5))).toBe(
      'Still unavailable — tried at 00:05',
    );
  });

  it('WF2P-S03 (P222): a GPS line with a nearest place and the plain forms', () => {
    const fix = { latitude: 6.5888, longitude: 79.964, accuracyMetres: 7.6 };

    expect(locationSource(fix, true, 'Kalutara')).toBe('GPS · ±8 m · near Kalutara');
    expect(locationSource({ latitude: 1, longitude: 1 }, true, 'Kalutara')).toBe(
      'GPS · near Kalutara',
    );
    expect(locationSource(fix, true)).toBe('GPS · accuracy ±8 m');
    expect(locationSource(fix, false, 'Kalutara')).toBe('Entered manually');
  });
});
