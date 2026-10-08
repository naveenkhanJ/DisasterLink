/**
 * appServices.ts — the composition of the app's services (implementation-only): one HTTP client from
 * `getEnv()` (called once), the two API clients, the phone's stores, the location and photo
 * services and the capture controller. UC2-6's screens take what they need from the result.
 */
import * as Crypto from 'expo-crypto';
import { createHttpClient } from '../api/httpClient';
import { createReportApi, type ReportApi } from '../api/reportApi';
import { createSkeletonApi, type SkeletonApi } from '../api/skeletonApi';
import { getEnv, type AppEnv } from '../config/env';
import { createLocationService, type LocationService } from '../location/LocationService';
import { createPhotoService, type PhotoService } from '../report/PhotoService';
import {
  createReportCaptureController,
  type ReportCaptureController,
} from '../report/ReportCaptureController';
import { createHomeCache, type HomeCache } from '../store/HomeCache';
import { createOfflineReportStore, type OfflineReportStore } from '../store/OfflineReportStore';
import { isOnline } from '../sync/connectivityTriggers';

/** Everything the screens use. */
export interface AppServices {
  env: AppEnv;
  skeletonApi: SkeletonApi;
  reportApi: ReportApi;
  store: OfflineReportStore;
  homeCache: HomeCache;
  location: LocationService;
  photos: PhotoService;
  controller: ReportCaptureController;
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
  const reportApi = createReportApi(client);
  const store = createOfflineReportStore();
  const homeCache = createHomeCache(env.citizenLanguage);
  const location = createLocationService();
  const photos = createPhotoService();
  const controller = createReportCaptureController({
    reportApi,
    skeletonApi,
    store,
    homeCache,
    location,
    photos,
    isOnline,
    citizenId: env.citizenId,
    registeredDistrict: env.citizenDistrict,
    newId: () => Crypto.randomUUID(),
    now: () => new Date(),
  });
  return { env, skeletonApi, reportApi, store, homeCache, location, photos, controller };
}
