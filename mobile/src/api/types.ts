/**
 * types.ts — the wire types of the skeleton's endpoints and the shared enums, with the names exactly
 * as the server sends them (JSON bodies use the dictionary's names; enums travel as their constant
 * names; date-times are ISO-8601 strings with an offset, for example `2026-05-21T08:15:00+05:30` —
 * implementation standards §12.1 items 1 and 3).
 */

/** The 25 districts (`shared.domain.District`). */
export type District =
  | 'AMPARA'
  | 'ANURADHAPURA'
  | 'BADULLA'
  | 'BATTICALOA'
  | 'COLOMBO'
  | 'GALLE'
  | 'GAMPAHA'
  | 'HAMBANTOTA'
  | 'JAFFNA'
  | 'KALUTARA'
  | 'KANDY'
  | 'KEGALLE'
  | 'KILINOCHCHI'
  | 'KURUNEGALA'
  | 'MANNAR'
  | 'MATALE'
  | 'MATARA'
  | 'MONARAGALA'
  | 'MULLAITIVU'
  | 'NUWARA_ELIYA'
  | 'POLONNARUWA'
  | 'PUTTALAM'
  | 'RATNAPURA'
  | 'TRINCOMALEE'
  | 'VAVUNIYA';

/** The 25 district constants, in the server's order. */
export const DISTRICTS: readonly District[] = [
  'AMPARA',
  'ANURADHAPURA',
  'BADULLA',
  'BATTICALOA',
  'COLOMBO',
  'GALLE',
  'GAMPAHA',
  'HAMBANTOTA',
  'JAFFNA',
  'KALUTARA',
  'KANDY',
  'KEGALLE',
  'KILINOCHCHI',
  'KURUNEGALA',
  'MANNAR',
  'MATALE',
  'MATARA',
  'MONARAGALA',
  'MULLAITIVU',
  'NUWARA_ELIYA',
  'POLONNARUWA',
  'PUTTALAM',
  'RATNAPURA',
  'TRINCOMALEE',
  'VAVUNIYA',
];

/** The three languages (`shared.domain.Language`). */
export type Language = 'SINHALA' | 'TAMIL' | 'ENGLISH';

/** The warning levels (`shared.domain.WarningLevel`). */
export type WarningLevel = 'LEVEL_1_YELLOW' | 'LEVEL_2_AMBER' | 'LEVEL_3_RED';

/** The hazard types (`shared.domain.HazardType`). */
export type HazardType =
  | 'FLOOD'
  | 'LANDSLIDE'
  | 'CYCLONE'
  | 'TSUNAMI'
  | 'DROUGHT'
  | 'BLOCKED_ROAD'
  | 'OTHER';

/** The status of a hazard report (`shared.domain.ReportStatus`). */
export type ReportStatus = 'PENDING_VERIFICATION' | 'VERIFIED' | 'REJECTED';

/** The notification channels (`shared.domain.NotificationChannel`). */
export type NotificationChannel = 'PUSH' | 'SMS' | 'AUDIBLE_ALERT';

/** The kinds of push entry (`shared.notification.PushKind`). */
export type PushKind = 'WARNING_ALERT' | 'REPORT_DECISION';

/** A text in the three languages. */
export interface LocalisedText {
  sinhala: string;
  tamil: string;
  english: string;
}

/** An active warning as `GET /api/warnings/active` returns it (`warning.web.dto.ActiveWarningResponse`). */
export interface ActiveWarning {
  warningId: string;
  warningLevel: WarningLevel;
  message: LocalisedText;
  instructions: LocalisedText;
  /** ISO-8601 with offset. */
  issuedTime: string;
  /** ISO-8601 with offset. */
  expiryTime: string;
  targetDistricts: District[];
  hazardType: HazardType;
  locationName: string;
}

/**
 * An entry of the push outbox (`shared.notification.PushEntryResponse`). Fields that do not apply to
 * the kind are absent (the server leaves out `null`).
 */
export interface PushEntry {
  id: string;
  kind: PushKind;
  warningId?: string;
  warningLevel?: WarningLevel;
  reportId?: string;
  status?: ReportStatus;
  reason?: string;
  text: string;
  /** ISO-8601 with offset. */
  createdAt: string;
  /** ISO-8601 with offset; set on a report decision. */
  decidedAt?: string;
}

/** The only error body of the server: an RFC 9457 problem detail (§12.1 item 2). */
export interface ProblemDetail {
  status: number;
  detail: string;
  title?: string;
}
