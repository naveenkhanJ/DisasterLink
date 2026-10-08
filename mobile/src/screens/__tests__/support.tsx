/**
 * support.tsx — what the screen tests share: a fake `ReportCaptureController` (UC2-5's, mocked), a
 * fake photo service, sample data from the SC2 storyline and a harness that runs the real
 * `useCitizenAppUI` hook and `CitizenApp` shell over them.
 */
import { act, render } from '@testing-library/react-native';
import React, { useEffect } from 'react';
import type { GeoPoint, HazardReport } from '../../api/reportApi';
import type { ActiveWarning } from '../../api/types';
import { useCitizenAppUI, type CitizenAppUI, type UiServices } from '../../app/useCitizenAppUI';
import type { ReportCaptureController } from '../../report/ReportCaptureController';
import type { HomeView, ReportDraft } from '../../report/ReportDraft';
import { CitizenApp } from '../CitizenApp';

/** The GPS fix of SC2 step 13. */
export const KALUTARA_BRIDGE: GeoPoint = {
  latitude: 6.5888,
  longitude: 79.964,
  accuracyMetres: 8,
};

/** WRN-2026-0087 as `GET /api/warnings/active` returns it (SC2 step 2). */
export const WARNING: ActiveWarning = {
  warningId: 'WRN-2026-0087',
  warningLevel: 'LEVEL_2_AMBER',
  message: {
    sinhala: 'සිංහල පණිවිඩය',
    tamil: 'தமிழ் செய்தி',
    english: 'DMC FLOOD WARNING - Level 2 Amber: Kalu Ganga rising.',
  },
  instructions: {
    sinhala: 'සිංහල උපදෙස්',
    tamil: 'தமிழ் அறிவுறுத்தல்',
    english: 'Move documents and valuables to higher ground.',
  },
  issuedTime: '2026-05-20T10:45:00+05:30',
  expiryTime: '2026-05-21T10:45:00+05:30',
  targetDistricts: ['RATNAPURA', 'KALUTARA'],
  hazardType: 'FLOOD',
  locationName: 'Kalu Ganga at Ratnapura',
};

/** A report as the server sends it. */
export const REPORT: HazardReport = {
  reportId: 'RPT-2026-KLT-0847',
  hazardType: 'FLOOD',
  description: 'Kalu Ganga water has breached the sandbag barriers.',
  location: KALUTARA_BRIDGE,
  locationFromGps: true,
  district: 'KALUTARA',
  capturedAt: '2026-05-21T07:40:00+05:30',
  reportTime: '2026-05-21T07:42:00+05:30',
  status: 'PENDING_VERIFICATION',
};

/** A complete draft that passes `validateDraft`. */
export const FILLED_DRAFT: ReportDraft = {
  draftId: 'draft-1',
  hazardType: 'FLOOD',
  description: 'Kalu Ganga water has breached the sandbag barriers.',
  photoUri: 'file:///documents/draft-1.jpg',
  location: KALUTARA_BRIDGE,
  locationFromGps: true,
  capturedAt: '2026-05-21T02:10:00.000Z',
};

/** The home of SC2 step 2, online. */
export const ONLINE_HOME: HomeView = {
  warnings: [WARNING],
  myReports: [],
  pendingDrafts: [],
  language: 'ENGLISH',
  fromCache: false,
};

/** The fake controller and the call records of the tests. */
export interface Rig {
  controller: jest.Mocked<ReportCaptureController>;
  photos: { takePhoto: jest.Mock; choosePhoto: jest.Mock };
  services: UiServices;
}

let latest: CitizenAppUI | undefined;

/**
 * Gives the latest state and inputs of the hook, as the screens see them.
 *
 * @returns the state and the inputs
 */
export function ui(): CitizenAppUI {
  return latest as CitizenAppUI;
}

/**
 * Makes the fake controller: an online home, a GPS fix, a valid draft check and a submit that
 * returns SUBMITTED; a test overrides what it needs.
 *
 * @returns the rig
 */
export function makeRig(): Rig {
  const controller = {
    loadHome: jest.fn(async () => ONLINE_HOME),
    startReport: jest.fn(() => ({
      draftId: 'draft-1',
      locationFromGps: false,
      capturedAt: '2026-05-21T02:10:00.000Z',
    })),
    captureLocation: jest.fn(async () => KALUTARA_BRIDGE),
    validateDraft: jest.fn(() => ({ valid: true, errors: [] })),
    submit: jest.fn(async (draft: ReportDraft) => ({
      draftId: draft.draftId,
      state: 'SUBMITTED' as const,
      report: REPORT,
      errors: [],
    })),
    syncPending: jest.fn(async () => []),
    getDraft: jest.fn(async () => undefined),
    discardDraft: jest.fn(async () => undefined),
    changeLanguage: jest.fn(async () => undefined),
    getReportStatus: jest.fn(async () => REPORT),
  } as unknown as jest.Mocked<ReportCaptureController>;
  const photos = {
    takePhoto: jest.fn(async () => ({
      kind: 'photo' as const,
      uri: 'file:///documents/draft-1.jpg',
    })),
    choosePhoto: jest.fn(async () => ({
      kind: 'photo' as const,
      uri: 'file:///documents/draft-1.jpg',
    })),
  };
  return {
    controller,
    photos,
    services: { controller, photos, env: { citizenLanguage: 'ENGLISH' } },
  };
}

function Harness({ rig }: { rig: Rig }) {
  const state = useCitizenAppUI(rig.services);
  useEffect(() => {
    latest = state;
  });
  const { openHome } = state;
  useEffect(() => {
    void openHome();
  }, [openHome]);
  return <CitizenApp ui={state} />;
}

/**
 * Renders the app over the rig and lets the first home load finish.
 *
 * @param rig the fakes
 */
export async function renderApp(rig: Rig): Promise<void> {
  await render(<Harness rig={rig} />);
  await act(async () => undefined);
}

/**
 * Runs an input of the screen boundary the way a found message or a handler does, inside `act`.
 *
 * @param action the call
 */
export async function run(action: () => unknown): Promise<void> {
  await act(async () => {
    await action();
  });
}
