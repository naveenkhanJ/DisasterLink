import * as Location from 'expo-location';
import { createLocationService, resolveDistrict } from '../LocationService';

jest.mock('expo-location', () => ({
  requestForegroundPermissionsAsync: jest.fn(),
  hasServicesEnabledAsync: jest.fn(),
  getCurrentPositionAsync: jest.fn(),
  Accuracy: { Balanced: 3 },
}));

const mocked = Location as jest.Mocked<typeof Location>;

describe('resolveDistrict', () => {
  it('SC2 step 18: Kaduwela (6.9356 N, 79.9843 E) is in COLOMBO', () => {
    expect(resolveDistrict({ latitude: 6.9356, longitude: 79.9843 })).toBe('COLOMBO');
  });

  it('SC2 step 18: the Kalutara bridge (6.5888 N, 79.9640 E) is in KALUTARA', () => {
    expect(resolveDistrict({ latitude: 6.5888, longitude: 79.964 })).toBe('KALUTARA');
  });

  it('SC2 step 18: a point in the sea is in no district', () => {
    expect(resolveDistrict({ latitude: 6.5, longitude: 79.5 })).toBeUndefined();
  });
});

describe('getCurrentLocation', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('SC2 step 13: gives the fix with its accuracy when the permission is granted', async () => {
    mocked.requestForegroundPermissionsAsync.mockResolvedValue({ granted: true } as never);
    mocked.hasServicesEnabledAsync.mockResolvedValue(true);
    mocked.getCurrentPositionAsync.mockResolvedValue({
      coords: { latitude: 6.5888, longitude: 79.964, accuracy: 8 },
    } as never);

    const fix = await createLocationService().getCurrentLocation();

    expect(fix).toEqual({ latitude: 6.5888, longitude: 79.964, accuracyMetres: 8 });
    expect(mocked.getCurrentPositionAsync).toHaveBeenCalledWith({ accuracy: 3 });
  });

  it('SC2 A1: a denied permission gives no fix', async () => {
    mocked.requestForegroundPermissionsAsync.mockResolvedValue({ granted: false } as never);

    expect(await createLocationService().getCurrentLocation()).toBeUndefined();
    expect(mocked.getCurrentPositionAsync).not.toHaveBeenCalled();
  });

  it('SC2 A1: Location Services switched off give no fix', async () => {
    mocked.requestForegroundPermissionsAsync.mockResolvedValue({ granted: true } as never);
    mocked.hasServicesEnabledAsync.mockResolvedValue(false);

    expect(await createLocationService().getCurrentLocation()).toBeUndefined();
  });

  it('SC2 A1: a failure to get a fix gives no fix', async () => {
    mocked.requestForegroundPermissionsAsync.mockResolvedValue({ granted: true } as never);
    mocked.hasServicesEnabledAsync.mockResolvedValue(true);
    mocked.getCurrentPositionAsync.mockRejectedValue(new Error('no fix'));

    expect(await createLocationService().getCurrentLocation()).toBeUndefined();
  });

  it('exposes resolveDistrict on the service', () => {
    expect(createLocationService().resolveDistrict({ latitude: 6.9356, longitude: 79.9843 })).toBe(
      'COLOMBO',
    );
  });
});
