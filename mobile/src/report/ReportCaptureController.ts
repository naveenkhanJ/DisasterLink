/**
 * ReportCaptureController.ts — the control of UC2 on the phone (model dictionary: `ReportCaptureController`,
 * "Runs on: device"; includes Save Data Offline): the home view, a new draft, the location, the
 * step-16 checks, the submission with its offline fallback, the replay of queued drafts, the
 * language. It has no screens; UC2-6's screens call it.
 *
 * Outcome rules (implementation standards §12.1): a 200 reply with ACCEPTED or DUPLICATE is
 * submitted; a REJECTED reply (200) and a 4xx are rejections (the draft is invalid, or marked
 * REJECTED on replay, and never sent again); a 5xx, a network error and being offline keep the
 * draft QUEUED. Any other failure — for example an error while building the request — is a
 * programming fault and is thrown as it is.
 *
 * Implements CH-10, CH-26 and CH-27 (SC2 steps 1 to 20, A1 to A3, A8, E1 to E4, E6, E8).
 */
import { ApiError, NetworkError } from '../api/httpClient';
import type {
  ClientMeta,
  GeoPoint,
  HazardReport,
  ReportApi,
  ReportReceipt,
  SyncResult,
} from '../api/reportApi';
import type { SkeletonApi } from '../api/skeletonApi';
import type { District, Language } from '../api/types';
import {
  ERROR_SEPARATOR,
  HTTP_PAYLOAD_TOO_LARGE,
  MAX_DESCRIPTION_LENGTH,
  PHOTO_TOO_LARGE_MESSAGE,
  SRI_LANKA_MAX_LATITUDE,
  SRI_LANKA_MAX_LONGITUDE,
  SRI_LANKA_MIN_LATITUDE,
  SRI_LANKA_MIN_LONGITUDE,
} from '../constants';
import type { LocationService } from '../location/LocationService';
import type { HomeCache } from '../store/HomeCache';
import type { OfflineReportStore } from '../store/OfflineReportStore';
import type { PhotoService } from './PhotoService';
import { withStatus, type HomeView, type ReportDraft, type SubmissionResult } from './ReportDraft';

/** The result of checking a draft (`ValidationResult`); each error starts with its field and a colon. */
export interface ValidationResult {
  valid: boolean;
  errors: string[];
}

/** What the controller needs, injected so that tests can replace each part (§10). */
export interface CaptureDependencies {
  reportApi: ReportApi;
  skeletonApi: SkeletonApi;
  store: OfflineReportStore;
  homeCache: HomeCache;
  location: LocationService;
  photos: Pick<PhotoService, 'deletePhoto'>;
  /** Whether the phone is online: connected and, when known, the internet is reachable. */
  isOnline: () => Promise<boolean>;
  /** The demo citizen's identifier: the reporter of every report. */
  citizenId: string;
  /** The demo citizen's registered district: the fallback district of a report and of the home. */
  registeredDistrict: District;
  /** Makes a UUID (`expo-crypto`'s `randomUUID`). */
  newId: () => string;
  /** The current time. */
  now: () => Date;
}

/** The operations of the control (model dictionary section 5). */
export interface ReportCaptureController {
  /**
   * Builds the home screen's data (SC2 steps 1 and 2; A8).
   *
   * @returns the home: from the server when online, from the cache when offline or unreachable
   *   (`fromCache`); a 5xx on a call leaves that part empty and sets `error` for the banner
   */
  loadHome(): Promise<HomeView>;
  /**
   * Starts a new draft (SC2 step 6): a new identifier and the time it was started.
   *
   * @returns the draft
   */
  startReport(): ReportDraft;
  /**
   * Gets the current location (SC2 steps 12 to 14).
   *
   * @returns the fix, or `undefined` if there is none (A1: the citizen types it)
   */
  captureLocation(): Promise<GeoPoint | undefined>;
  /**
   * Checks a draft against the rules of step 16, all at once (E1, E2, E3).
   *
   * @param draft the draft
   * @returns the result; each error is field-prefixed ("hazardType: …", "photo: …")
   */
  validateDraft(draft: ReportDraft): ValidationResult;
  /**
   * Submits a draft (SC2 steps 16 to 20, A2, E4, E6).
   *
   * @param draft the draft
   * @returns SUBMITTED with the report, INVALID with the errors, or QUEUED
   */
  submit(draft: ReportDraft): Promise<SubmissionResult>;
  /**
   * Sends the QUEUED drafts (SC2 18d to 18g, E4, E8). At most one run at a time: a call during a run
   * returns that run's promise.
   *
   * @returns the results of the drafts that were sent
   */
  syncPending(): Promise<SyncResult[]>;
  /**
   * Opens a stored draft, for example a REJECTED one for correction (E8 18l); it keeps its `draftId`.
   *
   * @param draftId the identifier of the draft
   * @returns the draft, or `undefined` if there is none
   */
  getDraft(draftId: string): Promise<ReportDraft | undefined>;
  /**
   * Discards a stored draft and its photo (E8 18o).
   *
   * @param draftId the identifier of the draft
   */
  discardDraft(draftId: string): Promise<void>;
  /**
   * Changes the citizen's language: on the server first, then in the cache (SC2 steps 3 to 5).
   *
   * @param language the new language
   * @throws ApiError or NetworkError if the server call fails (the cache is then unchanged)
   */
  changeLanguage(language: Language): Promise<void>;
  /**
   * Reads one report's status (SC2 step 30; "Track report status").
   *
   * @param reportId the identifier of the report
   * @returns the report
   */
  getReportStatus(reportId: string): Promise<HazardReport>;
}

/** A call that may fail with a 5xx: its value, or the fallback and `failed`. */
async function tolerateServerFault<T>(
  call: () => Promise<T>,
  fallback: T,
): Promise<{ value: T; failed: boolean }> {
  try {
    return { value: await call(), failed: false };
  } catch (error) {
    if (error instanceof ApiError && !error.isRejection) {
      return { value: fallback, failed: true };
    }
    throw error;
  }
}

/** Whether a failure means the send got no receipt and may be tried again: a 5xx or no connection. */
function isRetryable(error: unknown): boolean {
  return error instanceof NetworkError || (error instanceof ApiError && !error.isRejection);
}

/** Whether a failure is the server refusing the request itself: a 4xx. */
function isRefusal(error: unknown): error is ApiError {
  return error instanceof ApiError && error.isRejection;
}

/** The plain message of a refusal (413: the photo is too large). */
function refusalMessage(error: ApiError): string {
  return error.status === HTTP_PAYLOAD_TOO_LARGE ? PHOTO_TOO_LARGE_MESSAGE : error.detail;
}

/** Splits the message of a REJECTED receipt into its errors. */
function splitErrors(message: string | null | undefined): string[] {
  return message ? message.split(ERROR_SEPARATOR) : ['The report was not accepted'];
}

/** Whether a point lies within the box of Sri Lanka (the server's rule). */
function isWithinSriLanka(point: GeoPoint): boolean {
  return (
    point.latitude >= SRI_LANKA_MIN_LATITUDE &&
    point.latitude <= SRI_LANKA_MAX_LATITUDE &&
    point.longitude >= SRI_LANKA_MIN_LONGITUDE &&
    point.longitude <= SRI_LANKA_MAX_LONGITUDE
  );
}

/**
 * Builds the control over its collaborators.
 *
 * @param deps the collaborators
 * @returns the control
 */
export function createReportCaptureController(deps: CaptureDependencies): ReportCaptureController {
  let running: Promise<SyncResult[]> | undefined;

  /** The QUEUED drafts other than one: the `pendingCount` of a meta, recomputed at send time. */
  async function queuedOthers(draftId: string): Promise<number> {
    const stored = await deps.store.findPending();
    return stored.filter((other) => other.status === 'QUEUED' && other.draftId !== draftId).length;
  }

  async function send(draft: ReportDraft & { photoUri: string }): Promise<ReportReceipt> {
    const meta: ClientMeta = {
      clientId: await deps.store.getClientId(),
      userId: deps.citizenId,
      submissionId: draft.draftId,
      capturedAt: draft.capturedAt,
      pendingCount: await queuedOthers(draft.draftId),
    };
    return deps.reportApi.receiveReport(draft, meta, draft.photoUri);
  }

  /** Saves a QUEUED copy; a failed attempt (a send that got no receipt) adds one to `attempts`. */
  async function queue(draft: ReportDraft, failedAttempt: boolean): Promise<void> {
    const attempts = (draft.attempts ?? 0) + (failedAttempt ? 1 : 0);
    await deps.store.save(withStatus({ ...draft, attempts }, 'QUEUED'));
  }

  async function finish(draft: ReportDraft): Promise<void> {
    await deps.store.remove(draft.draftId);
    if (draft.photoUri) {
      deps.photos.deletePhoto(draft.photoUri);
    }
  }

  async function cachedHome(pendingDrafts: ReportDraft[], language: Language): Promise<HomeView> {
    const cached = await deps.homeCache.loadHome();
    return {
      warnings: cached?.warnings ?? [],
      myReports: cached?.myReports ?? [],
      pendingDrafts,
      language,
      fromCache: true,
    };
  }

  /**
   * Saves the home per part (standards section 10 item 16, P225): a part whose call worked is saved
   * as loaded, a part whose call failed keeps what was saved before (or nothing); nothing is saved
   * when both failed. So a fault in one call no longer leaves the other part out of the saved copy.
   */
  async function cacheWorkingParts(
    view: HomeView,
    warningsFailed: boolean,
    reportsFailed: boolean,
  ): Promise<void> {
    if (warningsFailed && reportsFailed) {
      return;
    }
    const saved = await deps.homeCache.loadHome();
    await deps.homeCache.saveHome({
      warnings: warningsFailed ? (saved?.warnings ?? []) : view.warnings,
      myReports: reportsFailed ? (saved?.myReports ?? []) : view.myReports,
      pendingDrafts: view.pendingDrafts,
      language: view.language,
      fromCache: false,
    });
  }

  function validateDraft(draft: ReportDraft): ValidationResult {
    const errors: string[] = [];
    if (!draft.hazardType) {
      errors.push('hazardType: a hazard type is required');
    }
    if (!draft.description || draft.description.trim().length === 0) {
      errors.push('description: a description is required');
    } else if (draft.description.length > MAX_DESCRIPTION_LENGTH) {
      errors.push(`description: at most ${MAX_DESCRIPTION_LENGTH} characters`);
    }
    if (!draft.photoUri) {
      errors.push('photo: one photo is required');
    }
    if (!draft.location) {
      errors.push('location: a location is required');
    } else if (!isWithinSriLanka(draft.location)) {
      errors.push('location: outside Sri Lanka');
    }
    return { valid: errors.length === 0, errors };
  }

  async function runSync(): Promise<SyncResult[]> {
    const results: SyncResult[] = [];
    if (!(await deps.isOnline())) {
      return results;
    }
    const queued = (await deps.store.findPending()).filter((draft) => draft.status === 'QUEUED');
    for (const draft of queued) {
      if (!draft.photoUri) {
        const reason = 'photo: one photo is required';
        await deps.store.markRejected(draft.draftId, reason);
        results.push({ submissionId: draft.draftId, outcome: 'REJECTED', message: reason });
        continue;
      }
      try {
        const receipt = await send({ ...draft, photoUri: draft.photoUri });
        const result = receipt.result;
        if (result.outcome === 'REJECTED') {
          const reason = result.message ?? 'The report was not accepted';
          await deps.store.markRejected(draft.draftId, reason);
          results.push({ ...result, submissionId: draft.draftId });
        } else {
          await finish(draft);
          results.push({
            ...result,
            submissionId: draft.draftId,
            message: receipt.report?.reportId ?? result.message,
          });
        }
      } catch (error) {
        if (isRefusal(error)) {
          const reason = refusalMessage(error);
          await deps.store.markRejected(draft.draftId, reason);
          results.push({
            submissionId: draft.draftId,
            outcome: 'REJECTED',
            message: reason,
          });
        } else if (isRetryable(error)) {
          await queue(draft, true);
          break;
        } else {
          throw error;
        }
      }
    }
    return results;
  }

  return {
    async loadHome() {
      const pendingDrafts = await deps.store.findPending();
      const language = await deps.homeCache.getLanguage();
      if (!(await deps.isOnline())) {
        return cachedHome(pendingDrafts, language);
      }
      try {
        const warnings = await tolerateServerFault(
          () => deps.skeletonApi.getActiveWarnings(deps.registeredDistrict),
          [],
        );
        const reports = await tolerateServerFault(
          () => deps.reportApi.getReportsByReporter(deps.citizenId),
          [],
        );
        const failed = warnings.failed || reports.failed;
        const view: HomeView = {
          warnings: warnings.value,
          myReports: reports.value,
          pendingDrafts,
          language,
          fromCache: false,
          ...(failed ? { error: true } : {}),
        };
        await cacheWorkingParts(view, warnings.failed, reports.failed);
        return view;
      } catch (error) {
        if (error instanceof NetworkError) {
          return cachedHome(pendingDrafts, language);
        }
        throw error;
      }
    },
    startReport() {
      return {
        draftId: deps.newId(),
        locationFromGps: false,
        capturedAt: deps.now().toISOString(),
      };
    },
    captureLocation: () => deps.location.getCurrentLocation(),
    validateDraft,
    async submit(draft) {
      const prepared: ReportDraft = {
        ...draft,
        district:
          (draft.location ? deps.location.resolveDistrict(draft.location) : undefined) ??
          deps.registeredDistrict,
      };
      const validation = validateDraft(prepared);
      if (!validation.valid || !prepared.photoUri) {
        return { draftId: draft.draftId, state: 'INVALID', errors: validation.errors };
      }
      if (!(await deps.isOnline())) {
        await queue(prepared, false);
        return { draftId: draft.draftId, state: 'QUEUED', errors: [] };
      }
      try {
        const receipt = await send({ ...prepared, photoUri: prepared.photoUri });
        if (receipt.result.outcome === 'REJECTED') {
          return {
            draftId: draft.draftId,
            state: 'INVALID',
            errors: splitErrors(receipt.result.message),
          };
        }
        await finish(prepared);
        return {
          draftId: draft.draftId,
          state: 'SUBMITTED',
          report: receipt.report ?? undefined,
          errors: [],
        };
      } catch (error) {
        if (isRefusal(error)) {
          return { draftId: draft.draftId, state: 'INVALID', errors: [refusalMessage(error)] };
        }
        if (isRetryable(error)) {
          await queue(prepared, true);
          return { draftId: draft.draftId, state: 'QUEUED', errors: [] };
        }
        throw error;
      }
    },
    syncPending() {
      if (!running) {
        running = runSync().finally(() => {
          running = undefined;
        });
      }
      return running;
    },
    async getDraft(draftId) {
      return (await deps.store.findPending()).find((draft) => draft.draftId === draftId);
    },
    async discardDraft(draftId) {
      const draft = (await deps.store.findPending()).find((stored) => stored.draftId === draftId);
      if (draft?.photoUri) {
        deps.photos.deletePhoto(draft.photoUri);
      }
      await deps.store.remove(draftId);
    },
    async changeLanguage(language) {
      await deps.reportApi.changePreferredLanguage(deps.citizenId, language);
      await deps.homeCache.changeLanguage(language);
    },
    getReportStatus: (reportId) => deps.reportApi.getReportStatus(reportId),
  };
}
