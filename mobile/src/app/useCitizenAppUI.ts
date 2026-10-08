/**
 * useCitizenAppUI.ts — the inputs of the citizen app's screen boundary (model dictionary:
 * `CitizenAppUI`, "Runs on: device"). Each input has the dictionary's name; each calls
 * `ReportCaptureController` (never the server or a store directly, round-2 MD N13) and keeps the
 * state the `show…` screens display. The screens are components named after their displays
 * (`ShowHome`, `ShowAlert`, …); the draft being edited is held here (C3) and passed to
 * `validateDraft` and `submit`.
 *
 * Technical failures of an online action (5xx, timeout, unreachable) set `serviceUnavailable`, which
 * the shared screen wrapper draws as the common error banner (C1); a form keeps its values on every
 * error (C2). The submission is offline-capable, so it never sets the banner: it queues.
 *
 * Implements CH-10, CH-25, CH-26, CH-27 (SC2 steps 1 to 20, 30 to 32; A1 to A3, A8; E1 to E4, E6, E8).
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import type { GeoPoint, HazardReport, ReportDecisionNotice, SyncResult } from '../api/reportApi';
import type { HazardType, Language, WarningLevel } from '../api/types';
import { presentLocalAlert } from '../notifications/notifications';
import type { PhotoService } from '../report/PhotoService';
import type { HomeView, ReportDraft, SubmissionResult } from '../report/ReportDraft';
import type { ReportCaptureController } from '../report/ReportCaptureController';
import {
  decisionNoticeText,
  LEVEL_LABELS,
  NO_ERRORS,
  REPORT_NOT_PREPARED_TEXT,
  toFieldErrors,
  type FieldErrors,
} from '../screens/texts';
import type { AppEnv } from '../config/env';

/** How long the "not accepted" confirmation shows before the app returns to the review screen. */
export const RETURN_TO_REVIEW_MS = 2_500;

/** The title of the local notification of a report decision. */
const DECISION_TITLE = 'Report decision';

/** The screens the in-memory navigator moves between (the WF2P screen ids). */
export type Route =
  | { name: 'home' }
  | { name: 'chooseHazardType' }
  | { name: 'reportForm' }
  | { name: 'review' }
  | { name: 'submissionResult' }
  | { name: 'reportStatus'; reportId: string };

/** Where a photo comes from. */
export type PhotoSource = 'camera' | 'gallery';

/** One in-app alert banner (`showAlert`). */
export interface AlertNotice {
  id: number;
  warningId: string;
  warningLevel: WarningLevel;
  text: string;
}

/** The decision card (`showReportDecision`): the notice and its text in the citizen's language. */
export interface DecisionCard {
  notice: ReportDecisionNotice;
  text: string;
  language: Language;
}

/** What the hook needs from the app's services. */
export interface UiServices {
  controller: ReportCaptureController;
  photos: Pick<PhotoService, 'takePhoto' | 'choosePhoto'>;
  env: Pick<AppEnv, 'citizenLanguage'>;
}

/** What the screens display. */
export interface CitizenAppState {
  route: Route;
  /** The last home view; an empty one (the configured language) until the first load. */
  home: HomeView;
  /** The current location shown on the home screen (`showLocation`), if there is a fix. */
  currentLocation: GeoPoint | undefined;
  /** The draft being edited (C3). */
  draft: ReportDraft | undefined;
  /** The GPS fix failed: the form shows `showLocationUnavailable`. */
  locationUnavailable: boolean;
  /** The errors of the last review, under their fields (`showValidationErrors`, `showPhotoError`). */
  errors: FieldErrors;
  /** The source whose permission was refused (WF2P-S03d). */
  permissionDenied: PhotoSource | undefined;
  /** The last submission (`showSubmissionResult`). */
  result: SubmissionResult | undefined;
  /** The report of the status screen (`showReportStatus`). */
  report: HazardReport | undefined;
  /** An online action failed with a technical fault: the common error banner (C1). */
  serviceUnavailable: boolean;
  /** The last synchronisation results (`showSyncResults`). */
  syncResults: SyncResult[];
  /** A rejected draft was just discarded (WF2P-S01d). */
  draftDiscarded: boolean;
  /** The in-app alert banners, one per merged alert. */
  alerts: AlertNotice[];
  /** The decision card, if a decision arrived. */
  decision: DecisionCard | undefined;
  /** A submission is in progress. */
  submitting: boolean;
}

/** The inputs of the screen boundary, with the dictionary's names, and the screens' own handlers. */
export interface CitizenAppUI extends CitizenAppState {
  openHome(): Promise<void>;
  selectLanguage(language: Language): Promise<void>;
  startReport(): void;
  selectHazardType(type: HazardType): Promise<void>;
  enterDescription(text: string): void;
  attachPhoto(photoUri: string): void;
  enterLocationManually(location: GeoPoint): void;
  reviewReport(): void;
  editReport(): void;
  submitReport(): Promise<void>;
  openReportStatus(reportId: string): Promise<void>;
  openDraft(draftId: string): Promise<void>;
  discardDraft(draftId: string): Promise<void>;
  /** Found message: the connection is back (also raised at start-up and on returning to the app). */
  onConnectivityRestored(): Promise<void>;
  /** Found message: a merged warning alert was polled. */
  receiveAlert(warningId: string, warningLevel: WarningLevel, text: string): void;
  /** Found message: a report decision was polled. */
  receiveReportDecision(notice: ReportDecisionNotice): void;
  /** The photo buttons: takes or chooses a photo and attaches it (`attachPhoto`). */
  pickPhoto(source: PhotoSource): Promise<void>;
  /** "Change" on the report form: back to the hazard-type list with the draft kept. */
  changeHazardType(): void;
  /** Tries the failed online action again (the common error banner's Retry). */
  retry(): Promise<void>;
  dismissSyncResults(): void;
  dismissDiscarded(): void;
  dismissAlert(id: number): void;
  dismissDecision(): void;
}

/** The home before the first load: nothing to show yet, in the configured language. */
function emptyHome(language: Language): HomeView {
  return { warnings: [], myReports: [], pendingDrafts: [], language, fromCache: false };
}

/**
 * Holds the state of the screens and gives the inputs.
 *
 * @param services the controller, the photo service and the settings
 * @returns the state and the inputs; the inputs keep their identity between renders
 */
export function useCitizenAppUI(services: UiServices): CitizenAppUI {
  const { controller, photos, env } = services;
  const [route, setRoute] = useState<Route>({ name: 'home' });
  const [home, setHome] = useState<HomeView>(() => emptyHome(env.citizenLanguage));
  const [currentLocation, setCurrentLocation] = useState<GeoPoint>();
  const [draft, setDraftState] = useState<ReportDraft>();
  const [locationUnavailable, setLocationUnavailable] = useState(false);
  const [errors, setErrors] = useState<FieldErrors>(NO_ERRORS);
  const [permissionDenied, setPermissionDenied] = useState<PhotoSource>();
  const [result, setResult] = useState<SubmissionResult>();
  const [report, setReport] = useState<HazardReport>();
  const [serviceUnavailable, setServiceUnavailable] = useState(false);
  const [syncResults, setSyncResults] = useState<SyncResult[]>([]);
  const [draftDiscarded, setDraftDiscarded] = useState(false);
  const [alerts, setAlerts] = useState<AlertNotice[]>([]);
  const [decision, setDecision] = useState<DecisionCard>();
  const [submitting, setSubmitting] = useState(false);
  const draftRef = useRef<ReportDraft>(undefined);
  const homeRef = useRef<HomeView>(undefined);
  const routeRef = useRef<Route>({ name: 'home' });
  const submittingRef = useRef(false);
  const nextAlertId = useRef(0);

  const navigate = useCallback((next: Route) => {
    routeRef.current = next;
    setRoute(next);
  }, []);

  const setDraft = useCallback((next: ReportDraft | undefined) => {
    draftRef.current = next;
    setDraftState(next);
  }, []);

  const updateDraft = useCallback(
    (patch: Partial<ReportDraft>) => {
      if (draftRef.current) {
        setDraft({ ...draftRef.current, ...patch });
      }
    },
    [setDraft],
  );

  const resetForm = useCallback(() => {
    setLocationUnavailable(false);
    setErrors(NO_ERRORS);
    setPermissionDenied(undefined);
    setResult(undefined);
  }, []);

  /** Runs an online action; a failure shows the common error banner and changes nothing (C1). */
  const guarded = useCallback(async (action: () => Promise<void>) => {
    setServiceUnavailable(false);
    try {
      await action();
    } catch (error) {
      console.warn('[CitizenAppUI] an online action failed:', error);
      setServiceUnavailable(true);
    }
  }, []);

  const refreshHome = useCallback(async () => {
    const view = await controller.loadHome();
    homeRef.current = view;
    setHome(view);
  }, [controller]);

  const refreshLocation = useCallback(async () => {
    setCurrentLocation(await controller.captureLocation());
  }, [controller]);

  const openHome = useCallback(async () => {
    navigate({ name: 'home' });
    setDraft(undefined);
    resetForm();
    await guarded(async () => {
      await Promise.all([refreshHome(), refreshLocation()]);
    });
  }, [navigate, setDraft, resetForm, guarded, refreshHome, refreshLocation]);

  const selectLanguage = useCallback(
    async (language: Language) => {
      if (!homeRef.current || homeRef.current.fromCache) {
        return;
      }
      await guarded(async () => {
        await controller.changeLanguage(language);
        await refreshHome();
      });
    },
    [controller, guarded, refreshHome],
  );

  const startReport = useCallback(() => {
    setDraft(controller.startReport());
    resetForm();
    navigate({ name: 'chooseHazardType' });
  }, [controller, setDraft, resetForm, navigate]);

  const selectHazardType = useCallback(
    async (type: HazardType) => {
      const current = draftRef.current;
      if (!current) {
        return;
      }
      updateDraft({ hazardType: type });
      navigate({ name: 'reportForm' });
      if (current.location) {
        return;
      }
      const fix = await controller.captureLocation();
      if (draftRef.current?.draftId !== current.draftId || draftRef.current.location) {
        return;
      }
      if (fix) {
        updateDraft({ location: fix, locationFromGps: true });
        setLocationUnavailable(false);
      } else {
        setLocationUnavailable(true);
      }
    },
    [controller, updateDraft, navigate],
  );

  const enterDescription = useCallback(
    (text: string) => updateDraft({ description: text }),
    [updateDraft],
  );

  const attachPhoto = useCallback(
    (photoUri: string) => {
      if (!draftRef.current) {
        return;
      }
      updateDraft({ photoUri });
      setPermissionDenied(undefined);
      setErrors((previous) => ({ ...previous, photo: undefined }));
    },
    [updateDraft],
  );

  const pickPhoto = useCallback(
    async (source: PhotoSource) => {
      const current = draftRef.current;
      if (!current) {
        return;
      }
      const picked =
        source === 'camera'
          ? await photos.takePhoto(current.draftId)
          : await photos.choosePhoto(current.draftId);
      if (picked.kind === 'photo') {
        attachPhoto(picked.uri);
      } else if (picked.kind === 'permissionDenied') {
        setPermissionDenied(source);
      }
    },
    [photos, attachPhoto],
  );

  const enterLocationManually = useCallback(
    (location: GeoPoint) => {
      const current = draftRef.current;
      if (!current) {
        return;
      }
      const problems = controller
        .validateDraft({ ...current, location })
        .errors.filter((error) => error.startsWith('location:'));
      if (problems.length > 0) {
        setErrors((previous) => ({ ...previous, location: toFieldErrors(problems).location }));
        return;
      }
      updateDraft({ location, locationFromGps: false });
      setLocationUnavailable(false);
      setErrors((previous) => ({ ...previous, location: undefined }));
    },
    [controller, updateDraft],
  );

  const reviewReport = useCallback(() => {
    const current = draftRef.current;
    if (!current) {
      return;
    }
    const validation = controller.validateDraft(current);
    if (validation.valid) {
      setErrors(NO_ERRORS);
      navigate({ name: 'review' });
    } else {
      setErrors(toFieldErrors(validation.errors));
      navigate({ name: 'reportForm' });
    }
  }, [controller, navigate]);

  const editReport = useCallback(() => navigate({ name: 'reportForm' }), [navigate]);

  const changeHazardType = useCallback(() => navigate({ name: 'chooseHazardType' }), [navigate]);

  const submitReport = useCallback(async () => {
    const current = draftRef.current;
    if (!current || submittingRef.current) {
      return;
    }
    submittingRef.current = true;
    setSubmitting(true);
    try {
      setResult(await controller.submit(current));
    } catch (error) {
      console.warn('[CitizenAppUI] the report could not be prepared:', error);
      setResult({ draftId: current.draftId, state: 'INVALID', errors: [REPORT_NOT_PREPARED_TEXT] });
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
    navigate({ name: 'submissionResult' });
  }, [controller, navigate]);

  const openReportStatus = useCallback(
    async (reportId: string) => {
      navigate({ name: 'reportStatus', reportId });
      setDecision(undefined);
      setReport(undefined);
      await guarded(async () => {
        setReport(await controller.getReportStatus(reportId));
      });
    },
    [controller, navigate, guarded],
  );

  const openDraft = useCallback(
    async (draftId: string) => {
      const stored = await controller.getDraft(draftId);
      if (!stored) {
        await openHome();
        return;
      }
      setDraft(stored);
      resetForm();
      navigate({ name: 'reportForm' });
    },
    [controller, openHome, setDraft, resetForm, navigate],
  );

  const discardDraft = useCallback(
    async (draftId: string) => {
      await controller.discardDraft(draftId);
      setDraftDiscarded(true);
      await openHome();
    },
    [controller, openHome],
  );

  const onConnectivityRestored = useCallback(async () => {
    try {
      const results = await controller.syncPending();
      if (results.length > 0) {
        setSyncResults(results);
      }
    } catch (error) {
      console.warn('[CitizenAppUI] the synchronisation failed:', error);
    }
    await guarded(refreshHome);
  }, [controller, guarded, refreshHome]);

  const receiveAlert = useCallback(
    (warningId: string, warningLevel: WarningLevel, text: string) => {
      nextAlertId.current += 1;
      const notice: AlertNotice = { id: nextAlertId.current, warningId, warningLevel, text };
      setAlerts((previous) => [...previous, notice]);
      Promise.resolve()
        .then(() => presentLocalAlert(LEVEL_LABELS[warningLevel], text))
        .catch((error: unknown) =>
          console.warn('[CitizenAppUI] the alert could not be shown:', error),
        );
    },
    [],
  );

  const receiveReportDecision = useCallback(
    (notice: ReportDecisionNotice) => {
      const language = homeRef.current?.language ?? env.citizenLanguage;
      const text = decisionNoticeText(notice, language);
      setDecision({ notice, text, language });
      Promise.resolve()
        .then(() => presentLocalAlert(DECISION_TITLE, text))
        .catch((error: unknown) =>
          console.warn('[CitizenAppUI] the notice could not be shown:', error),
        );
    },
    [env.citizenLanguage],
  );

  const retry = useCallback(async () => {
    const current = routeRef.current;
    if (current.name === 'reportStatus') {
      await openReportStatus(current.reportId);
    } else {
      await guarded(refreshHome);
    }
  }, [openReportStatus, guarded, refreshHome]);

  useEffect(() => {
    if (route.name !== 'submissionResult' || result?.state !== 'INVALID') {
      return undefined;
    }
    const timer = setTimeout(() => navigate({ name: 'review' }), RETURN_TO_REVIEW_MS);
    return () => clearTimeout(timer);
  }, [route, result, navigate]);

  const dismissSyncResults = useCallback(() => setSyncResults([]), []);
  const dismissDiscarded = useCallback(() => setDraftDiscarded(false), []);
  const dismissAlert = useCallback(
    (id: number) => setAlerts((previous) => previous.filter((alert) => alert.id !== id)),
    [],
  );
  const dismissDecision = useCallback(() => setDecision(undefined), []);

  return {
    route,
    home,
    currentLocation,
    draft,
    locationUnavailable,
    errors,
    permissionDenied,
    result,
    report,
    serviceUnavailable,
    syncResults,
    draftDiscarded,
    alerts,
    decision,
    submitting,
    openHome,
    selectLanguage,
    startReport,
    selectHazardType,
    enterDescription,
    attachPhoto,
    enterLocationManually,
    reviewReport,
    editReport,
    submitReport,
    openReportStatus,
    openDraft,
    discardDraft,
    onConnectivityRestored,
    receiveAlert,
    receiveReportDecision,
    pickPhoto,
    changeHazardType,
    retry,
    dismissSyncResults,
    dismissDiscarded,
    dismissAlert,
    dismissDecision,
  };
}
