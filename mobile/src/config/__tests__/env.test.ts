import { getEnv, parseEnv, resolveApiUrl, type RawEnv } from '../env';

/** The development server's address that the mocked `Constants.expoConfig.hostUri` returns. */
let mockHostUri: string | undefined;

jest.mock('expo-constants', () => ({
  __esModule: true,
  default: {
    get expoConfig() {
      return { hostUri: mockHostUri };
    },
  },
}));

/** A raw environment with only the address set; each test overrides what it checks. */
function raw(overrides: Partial<RawEnv> = {}): RawEnv {
  return {
    apiUrl: 'http://192.168.1.10:8080',
    citizenId: undefined,
    citizenDistrict: undefined,
    citizenLanguage: undefined,
    devServerHostUri: undefined,
    ...overrides,
  };
}

describe('resolveApiUrl', () => {
  it('builds the address from the development server for auto', () => {
    expect(resolveApiUrl('auto', '192.168.1.100:8081')).toBe('http://192.168.1.100:8080');
  });

  it('accepts auto in any case and with spaces', () => {
    expect(resolveApiUrl(' AUTO ', '192.168.1.100:8081')).toBe('http://192.168.1.100:8080');
  });

  it('drops a path after the development server address', () => {
    expect(resolveApiUrl('auto', '192.168.1.100:8081/--/x')).toBe('http://192.168.1.100:8080');
  });

  it('accepts a development server address without a port', () => {
    expect(resolveApiUrl('auto', '10.0.0.7')).toBe('http://10.0.0.7:8080');
  });

  it('refuses auto when the development server address is missing and says so', () => {
    expect(() => resolveApiUrl('auto', undefined)).toThrow(
      /EXPO_PUBLIC_API_URL is auto.*was nothing.*http:\/\/<the PC's LAN IP>:8080.*LAN mode/,
    );
    expect(() => resolveApiUrl('auto', '  ')).toThrow(/was nothing/);
  });

  it('refuses auto for a tunnel host and names what was found', () => {
    expect(() => resolveApiUrl('auto', 'abc-anonymous-8081.exp.direct:80')).toThrow(
      /EXPO_PUBLIC_API_URL.*"abc-anonymous-8081\.exp\.direct:80", which is not an IPv4 address.*not --tunnel/,
    );
  });

  it('refuses auto for localhost', () => {
    expect(() => resolveApiUrl('auto', 'localhost:8081')).toThrow(/"localhost:8081"/);
  });

  it.each(['300.1.1.1:8081', '1.2.3:8081', '1.2.3.4.5:8081', '1.2.3.x:8081', '1.2.3.-4:8081'])(
    'refuses auto for %s, which is not an IPv4 address',
    (hostUri) => {
      expect(() => resolveApiUrl('auto', hostUri)).toThrow(/not an IPv4 address/);
    },
  );

  it('keeps an explicit address without its trailing slashes and ignores the development server', () => {
    expect(resolveApiUrl(' http://192.168.1.10:8080// ', '10.9.9.9:8081')).toBe(
      'http://192.168.1.10:8080',
    );
  });

  it('still fails when the setting is missing or blank, and says auto is possible', () => {
    expect(() => resolveApiUrl(undefined, '192.168.1.100:8081')).toThrow(
      'EXPO_PUBLIC_API_URL is not set',
    );
    expect(() => resolveApiUrl('  ', undefined)).toThrow(
      /is not set.*auto.*Expo development server/,
    );
  });
});

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
  // The variables are restored in place: babel-preset-expo makes the code under test read the
  // `process.env` object that existed when the module loaded, so replacing the object would hide
  // what a later test sets.
  const names = [
    'EXPO_PUBLIC_API_URL',
    'EXPO_PUBLIC_CITIZEN_ID',
    'EXPO_PUBLIC_CITIZEN_DISTRICT',
    'EXPO_PUBLIC_CITIZEN_LANGUAGE',
  ];
  const saved = Object.fromEntries(names.map((name) => [name, process.env[name]]));

  afterEach(() => {
    for (const name of names) {
      if (saved[name] === undefined) {
        delete process.env[name];
      } else {
        process.env[name] = saved[name];
      }
    }
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

  describe('with EXPO_PUBLIC_API_URL=auto', () => {
    let info: jest.SpyInstance;

    beforeEach(() => {
      info = jest.spyOn(console, 'info').mockImplementation(() => undefined);
      mockHostUri = '192.168.1.100:8081';
    });

    afterEach(() => {
      info.mockRestore();
      mockHostUri = undefined;
    });

    it('builds the address from the development server and logs it once', () => {
      process.env.EXPO_PUBLIC_API_URL = 'auto';

      expect(getEnv().apiUrl).toBe('http://192.168.1.100:8080');
      expect(info).toHaveBeenCalledTimes(1);
      expect(info).toHaveBeenCalledWith(
        '[env] API address from the Expo development server:',
        'http://192.168.1.100:8080',
      );
    });

    it('does not log for an explicit address', () => {
      process.env.EXPO_PUBLIC_API_URL = 'http://10.0.0.5:8080';

      expect(getEnv().apiUrl).toBe('http://10.0.0.5:8080');
      expect(info).not.toHaveBeenCalled();
    });

    it('fails clearly, without logging, when the development server has no usable address', () => {
      process.env.EXPO_PUBLIC_API_URL = 'auto';
      mockHostUri = undefined;

      expect(() => getEnv()).toThrow(/EXPO_PUBLIC_API_URL is auto/);
      expect(info).not.toHaveBeenCalled();
    });
  });
});
