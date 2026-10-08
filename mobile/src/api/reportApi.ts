/**
 * reportApi.ts — the client of UC2's endpoints (implementation standards §12.1 and §12.3) and the
 * wire types of their bodies, matching the server's `hazardreport.web.dto` records. The upload goes
 * through the skeleton's `postMultipart` and `jsonPart`, as verified on the iPhone (P188, S9).
 */
import { CITIZEN_LANGUAGE_PATH, CITIZEN_REPORTS_PATH, REPORTS_PATH } from '../constants';
import { jsonPart, type HttpClient } from './httpClient';
import type { District, HazardType, Language, ReportStatus } from './types';

/** A place on the wire (`GeoPointRequest` / `GeoPointResponse`). */
export interface GeoPoint {
  latitude: number;
  longitude: number;
  /** The accuracy of the fix in metres; absent or `null` if not known. */
  accuracyMetres?: number | null;
}

/** The outcome of sending a submission (`shared.domain.SyncOutcome`). */
export type SyncOutcome = 'ACCEPTED' | 'DUPLICATE' | 'REJECTED';

/** The reason of a REJECTED result (`shared.domain.RejectionReason`); only validation applies to reports. */
export type RejectionReason = 'OVER_CAPACITY' | 'INVALID_VALUE' | 'VALIDATION_FAILED';

/** The result of a submission (`SyncResultResponse`). */
export interface SyncResult {
  submissionId: string;
  outcome: SyncOutcome;
  reason?: RejectionReason | null;
  message?: string | null;
}

/** A report as the server sends it (`HazardReportResponse`); times are ISO-8601 with an offset. */
export interface HazardReport {
  reportId: string;
  hazardType: HazardType;
  description: string;
  location: GeoPoint;
  locationFromGps: boolean;
  district: District;
  capturedAt: string;
  reportTime: string;
  status: ReportStatus;
  verificationTime?: string | null;
  rejectionReason?: string | null;
  hazardId?: string | null;
}

/** The server's reply to an upload (`ReportReceiptResponse`). */
export interface ReportReceipt {
  result: SyncResult;
  report?: HazardReport | null;
}

/** The `draft` part of an upload: the Java `ReportDraft`'s members only (`ReportDraftRequest`). */
export interface ReportDraftWire {
  draftId: string;
  hazardType?: HazardType;
  description?: string;
  location?: GeoPoint;
  locationFromGps: boolean;
  district?: District;
  capturedAt: string;
}

/** The `meta` part of an upload (`ClientMetaRequest`). */
export interface ClientMeta {
  clientId: string;
  userId: string;
  submissionId: string;
  capturedAt: string;
  pendingCount: number;
}

/** The decision notice the app shows for a REPORT_DECISION push entry (`ReportDecisionNotice`). */
export interface ReportDecisionNotice {
  reportId: string;
  status: ReportStatus;
  reason?: string;
  decidedAt: string;
}

/** What the upload needs from a draft: the Java record's members; a phone draft has more, which are not sent. */
export type DraftToSend = ReportDraftWire;

/** The calls the app makes to UC2's endpoints. */
export interface ReportApi {
  /**
   * `POST /api/reports` as `multipart/form-data`: the parts `draft` and `meta` as JSON and `photo`
   * as the file. A REJECTED receipt is an ordinary 200 reply.
   *
   * @param draft the draft; only the Java record's members are sent
   * @param meta the client's metadata
   * @param photoUri the local file of the photo (a `.jpg` or `.png` name)
   * @returns the receipt
   */
  receiveReport(draft: DraftToSend, meta: ClientMeta, photoUri: string): Promise<ReportReceipt>;
  /**
   * `GET /api/reports/{reportId}`.
   *
   * @param reportId the identifier of the report
   * @returns the report
   */
  getReportStatus(reportId: string): Promise<HazardReport>;
  /**
   * `GET /api/citizens/{citizenId}/reports`, newest first.
   *
   * @param citizenId the identifier of the citizen
   * @returns the citizen's reports
   */
  getReportsByReporter(citizenId: string): Promise<HazardReport[]>;
  /**
   * `PUT /api/citizens/{citizenId}/language` (204).
   *
   * @param citizenId the identifier of the citizen
   * @param language the new language
   */
  changePreferredLanguage(citizenId: string, language: Language): Promise<void>;
}

/**
 * Names the file of a local photo and its content type from the extension.
 *
 * @param uri the local URI of the photo
 * @returns the file name and `image/png` for `.png`, else `image/jpeg`
 */
export function photoFile(uri: string): { name: string; type: string } {
  const name = uri.substring(uri.lastIndexOf('/') + 1);
  return { name, type: name.toLowerCase().endsWith('.png') ? 'image/png' : 'image/jpeg' };
}

/**
 * Builds the UC2 API over an HTTP client.
 *
 * @param client the HTTP client
 * @returns the API
 */
export function createReportApi(client: HttpClient): ReportApi {
  return {
    receiveReport: (draft, meta, photoUri) =>
      client.postMultipart<ReportReceipt>(REPORTS_PATH, [
        { name: 'draft', value: jsonPart(toWire(draft)) },
        { name: 'meta', value: jsonPart(meta) },
        { name: 'photo', value: { uri: photoUri, ...photoFile(photoUri) } },
      ]),
    getReportStatus: (reportId) =>
      client.getJson<HazardReport>(`${REPORTS_PATH}/${encodeURIComponent(reportId)}`),
    getReportsByReporter: (citizenId) =>
      client.getJson<HazardReport[]>(
        CITIZEN_REPORTS_PATH.replace('{citizenId}', encodeURIComponent(citizenId)),
      ),
    changePreferredLanguage: (citizenId, language) =>
      client.putJson<void>(
        CITIZEN_LANGUAGE_PATH.replace('{citizenId}', encodeURIComponent(citizenId)),
        { language },
      ),
  };
}

/** Keeps only the members of the Java `ReportDraft` (no `photoUri`, no queue fields). */
function toWire(draft: DraftToSend): ReportDraftWire {
  return {
    draftId: draft.draftId,
    hazardType: draft.hazardType,
    description: draft.description,
    location: draft.location,
    locationFromGps: draft.locationFromGps,
    district: draft.district,
    capturedAt: draft.capturedAt,
  };
}
