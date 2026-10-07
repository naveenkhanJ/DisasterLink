import { getEnv, parseEnv } from '../env';

describe('parseEnv', () => {
  it('names EXPO_PUBLIC_API_URL in the error when the address is missing', () => {
    expect(() =>
      parseEnv({ apiUrl: undefined, citizenId: undefined, firstRunCheck: undefined }),
    ).toThrow('EXPO_PUBLIC_API_URL is not set');
  });

  it('treats a blank address as missing', () => {
    expect(() => parseEnv({ apiUrl: '   ', citizenId: 'x', firstRunCheck: undefined })).toThrow(
      'EXPO_PUBLIC_API_URL',
    );
  });

  it('defaults the citizen to the demo citizen CIT-KLT-0001', () => {
    const env = parseEnv({
      apiUrl: 'http://192.168.1.10:8080',
      citizenId: undefined,
      firstRunCheck: undefined,
    });

    expect(env.citizenId).toBe('CIT-KLT-0001');
  });

  it('defaults a blank citizen to the demo citizen too', () => {
    expect(parseEnv({ apiUrl: 'http://h:1', citizenId: ' ', firstRunCheck: undefined }).citizenId).toBe(
      'CIT-KLT-0001',
    );
  });

  it('takes the citizen and the address as given and removes trailing slashes', () => {
    const env = parseEnv({
      apiUrl: ' http://192.168.1.10:8080// ',
      citizenId: 'CIT-COL-0002',
      firstRunCheck: undefined,
    });

    expect(env).toEqual({
      apiUrl: 'http://192.168.1.10:8080',
      citizenId: 'CIT-COL-0002',
      firstRunCheck: false,
    });
  });

  it('switches the first-run check on only for the value 1', () => {
    const base = { apiUrl: 'http://h:1', citizenId: undefined };

    expect(parseEnv({ ...base, firstRunCheck: '1' }).firstRunCheck).toBe(true);
    expect(parseEnv({ ...base, firstRunCheck: '0' }).firstRunCheck).toBe(false);
    expect(parseEnv({ ...base, firstRunCheck: 'true' }).firstRunCheck).toBe(false);
  });
});

describe('getEnv', () => {
  const saved = { ...process.env };

  afterEach(() => {
    process.env = { ...saved };
  });

  it('reads the three EXPO_PUBLIC variables from the environment', () => {
    process.env.EXPO_PUBLIC_API_URL = 'http://10.0.0.5:8080';
    process.env.EXPO_PUBLIC_CITIZEN_ID = 'CIT-GAM-0003';
    process.env.EXPO_PUBLIC_FIRST_RUN_CHECK = '1';

    expect(getEnv()).toEqual({
      apiUrl: 'http://10.0.0.5:8080',
      citizenId: 'CIT-GAM-0003',
      firstRunCheck: true,
    });
  });

  it('fails clearly when EXPO_PUBLIC_API_URL is not set', () => {
    delete process.env.EXPO_PUBLIC_API_URL;

    expect(() => getEnv()).toThrow('EXPO_PUBLIC_API_URL is not set');
  });
});
