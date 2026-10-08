import * as Location from 'expo-location';
import { searchPlace } from '../placeSearch';

jest.mock('expo-location', () => ({ geocodeAsync: jest.fn() }));

const mocked = Location as jest.Mocked<typeof Location>;

describe('searchPlace', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('SC2 A1 12c: geocodes the trimmed query with ", Sri Lanka" and gives the first result', async () => {
    mocked.geocodeAsync.mockResolvedValue([
      { latitude: 6.5888, longitude: 79.964, altitude: 1, accuracy: 5 },
      { latitude: 1, longitude: 2 },
    ] as never);

    const point = await searchPlace('  Kalutara bridge ');

    expect(mocked.geocodeAsync).toHaveBeenCalledWith('Kalutara bridge, Sri Lanka');
    expect(point).toEqual({ latitude: 6.5888, longitude: 79.964 });
  });

  it('SC2 A1 12c: no result gives undefined', async () => {
    mocked.geocodeAsync.mockResolvedValue([]);

    expect(await searchPlace('nowhere')).toBeUndefined();
  });

  it('SC2 A1 12c: an empty or blank query gives undefined without a call', async () => {
    expect(await searchPlace('')).toBeUndefined();
    expect(await searchPlace('   ')).toBeUndefined();
    expect(mocked.geocodeAsync).not.toHaveBeenCalled();
  });

  it('SC2 A1 12c: an error of the geocoder is thrown', async () => {
    mocked.geocodeAsync.mockRejectedValue(new Error('no network'));

    await expect(searchPlace('Galle')).rejects.toThrow('no network');
  });
});
