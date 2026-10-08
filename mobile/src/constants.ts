/**
 * constants.ts — the named values of the skeleton modules (implementation standards §10: no magic
 * values in code).
 */
import type { District, Language } from './api/types';

/** The demo citizen of the app (standards §11): the Kalutara citizen with a device token. */
export const DEFAULT_CITIZEN_ID = 'CIT-KLT-0001';

/**
 * The demo citizen's registered district (CIT-KLT-0001 in the seed, standards §11): the fallback
 * district of a report and the district of the home screen's warnings.
 */
export const DEFAULT_CITIZEN_DISTRICT: District = 'KALUTARA';

/**
 * The demo citizen's preferred language in the seed: the app's starting language (standards §10,
 * PR13).
 */
export const DEFAULT_CITIZEN_LANGUAGE: Language = 'ENGLISH';

/** How long a request may take before it counts as a network failure, in milliseconds (§12.1). */
export const REQUEST_TIMEOUT_MS = 15_000;

/** The back end's port; `EXPO_PUBLIC_API_URL=auto` builds the API address with it. */
export const API_PORT = 8080;

/** The id of the Android notification channel that carries warnings (§10, Expo Go). */
export const WARNINGS_CHANNEL_ID = 'warnings';

/** The visible name of the warnings channel. */
export const WARNINGS_CHANNEL_NAME = 'Warnings';

/** The content type of a JSON body. */
export const JSON_CONTENT_TYPE = 'application/json';

/** The HTTP status that carries no body. */
export const HTTP_NO_CONTENT = 204;

/** The first HTTP status of a refused request. */
export const HTTP_ERROR_MIN = 400;

/** The path of the active-warnings endpoint (§12.1). */
export const ACTIVE_WARNINGS_PATH = '/api/warnings/active';

/** The path of a recipient's push outbox (§12.1); `{recipientId}` is replaced. */
export const PUSH_OUTBOX_PATH = '/api/push/{recipientId}/outbox';

/** The path of the report upload and of one report (§12.1); `/{reportId}` follows for one report. */
export const REPORTS_PATH = '/api/reports';

/** The path of a citizen's reports (§12.1); `{citizenId}` is replaced. */
export const CITIZEN_REPORTS_PATH = '/api/citizens/{citizenId}/reports';

/** The path of a citizen's language (§12.1); `{citizenId}` is replaced. */
export const CITIZEN_LANGUAGE_PATH = '/api/citizens/{citizenId}/language';

/** The southern edge of the box that counts as within Sri Lanka, in degrees (as the server's). */
export const SRI_LANKA_MIN_LATITUDE = 5.85;

/** The northern edge of the box that counts as within Sri Lanka, in degrees. */
export const SRI_LANKA_MAX_LATITUDE = 9.9;

/** The western edge of the box that counts as within Sri Lanka, in degrees. */
export const SRI_LANKA_MIN_LONGITUDE = 79.5;

/** The eastern edge of the box that counts as within Sri Lanka, in degrees. */
export const SRI_LANKA_MAX_LONGITUDE = 81.95;

/** The longest description of a report, in characters (SC2 step 16). */
export const MAX_DESCRIPTION_LENGTH = 500;

/** How often the app asks the server for push entries while it is active, in milliseconds (§10). */
export const PUSH_POLL_INTERVAL_MS = 5_000;

/** The quality of a picked photo, from 0 (small) to 1 (large): verified on the iPhone (P188). */
export const PHOTO_QUALITY = 0.7;

/** The key of the draft list in the phone's storage. */
export const DRAFTS_STORAGE_KEY = 'disasterlink.drafts';

/** The key of the phone's client identifier in its storage. */
export const CLIENT_ID_STORAGE_KEY = 'disasterlink.clientId';

/** The key of the cached home screen in the phone's storage. */
export const HOME_STORAGE_KEY = 'disasterlink.home';

/** The key of the citizen's chosen language in the phone's storage. */
export const LANGUAGE_STORAGE_KEY = 'disasterlink.language';

/** The text shown when the server refuses a photo that is too large (HTTP 413, §12.3). */
export const PHOTO_TOO_LARGE_MESSAGE = 'The photo is too large to send';

/** The HTTP status of a request that is larger than the server accepts. */
export const HTTP_PAYLOAD_TOO_LARGE = 413;

/** The separator of the errors in the message of a REJECTED receipt. */
export const ERROR_SEPARATOR = '; ';

/**
 * How often the app tries to send a QUEUED draft while it is open and active, in milliseconds
 * (standards section 10 "Foreground retry … and timed retry", P206; deviation D-UC2-1).
 */
export const QUEUED_RETRY_INTERVAL_MS = 30_000;

/** The most in-app warning alerts the shell draws at once; the rest are counted (standards section 10, P206). */
export const MAX_ALERTS_SHOWN = 2;

/** The fewest characters of a place query that start the type-ahead list (standards §10, P213). */
export const MIN_PLACE_QUERY_LENGTH = 2;

/** The most places the type-ahead list shows (standards §10, P213). */
export const MAX_PLACE_SUGGESTIONS = 8;

/** The mean radius of the earth, in kilometres, for the haversine distance. */
export const EARTH_RADIUS_KM = 6371;

/**
 * How near the pin must be to the chosen place for the entered location to be named after it, in
 * kilometres (standards §10 "Manual location", P213).
 */
export const MANUAL_PLACE_RADIUS_KM = 5;

/** The longest "Address or landmark" text, in characters (standards §10 "The address", P214). */
export const MAX_ADDRESS_LENGTH = 200;

/** The farthest a place can be from the pin and still be named in the picker's label, in kilometres (P222). */
export const NO_TOWN_RADIUS_KM = 10;

/**
 * How far the map may move from a place chosen in the list before the choice is cleared, in
 * kilometres (standards §10 "Manual location", Nearest-place label, P222).
 */
export const CHOSEN_PLACE_KEEP_KM = 1;

/** The kilometres in one degree of latitude, for the box that pre-filters the nearest-place search. */
export const KM_PER_DEGREE = 111.2;

/**
 * How long a request of the push poll may take before it counts as a network failure, in milliseconds
 * (standards §10 item 11, P222): "Server not reachable" then appears about 5 to 9 seconds after the
 * server stops. Other requests keep `REQUEST_TIMEOUT_MS`.
 */
export const POLL_TIMEOUT_MS = 4_000;

/** How long "<item> — not available in this version" shows before it fades, in milliseconds (standards §10 item 12, P222). */
export const SOON_NOTICE_MS = 3_000;

/**
 * The least time "Trying again…" stays on the error banner after Retry is pressed, so a quick answer
 * is still seen to have happened (standards section 10 item 12, P225).
 */
export const MIN_RETRY_FEEDBACK_MS = 3_000;

/** How long the sync-result banner "Report sent — …" shows before it fades, in milliseconds (standards §10 item 12, P222). */
export const SENT_NOTICE_MS = 6_000;

/** How long "Draft discarded — nothing was sent" shows before it fades, in milliseconds (standards §10 item 12, P222). */
export const DISCARDED_NOTICE_MS = 4_000;

/**
 * How long the location picker waits for the keyboard to show before it scrolls its place field to the
 * top anyway, in milliseconds: the keyboard may already be up, or a hardware keyboard is used
 * (standards §10 item 13, P225).
 */
export const PICKER_SCROLL_FALLBACK_MS = 400;

/** The space kept above content that is scrolled into view, in points (standards §10 item 13, P222). */
export const SCROLL_TOP_MARGIN = 16;

/**
 * The width of "Choose from gallery" / "Choose another" relative to "Take photo" / "Retake photo" (which
 * is 1), so the longer label fits on one line next to its icon (standards §10 item 9, P225).
 */
export const PICK_GALLERY_FLEX = 1.3;

/** The padding above the icon of every tab of the tab bar, in points (standards §10 item 15, P222). */
export const TAB_PADDING_TOP = 8;

/** The inset at each side of the active tab's underline, in points (standards §10 item 15, P222). */
export const TAB_UNDERLINE_INSET = 16;

/** The height of the active tab's underline, in points (standards §10 item 15, P222). */
export const TAB_UNDERLINE_HEIGHT = 2;

/** The line height of a tab's label, in points, so the underline below it sits at a known distance (standards §10 item 15, P226). */
export const TAB_LABEL_LINE_HEIGHT = 15;

/** The gap between a tab's icon and its label, in points (standards §10 item 15, P226). */
export const TAB_ICON_LABEL_GAP = 2;

/** The gap between a tab's label and "Not in this version" on the Profile tab, in points (standards §10 item 15, P226). */
export const TAB_SOON_GAP = 2;
