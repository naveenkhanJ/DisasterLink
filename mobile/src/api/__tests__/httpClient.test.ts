import {
  ApiError,
  createHttpClient,
  jsonPart,
  NetworkError,
  type FetchFunction,
} from '../httpClient';

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

describe('multipart', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('appends a plain part, a JSON part and a file part to the FormData in the shapes React Native needs', async () => {
    // jest-expo's FormData is not React Native's, so the appended values are recorded, not stored.
    const append = jest.spyOn(FormData.prototype, 'append').mockImplementation(() => undefined);
    const fetchFn = fakeFetch(reply(200, { outcome: 'ACCEPTED' }));
    const file = { uri: 'file:///photo.jpg', name: 'photo.jpg', type: 'image/jpeg' };

    const result = await createHttpClient(BASE, fetchFn as FetchFunction).postMultipart('/api/r', [
      { name: 'note', value: 'plain' },
      { name: 'draft', value: jsonPart({ draftId: 'd-1' }) },
      { name: 'photo', value: file },
    ]);

    expect(result).toEqual({ outcome: 'ACCEPTED' });
    expect(append.mock.calls).toEqual([
      ['note', 'plain'],
      ['draft', { string: JSON.stringify({ draftId: 'd-1' }), type: 'application/json' }],
      ['photo', file],
    ]);
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe(`${BASE}/api/r`);
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
    expect(init.headers['Content-Type']).toBeUndefined();
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
      expect(globalFetch.mock.calls[0]).toEqual([`${BASE}/api/g`, expect.objectContaining({ method: 'GET' })]);
    } finally {
      globalThis.fetch = original;
    }
  });
});
