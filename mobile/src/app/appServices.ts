/**
 * appServices.ts — the composition of the app's services (implementation-only): one HTTP client from
 * `getEnv()` (called once), the two API clients, the phone's stores, the location and photo
 * services and the capture controller. UC2-6's screens take what they need from the result.
 */
import * as Crypto from 'expo-crypto';
import { createHttpClient } from '../api/httpClient';
import { createReportApi, type ReportApi } from '../api/reportApi';
import { createSkeletonApi, type SkeletonApi } from '../api/skeletonApi';
import { POLL_TIMEOUT_MS } from '../constants';
import { getEnv, type AppEnv } from '../config/env';
import { createLocationService, type LocationService } from '../location/LocationService';
import { createLocationAccess, type LocationAccess } from '../location/locationAccess';
import { createPhotoService, type PhotoService } from '../report/PhotoService';
import {
  createReportCaptureController,
  type ReportCaptureController,
} from '../report/ReportCaptureController';
import { createHomeCache, type HomeCache } from '../store/HomeCache';
import { createOfflineReportStore, type OfflineReportStore } from '../store/OfflineReportStore';
import { isOnline } from '../sync/connectivityTriggers';
import { createServerState, type ServerState } from '../sync/serverState';

/** Everything the screens use. */
export interface AppServices {
  env: AppEnv;
  skeletonApi: SkeletonApi;
  /** The skeleton's calls over a client with the push poll's own short timeout (`POLL_TIMEOUT_MS`, P222). */
  pollApi: SkeletonApi;
  reportApi: ReportApi;
  store: OfflineReportStore;
  homeCache: HomeCache;
  location: LocationService;
  locationAccess: LocationAccess;
  photos: PhotoService;
  controller: ReportCaptureController;
  /** Whether the strip says "Server not reachable"; the controller's `isOnline` reads it (standards §10 item 11, P231). */
  serverState: ServerState;
}

/**
 * Builds the services. Reads the environment once; an address that cannot be resolved throws here.
 *
 * @returns the services
 */
export function createAppServices(): AppServices {
  const env = getEnv();
  const client = createHttpClient(env.apiUrl);
  const skeletonApi = createSkeletonApi(client);
  // The push poll is also the server signal of the connection strip: a short timeout (standards §10 item 11, P222).
  const pollApi = createSkeletonApi(createHttpClient(env.apiUrl, undefined, POLL_TIMEOUT_MS));
  const reportApi = createReportApi(client);
  const store = createOfflineReportStore();
  const homeCache = createHomeCache(env.citizenLanguage);
  const location = createLocationService();
  const locationAccess = createLocationAccess();
  const photos = createPhotoService();
  const serverState = createServerState();
  const controller = createReportCaptureController({
    reportApi,
    skeletonApi,
    store,
    homeCache,
    location,
    photos,
    // The strip's "Server not reachable" counts as offline for the app's own calls; the connectivity
    // triggers keep the network-only `isOnline` (standards §10 item 11, P231).
    isOnline: async () => !serverState.isDown() && (await isOnline()),
    citizenId: env.citizenId,
    registeredDistrict: env.citizenDistrict,
    newId: () => Crypto.randomUUID(),
    now: () => new Date(),
  });
  return {
    env,
    skeletonApi,
    pollApi,
    reportApi,
    store,
    homeCache,
    location,
    locationAccess,
    photos,
    controller,
    serverState,
  };
}
