import * as httpClient from '../../api/httpClient';
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
