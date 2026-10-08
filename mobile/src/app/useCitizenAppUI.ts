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
 * Beyond the dictionary's inputs the hook has the screens' own (non-dictionary) handlers, as
 * standards §10 "Phone screens beyond the frames" and "Manual location" describe (P206, UC2-7a):
 * the back history and the discard question (`goBack`, `requestLeave`, `confirmDiscard`,
 * `keepEditing`), the loading and refresh state, the S05c update of a sent draft, the location
 * access notice (`allowLocationAccess`), the photo removal and the photo buttons.
 *
 * Implements CH-10, CH-25, CH-26, CH-27 (SC2 steps 1 to 20, 30 to 32; A1 to A3, A8; E1 to E4, E6, E8).
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Linking } from 'react-native';
import type { GeoPoint, HazardReport, ReportDecisionNotice, SyncResult } from '../api/reportApi';
import type { HazardType, Language, WarningLevel } from '../api/types';
import type { AppEnv } from '../config/env';
import { districtCentre } from '../location/districtCentre';
import type { LocationAccess, LocationAccessState } from '../location/locationAccess';
import { presentLocalAlert } from '../notifications/notifications';
import type { PhotoService } from '../report/PhotoService';
import type { ReportCaptureController } from '../report/ReportCaptureController';
import type { HomeView, ReportDraft, SubmissionResult } from '../report/ReportDraft';
import {
  decisionNoticeText,
  LEVEL_LABELS,
  NO_ERRORS,
  REPORT_NOT_PREPARED_TEXT,
  toFieldErrors,
  type FieldErrors,
} from '../screens/texts';

/** How long the "not accepted" confirmation shows before the app returns to the review screen. */
export const RETURN_TO_REVIEW_MS = 2_500;

/** The title of the local notification of a report decision. */
const DECISION_TITLE = 'Report decision';

/** The screen a report status was opened from (the Back link returns there). */
export type StatusOrigin = 'home' | 'submissionResult';

/** The screens the in-memory navigator moves between (the WF2P screen ids). */
export type Route =
  | { name: 'home' }
  | { name: 'chooseHazardType' }
  | { name: 'reportForm' }
  | { name: 'review' }
  | { name: 'submissionResult' }
  | { name: 'reportStatus'; reportId: string; from: StatusOrigin };

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
  photos: Pick<PhotoService, 'takePhoto' | 'choosePhoto' | 'deletePhoto'>;
  locationAccess: LocationAccess;
  env: Pick<AppEnv, 'citizenLanguage' | 'citizenDistrict'>;
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
  /** Why the last capture gave no fix, when it is known (WF2P-S03g). */
  locationAccess: LocationAccessState | undefined;
  /** Where the manual-location map starts: the centre of the registered district. */
  mapStart: GeoPoint;
  /** The errors of the last review, under their fields (`showValidationErrors`, `showPhotoError`). */
  errors: FieldErrors;
  /** The source whose permission was refused (WF2P-S03d). */
  permissionDenied: PhotoSource | undefined;
  /** The last submission (`showSubmissionResult`). */
  result: SubmissionResult | undefined;
  /** The report ID once the queued draft of `result` was sent (WF2P-S05c). */
  sentReportId: string | undefined;
  /** The report of the status screen (`showReportStatus`). */
  report: HazardReport | undefined;
  /** An online action failed with a technical fault: the common error banner (C1). */
  serviceUnavailable: boolean;
  /** The last synchronisation results (`showSyncResults`). */
  syncResults: SyncResult[];
  /** A rejected draft was just discarded (WF2P-S01d). */
  draftDiscarded: boolean;
  /** The in-app alert banners, one per merged alert; the shell draws the newest few. */
  alerts: AlertNotice[];
  /** The decision card, if a decision arrived. */
  decision: DecisionCard | undefined;
  /** A submission is in progress. */
  submitting: boolean;
  /** An online action (home, status, language) is running. */
  loading: boolean;
  /** A pull-to-refresh is running. */
  refreshing: boolean;
  /** The question "Discard this report?" is open. */
  discardQuestion: boolean;
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
  /** The dictionary input; the status is opened from the home (the Back link returns there). */
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
  /** "Remove photo" in the preview: the photo is required again before review. */
  removePhoto(): void;
  /** "Change" on the report form: back to the hazard-type list with the draft kept. */
  changeHazardType(): void;
  /** Opens a report status and remembers the screen it came from (the Back link returns there). */
  openReportStatusFrom(reportId: string, from: StatusOrigin): Promise<void>;
  /** Pull-to-refresh on the home: reloads the home and the location, the draft is untouched. */
  refreshHomeScreen(): Promise<void>;
  /** Pull-to-refresh on the report status: reads the open report again. */
  refreshReportStatus(): Promise<void>;
  /** Tries the failed online action again (the common error banner's Retry). */
  retry(): Promise<void>;
  /**
   * The app returned to the foreground (for example from Settings): reads the location again on the home, and
   * for the draft on the report form while it has none; nothing on other screens (standards §10 item 8, P206).
   */
  onAppForeground(): Promise<void>;
  /**
   * The Back link and Android's Back button.
   *
   * @returns whether the press was handled (false on the home and the confirmation screens)
   */
  goBack(): boolean;
  /** Runs `next` at once, or after the question "Discard this report?" if the draft has content. */
  requestLeave(next: () => void | Promise<void>): void;
  /** "Discard" in the question: drops the unsaved draft's photo copy and runs what was asked. */
  confirmDiscard(): Promise<void>;
  /** "Keep editing" in the question. */
  keepEditing(): void;
  /** "Allow location access": opens Settings, or asks the permission again and captures anew. */
  allowLocationAccess(): Promise<void>;
  dismissSyncResults(): void;
  dismissDiscarded(): void;
  dismissAlert(id: number): void;
  dismissDecision(): void;
}

/** The home before the first load: nothing to show yet, in the configured language. */
function emptyHome(language: Language): HomeView {
  return { warnings: [], myReports: [], pendingDrafts: [], language, fromCache: false };
}

/** The screens of the report flow, where leaving may need the discard question. */
const FLOW_ROUTES: ReadonlySet<string> = new Set(['chooseHazardType', 'reportForm', 'review']);

/**
 * Tells whether an unsaved draft has something worth asking about: a description, a photo or a
 * manual location (standards §10 item 2).
 *
 * @param draft the draft
 * @returns whether it has content
 */
function hasContent(draft: ReportDraft): boolean {
  return (
    (draft.description ?? '').trim() !== '' ||
    Boolean(draft.photoUri) ||
    (draft.location !== undefined && !draft.locationFromGps)
  );
}

/**
 * Holds the state of the screens and gives the inputs.
 *
 * @param services the controller, the photo service, the location access and the settings
 * @returns the state and the inputs; the inputs keep their identity between renders
 */
export function useCitizenAppUI(services: UiServices): CitizenAppUI {
  const { controller, photos, env, locationAccess: access } = services;
  const [route, setRoute] = useState<Route>({ name: 'home' });
  const [home, setHome] = useState<HomeView>(() => emptyHome(env.citizenLanguage));
  const [currentLocation, setCurrentLocation] = useState<GeoPoint>();
  const [draft, setDraftState] = useState<ReportDraft>();
  const [locationUnavailable, setLocationUnavailableState] = useState(false);
  const [locationAccessState, setLocationAccessState] = useState<LocationAccessState>();
  const [errors, setErrors] = useState<FieldErrors>(NO_ERRORS);
  const [permissionDenied, setPermissionDenied] = useState<PhotoSource>();
  const [result, setResultState] = useState<SubmissionResult>();
  const [sentReportId, setSentReportId] = useState<string>();
  const [report, setReport] = useState<HazardReport>();
  const [serviceUnavailable, setServiceUnavailable] = useState(false);
  const [syncResults, setSyncResults] = useState<SyncResult[]>([]);
  const [draftDiscarded, setDraftDiscarded] = useState(false);
  const [alerts, setAlerts] = useState<AlertNotice[]>([]);
  const [decision, setDecision] = useState<DecisionCard>();
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [discardQuestion, setDiscardQuestionState] = useState(false);
  const draftRef = useRef<ReportDraft>(undefined);
  const homeRef = useRef<HomeView>(undefined);
  const routeRef = useRef<Route>({ name: 'home' });
  const resultRef = useRef<SubmissionResult>(undefined);
  const accessRef = useRef<LocationAccessState>(undefined);
  const discardQuestionRef = useRef(false);
  const unavailableRef = useRef(false);
  const pendingLeave = useRef<() => void | Promise<void>>(undefined);
  const submittingRef = useRef(false);
  const loadingCount = useRef(0);
  const nextAlertId = useRef(0);
  const mapStart = useMemo(() => districtCentre(env.citizenDistrict), [env.citizenDistrict]);

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

  const setResult = useCallback((next: SubmissionResult | undefined) => {
    resultRef.current = next;
    setResultState(next);
    // The report ID belongs to the stored result: it goes when that result is replaced (a new
    // submission, a new draft, the home), so the Back link can return to S05c (UC2-7b E1).
    setSentReportId(undefined);
  }, []);

  const setLocationUnavailable = useCallback((next: boolean) => {
    unavailableRef.current = next;
    setLocationUnavailableState(next);
  }, []);

  const setAccess = useCallback((next: LocationAccessState | undefined) => {
    accessRef.current = next;
    setLocationAccessState(next);
  }, []);

  const setDiscardQuestion = useCallback((open: boolean) => {
    discardQuestionRef.current = open;
    setDiscardQuestionState(open);
  }, []);

  const resetForm = useCallback(() => {
    setLocationUnavailable(false);
    setAccess(undefined);
    setErrors(NO_ERRORS);
    setPermissionDenied(undefined);
    setResult(undefined);
  }, [setAccess, setResult, setLocationUnavailable]);

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

  /** Runs an action with the loading flag set; overlapping actions share one count. */
  const withLoading = useCallback(async (action: () => Promise<void>) => {
    loadingCount.current += 1;
    setLoading(true);
    try {
      await action();
    } finally {
      loadingCount.current -= 1;
      setLoading(loadingCount.current > 0);
    }
  }, []);

  const refreshHome = useCallback(async () => {
    const view = await controller.loadHome();
    homeRef.current = view;
    setHome(view);
  }, [controller]);

  const refreshLocation = useCallback(async () => {
    const fix = await controller.captureLocation();
    setCurrentLocation(fix);
    setAccess(fix ? undefined : await access.check());
  }, [controller, access, setAccess]);

  /** Captures the location for the draft; a fix that arrives for another draft is ignored. */
  const captureForDraft = useCallback(
    async (draftId: string) => {
      const fix = await controller.captureLocation();
      const current = draftRef.current;
      if (current?.draftId !== draftId || current.location) {
        return;
      }
      if (fix) {
        updateDraft({ location: fix, locationFromGps: true });
        setLocationUnavailable(false);
        setAccess(undefined);
      } else {
        setLocationUnavailable(true);
        setAccess(await access.check());
      }
    },
    [controller, access, updateDraft, setAccess, setLocationUnavailable],
  );

  const onAppForeground = useCallback(async () => {
    const current = routeRef.current;
    if (current.name === 'home') {
      await refreshLocation();
      return;
    }
    const waiting = draftRef.current;
    if (current.name === 'reportForm' && waiting && !waiting.location && unavailableRef.current) {
      await captureForDraft(waiting.draftId);
    }
  }, [refreshLocation, captureForDraft]);

  const openHome = useCallback(async () => {
    navigate({ name: 'home' });
    setDraft(undefined);
    resetForm();
    await guarded(() =>
      withLoading(async () => {
        await Promise.all([refreshHome(), refreshLocation()]);
      }),
    );
  }, [navigate, setDraft, resetForm, guarded, withLoading, refreshHome, refreshLocation]);

  const refreshHomeScreen = useCallback(async () => {
    setRefreshing(true);
    try {
      await guarded(async () => {
        await Promise.all([refreshHome(), refreshLocation()]);
      });
    } finally {
      setRefreshing(false);
    }
  }, [guarded, refreshHome, refreshLocation]);

  const selectLanguage = useCallback(
    async (language: Language) => {
      if (!homeRef.current || homeRef.current.fromCache) {
        return;
      }
      await guarded(() =>
        withLoading(async () => {
          await controller.changeLanguage(language);
          await refreshHome();
        }),
      );
    },
    [controller, guarded, withLoading, refreshHome],
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
      if (!current.location) {
        await captureForDraft(current.draftId);
      }
    },
    [updateDraft, navigate, captureForDraft],
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

  const removePhoto = useCallback(() => {
    const current = draftRef.current;
    if (!current?.photoUri) {
      return;
    }
    // A stored (REJECTED) draft's file stays: the store still points at it until the corrected draft
    // is resubmitted.
    if (current.status === undefined) {
      photos.deletePhoto(current.photoUri);
    }
    updateDraft({ photoUri: undefined });
  }, [photos, updateDraft]);

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
    [controller, updateDraft, setLocationUnavailable],
  );

  const allowLocationAccess = useCallback(async () => {
    if (accessRef.current === 'blocked') {
      await Linking.openSettings();
      return;
    }
    if (accessRef.current !== 'askable') {
      return;
    }
    if (await access.request()) {
      await refreshLocation();
      const current = draftRef.current;
      if (current && !current.location) {
        await captureForDraft(current.draftId);
      }
    } else {
      setAccess(await access.check());
    }
  }, [access, refreshLocation, captureForDraft, setAccess]);

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
  }, [controller, navigate, setResult]);

  /**
   * Reads a report for the status screen; the old report stays shown while it reloads. A pull-to-refresh passes
   * `showLoading: false`, so only the pull indicator shows, as on the home.
   */
  const readReport = useCallback(
    async (reportId: string, { showLoading = true }: { showLoading?: boolean } = {}) => {
      const read = async () => {
        setReport(await controller.getReportStatus(reportId));
      };
      await guarded(() => (showLoading ? withLoading(read) : read()));
    },
    [controller, guarded, withLoading],
  );

  const openReportStatusFrom = useCallback(
    async (reportId: string, from: StatusOrigin) => {
      navigate({ name: 'reportStatus', reportId, from });
      setDecision(undefined);
      setReport(undefined);
      await readReport(reportId);
    },
    [navigate, readReport],
  );

  const openReportStatus = useCallback(
    (reportId: string) => openReportStatusFrom(reportId, 'home'),
    [openReportStatusFrom],
  );

  const refreshReportStatus = useCallback(async () => {
    const current = routeRef.current;
    if (current.name !== 'reportStatus') {
      return;
    }
    setRefreshing(true);
    try {
      await readReport(current.reportId, { showLoading: false });
    } finally {
      setRefreshing(false);
    }
  }, [readReport]);

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

  const requestLeave = useCallback(
    (next: () => void | Promise<void>) => {
      const current = draftRef.current;
      const asks =
        FLOW_ROUTES.has(routeRef.current.name) &&
        current !== undefined &&
        current.status === undefined &&
        hasContent(current);
      if (asks) {
        pendingLeave.current = next;
        setDiscardQuestion(true);
      } else {
        void next();
      }
    },
    [setDiscardQuestion],
  );

  const confirmDiscard = useCallback(async () => {
    const current = draftRef.current;
    if (current?.status === undefined && current?.photoUri) {
      photos.deletePhoto(current.photoUri);
    }
    const next = pendingLeave.current;
    pendingLeave.current = undefined;
    setDiscardQuestion(false);
    await next?.();
  }, [photos, setDiscardQuestion]);

  const keepEditing = useCallback(() => {
    pendingLeave.current = undefined;
    setDiscardQuestion(false);
  }, [setDiscardQuestion]);

  const goBack = useCallback((): boolean => {
    if (discardQuestionRef.current) {
      keepEditing();
      return true;
    }
    const current = routeRef.current;
    switch (current.name) {
      case 'chooseHazardType':
        requestLeave(openHome);
        return true;
      case 'reportForm':
        if (draftRef.current?.status === 'REJECTED') {
          void openHome();
        } else {
          navigate({ name: 'chooseHazardType' });
        }
        return true;
      case 'review':
        navigate({ name: 'reportForm' });
        return true;
      case 'reportStatus':
        if (current.from === 'home') {
          void openHome();
        } else {
          navigate({ name: 'submissionResult' });
        }
        return true;
      default:
        return false;
    }
  }, [keepEditing, requestLeave, openHome, navigate]);

  const onConnectivityRestored = useCallback(async () => {
    try {
      const results = await controller.syncPending();
      if (results.length > 0) {
        setSyncResults(results);
        const waiting = resultRef.current;
        if (routeRef.current.name === 'submissionResult' && waiting?.state === 'QUEUED') {
          const sent = results.find(
            (item) =>
              (item.outcome === 'ACCEPTED' || item.outcome === 'DUPLICATE') &&
              item.submissionId === waiting.draftId,
          );
          if (sent?.message) {
            setSentReportId(sent.message);
          }
        }
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
      const current = routeRef.current;
      if (current.name === 'reportStatus' && current.reportId === notice.reportId) {
        void readReport(notice.reportId);
      }
    },
    [env.citizenLanguage, readReport],
  );

  const retry = useCallback(async () => {
    const current = routeRef.current;
    if (current.name === 'reportStatus') {
      await readReport(current.reportId);
    } else {
      await guarded(refreshHome);
    }
  }, [readReport, guarded, refreshHome]);

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
    locationAccess: locationAccessState,
    mapStart,
    errors,
    permissionDenied,
    result,
    sentReportId,
    report,
    serviceUnavailable,
    syncResults,
    draftDiscarded,
    alerts,
    decision,
    submitting,
    loading,
    refreshing,
    discardQuestion,
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
    removePhoto,
    changeHazardType,
    openReportStatusFrom,
    refreshHomeScreen,
    refreshReportStatus,
    retry,
    onAppForeground,
    goBack,
    requestLeave,
    confirmDiscard,
    keepEditing,
    allowLocationAccess,
    dismissSyncResults,
    dismissDiscarded,
    dismissAlert,
    dismissDecision,
  };
}
