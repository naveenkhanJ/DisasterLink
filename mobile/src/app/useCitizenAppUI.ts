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
import { NetworkError } from '../api/httpClient';
import type { GeoPoint, HazardReport, ReportDecisionNotice, SyncResult } from '../api/reportApi';
import type { HazardType, Language, WarningLevel } from '../api/types';
import type { AppEnv } from '../config/env';
import { MAX_ADDRESS_LENGTH, MIN_RETRY_FEEDBACK_MS } from '../constants';
import { districtCentre } from '../location/districtCentre';
import type { LocationAccess, LocationAccessState } from '../location/locationAccess';
import { presentLocalAlert } from '../notifications/notifications';
import type { PhotoService } from '../report/PhotoService';
import { composeDescription, splitDescription } from '../report/descriptionAddress';
import type { ReportCaptureController } from '../report/ReportCaptureController';
import type { HomeView, ReportDraft, SubmissionResult } from '../report/ReportDraft';
import type { ServerState } from '../sync/serverState';
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

/** What the shell's connection strip says (standards §10 item 11, P213). */
export type ConnectionState = 'online' | 'noInternet' | 'serverDown';

/** Where a photo comes from. */
export type PhotoSource = 'camera' | 'gallery';

/** One in-app alert banner (`showAlert`). */
export interface AlertNotice {
  id: number;
  warningId: string;
  warningLevel: WarningLevel;
  text: string;
}

/**
 * The decision card (`showReportDecision`): the notice. The card builds its text from the notice in
 * the selected language, so it follows the selector (standards section 10 item 18, P240); the local
 * notification keeps the text it arrived with.
 */
export interface DecisionCard {
  notice: ReportDecisionNotice;
}

/** What the hook needs from the app's services. */
export interface UiServices {
  controller: ReportCaptureController;
  photos: Pick<PhotoService, 'takePhoto' | 'choosePhoto' | 'deletePhoto'>;
  locationAccess: LocationAccess;
  /** The holder the hook sets from the connection strip (standards §10 item 11, P231). */
  serverState: Pick<ServerState, 'setDown'>;
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
  /** What the Description field shows; `draft.description` is its composition with the address. */
  descriptionText: string;
  /** What the "Address or landmark" field shows (standards §10 "The address", P214). */
  addressText: string;
  /** The place the manual location is named after, when the pin is near the place chosen (P213). */
  manualPlaceName: string | undefined;
  /** The picker was reopened by "Change" on a manual location (WF2P-S03a, P213). */
  changingLocation: boolean;
  /** The connection strip: no internet from the network listener, server down from the push poll (P213). */
  connection: ConnectionState;
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
  /** The report shown is the copy from the home or the submission, not read live (standards §10 item 17, P231). */
  reportFromCopy: boolean;
  /** Offline and no copy has the report: the status screen says it loads when online (standards §10 item 17, P231). */
  reportMissing: boolean;
  /**
   * A fix of the phone's position is being read: while it is and none is shown, the home's location card reads
   * "Finding your location…" (standards §10 item 4, P231).
   */
  locating: boolean;
  /** An online action failed with a technical fault: the common error banner (C1). */
  serviceUnavailable: boolean;
  /** When the last Retry ended with the fault still there; cleared when the banner goes (P222). */
  retriedAt: Date | undefined;
  /**
   * A Retry is running: from the press until both the call and the minimum feedback time are over
   * (standards section 10 item 12, P225); the banner's "Trying again…" follows this, not `loading`.
   */
  retrying: boolean;
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
  /** An online action started on the screen shown (the home, that report's status, a retry) is running (standards §10 item 4, P231). */
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
  /** Writes the address part of the description (non-dictionary; the description part is `enterDescription`). */
  enterAddress(text: string): void;
  /** Tells the hook which place the next manual point is named after (`undefined`: none). */
  nameManualPlace(place: string | undefined): void;
  /** "Change" on a manual location: the form shows the picker at once, at that point. */
  changeManualLocation(): void;
  /** "Keep this location": closes the picker opened by "Change" and keeps the location. */
  keepManualLocation(): void;
  /** The phone's network listener: whether the phone is online (standards §10 item 11). */
  onNetworkChange(online: boolean): void;
  /** The push poll's result: whether the server answered (standards §10 item 11). */
  onServerReachability(reachable: boolean): void;
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

/**
 * Gives the key under which a screen's loads are counted, so the loading row shows only on the screen
 * that started the load (standards §10 item 4, P231).
 *
 * @param route the route
 * @returns `status:<id>` for a report status, otherwise the route's name (`home` for the home)
 */
function loadingKey(route: Route): string {
  return route.name === 'reportStatus' ? `status:${route.reportId}` : route.name;
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
  const { controller, photos, env, locationAccess: access, serverState } = services;
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
  const [reportFromCopy, setReportFromCopy] = useState(false);
  const [reportMissing, setReportMissing] = useState(false);
  const [locating, setLocating] = useState(false);
  const [serviceUnavailable, setServiceUnavailable] = useState(false);
  const [retriedAt, setRetriedAt] = useState<Date>();
  const [retrying, setRetrying] = useState(false);
  const [syncResults, setSyncResults] = useState<SyncResult[]>([]);
  const [draftDiscarded, setDraftDiscarded] = useState(false);
  const [alerts, setAlerts] = useState<AlertNotice[]>([]);
  const [decision, setDecision] = useState<DecisionCard>();
  const [descriptionText, setDescriptionText] = useState('');
  const [addressText, setAddressText] = useState('');
  const [manualPlaceName, setManualPlaceName] = useState<string>();
  const [changingLocation, setChangingLocation] = useState(false);
  const [connection, setConnection] = useState<ConnectionState>('online');
  const [submitting, setSubmitting] = useState(false);
  const [loadingCounts, setLoadingCounts] = useState<Record<string, number>>({});
  const [refreshing, setRefreshing] = useState(false);
  const [discardQuestion, setDiscardQuestionState] = useState(false);
  const draftRef = useRef<ReportDraft>(undefined);
  const homeRef = useRef<HomeView>(undefined);
  const routeRef = useRef<Route>({ name: 'home' });
  const resultRef = useRef<SubmissionResult>(undefined);
  const accessRef = useRef<LocationAccessState>(undefined);
  const discardQuestionRef = useRef(false);
  const unavailableRef = useRef(false);
  const serviceRef = useRef(false);
  const retryingRef = useRef(false);
  const descriptionRef = useRef('');
  const addressRef = useRef('');
  const connectionRef = useRef<ConnectionState>('online');
  const networkRef = useRef(true);
  const reachableRef = useRef(true);
  const pendingLeave = useRef<() => void | Promise<void>>(undefined);
  const submittingRef = useRef(false);
  const locatingCount = useRef(0);
  const homeSeq = useRef(0);
  const readSeq = useRef(0);
  const reportRef = useRef<HazardReport>(undefined);
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
    descriptionRef.current = '';
    addressRef.current = '';
    setDescriptionText('');
    setAddressText('');
    setManualPlaceName(undefined);
    setChangingLocation(false);
  }, [setAccess, setResult, setLocationUnavailable]);

  /** Runs an online action; a failure shows the common error banner and changes nothing (C1). */
  const guarded = useCallback(async (action: () => Promise<void>, keepBanner = false) => {
    serviceRef.current = false;
    // Retry keeps the banner on screen while it runs (it shows "Trying again…"); it goes when the call works.
    if (!keepBanner) {
      setServiceUnavailable(false);
      setRetriedAt(undefined);
    }
    try {
      await action();
      setServiceUnavailable(false);
    } catch (error) {
      console.warn('[CitizenAppUI] an online action failed:', error);
      serviceRef.current = true;
      setServiceUnavailable(true);
    }
  }, []);

  /**
   * Runs an action with the loading count of a screen's key raised; overlapping actions of one key share a
   * count, and the hook's `loading` looks only at the key of the screen shown (standards §10 item 4, P231).
   */
  const withLoading = useCallback(async (key: string, action: () => Promise<void>) => {
    setLoadingCounts((previous) => ({ ...previous, [key]: (previous[key] ?? 0) + 1 }));
    try {
      await action();
    } finally {
      setLoadingCounts((previous) => ({ ...previous, [key]: previous[key] - 1 }));
    }
  }, []);

  /**
   * Loads the home. Only the latest load applies its view, and a load that was superseded and then fails is
   * ignored, so a late reply never replaces a newer one or raises the banner (standards §10 item 4, P231).
   */
  const refreshHome = useCallback(async () => {
    homeSeq.current += 1;
    const mine = homeSeq.current;
    let view: HomeView;
    try {
      view = await controller.loadHome();
    } catch (error) {
      if (mine !== homeSeq.current) {
        return;
      }
      throw error;
    }
    if (mine !== homeSeq.current) {
      return;
    }
    homeRef.current = view;
    setHome(view);
  }, [controller]);

  /** Reads the phone's position for the home; `locating` is true from the start until it ends (P231). */
  const refreshLocation = useCallback(async () => {
    locatingCount.current += 1;
    setLocating(true);
    try {
      const fix = await controller.captureLocation();
      setCurrentLocation(fix);
      setAccess(fix ? undefined : await access.check());
    } finally {
      locatingCount.current -= 1;
      setLocating(locatingCount.current > 0);
    }
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

  const nameManualPlace = useCallback((place: string | undefined) => setManualPlaceName(place), []);

  const changeManualLocation = useCallback(() => setChangingLocation(true), []);

  const keepManualLocation = useCallback(() => setChangingLocation(false), []);

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
    // The loading row covers the server part only; the fix is read alongside, outside the row (P231).
    await guarded(async () => {
      await Promise.all([withLoading('home', refreshHome), refreshLocation()]);
    });
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
        withLoading('home', async () => {
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
    (text: string) => {
      descriptionRef.current = text;
      setDescriptionText(text);
      updateDraft({ description: composeDescription(text, addressRef.current) });
    },
    [updateDraft],
  );

  const enterAddress = useCallback(
    (text: string) => {
      const address = text.slice(0, MAX_ADDRESS_LENGTH);
      addressRef.current = address;
      setAddressText(address);
      updateDraft({ description: composeDescription(descriptionRef.current, address) });
    },
    [updateDraft],
  );

  const attachPhoto = useCallback(
    (photoUri: string) => {
      if (!draftRef.current) {
        return;
      }
      const previous = draftRef.current.photoUri;
      const unsaved = draftRef.current.status === undefined;
      updateDraft({ photoUri });
      // The new copy has its own file; an unsaved draft's old copy is deleted, a stored one's stays.
      if (previous && previous !== photoUri && unsaved) {
        photos.deletePhoto(previous);
      }
      setPermissionDenied(undefined);
      setErrors((previous) => ({ ...previous, photo: undefined }));
    },
    [updateDraft, photos],
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
      setChangingLocation(false);
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
    const errors = [...validation.errors];
    // The address alone must not stand in for the description (standards §10 "The address", P214).
    if (
      descriptionRef.current.trim() === '' &&
      !errors.some((error) => error.startsWith('description:'))
    ) {
      errors.push('description: a description is required');
    }
    if (errors.length === 0) {
      setErrors(NO_ERRORS);
      navigate({ name: 'review' });
    } else {
      setErrors(toFieldErrors(errors));
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

  /** Sets what the status screen shows: a report (live or copy), or the "loads when online" state. */
  const showStatus = useCallback(
    (next: { report?: HazardReport; fromCopy?: boolean; missing?: boolean }) => {
      reportRef.current = next.report;
      setReport(next.report);
      setReportFromCopy(next.fromCopy ?? false);
      setReportMissing(next.missing ?? false);
    },
    [],
  );

  /**
   * Offline, the status comes from the copy: the report with that ID in the home on screen, else the
   * submission's report; with neither, the screen says it loads when online (standards §10 item 17, P231).
   */
  const showReportCopy = useCallback(
    (reportId: string) => {
      const submitted = resultRef.current?.report;
      const copy =
        homeRef.current?.myReports.find((item) => item.reportId === reportId) ??
        (submitted?.reportId === reportId ? submitted : undefined);
      showStatus(copy ? { report: copy, fromCopy: true } : { missing: true });
    },
    [showStatus],
  );

  /**
   * Reads a report for the status screen; the old report stays shown while it reloads. A pull-to-refresh passes
   * `showLoading: false`, so only the pull indicator shows, as on the home.
   *
   * Offline (the strip is not Online) no request is made and the copy is used. Online, an unreachable server or a
   * timeout (`NetworkError`) falls back to the copy; any other error is the C1 banner. The result applies only while
   * that report's status is open and this is the latest read, so a late reply never replaces a newer one and a
   * superseded failure raises nothing (standards §10 items 4 and 17, P231). `assumeOnline` is for the reconnect
   * read, which runs when the connectivity trigger may be ahead of the strip.
   */
  const readReport = useCallback(
    async (
      reportId: string,
      {
        showLoading = true,
        keepBanner = false,
        assumeOnline = false,
      }: { showLoading?: boolean; keepBanner?: boolean; assumeOnline?: boolean } = {},
    ) => {
      readSeq.current += 1;
      const mine = readSeq.current;
      const current = () => {
        const open = routeRef.current;
        return (
          mine === readSeq.current && open.name === 'reportStatus' && open.reportId === reportId
        );
      };
      const read = async () => {
        if (!assumeOnline && connectionRef.current !== 'online') {
          showReportCopy(reportId);
          return;
        }
        try {
          const fresh = await controller.getReportStatus(reportId);
          if (current()) {
            showStatus({ report: fresh });
          }
        } catch (error) {
          if (!current()) {
            return;
          }
          if (error instanceof NetworkError) {
            showReportCopy(reportId);
            return;
          }
          throw error;
        }
      };
      await guarded(
        () => (showLoading ? withLoading(`status:${reportId}`, read) : read()),
        keepBanner,
      );
    },
    [controller, guarded, withLoading, showReportCopy, showStatus],
  );

  const openReportStatusFrom = useCallback(
    async (reportId: string, from: StatusOrigin) => {
      navigate({ name: 'reportStatus', reportId, from });
      setDecision(undefined);
      showStatus({});
      await readReport(reportId);
    },
    [navigate, readReport, showStatus],
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
      const parts = splitDescription(stored.description);
      descriptionRef.current = parts.description;
      addressRef.current = parts.address;
      setDescriptionText(parts.description);
      setAddressText(parts.address);
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
    // A report status that is open is read live again, with its loading row; the copy stays until it arrives
    // (standards §10 item 17, P231). The trigger may run ahead of the strip, so only "Server not reachable" holds it back.
    const open = routeRef.current;
    if (open.name === 'reportStatus') {
      await readReport(open.reportId, { assumeOnline: connectionRef.current !== 'serverDown' });
    }
  }, [controller, guarded, refreshHome, readReport]);

  /**
   * Works out the strip's state from the two signals. A return from serverDown to online syncs; a loss
   * of the connection (online to noInternet or serverDown) while the home is shown reloads the home at
   * once: offline it reads the saved copy with no network call, and the C1 banner of the failed live
   * load goes with the view that carried it (standards section 10 item 11, P229; R8). With a report status open,
   * a loss reads it again when the C1 banner is up or no report is shown, which takes the copy path; a live report
   * already shown stays (standards section 10 item 17, P231). The holder `serverState` is set first, so the
   * synchronisation and the reload see the new value (P231).
   */
  const updateConnection = useCallback(() => {
    const next: ConnectionState = !networkRef.current
      ? 'noInternet'
      : !reachableRef.current
        ? 'serverDown'
        : 'online';
    const previous = connectionRef.current;
    connectionRef.current = next;
    serverState.setDown(next === 'serverDown');
    setConnection(next);
    // noInternet to online is raised by the connectivity triggers; only the server's return is raised here.
    if (previous === 'serverDown' && next === 'online') {
      void onConnectivityRestored();
    }
    if (previous === 'online' && next !== 'online') {
      const open = routeRef.current;
      if (open.name === 'home') {
        void guarded(refreshHome);
      } else if (open.name === 'reportStatus' && (serviceRef.current || !reportRef.current)) {
        void readReport(open.reportId, { showLoading: false });
      }
    }
  }, [serverState, onConnectivityRestored, guarded, refreshHome, readReport]);

  const onNetworkChange = useCallback(
    (online: boolean) => {
      const wasOnline = networkRef.current;
      networkRef.current = online;
      if (online && !wasOnline) {
        // The server counts as reachable again until the next poll, so the strip does not flash.
        reachableRef.current = true;
      }
      updateConnection();
    },
    [updateConnection],
  );

  const onServerReachability = useCallback(
    (reachable: boolean) => {
      reachableRef.current = reachable;
      updateConnection();
    },
    [updateConnection],
  );

  /**
   * A merged warning alert arrived: the banner and the local notification are queued, and while the home is shown
   * it reloads at once so Active warnings shows the new level without a pull (standards section 10 item 5, P237).
   * The reload goes through the C1 guard with no loading row, and other screens reload nothing, because opening the
   * home reloads it.
   */
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
      if (routeRef.current.name === 'home') {
        void guarded(refreshHome);
      }
    },
    [guarded, refreshHome],
  );

  /**
   * A report decision arrived: the decision card and the local notification are queued, an open status of that
   * report is read again, and while the home is shown it reloads at once so My reports shows the new status
   * (standards section 10 item 5, P237; no loading row).
   */
  const receiveReportDecision = useCallback(
    (notice: ReportDecisionNotice) => {
      const language = homeRef.current?.language ?? env.citizenLanguage;
      const text = decisionNoticeText(notice, language);
      setDecision({ notice });
      Promise.resolve()
        .then(() => presentLocalAlert(DECISION_TITLE, text))
        .catch((error: unknown) =>
          console.warn('[CitizenAppUI] the notice could not be shown:', error),
        );
      const current = routeRef.current;
      if (current.name === 'reportStatus' && current.reportId === notice.reportId) {
        void readReport(notice.reportId);
      } else if (current.name === 'home') {
        void guarded(refreshHome);
      }
    },
    [env.citizenLanguage, readReport, guarded, refreshHome],
  );

  /**
   * Retry (standards section 10 item 12, P225): the call and a delay of `MIN_RETRY_FEEDBACK_MS` run
   * together, so "Trying again…" stays at least that long; a call that works applies its result when
   * it arrives (the banner goes at once), and a fault that remains shows the "tried at" line when
   * both are over, with the time of that moment.
   */
  const retry = useCallback(async () => {
    if (retryingRef.current) {
      return;
    }
    retryingRef.current = true;
    setRetrying(true);
    const current = routeRef.current;
    try {
      await withLoading(loadingKey(current), async () => {
        const attempt =
          current.name === 'reportStatus'
            ? readReport(current.reportId, { showLoading: false, keepBanner: true })
            : guarded(refreshHome, true);
        const feedback = new Promise<void>((resolve) => setTimeout(resolve, MIN_RETRY_FEEDBACK_MS));
        await Promise.all([attempt, feedback]);
      });
      // The banner stays when the status call failed again, or the home loaded with part of it failing.
      const still =
        serviceRef.current || (routeRef.current.name === 'home' && homeRef.current?.error === true);
      setRetriedAt(still ? new Date() : undefined);
    } finally {
      retryingRef.current = false;
      setRetrying(false);
    }
  }, [withLoading, readReport, guarded, refreshHome]);

  // The "tried at" line belongs to the banner: it is shown only while the banner condition holds, and a new
  // online action clears it (so an old time never comes back with a later banner).
  const bannerCondition = serviceUnavailable || (route.name === 'home' && home.error === true);

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
    descriptionText,
    addressText,
    manualPlaceName,
    changingLocation,
    connection,
    errors,
    permissionDenied,
    result,
    sentReportId,
    report,
    reportFromCopy,
    reportMissing,
    locating,
    serviceUnavailable,
    retriedAt: bannerCondition ? retriedAt : undefined,
    retrying,
    syncResults,
    draftDiscarded,
    alerts,
    decision,
    submitting,
    loading: (loadingCounts[loadingKey(route)] ?? 0) > 0,
    refreshing,
    discardQuestion,
    openHome,
    selectLanguage,
    startReport,
    selectHazardType,
    enterDescription,
    enterAddress,
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
    nameManualPlace,
    changeManualLocation,
    keepManualLocation,
    onNetworkChange,
    onServerReachability,
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
