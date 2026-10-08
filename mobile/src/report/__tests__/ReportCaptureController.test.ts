import { ApiError, NetworkError } from '../../api/httpClient';
import type { GeoPoint, HazardReport, ReportApi, ReportReceipt } from '../../api/reportApi';
import type { SkeletonApi } from '../../api/skeletonApi';
import type { ActiveWarning, Language } from '../../api/types';
import type { LocationService } from '../../location/LocationService';
import type { HomeCache } from '../../store/HomeCache';
import type { OfflineReportStore } from '../../store/OfflineReportStore';
import {
  createReportCaptureController,
  type CaptureDependencies,
  type ReportCaptureController,
} from '../ReportCaptureController';
import { withStatus, type HomeView, type ReportDraft } from '../ReportDraft';

const NOW = new Date('2026-05-21T02:10:00.000Z');
const KADUWELA: GeoPoint = { latitude: 6.9356, longitude: 79.9843, accuracyMetres: 10 };

const REPORT: HazardReport = {
  reportId: 'RPT-2026-COL-0905',
  hazardType: 'FLOOD',
  description: 'Water is rising',
  location: KADUWELA,
  locationFromGps: true,
  district: 'COLOMBO',
  capturedAt: '2026-05-21T07:40:00+05:30',
  reportTime: '2026-05-21T07:42:00+05:30',
  status: 'PENDING_VERIFICATION',
};

function receipt(
  outcome: 'ACCEPTED' | 'DUPLICATE' | 'REJECTED',
  message: string | null = null,
  withReport = outcome !== 'REJECTED',
): ReportReceipt {
  return {
    result: {
      submissionId: 'sub',
      outcome,
      reason: outcome === 'REJECTED' ? 'VALIDATION_FAILED' : null,
      message,
    },
    report: withReport ? REPORT : null,
  };
}

function valid(id: string, extra: Partial<ReportDraft> = {}): ReportDraft {
  return {
    draftId: id,
    hazardType: 'FLOOD',
    description: 'Water is rising',
    photoUri: `file:///documents/${id}.jpg`,
    location: KADUWELA,
    locationFromGps: true,
    capturedAt: '2026-05-21T02:00:00.000Z',
    ...extra,
  };
}

interface Rig {
  controller: ReportCaptureController;
  deps: CaptureDependencies;
  stored: ReportDraft[];
  api: jest.Mocked<ReportApi>;
  skeleton: jest.Mocked<SkeletonApi>;
  homeCache: jest.Mocked<HomeCache>;
  location: jest.Mocked<LocationService>;
  deletePhoto: jest.Mock;
  isOnline: jest.Mock;
}

function rig(initial: ReportDraft[] = []): Rig {
  const stored = [...initial];
  const store: OfflineReportStore = {
    getClientId: async () => 'client-1',
    save: jest.fn(async (draft: ReportDraft) => {
      const index = stored.findIndex((d) => d.draftId === draft.draftId);
      if (index < 0) {
        stored.push(draft);
      } else {
        stored[index] = draft;
      }
    }),
    findPending: jest.fn(async () => stored.map((d) => ({ ...d }))),
    remove: jest.fn(async (draftId: string) => {
      const index = stored.findIndex((d) => d.draftId === draftId);
      if (index >= 0) {
        stored.splice(index, 1);
      }
    }),
    markRejected: jest.fn(async (draftId: string, reason: string) => {
      const index = stored.findIndex((d) => d.draftId === draftId);
      stored[index] = { ...stored[index], status: 'REJECTED', rejectionReason: reason };
    }),
  };
  const api = {
    receiveReport: jest.fn(),
    getReportStatus: jest.fn(),
    getReportsByReporter: jest.fn(async () => [REPORT]),
    changePreferredLanguage: jest.fn(async () => undefined),
  } as unknown as jest.Mocked<ReportApi>;
  const skeleton = {
    getActiveWarnings: jest.fn(async () => [] as ActiveWarning[]),
    pollOutbox: jest.fn(),
    acknowledge: jest.fn(),
  } as unknown as jest.Mocked<SkeletonApi>;
  let language: Language = 'ENGLISH';
  let cachedHome: HomeView | undefined;
  const homeCache = {
    saveHome: jest.fn(async (view: HomeView) => {
      cachedHome = view;
    }),
    loadHome: jest.fn(async () => cachedHome),
    getLanguage: jest.fn(async () => language),
    changeLanguage: jest.fn(async (next: Language) => {
      language = next;
    }),
  } as jest.Mocked<HomeCache>;
  const location = {
    getCurrentLocation: jest.fn(async () => KADUWELA as GeoPoint | undefined),
    resolveDistrict: jest.fn(() => 'COLOMBO' as const as 'COLOMBO' | undefined),
  } as unknown as jest.Mocked<LocationService>;
  const deletePhoto = jest.fn();
  const isOnline = jest.fn(async () => true);
  const deps: CaptureDependencies = {
    reportApi: api,
    skeletonApi: skeleton,
    store,
    homeCache,
    location,
    photos: { deletePhoto },
    isOnline,
    citizenId: 'CIT-KLT-0001',
    registeredDistrict: 'KALUTARA',
    newId: () => 'new-uuid',
    now: () => NOW,
  };
  return {
    controller: createReportCaptureController(deps),
    deps,
    stored,
    api,
    skeleton,
    homeCache,
    location,
    deletePhoto,
    isOnline,
  };
}

describe('startReport, captureLocation and withStatus', () => {
  it('SC2 step 6: a new draft has a new identifier and the time it was started', () => {
    const { controller } = rig();

    expect(controller.startReport()).toEqual({
      draftId: 'new-uuid',
      locationFromGps: false,
      capturedAt: '2026-05-21T02:10:00.000Z',
    });
  });

  it('SC2 steps 12 to 14: the location is the fix of the location service', async () => {
    const { controller, location } = rig();

    expect(await controller.captureLocation()).toBe(KADUWELA);
    location.getCurrentLocation.mockResolvedValue(undefined);
    expect(await controller.captureLocation()).toBeUndefined();
  });

  it('FD-17: withStatus copies the draft with the queue status', () => {
    const draft = valid('d-1');

    expect(withStatus(draft, 'QUEUED')).toEqual({ ...draft, status: 'QUEUED' });
    expect(draft.status).toBeUndefined();
  });
});

describe('validateDraft', () => {
  it('SC2 step 16: a complete draft is valid', () => {
    expect(rig().controller.validateDraft(valid('d-1'))).toEqual({ valid: true, errors: [] });
  });

  it.each([
    ['hazardType: a hazard type is required', { hazardType: undefined }],
    ['description: a description is required', { description: undefined }],
    ['description: a description is required', { description: '   ' }],
    ['description: at most 500 characters', { description: 'x'.repeat(501) }],
    ['photo: one photo is required', { photoUri: undefined }],
    ['location: a location is required', { location: undefined }],
    ['location: outside Sri Lanka', { location: { latitude: 51.5, longitude: -0.12 } }],
    ['location: outside Sri Lanka', { location: { latitude: 5.84, longitude: 80 } }],
    ['location: outside Sri Lanka', { location: { latitude: 9.91, longitude: 80 } }],
    ['location: outside Sri Lanka', { location: { latitude: 7, longitude: 79.49 } }],
    ['location: outside Sri Lanka', { location: { latitude: 7, longitude: 81.96 } }],
  ])('SC2 E1/E2/E3: %s', (error, change) => {
    const result = rig().controller.validateDraft({ ...valid('d-1'), ...change });

    expect(result).toEqual({ valid: false, errors: [error] });
  });

  it('SC2 step 16: a description of exactly 500 characters and a border point are valid', () => {
    const draft = valid('d-1', {
      description: 'x'.repeat(500),
      location: { latitude: 5.85, longitude: 81.95 },
    });

    expect(rig().controller.validateDraft(draft).valid).toBe(true);
  });

  it('SC2 E1: all the errors are collected, field-prefixed, in form order', () => {
    const empty = { draftId: 'd-1', locationFromGps: false, capturedAt: 'x' };

    expect(rig().controller.validateDraft(empty).errors).toEqual([
      'hazardType: a hazard type is required',
      'description: a description is required',
      'photo: one photo is required',
      'location: a location is required',
    ]);
  });
});

describe('submit', () => {
  it('SC2 steps 18 to 20: online ACCEPTED is SUBMITTED, the photo copy is deleted, the district is the GPS one', async () => {
    const { controller, api, stored, deletePhoto, location } = rig([
      valid('other', { status: 'QUEUED' }),
    ]);
    api.receiveReport.mockResolvedValue(receipt('ACCEPTED', 'Report received'));

    const result = await controller.submit(valid('d-1'));

    expect(result).toEqual({ draftId: 'd-1', state: 'SUBMITTED', report: REPORT, errors: [] });
    expect(location.resolveDistrict).toHaveBeenCalledWith(KADUWELA);
    const [draftSent, meta, photoUri] = api.receiveReport.mock.calls[0];
    expect(draftSent.district).toBe('COLOMBO');
    expect(meta).toEqual({
      clientId: 'client-1',
      userId: 'CIT-KLT-0001',
      submissionId: 'd-1',
      capturedAt: '2026-05-21T02:00:00.000Z',
      pendingCount: 1,
    });
    expect(photoUri).toBe('file:///documents/d-1.jpg');
    expect(deletePhoto).toHaveBeenCalledWith('file:///documents/d-1.jpg');
    expect(stored.map((d) => d.draftId)).toEqual(['other']);
  });

  it('SC2 step 18: a point in no district falls back to the registered district', async () => {
    const { controller, api, location } = rig();
    location.resolveDistrict.mockReturnValue(undefined);
    api.receiveReport.mockResolvedValue(receipt('ACCEPTED'));

    await controller.submit(valid('d-1'));

    expect(api.receiveReport.mock.calls[0][0].district).toBe('KALUTARA');
  });

  it('SC2 E8 18g: a DUPLICATE reply is SUBMITTED with the earlier report', async () => {
    const { controller, api } = rig();
    api.receiveReport.mockResolvedValue(receipt('DUPLICATE', 'Already received as RPT-1'));

    const result = await controller.submit(valid('d-1'));

    expect(result).toMatchObject({ state: 'SUBMITTED', report: REPORT });
  });

  it('SC2 E6 19b: a REJECTED reply (200) is INVALID with the message split into errors', async () => {
    const { controller, api, stored, deletePhoto } = rig();
    api.receiveReport.mockResolvedValue(
      receipt('REJECTED', 'photo: not a readable JPEG or PNG image; location: outside Sri Lanka'),
    );

    const result = await controller.submit(valid('d-1'));

    expect(result).toEqual({
      draftId: 'd-1',
      state: 'INVALID',
      errors: ['photo: not a readable JPEG or PNG image', 'location: outside Sri Lanka'],
    });
    expect(stored).toEqual([]);
    expect(deletePhoto).not.toHaveBeenCalled();
  });

  it('SC2 E6: a REJECTED reply without a message is INVALID with a plain error', async () => {
    const { controller, api } = rig();
    api.receiveReport.mockResolvedValue(receipt('REJECTED', null));

    expect((await controller.submit(valid('d-1'))).errors).toEqual(['The report was not accepted']);
  });

  it('SC2 E6 (§12.3): a 413 is INVALID with "The photo is too large to send"', async () => {
    const { controller, api } = rig();
    api.receiveReport.mockRejectedValue(new ApiError(413, 'Payload Too Large'));

    expect(await controller.submit(valid('d-1'))).toEqual({
      draftId: 'd-1',
      state: 'INVALID',
      errors: ['The photo is too large to send'],
    });
  });

  it("SC2 E6: another 4xx is INVALID with the server's plain message and nothing is queued", async () => {
    const { controller, api, stored } = rig();
    api.receiveReport.mockRejectedValue(new ApiError(404, 'The requested item does not exist.'));

    const result = await controller.submit(valid('d-1'));

    expect(result.errors).toEqual(['The requested item does not exist.']);
    expect(result.state).toBe('INVALID');
    expect(stored).toEqual([]);
  });

  it('SC2 E6 19d / E4: a 5xx saves a QUEUED copy with attempts 1', async () => {
    const { controller, api, stored, deletePhoto } = rig();
    api.receiveReport.mockRejectedValue(new ApiError(503, 'The service is unavailable.'));

    const result = await controller.submit(valid('d-1'));

    expect(result).toEqual({ draftId: 'd-1', state: 'QUEUED', errors: [] });
    expect(stored).toHaveLength(1);
    expect(stored[0]).toMatchObject({
      draftId: 'd-1',
      status: 'QUEUED',
      attempts: 1,
      district: 'COLOMBO',
    });
    expect(deletePhoto).not.toHaveBeenCalled();
  });

  it('SC2 E6 19d: a network error saves a QUEUED copy, and a second failure counts attempts 2', async () => {
    const { controller, api, stored } = rig();
    api.receiveReport.mockRejectedValue(new NetworkError('The server could not be reached.'));

    await controller.submit(valid('d-1'));
    await controller.submit({ ...stored[0] });

    expect(stored).toHaveLength(1);
    expect(stored[0]).toMatchObject({ status: 'QUEUED', attempts: 2 });
  });

  it('SC2 A2 18a to 18c: offline at the start saves the draft QUEUED without a call and without an attempt', async () => {
    const { controller, api, stored, isOnline } = rig();
    isOnline.mockResolvedValue(false);

    const result = await controller.submit(valid('d-1'));

    expect(result.state).toBe('QUEUED');
    expect(api.receiveReport).not.toHaveBeenCalled();
    expect(stored[0]).toMatchObject({ status: 'QUEUED', attempts: 0, district: 'COLOMBO' });
  });

  it('SC2 E1: an invalid draft is INVALID with its errors and is neither sent nor saved', async () => {
    const { controller, api, stored } = rig();

    const result = await controller.submit(valid('d-1', { photoUri: undefined }));

    expect(result).toEqual({
      draftId: 'd-1',
      state: 'INVALID',
      errors: ['photo: one photo is required'],
    });
    expect(api.receiveReport).not.toHaveBeenCalled();
    expect(stored).toEqual([]);
  });

  it('SC2 steps 16 to 18: an unexpected failure (a programming fault) is thrown, not queued', async () => {
    const { controller, api, stored } = rig();
    api.receiveReport.mockRejectedValue(new Error('Unsupported FormDataPart implementation'));

    await expect(controller.submit(valid('d-1'))).rejects.toThrow('Unsupported FormDataPart');
    expect(stored).toEqual([]);
  });

  it('SC2 E8 18n: a corrected REJECTED draft keeps its draftId and is removed once accepted', async () => {
    const { controller, api, stored } = rig([
      valid('d-1', { status: 'REJECTED', rejectionReason: 'photo: …' }),
    ]);
    api.receiveReport.mockResolvedValue(receipt('ACCEPTED'));

    const corrected = (await controller.getDraft('d-1')) as ReportDraft;
    await controller.submit({ ...corrected, status: undefined, rejectionReason: undefined });

    expect(api.receiveReport.mock.calls[0][1].submissionId).toBe('d-1');
    expect(stored).toEqual([]);
  });
});

describe('syncPending', () => {
  it('SC2 18d to 18g: a mixed batch — ACCEPTED removes, REJECTED marks, a 5xx stops and counts', async () => {
    const queued = ['a', 'b', 'c', 'd'].map((id) => valid(id, { status: 'QUEUED', attempts: 0 }));
    const { controller, api, stored, deletePhoto } = rig(queued);
    api.receiveReport
      .mockResolvedValueOnce(receipt('ACCEPTED'))
      .mockResolvedValueOnce(receipt('REJECTED', 'photo: not a readable JPEG or PNG image'))
      .mockRejectedValueOnce(new ApiError(500, 'The service is unavailable.'));

    const results = await controller.syncPending();

    expect(results).toEqual([
      { submissionId: 'a', outcome: 'ACCEPTED', reason: null, message: 'RPT-2026-COL-0905' },
      {
        submissionId: 'b',
        outcome: 'REJECTED',
        reason: 'VALIDATION_FAILED',
        message: 'photo: not a readable JPEG or PNG image',
      },
    ]);
    expect(api.receiveReport).toHaveBeenCalledTimes(3);
    expect(deletePhoto).toHaveBeenCalledWith('file:///documents/a.jpg');
    expect(stored.map((d) => [d.draftId, d.status, d.attempts])).toEqual([
      ['b', 'REJECTED', 0],
      ['c', 'QUEUED', 1],
      ['d', 'QUEUED', 0],
    ]);
    expect(stored[0].rejectionReason).toBe('photo: not a readable JPEG or PNG image');
  });

  it('SC2 18d: the meta is rebuilt from the draft and pendingCount is recomputed for each send', async () => {
    const { controller, api } = rig([
      valid('a', { status: 'QUEUED', capturedAt: '2026-05-20T09:00:00.000Z' }),
      valid('b', { status: 'QUEUED' }),
      valid('c', { status: 'QUEUED' }),
      valid('r', { status: 'REJECTED' }),
    ]);
    api.receiveReport.mockResolvedValue(receipt('ACCEPTED'));

    await controller.syncPending();

    expect(api.receiveReport.mock.calls.map((call) => call[1])).toEqual([
      {
        clientId: 'client-1',
        userId: 'CIT-KLT-0001',
        submissionId: 'a',
        capturedAt: '2026-05-20T09:00:00.000Z',
        pendingCount: 2,
      },
      expect.objectContaining({ submissionId: 'b', pendingCount: 1 }),
      expect.objectContaining({ submissionId: 'c', pendingCount: 0 }),
    ]);
  });

  it('SC2 E8 18j to 18k: a REJECTED draft is never sent again', async () => {
    const { controller, api } = rig([valid('r', { status: 'REJECTED', rejectionReason: 'x' })]);

    expect(await controller.syncPending()).toEqual([]);
    expect(api.receiveReport).not.toHaveBeenCalled();
  });

  it("SC2 E8: a 4xx on replay marks the draft REJECTED with the server's message", async () => {
    const { controller, api, stored } = rig([
      valid('a', { status: 'QUEUED' }),
      valid('b', { status: 'QUEUED' }),
    ]);
    api.receiveReport
      .mockRejectedValueOnce(new ApiError(413, 'Payload Too Large'))
      .mockResolvedValueOnce(receipt('DUPLICATE', 'Already received as RPT-1'));

    const results = await controller.syncPending();

    expect(results.map((r) => [r.submissionId, r.outcome])).toEqual([
      ['a', 'REJECTED'],
      ['b', 'DUPLICATE'],
    ]);
    expect(stored).toHaveLength(1);
    expect(stored[0]).toMatchObject({
      draftId: 'a',
      status: 'REJECTED',
      rejectionReason: 'The photo is too large to send',
    });
  });

  it("SC2 E8: an ACCEPTED reply without a report keeps the server's message", async () => {
    const { controller, api } = rig([valid('a', { status: 'QUEUED' })]);
    api.receiveReport.mockResolvedValue(receipt('ACCEPTED', 'received', false));

    expect((await controller.syncPending())[0].message).toBe('received');
  });

  it('SC2 E8: a REJECTED reply without a message is marked with a plain reason', async () => {
    const { controller, api, stored } = rig([valid('a', { status: 'QUEUED' })]);
    api.receiveReport.mockResolvedValue(receipt('REJECTED', null));

    await controller.syncPending();

    expect(stored[0].rejectionReason).toBe('The report was not accepted');
  });

  it('SC2 E4 18h to 18i: a network error keeps the draft QUEUED, counts the attempt and stops the run', async () => {
    const { controller, api, stored } = rig([
      valid('a', { status: 'QUEUED' }),
      valid('b', { status: 'QUEUED' }),
    ]);
    api.receiveReport.mockRejectedValue(new NetworkError('The server could not be reached.'));

    expect(await controller.syncPending()).toEqual([]);
    expect(api.receiveReport).toHaveBeenCalledTimes(1);
    expect(stored.map((d) => d.attempts)).toEqual([1, undefined]);
  });

  it('R4E-21: single-flight — a second call during a run joins it and only one run happens', async () => {
    const { controller, api } = rig([valid('a', { status: 'QUEUED' })]);
    let release: (value: ReportReceipt) => void = () => undefined;
    api.receiveReport.mockReturnValue(new Promise((resolve) => (release = resolve)));

    const first = controller.syncPending();
    const second = controller.syncPending();
    await new Promise<void>((resolve) => setImmediate(() => resolve()));
    release(receipt('ACCEPTED'));
    const [one, two] = await Promise.all([first, second]);

    expect(one).toBe(two);
    expect(api.receiveReport).toHaveBeenCalledTimes(1);
  });

  it('R4E-21: after a run has finished the next call starts a new run', async () => {
    const { controller, api, stored } = rig([valid('a', { status: 'QUEUED' })]);
    api.receiveReport.mockResolvedValue(receipt('ACCEPTED'));

    await controller.syncPending();
    stored.push(valid('b', { status: 'QUEUED' }));
    await controller.syncPending();

    expect(api.receiveReport).toHaveBeenCalledTimes(2);
  });

  it('SC2 18c: offline, nothing is sent', async () => {
    const { controller, api, isOnline } = rig([valid('a', { status: 'QUEUED' })]);
    isOnline.mockResolvedValue(false);

    expect(await controller.syncPending()).toEqual([]);
    expect(api.receiveReport).not.toHaveBeenCalled();
  });

  it('SC2 E8: a queued draft without a photo is marked REJECTED and reported, not sent', async () => {
    const { controller, api, stored } = rig([
      valid('a', { status: 'QUEUED', photoUri: undefined }),
    ]);

    const results = await controller.syncPending();

    expect(results).toEqual([
      { submissionId: 'a', outcome: 'REJECTED', message: 'photo: one photo is required' },
    ]);
    expect(api.receiveReport).not.toHaveBeenCalled();
    expect(stored[0]).toMatchObject({
      status: 'REJECTED',
      rejectionReason: 'photo: one photo is required',
    });
  });

  it('SC2 18e: an unexpected failure on replay is thrown', async () => {
    const { controller, api } = rig([valid('a', { status: 'QUEUED' })]);
    api.receiveReport.mockRejectedValue(new Error('programming fault'));

    await expect(controller.syncPending()).rejects.toThrow('programming fault');
  });
});

describe('loadHome', () => {
  const WARNING = { warningId: 'WRN-1' } as ActiveWarning;

  it('SC2 steps 1 and 2: online it reads the warnings and my reports, adds the drafts and caches the home', async () => {
    const { controller, skeleton, homeCache } = rig([valid('a', { status: 'QUEUED' })]);
    skeleton.getActiveWarnings.mockResolvedValue([WARNING]);

    const home = await controller.loadHome();

    expect(skeleton.getActiveWarnings).toHaveBeenCalledWith('KALUTARA');
    expect(home).toMatchObject({
      warnings: [WARNING],
      myReports: [REPORT],
      language: 'ENGLISH',
      fromCache: false,
    });
    expect(home.pendingDrafts.map((d) => d.draftId)).toEqual(['a']);
    expect(home.error).toBeUndefined();
    expect(homeCache.saveHome).toHaveBeenCalledWith(home);
  });

  it('SC2 A8: offline it shows the cached home with the current drafts and fromCache', async () => {
    const { controller, skeleton, isOnline, stored } = rig();
    skeleton.getActiveWarnings.mockResolvedValue([WARNING]);
    await controller.loadHome();
    isOnline.mockResolvedValue(false);
    stored.push(valid('a', { status: 'QUEUED' }));

    const home = await controller.loadHome();

    expect(home).toMatchObject({ warnings: [WARNING], myReports: [REPORT], fromCache: true });
    expect(home.pendingDrafts.map((d) => d.draftId)).toEqual(['a']);
  });

  it('SC2 A8: offline with nothing cached the home is empty and fromCache', async () => {
    const { controller, isOnline } = rig();
    isOnline.mockResolvedValue(false);

    expect(await controller.loadHome()).toEqual({
      warnings: [],
      myReports: [],
      pendingDrafts: [],
      language: 'ENGLISH',
      fromCache: true,
    });
  });

  it('SC2 A8: a network error while loading falls back to the cached home', async () => {
    const { controller, skeleton } = rig();
    await controller.loadHome();
    skeleton.getActiveWarnings.mockRejectedValue(
      new NetworkError('The server could not be reached.'),
    );

    expect((await controller.loadHome()).fromCache).toBe(true);
  });

  it('C1: a 5xx on the warnings leaves them empty, flags the error and keeps my reports', async () => {
    const { controller, skeleton } = rig();
    skeleton.getActiveWarnings.mockRejectedValue(new ApiError(500, 'The service is unavailable.'));

    const home = await controller.loadHome();

    expect(home).toMatchObject({
      warnings: [],
      myReports: [REPORT],
      fromCache: false,
      error: true,
    });
  });

  describe('the home is cached per part (standards §10 item 16, P225; UC2-7h R6)', () => {
    const OLD_WARNING = { warningId: 'WRN-OLD' } as ActiveWarning;
    const OLD_REPORT = { ...REPORT, reportId: 'RPT-OLD' };

    /** A rig whose cache already holds a home from an earlier, fully working load. */
    async function withSavedHome() {
      const parts = rig();
      parts.skeleton.getActiveWarnings.mockResolvedValue([OLD_WARNING]);
      parts.api.getReportsByReporter.mockResolvedValue([OLD_REPORT]);
      await parts.controller.loadHome();
      parts.homeCache.saveHome.mockClear();
      return parts;
    }

    it('SC2 A8 (R6): warnings fail and reports work: the new reports are saved, the old warnings kept, and the view still flags the error', async () => {
      const { controller, api, skeleton, homeCache } = await withSavedHome();
      api.getReportsByReporter.mockResolvedValue([REPORT]);
      skeleton.getActiveWarnings.mockRejectedValue(
        new ApiError(500, 'The service is unavailable.'),
      );

      const home = await controller.loadHome();

      expect(home).toMatchObject({ warnings: [], myReports: [REPORT], error: true });
      expect(homeCache.saveHome).toHaveBeenCalledWith(
        expect.objectContaining({ warnings: [OLD_WARNING], myReports: [REPORT], fromCache: false }),
      );
      expect(homeCache.saveHome.mock.calls[0][0].error).toBeUndefined();
    });

    it('SC2 A8 (R6): reports fail and warnings work: the new warnings are saved, the old reports kept', async () => {
      const { controller, api, skeleton, homeCache } = await withSavedHome();
      skeleton.getActiveWarnings.mockResolvedValue([WARNING]);
      api.getReportsByReporter.mockRejectedValue(new ApiError(502, 'Bad Gateway'));

      const home = await controller.loadHome();

      expect(home).toMatchObject({ warnings: [WARNING], myReports: [], error: true });
      expect(homeCache.saveHome).toHaveBeenCalledWith(
        expect.objectContaining({ warnings: [WARNING], myReports: [OLD_REPORT] }),
      );
    });

    it('SC2 A8 (R6): with nothing saved before, the failed part is saved as empty', async () => {
      const { controller, skeleton, homeCache } = rig();
      skeleton.getActiveWarnings.mockRejectedValue(
        new ApiError(500, 'The service is unavailable.'),
      );

      await controller.loadHome();

      expect(homeCache.saveHome).toHaveBeenCalledWith(
        expect.objectContaining({ warnings: [], myReports: [REPORT] }),
      );
    });

    it('SC2 A8 (R6): both parts fail: nothing is saved and the old copy stays', async () => {
      const { controller, api, skeleton, homeCache, isOnline } = await withSavedHome();
      skeleton.getActiveWarnings.mockRejectedValue(
        new ApiError(500, 'The service is unavailable.'),
      );
      api.getReportsByReporter.mockRejectedValue(new ApiError(502, 'Bad Gateway'));

      const home = await controller.loadHome();
      isOnline.mockResolvedValue(false);
      const offline = await controller.loadHome();

      expect(home.error).toBe(true);
      expect(homeCache.saveHome).not.toHaveBeenCalled();
      expect(offline).toMatchObject({ warnings: [OLD_WARNING], myReports: [OLD_REPORT] });
    });

    it('SC2 A8 (R6): both parts work: both are saved as loaded', async () => {
      const { controller, api, skeleton, homeCache } = await withSavedHome();
      api.getReportsByReporter.mockResolvedValue([REPORT]);
      skeleton.getActiveWarnings.mockResolvedValue([WARNING]);

      const home = await controller.loadHome();

      expect(home.error).toBeUndefined();
      expect(homeCache.saveHome).toHaveBeenCalledWith(
        expect.objectContaining({ warnings: [WARNING], myReports: [REPORT] }),
      );
    });

    it('SC2 A8 (R6): going offline after a partial load shows the reports that were saved', async () => {
      const { controller, skeleton, isOnline } = rig();
      skeleton.getActiveWarnings.mockRejectedValue(
        new ApiError(500, 'The service is unavailable.'),
      );
      await controller.loadHome();
      isOnline.mockResolvedValue(false);

      const offline = await controller.loadHome();

      expect(offline).toMatchObject({ warnings: [], myReports: [REPORT], fromCache: true });
    });
  });

  it('C1: a 5xx on my reports leaves them empty and flags the error', async () => {
    const { controller, api } = rig();
    api.getReportsByReporter.mockRejectedValue(new ApiError(502, 'Bad Gateway'));

    expect(await controller.loadHome()).toMatchObject({ myReports: [], error: true });
  });

  it('SC2 step 2: a 4xx on the warnings is thrown (a programming fault, not an outage)', async () => {
    const { controller, skeleton } = rig();
    skeleton.getActiveWarnings.mockRejectedValue(new ApiError(400, 'The request is not valid.'));

    await expect(controller.loadHome()).rejects.toThrow('HTTP 400');
  });
});

describe('drafts, language and status', () => {
  it('SC2 E8 18l: getDraft gives a stored draft, or nothing for an unknown one', async () => {
    const { controller } = rig([valid('a', { status: 'REJECTED', rejectionReason: 'x' })]);

    expect(await controller.getDraft('a')).toMatchObject({ draftId: 'a', status: 'REJECTED' });
    expect(await controller.getDraft('nope')).toBeUndefined();
  });

  it('SC2 E8 18o: discardDraft removes the draft and deletes its photo copy', async () => {
    const { controller, stored, deletePhoto } = rig([valid('a', { status: 'REJECTED' })]);

    await controller.discardDraft('a');

    expect(stored).toEqual([]);
    expect(deletePhoto).toHaveBeenCalledWith('file:///documents/a.jpg');
  });

  it('SC2 E8 18o: discarding an unknown draft or one without a photo deletes nothing', async () => {
    const { controller, deletePhoto } = rig([valid('a', { photoUri: undefined })]);

    await controller.discardDraft('a');
    await controller.discardDraft('nope');

    expect(deletePhoto).not.toHaveBeenCalled();
  });

  it('PR13: changeLanguage tells the server first and then updates the cache', async () => {
    const { controller, api, homeCache } = rig();

    await controller.changeLanguage('TAMIL');

    expect(api.changePreferredLanguage).toHaveBeenCalledWith('CIT-KLT-0001', 'TAMIL');
    expect(homeCache.changeLanguage).toHaveBeenCalledWith('TAMIL');
  });

  it('PR13: a failed changeLanguage is thrown and the cache is unchanged', async () => {
    const { controller, api, homeCache } = rig();
    api.changePreferredLanguage.mockRejectedValue(
      new NetworkError('The server could not be reached.'),
    );

    await expect(controller.changeLanguage('TAMIL')).rejects.toBeInstanceOf(NetworkError);
    expect(homeCache.changeLanguage).not.toHaveBeenCalled();
  });

  it('SC2 step 30: getReportStatus reads the report from the server', async () => {
    const { controller, api } = rig();
    api.getReportStatus.mockResolvedValue(REPORT);

    expect(await controller.getReportStatus('RPT-2026-COL-0905')).toBe(REPORT);
    expect(api.getReportStatus).toHaveBeenCalledWith('RPT-2026-COL-0905');
  });
});
