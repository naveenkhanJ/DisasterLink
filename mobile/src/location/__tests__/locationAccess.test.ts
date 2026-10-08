import * as Location from 'expo-location';
import { createLocationAccess } from '../locationAccess';

jest.mock('expo-location', () => ({
  hasServicesEnabledAsync: jest.fn(),
  getForegroundPermissionsAsync: jest.fn(),
  requestForegroundPermissionsAsync: jest.fn(),
}));

const mocked = Location as jest.Mocked<typeof Location>;

describe('location access', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    mocked.hasServicesEnabledAsync.mockResolvedValue(true);
  });

  it('WF2P-S03g: Location Services off is servicesOff, whatever the permission', async () => {
    mocked.hasServicesEnabledAsync.mockResolvedValue(false);
    mocked.getForegroundPermissionsAsync.mockResolvedValue({ granted: true } as never);

    expect(await createLocationAccess().check()).toBe('servicesOff');
  });

  it('WF2P-S03g: a granted permission is granted', async () => {
    mocked.getForegroundPermissionsAsync.mockResolvedValue({ granted: true } as never);

    expect(await createLocationAccess().check()).toBe('granted');
  });

  it('WF2P-S03g: a permission that is not granted but can be asked again is askable', async () => {
    mocked.getForegroundPermissionsAsync.mockResolvedValue({
      granted: false,
      canAskAgain: true,
    } as never);

    expect(await createLocationAccess().check()).toBe('askable');
  });

  it('WF2P-S03g: a refused permission that cannot be asked again is blocked', async () => {
    mocked.getForegroundPermissionsAsync.mockResolvedValue({
      granted: false,
      canAskAgain: false,
    } as never);

    expect(await createLocationAccess().check()).toBe('blocked');
  });

  it('WF2P-S03g: an error while reading counts as servicesOff, with a warning', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    mocked.getForegroundPermissionsAsync.mockRejectedValue(new Error('no module'));

    expect(await createLocationAccess().check()).toBe('servicesOff');
    expect(warn).toHaveBeenCalledTimes(1);
  });

  it('WF2P-S03g: request asks for the foreground permission and tells whether it was granted', async () => {
    mocked.requestForegroundPermissionsAsync.mockResolvedValueOnce({ granted: true } as never);
    mocked.requestForegroundPermissionsAsync.mockResolvedValueOnce({ granted: false } as never);

    expect(await createLocationAccess().request()).toBe(true);
    expect(await createLocationAccess().request()).toBe(false);
  });
});
