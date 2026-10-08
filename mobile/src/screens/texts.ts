/**
 * texts.ts — the words and formats of the citizen app's screens (implementation-only): the labels of
 * the enumerations, the date-time and coordinate formats of wireframe-spec 2.1, the wording of the
 * error lines and the decision notice. App labels are English on every screen; only the warning
 * text and the decision notice follow the chosen language (SC2 Conventions).
 *
 * Implements CH-25, CH-27 (frames WF2P-S01 to S06 and their states).
 */
import type { GeoPoint, ReportDecisionNotice, SyncResult } from '../api/reportApi';
import type {
  District,
  HazardType,
  Language,
  LocalisedText,
  ReportStatus,
  WarningLevel,
} from '../api/types';
import type { IconName } from '../components/iconData';

/** Sri Lanka's offset from UTC in minutes (+05:30, no daylight saving): the time every screen shows. */
const COLOMBO_OFFSET_MINUTES = 330;

/** The number of milliseconds in a minute. */
const MS_PER_MINUTE = 60_000;

/** The width of a time part such as the hour. */
const TIME_PART_WIDTH = 2;

/** The digits shown after the decimal point of a coordinate (wireframe-spec 2.1: 6.5888 N). */
const COORDINATE_DIGITS = 4;

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const SINHALA_MONTHS = [
  'ජනවාරි',
  'පෙබරවාරි',
  'මාර්තු',
  'අප්‍රේල්',
  'මැයි',
  'ජූනි',
  'ජූලි',
  'අගෝස්තු',
  'සැප්තැම්බර්',
  'ඔක්තෝබර්',
  'නොවැම්බර්',
  'දෙසැම්බර්',
];

/** The three languages of the selector, in the order the frames draw them. */
export const LANGUAGE_OPTIONS: readonly { value: Language; label: string }[] = [
  { value: 'SINHALA', label: 'Sinhala' },
  { value: 'TAMIL', label: 'Tamil' },
  { value: 'ENGLISH', label: 'English' },
];

/** The warning levels as the screens write them in full (model dictionary section 1). */
export const LEVEL_LABELS: Record<WarningLevel, string> = {
  LEVEL_1_YELLOW: 'Level 1 – Yellow (Remain Vigilant)',
  LEVEL_2_AMBER: 'Level 2 – Amber (Be Alert)',
  LEVEL_3_RED: 'Level 3 – Red (Evacuate)',
};

/**
 * The error under the map when the pin is outside Sri Lanka (SC2 E3 12f; WF2P-S03b, P206). The
 * coordinate fields were removed (P213), so this is the only wording.
 */
export const LOCATION_OUTSIDE_TEXT =
  'This location is outside Sri Lanka — move the pin to the place you saw';

/** The notice when the location permission is refused or can be asked again (WF2P-S03g, P206). */
export const LOCATION_ALLOW_TEXT = 'Allow location access to use your current location';

/** The notice when Location Services are off (WF2P-S03g, P206). */
export const LOCATION_SERVICES_OFF_TEXT = 'Turn on Location Services to use your current location';

/**
 * Words the line that says more alerts exist than the shell draws (standards §10 item 7).
 *
 * @param hidden the number of alerts not drawn
 * @returns for example "1 more — see Active warnings on Home"
 */
export function moreAlertsText(hidden: number): string {
  return `${hidden} more — see Active warnings on Home`;
}

/** The label written before the address in the stored description (standards §10 "The address", P214). */
export const ADDRESS_LABEL = 'Address: ';

/** The label of the optional address field (WF2P-S03a, P214). */
export const ADDRESS_FIELD_LABEL = 'Address or landmark';

/** The label of the place field of the picker (WF2P-S03a, P213). */
export const PLACE_FIELD_LABEL = 'Type a town or village';

/** The credit for the bundled place names, shown under the picker (CC BY 4.0; THIRD-PARTY-NOTICES.md). */
export const PLACE_CREDIT = 'Place names: GeoNames (CC BY 4.0)';

/** The link that closes the picker opened by "Change" and keeps the location (WF2P-S03a, P213). */
export const KEEP_LOCATION_LABEL = 'Keep this location';

/** The link on a manually entered location that reopens the picker (WF2P-S03a, P213). */
export const CHANGE_LOCATION_LABEL = 'Change';

/**
 * Words a suggestion of the place list.
 *
 * @param name the place's name
 * @param district the district it lies in, if known
 * @returns "<place> — <District>", or the name alone when the district is not known
 */
export function placeRowText(name: string, district: District | undefined): string {
  return district ? `${name} — ${districtLabel(district)}` : name;
}

/**
 * Words a manually entered location that is named after the place the citizen chose.
 *
 * @param place the chosen place's name
 * @param location the point entered
 * @returns "near <place>, <lat> N, <lon> E — Entered manually"
 */
export function namedLocationText(place: string, location: GeoPoint): string {
  return `near ${place}, ${formatCoordinates(location)} — Entered manually`;
}

/** The connection strip's states: its icon, its words and its tone (standards §10 item 11, P213). */
export const CONNECTION_TEXTS: Record<
  'online' | 'noInternet' | 'serverDown',
  { label: string; icon: IconName; tone: 'ok' | 'neutral' | 'warn' }
> = {
  online: { label: 'Online', icon: 'wifi', tone: 'ok' },
  noInternet: { label: 'No internet connection', icon: 'offline', tone: 'neutral' },
  serverDown: { label: 'Server not reachable', icon: 'error', tone: 'warn' },
};

/** The tone each warning level is drawn in: its colour, always together with its text (wireframe-spec 8 rule 1). */
export const LEVEL_TONES: Record<WarningLevel, 'l1' | 'l2' | 'l3'> = {
  LEVEL_1_YELLOW: 'l1',
  LEVEL_2_AMBER: 'l2',
  LEVEL_3_RED: 'l3',
};

/** The hazard types of the list on WF2P-S02, with their icons. */
export const HAZARD_OPTIONS: readonly { type: HazardType; label: string; icon: IconName }[] = [
  { type: 'FLOOD', label: 'Flood', icon: 'flood' },
  { type: 'LANDSLIDE', label: 'Landslide', icon: 'landslide' },
  { type: 'CYCLONE', label: 'Cyclone', icon: 'cyclone' },
  { type: 'TSUNAMI', label: 'Tsunami', icon: 'tsunami' },
  { type: 'DROUGHT', label: 'Drought', icon: 'drought' },
  { type: 'BLOCKED_ROAD', label: 'Blocked road', icon: 'road-blocked' },
  { type: 'OTHER', label: 'Other', icon: 'other' },
];

/** The text of the common error banner (C1): the service is unavailable and nothing was changed. */
export const SERVICE_UNAVAILABLE_TEXT = 'The service is unavailable and nothing was changed';

/** The text the review screen shows when the report could not be prepared (a programming fault). */
export const REPORT_NOT_PREPARED_TEXT = 'The report could not be prepared — try again';

/** The wording of a rejected draft's reason (WF2P-S01c and S03f). */
const VALIDATION_FAILED_LABEL = 'Validation failed';

/** The labels of a SyncResult's reason (`RejectionReason`). */
const REASON_LABELS: Record<string, string> = {
  VALIDATION_FAILED: VALIDATION_FAILED_LABEL,
  INVALID_VALUE: 'Invalid value',
  OVER_CAPACITY: 'Over capacity',
};

/**
 * Finds the label of a hazard type.
 *
 * @param type the hazard type
 * @returns its label, for example "Flood"
 */
export function hazardTypeLabel(type: HazardType | undefined): string {
  return HAZARD_OPTIONS.find((option) => option.type === type)?.label ?? 'Not chosen';
}

/**
 * Finds the icon of a hazard type.
 *
 * @param type the hazard type
 * @returns its icon, `other` if none is chosen
 */
export function hazardTypeIcon(type: HazardType | undefined): IconName {
  return HAZARD_OPTIONS.find((option) => option.type === type)?.icon ?? 'other';
}

/**
 * Writes a district as the screens do.
 *
 * @param district the district constant, for example `NUWARA_ELIYA`
 * @returns "Nuwara Eliya"
 */
export function districtLabel(district: District): string {
  return district
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

/** The numbers of a date-time in Sri Lanka's time. */
interface ColomboTime {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
}

function toColomboTime(iso: string): ColomboTime {
  const shifted = new Date(Date.parse(iso) + COLOMBO_OFFSET_MINUTES * MS_PER_MINUTE);
  return {
    year: shifted.getUTCFullYear(),
    month: shifted.getUTCMonth(),
    day: shifted.getUTCDate(),
    hour: shifted.getUTCHours(),
    minute: shifted.getUTCMinutes(),
  };
}

function clock(time: ColomboTime): string {
  const pad = (value: number) => String(value).padStart(TIME_PART_WIDTH, '0');
  return `${pad(time.hour)}:${pad(time.minute)}`;
}

/**
 * Writes a date-time as wireframe-spec 2.1 does, in Sri Lanka's time.
 *
 * @param iso an ISO-8601 date-time, with an offset or `Z`
 * @returns for example "21 May 2026, 07:40"
 */
export function formatDateTime(iso: string): string {
  const time = toColomboTime(iso);
  return `${time.day} ${MONTHS[time.month]} ${time.year}, ${clock(time)}`;
}

function formatSinhalaDateTime(iso: string): string {
  const time = toColomboTime(iso);
  return `${time.year} ${SINHALA_MONTHS[time.month]} ${time.day}, ${clock(time)}`;
}

/**
 * Writes a place as text, with no map (standards section 10).
 *
 * @param location the place
 * @returns for example "6.5888 N, 79.9640 E"
 */
export function formatCoordinates(location: GeoPoint): string {
  return `${location.latitude.toFixed(COORDINATE_DIGITS)} N, ${location.longitude.toFixed(COORDINATE_DIGITS)} E`;
}

/**
 * Writes where a place came from.
 *
 * @param location the place
 * @param fromGps whether it is a GPS fix
 * @param near the nearest place within 10 km, if any (P222)
 * @returns "GPS · accuracy ±8 m" ("GPS" when no accuracy is known), "GPS · ±8 m · near <place>" with a
 *   nearest place, or "Entered manually"
 */
export function locationSource(location: GeoPoint, fromGps: boolean, near?: string): string {
  if (!fromGps) {
    return 'Entered manually';
  }
  const known = location.accuracyMetres != null;
  if (near !== undefined) {
    // SC2 step 14 words the plain line verbatim; the nearest place (P222) makes it "GPS · ±8 m · near X".
    return known
      ? `GPS · ±${Math.round(location.accuracyMetres as number)} m · near ${near}`
      : `GPS · near ${near}`;
  }
  return known ? `GPS · accuracy ±${Math.round(location.accuracyMetres as number)} m` : 'GPS';
}

/**
 * Picks the text of a warning in a language.
 *
 * @param text the three texts
 * @param language the language
 * @returns the text in that language
 */
export function localised(text: LocalisedText, language: Language): string {
  if (language === 'SINHALA') {
    return text.sinhala;
  }
  return language === 'TAMIL' ? text.tamil : text.english;
}

/** The status tags of a report (frames WF2P-S01b, S06, S06a). */
export const STATUS_TAGS: Record<
  ReportStatus,
  { label: string; tone: 'ok' | 'warn' | 'neutral'; icon?: IconName }
> = {
  PENDING_VERIFICATION: { label: 'Pending verification', tone: 'warn' },
  VERIFIED: { label: 'Verified', tone: 'ok', icon: 'check' },
  REJECTED: { label: 'Rejected', tone: 'neutral', icon: 'cross' },
};

/**
 * Writes the reason of a rejected draft (WF2P-S01c, S03f).
 *
 * @param reason the reason the server gave
 * @returns for example "Validation failed — photo: not a readable JPEG or PNG image"
 */
export function rejectionText(reason: string | undefined): string {
  return reason ? `${VALIDATION_FAILED_LABEL} — ${reason}` : VALIDATION_FAILED_LABEL;
}

/**
 * Writes one synchronisation result (WF2P-S01b, S01c).
 *
 * @param result the result
 * @returns "Report sent — RPT-…, Pending verification" or "Report not accepted: …"
 */
export function syncResultText(result: SyncResult): string {
  if (result.outcome === 'REJECTED') {
    const label = result.reason ? (REASON_LABELS[result.reason] ?? result.reason) : undefined;
    const message = result.message ?? '';
    const reason = label && message ? `${label} — ${message}` : (label ?? message);
    return reason ? `Report not accepted: ${reason}` : 'Report not accepted';
  }
  return result.message
    ? `Report sent — ${result.message}, Pending verification`
    : 'Report sent — Pending verification';
}

/** The errors of a draft, sorted under the field they belong to (SC2 E1, E2, E3). */
export interface FieldErrors {
  hazardType?: string;
  description?: string;
  photo?: string;
  location?: string;
  other: string[];
}

/** No errors. */
export const NO_ERRORS: FieldErrors = { other: [] };

const FIELD_TEXTS: Record<string, Record<string, string>> = {
  hazardType: { default: 'Choose what you saw' },
  description: {
    default: 'Enter a description of what you saw (at most 500 characters)',
  },
  photo: {
    'one photo is required': 'Add one photo of what you saw',
    'not a readable JPEG or PNG image': 'This image cannot be used — choose a JPEG or PNG photo',
  },
  location: {
    default: LOCATION_OUTSIDE_TEXT,
    'a location is required': 'Enter the location of what you saw',
  },
};

/**
 * Sorts the field-prefixed errors of `validateDraft` under their fields and words them.
 *
 * @param errors the errors, each starting with its field and a colon
 * @returns the errors by field; an error of no known field goes to `other` as it is
 */
export function toFieldErrors(errors: string[]): FieldErrors {
  const sorted: FieldErrors = { other: [] };
  for (const error of errors) {
    const separator = error.indexOf(': ');
    const field = separator < 0 ? '' : error.slice(0, separator);
    const detail = separator < 0 ? error : error.slice(separator + 2);
    const texts = FIELD_TEXTS[field];
    if (texts) {
      sorted[field as 'hazardType' | 'description' | 'photo' | 'location'] =
        texts[detail] ?? texts.default ?? detail;
    } else {
      sorted.other.push(error);
    }
  }
  return sorted;
}

/**
 * Tells whether any error is set.
 *
 * @param errors the errors by field
 * @returns whether there is one
 */
export function hasErrors(errors: FieldErrors): boolean {
  return Boolean(
    errors.hazardType ||
    errors.description ||
    errors.photo ||
    errors.location ||
    errors.other.length > 0,
  );
}

/** The decision notice in each language (WF2P-S01e, S01f; Sinhala texts N-1 and N-2, drafts). */
const DECISION_PATTERNS: Record<
  Language,
  {
    verified: (id: string, when: string) => string;
    rejected: (id: string, when: string, reason: string) => string;
  }
> = {
  ENGLISH: {
    verified: (id, when) => `Your report ${id} was verified by the DMC (${when})`,
    rejected: (id, when, reason) =>
      `Your report ${id} was rejected by the DMC (${when}). Reason: ${reason}`,
  },
  SINHALA: {
    verified: (id, when) =>
      `ඔබේ වාර්තාව ${id} ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් තහවුරු කරන ලදී (${when})`,
    rejected: (id, when, reason) =>
      `ඔබේ වාර්තාව ${id} ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් ප්‍රතික්ෂේප කරන ලදී (${when}). හේතුව: ${reason}`,
  },
  TAMIL: {
    verified: (id, when) => `உங்கள் அபாய அறிக்கை ${id} உறுதிப்படுத்தப்பட்டது (${when})`,
    rejected: (id, when, reason) =>
      `உங்கள் அபாய அறிக்கை ${id} ஏற்றுக்கொள்ளப்படவில்லை (${when}): ${reason}`,
  },
};

/**
 * Writes a decision notice in the citizen's language (SC2 step 30, A4 25h).
 *
 * @param notice the decision
 * @param language the citizen's language
 * @returns the text; the reason is the officer's own words
 */
export function decisionNoticeText(notice: ReportDecisionNotice, language: Language): string {
  const patterns = DECISION_PATTERNS[language];
  const when =
    language === 'SINHALA'
      ? formatSinhalaDateTime(notice.decidedAt)
      : formatDateTime(notice.decidedAt);
  return notice.status === 'REJECTED'
    ? patterns.rejected(notice.reportId, when, notice.reason ?? '')
    : patterns.verified(notice.reportId, when);
}

/** The line under the map when no town lies within the radius (standards §10, Nearest-place label, P222). */
export const NO_TOWN_TEXT = 'No town nearby — the point is still used';

/** The error line under the map when the pin is outside Sri Lanka; "Use this location" is disabled (P222). */
export const OUTSIDE_ISLAND_TEXT = 'Outside Sri Lanka — move the pin onto the island';

/**
 * Words the live label under the map for the place nearest to the pin.
 *
 * @param place the nearest place's name
 * @param district the pin's district, if the pin is inside one
 * @param km the distance from the pin to the place, in kilometres
 * @returns "Near <place> — <District> · <d> km", one decimal under 10 km
 */
export function nearestPlaceLabel(
  place: string,
  district: District | undefined,
  km: number,
): string {
  const distance = km < 10 ? km.toFixed(1) : String(Math.round(km));
  return `Near ${placeRowText(place, district)} · ${distance} km`;
}

/** The title of the home's location card when there is no fix (E1, G6, P222). */
export const LOCATION_UNAVAILABLE_TITLE = 'Current location unavailable';

/**
 * Gives the muted second line of the home's location card when there is no fix.
 *
 * @param access why there is no fix, when it is known
 * @returns the reason and the action, or a plain waiting line when nothing is known
 */
export function homeLocationReason(
  access: 'granted' | 'askable' | 'blocked' | 'servicesOff' | undefined,
): string {
  switch (access) {
    case 'blocked':
      return 'Allow location access for Expo Go in Settings';
    case 'askable':
      return 'Allow location access to show it here';
    case 'servicesOff':
      return 'Turn on Location Services in Settings';
    default:
      return 'The phone has not found its position yet';
  }
}

/** The label of Retry while it runs (standards §10 item 12, P222). */
export const RETRYING_LABEL = 'Trying again…';

/**
 * Words the line that the C1 banner adds when Retry found the fault still there.
 *
 * @param at when the retry ended (the phone's local time)
 * @returns "Still unavailable — tried at HH:MM" (24-hour)
 */
export function stillUnavailableText(at: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `Still unavailable — tried at ${pad(at.getHours())}:${pad(at.getMinutes())}`;
}

/** The title of the home's second list (deviation D-UC2-5 from SC2's "Not sent yet"; P222). */
export const UNSENT_SECTION_TITLE = 'Not sent yet';

/** The muted line under the title of the home's second list (deviation D-UC2-5; P222). */
export const UNSENT_SECTION_LINE = 'Saved on this phone — sent automatically when you are online';
