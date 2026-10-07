import { getEnv, parseEnv, type RawEnv } from '../env';

/** A raw environment with only the address set; each test overrides what it checks. */
function raw(overrides: Partial<RawEnv> = {}): RawEnv {
  return {
    apiUrl: 'http://192.168.1.10:8080',
    citizenId: undefined,
    citizenDistrict: undefined,
    citizenLanguage: undefined,
    ...overrides,
  };
}

describe('parseEnv', () => {
  it('names EXPO_PUBLIC_API_URL in the error when the address is missing', () => {
    expect(() => parseEnv(raw({ apiUrl: undefined }))).toThrow('EXPO_PUBLIC_API_URL is not set');
  });

  it('treats a blank address as missing', () => {
    expect(() => parseEnv(raw({ apiUrl: '   ', citizenId: 'x' }))).toThrow('EXPO_PUBLIC_API_URL');
  });

  it('defaults the citizen to the demo citizen CIT-KLT-0001', () => {
    const env = parseEnv(raw());

    expect(env.citizenId).toBe('CIT-KLT-0001');
  });

  it('defaults a blank citizen to the demo citizen too', () => {
    expect(parseEnv(raw({ apiUrl: 'http://h:1', citizenId: ' ' })).citizenId).toBe('CIT-KLT-0001');
  });

  it('defaults the district to KALUTARA and the language to ENGLISH', () => {
    const env = parseEnv(raw());

    expect(env.citizenDistrict).toBe('KALUTARA');
    expect(env.citizenLanguage).toBe('ENGLISH');
  });

  it('defaults a blank district and a blank language too', () => {
    const env = parseEnv(raw({ citizenDistrict: '  ', citizenLanguage: '' }));

    expect(env.citizenDistrict).toBe('KALUTARA');
    expect(env.citizenLanguage).toBe('ENGLISH');
  });

  it('takes an explicit district and language, accepting lower case and spaces', () => {
    const env = parseEnv(raw({ citizenDistrict: ' colombo ', citizenLanguage: 'tamil' }));

    expect(env.citizenDistrict).toBe('COLOMBO');
    expect(env.citizenLanguage).toBe('TAMIL');
  });

  it('refuses an unknown district, names the variable and lists the districts', () => {
    expect(() => parseEnv(raw({ citizenDistrict: 'ATLANTIS' }))).toThrow(
      /EXPO_PUBLIC_CITIZEN_DISTRICT.*ATLANTIS.*AMPARA.*NUWARA_ELIYA.*VAVUNIYA/,
    );
  });

  it('refuses an unknown language, names the variable and lists the languages', () => {
    expect(() => parseEnv(raw({ citizenLanguage: 'FRENCH' }))).toThrow(
      /EXPO_PUBLIC_CITIZEN_LANGUAGE.*FRENCH.*SINHALA, TAMIL, ENGLISH/,
    );
  });

  it('takes the citizen and the address as given and removes trailing slashes', () => {
    const env = parseEnv(
      raw({ apiUrl: ' http://192.168.1.10:8080// ', citizenId: 'CIT-COL-0002' }),
    );

    expect(env).toEqual({
      apiUrl: 'http://192.168.1.10:8080',
      citizenId: 'CIT-COL-0002',
      citizenDistrict: 'KALUTARA',
      citizenLanguage: 'ENGLISH',
    });
  });
});

describe('getEnv', () => {
  const saved = { ...process.env };

  afterEach(() => {
    process.env = { ...saved };
  });

  it('reads the four EXPO_PUBLIC variables from the environment', () => {
    process.env.EXPO_PUBLIC_API_URL = 'http://10.0.0.5:8080';
    process.env.EXPO_PUBLIC_CITIZEN_ID = 'CIT-GAM-0003';
    process.env.EXPO_PUBLIC_CITIZEN_DISTRICT = 'GAMPAHA';
    process.env.EXPO_PUBLIC_CITIZEN_LANGUAGE = 'SINHALA';

    expect(getEnv()).toEqual({
      apiUrl: 'http://10.0.0.5:8080',
      citizenId: 'CIT-GAM-0003',
      citizenDistrict: 'GAMPAHA',
      citizenLanguage: 'SINHALA',
    });
  });

  it('fails clearly when EXPO_PUBLIC_API_URL is not set', () => {
    delete process.env.EXPO_PUBLIC_API_URL;

    expect(() => getEnv()).toThrow('EXPO_PUBLIC_API_URL is not set');
  });
});
