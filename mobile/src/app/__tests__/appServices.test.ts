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
});
