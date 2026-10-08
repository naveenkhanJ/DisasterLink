/**
 * ReportDraft.ts — the phone's report draft and the types the capture controller returns
 * (model dictionary section 2: `ReportDraft`, `SubmissionResult`, `HomeView`; TypeScript only for the
 * last two, PR15). The Java `ReportDraft` omits `photoUri` and the queue fields (RC-04).
 */
import type { ActiveWarning, District, HazardType, Language } from '../api/types';
import type { GeoPoint, HazardReport } from '../api/reportApi';

/** The status of a draft in the phone's store (`PendingStatus`). */
export type PendingStatus = 'QUEUED' | 'REJECTED';

/** What the phone shows after "Submit" (`SubmissionState`). */
export type SubmissionState = 'SUBMITTED' | 'QUEUED' | 'INVALID';

/** A report in the making, or waiting in the phone's store. */
export interface ReportDraft {
  /** Made on the phone (a UUID); sent as the submission identifier; kept when a rejected draft is corrected. */
  draftId: string;
  hazardType?: HazardType;
  description?: string;
  /** The copy of the photo in the app's document directory (FD-10). */
  photoUri?: string;
  location?: GeoPoint;
  locationFromGps: boolean;
  district?: District;
  /** ISO-8601 time the draft was started; sent as the original capture time on replay. */
  capturedAt: string;
  /** Empty while the draft is edited or sent online (RC-04). */
  status?: PendingStatus;
  /** The number of failed synchronisation attempts (a send that got no receipt); nothing reads it. */
  attempts?: number;
  /** The server's reason, for a REJECTED draft. */
  rejectionReason?: string;
}

/**
 * Copies a draft with a queue status (`ReportDraft.withStatus`, FD-17).
 *
 * @param draft the draft
 * @param status the new status
 * @returns the copy
 */
export function withStatus(draft: ReportDraft, status: PendingStatus): ReportDraft {
  return { ...draft, status };
}

/** The result of submitting a draft (`SubmissionResult`). */
export interface SubmissionResult {
  draftId: string;
  state: SubmissionState;
  report?: HazardReport;
  errors: string[];
}

/** The home screen's data (`HomeView`). */
export interface HomeView {
  warnings: ActiveWarning[];
  myReports: HazardReport[];
  pendingDrafts: ReportDraft[];
  language: Language;
  /** Whether the warnings and reports come from the last cached home (offline, A8). */
  fromCache: boolean;
  /** Whether a part of the home could not be loaded (a 5xx): the screen shows the common error banner. */
  error?: boolean;
}
