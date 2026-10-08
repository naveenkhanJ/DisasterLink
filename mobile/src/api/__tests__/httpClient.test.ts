import { File } from 'expo-file-system';
import {
  ApiError,
  createHttpClient,
  jsonPart,
  NetworkError,
  toFormValue,
  type FetchFunction,
} from '../httpClient';

/** Records every `File` the code under test makes; a test can make the constructor throw. */
const mockFileConstructed = jest.fn();

jest.mock('expo-file-system', () => ({
  File: class {
    readonly uri: string;

    constructor(uri: string) {
      mockFileConstructed(uri);
      this.uri = uri;
    }
  },
}));

const BASE = 'http://192.168.1.10:8080';

/** What the tests read from a thrown ApiError or NetworkError. */
type Failure = Error & { status: number; detail: string; isRejection: boolean; cause: unknown };

function reply(status: number, body?: unknown, statusText = ''): Response {
  return {
    status,
    statusText,
    json: async () => {
      if (body === undefined) {
        throw new SyntaxError('no body');
      }
      return body;
    },
  } as unknown as Response;
}

function fakeFetch(response: Response | Error): jest.Mock {
  return jest.fn(async () => {
    if (response instanceof Error) {
      throw response;
    }
    return response;
  });
}

/** Runs a call that must fail and returns what it threw. */
async function failureOf(call: Promise<unknown>): Promise<Failure> {
  try {
    await call;
  } catch (error) {
    return error as Failure;
  }
  throw new Error('The call did not fail.');
}

describe('JSON requests', () => {
  it('gets JSON from the base URL and parses the body', async () => {
    const fetchFn = fakeFetch(reply(200, [{ id: 'a' }]));

    const result = await createHttpClient(BASE, fetchFn as FetchFunction).getJson('/api/x?y=1');

    expect(result).toEqual([{ id: 'a' }]);
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe(`${BASE}/api/x?y=1`);
    expect(init.method).toBe('GET');
    expect(init.headers.Accept).toBe('application/json');
  });

  it('posts a JSON body with its content type', async () => {
    const fetchFn = fakeFetch(reply(200, { ok: true }));

    const result = await createHttpClient(BASE, fetchFn as FetchFunction).postJson('/api/y', {
      ids: ['1'],
    });

    expect(result).toEqual({ ok: true });
    const [, init] = fetchFn.mock.calls[0];
    expect(init.method).toBe('POST');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(init.body)).toEqual({ ids: ['1'] });
  });

  it('puts a JSON body', async () => {
    const fetchFn = fakeFetch(reply(200, { language: 'TAMIL' }));

    await createHttpClient(BASE, fetchFn as FetchFunction).putJson('/api/z', { language: 'TAMIL' });

    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe(`${BASE}/api/z`);
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body)).toEqual({ language: 'TAMIL' });
  });

  it('gives undefined for a 204 and does not read a body', async () => {
    const response = reply(204);
    const json = jest.spyOn(response, 'json');

    const result = await createHttpClient(BASE, fakeFetch(response) as FetchFunction).postJson(
      '/api/ack',
      { ids: [] },
    );

    expect(result).toBeUndefined();
    expect(json).not.toHaveBeenCalled();
  });
});

describe('errors', () => {
  let warn: jest.SpyInstance;

  beforeEach(() => {
    warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => {
    warn.mockRestore();
  });

  it('turns a 4xx problem detail into an ApiError that is a rejection', async () => {
    const fetchFn = fakeFetch(
      reply(400, { status: 400, detail: 'The request is not valid.', title: 'Bad Request' }),
    );

    const failure = await failureOf(
      createHttpClient(BASE, fetchFn as FetchFunction).getJson('/api/x'),
    );

    expect(failure).toBeInstanceOf(ApiError);
    expect(failure.status).toBe(400);
    expect(failure.detail).toBe('The request is not valid.');
    expect(failure.isRejection).toBe(true);
    expect(warn).not.toHaveBeenCalled();
  });

  it('turns a 5xx into an ApiError that is not a rejection', async () => {
    const fetchFn = fakeFetch(reply(500, { status: 500, detail: 'The service is unavailable.' }));

    const failure = await failureOf(
      createHttpClient(BASE, fetchFn as FetchFunction).getJson('/api/x'),
    );

    expect(failure).toBeInstanceOf(ApiError);
    expect(failure.status).toBe(500);
    expect(failure.isRejection).toBe(false);
  });

  it('uses the status text when an error reply has no problem detail', async () => {
    const fetchFn = fakeFetch(reply(502, undefined, 'Bad Gateway'));

    const failure = await failureOf(
      createHttpClient(BASE, fetchFn as FetchFunction).getJson('/api/x'),
    );

    expect(failure.detail).toBe('Bad Gateway');
  });

  it('uses the status when there is no detail and no status text', async () => {
    const fetchFn = fakeFetch(reply(503, { status: 503 }));

    const failure = await failureOf(
      createHttpClient(BASE, fetchFn as FetchFunction).getJson('/api/x'),
    );

    expect(failure.detail).toBe('HTTP 503');
  });

  it('turns a failure to reach the server into a NetworkError', async () => {
    const fetchFn = fakeFetch(new TypeError('Network request failed'));

    const failure = await failureOf(
      createHttpClient(BASE, fetchFn as FetchFunction).getJson('/api/x'),
    );

    expect(failure).toBeInstanceOf(NetworkError);
    expect(failure.message).toBe('The server could not be reached.');
    expect(failure.cause).toBeInstanceOf(TypeError);
  });

  it('logs the cause of a failed request once, with its method and path but not the base URL', async () => {
    const cause = new TypeError('Network request failed');
    const fetchFn = fakeFetch(cause);

    await failureOf(createHttpClient(BASE, fetchFn as FetchFunction).postJson('/api/x?y=1', {}));

    expect(warn).toHaveBeenCalledTimes(1);
    expect(warn).toHaveBeenCalledWith('[httpClient] POST /api/x?y=1 failed:', cause);
    expect(String(warn.mock.calls[0][0])).not.toContain(BASE);
  });

  it('turns a request that takes too long into a NetworkError and aborts it', async () => {
    jest.useFakeTimers();
    try {
      let signal: AbortSignal | undefined;
      const fetchFn: FetchFunction = (_url, init) =>
        new Promise((_resolve, reject) => {
          signal = init.signal ?? undefined;
          signal?.addEventListener('abort', () => reject(new Error('aborted')));
        });
      const pending = failureOf(createHttpClient(BASE, fetchFn, 1000).getJson('/api/slow'));

      await jest.advanceTimersByTimeAsync(1000);
      const failure = await pending;

      expect(failure).toBeInstanceOf(NetworkError);
      expect(failure.message).toBe('The server did not answer within 1000 ms.');
      expect(signal?.aborted).toBe(true);
    } finally {
      jest.useRealTimers();
    }
  });
});

describe('toFormValue', () => {
  beforeEach(() => {
    mockFileConstructed.mockReset();
  });

  it('keeps a plain string as it is', () => {
    expect(toFormValue('plain')).toBe('plain');
  });

  it('turns a JSON part into a Blob of its type holding its text', async () => {
    const value = toFormValue(jsonPart({ draftId: 'd-1' }));

    expect(value).toBeInstanceOf(Blob);
    const blob = value as Blob;
    expect(blob.type).toBe('application/json');
    expect(await blob.text()).toBe(JSON.stringify({ draftId: 'd-1' }));
  });

  it('turns a file part into a File made from its uri', () => {
    const value = toFormValue({ uri: 'file:///photo.jpg', name: 'photo.jpg', type: 'image/jpeg' });

    expect(value).toBeInstanceOf(File);
    expect((value as unknown as { uri: string }).uri).toBe('file:///photo.jpg');
    expect(mockFileConstructed).toHaveBeenCalledTimes(1);
    expect(mockFileConstructed).toHaveBeenCalledWith('file:///photo.jpg');
  });
});

describe('multipart', () => {
  beforeEach(() => {
    mockFileConstructed.mockReset();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('appends a plain part, a JSON Blob and a File to the FormData and sends it without a Content-Type', async () => {
    // jest-expo's FormData does not store what is appended, so the appended values are recorded.
    const append = jest.spyOn(FormData.prototype, 'append').mockImplementation(() => undefined);
    const fetchFn = fakeFetch(reply(200, { outcome: 'ACCEPTED' }));
    const file = { uri: 'file:///photo.jpg', name: 'photo.jpg', type: 'image/jpeg' };

    const result = await createHttpClient(BASE, fetchFn as FetchFunction).postMultipart('/api/r', [
      { name: 'note', value: 'plain' },
      { name: 'draft', value: jsonPart({ draftId: 'd-1' }) },
      { name: 'photo', value: file },
    ]);

    expect(result).toEqual({ outcome: 'ACCEPTED' });
    expect(append).toHaveBeenCalledTimes(3);
    const [note, draft, photo] = append.mock.calls as unknown as [string, string | Blob][];
    expect(note).toEqual(['note', 'plain']);
    expect(draft[0]).toBe('draft');
    expect(draft[1]).toBeInstanceOf(Blob);
    expect((draft[1] as Blob).type).toBe('application/json');
    expect(await (draft[1] as Blob).text()).toBe(JSON.stringify({ draftId: 'd-1' }));
    expect(photo[0]).toBe('photo');
    expect(photo[1]).toBeInstanceOf(File);
    expect((photo[1] as unknown as { uri: string }).uri).toBe(file.uri);
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe(`${BASE}/api/r`);
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
    expect(init.headers['Content-Type']).toBeUndefined();
  });

  it('rejects with the building error itself, not a NetworkError, and never calls fetch', async () => {
    const boom = new Error('cannot open file');
    mockFileConstructed.mockImplementationOnce(() => {
      throw boom;
    });
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    const fetchFn = fakeFetch(reply(200, {}));

    const failure = await failureOf(
      createHttpClient(BASE, fetchFn as FetchFunction).postMultipart('/api/r', [
        { name: 'photo', value: { uri: 'file:///x.jpg', name: 'x.jpg', type: 'image/jpeg' } },
      ]),
    );

    expect(failure).toBe(boom);
    expect(failure).not.toBeInstanceOf(NetworkError);
    expect(fetchFn).not.toHaveBeenCalled();
    expect(warn).not.toHaveBeenCalled();
  });

  it('builds a JSON part with the application/json type', () => {
    expect(jsonPart({ a: 1 })).toEqual({ string: '{"a":1}', type: 'application/json' });
  });
});

describe('the default fetch', () => {
  it('uses the global fetch when none is injected', async () => {
    const globalFetch = jest.fn(async () => reply(200, { ok: 1 }));
    const original = globalThis.fetch;
    globalThis.fetch = globalFetch as unknown as typeof fetch;
    try {
      const result = await createHttpClient(BASE).getJson('/api/g');

      expect(result).toEqual({ ok: 1 });
      expect(globalFetch.mock.calls[0]).toEqual([
        `${BASE}/api/g`,
        expect.objectContaining({ method: 'GET' }),
      ]);
    } finally {
      globalThis.fetch = original;
    }
  });
});
