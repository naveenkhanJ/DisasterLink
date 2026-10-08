import * as Network from 'expo-network';
import * as httpClient from '../../api/httpClient';
import type { ReportDraft } from '../../report/ReportDraft';
import { getEnv } from '../../config/env';
import { createAppServices } from '../appServices';

jest.mock('../../config/env', () => ({
  getEnv: jest.fn(() => ({
    apiUrl: 'http://192.168.1.100:8080',
    citizenId: 'CIT-KLT-0001',
    citizenDistrict: 'KALUTARA',
    citizenLanguage: 'ENGLISH',
  })),
}));
jest.mock('expo-crypto', () => ({ randomUUID: jest.fn(() => 'uuid-1') }));
jest.mock('expo-location', () => ({}));
jest.mock('expo-image-picker', () => ({
  UIImagePickerPreferredAssetRepresentationMode: { Compatible: 'compatible' },
}));
jest.mock('expo-file-system', () => ({ File: class {}, Paths: {} }));
jest.mock('expo-network', () => ({ getNetworkStateAsync: jest.fn() }));
jest.mock('@react-native-async-storage/async-storage', () =>
  jest.requireActual('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

jest.mock('../../api/httpClient', () => {
  const actual = jest.requireActual('../../api/httpClient');
  return { ...actual, createHttpClient: jest.fn(actual.createHttpClient) };
});

describe('createAppServices', () => {
  it('builds the services once from the environment, with a working capture controller', () => {
    const services = createAppServices();

    expect(getEnv).toHaveBeenCalledTimes(1);
    expect(services.env.apiUrl).toBe('http://192.168.1.100:8080');
    expect(services.controller.startReport()).toMatchObject({
      draftId: 'uuid-1',
      locationFromGps: false,
    });
    expect(Object.keys(services)).toEqual(
      expect.arrayContaining([
        'env',
        'skeletonApi',
        'reportApi',
        'store',
        'homeCache',
        'location',
        'photos',
        'controller',
      ]),
    );
  });

  it('standards §10 item 11 (P222): the push poll has its own client with the 4 s timeout; the other calls keep the default', () => {
    jest.mocked(httpClient.createHttpClient).mockClear();

    const services = createAppServices();

    const calls = jest.mocked(httpClient.createHttpClient).mock.calls;
    expect(calls).toHaveLength(2);
    expect(calls[0]).toEqual(['http://192.168.1.100:8080']);
    expect(calls[1]).toEqual(['http://192.168.1.100:8080', undefined, 4000]);
    expect(services.pollApi).toBeDefined();
    expect(services.pollApi).not.toBe(services.skeletonApi);
    expect(Object.keys(services.pollApi)).toEqual(Object.keys(services.skeletonApi));
  });
});

describe('"Server not reachable" counts as offline (standards §10 item 11, P231; UC2-7k F1)', () => {
  const ONLINE = { isConnected: true, isInternetReachable: true };

  /** A complete draft that passes the controller's checks. */
  const DRAFT: ReportDraft = {
    draftId: 'draft-1',
    hazardType: 'FLOOD',
    description: 'Water is rising',
    location: { latitude: 6.5888, longitude: 79.964, accuracyMetres: 8 },
    locationFromGps: true,
    photoUri: 'file:///documents/draft-1.jpg',
    capturedAt: '2026-05-21T02:10:00.000Z',
  };

  let fetchMock: jest.Mock;

  beforeEach(() => {
    jest.mocked(Network.getNetworkStateAsync).mockReset();
    jest.mocked(Network.getNetworkStateAsync).mockResolvedValue(ONLINE as never);
    fetchMock = jest.fn();
    globalThis.fetch = fetchMock as unknown as typeof fetch;
  });

  it('builds one serverState and starts with the server not down', () => {
    const services = createAppServices();

    expect(services.serverState.isDown()).toBe(false);
  });

  it('the controller is online while the server is not down and the phone is online', async () => {
    const services = createAppServices();
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
      text: async () => '[]',
    });

    const home = await services.controller.loadHome();

    expect(fetchMock).toHaveBeenCalled();
    expect(home.fromCache).toBe(false);
  });

  it('while the strip says Server not reachable, loadHome reads the saved copy with no request', async () => {
    const services = createAppServices();
    services.serverState.setDown(true);

    const home = await services.controller.loadHome();

    expect(home.fromCache).toBe(true);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('while the strip says Server not reachable, submit queues at once with no request', async () => {
    const services = createAppServices();
    services.serverState.setDown(true);

    const result = await services.controller.submit(DRAFT);

    expect(result.state).toBe('QUEUED');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('while the strip says Server not reachable, syncPending sends nothing', async () => {
    const services = createAppServices();
    services.serverState.setDown(true);
    await services.controller.submit({ ...DRAFT, draftId: 'draft-2' });

    const results = await services.controller.syncPending();

    expect(results).toEqual([]);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('when the holder is clear again the controller asks the network and sends', async () => {
    const services = createAppServices();
    services.serverState.setDown(true);
    services.serverState.setDown(false);
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
      text: async () => '[]',
    });

    const home = await services.controller.loadHome();

    expect(home.fromCache).toBe(false);
  });

  it('with no internet the controller is offline whatever the holder says, and the network is not asked while the server is down', async () => {
    const services = createAppServices();
    jest.mocked(Network.getNetworkStateAsync).mockResolvedValue({
      isConnected: false,
      isInternetReachable: false,
    } as never);

    expect((await services.controller.loadHome()).fromCache).toBe(true);
    services.serverState.setDown(true);
    jest.mocked(Network.getNetworkStateAsync).mockClear();
    await services.controller.loadHome();

    expect(Network.getNetworkStateAsync).not.toHaveBeenCalled();
  });
});
