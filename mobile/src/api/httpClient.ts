/**
 * httpClient.ts — the app's HTTP client over `fetch` (implementation standards §12.1).
 *
 * A reply with a status of 400 or more carries a `ProblemDetail`; it is thrown as {@link ApiError}.
 * A failure to reach the server, or a request that takes longer than the timeout, is thrown as
 * {@link NetworkError}. The caller treats a 4xx as a rejection and a 5xx or a network error as
 * "try again later" (§12.1 item 2).
 */
import { File } from 'expo-file-system';
import {
  HTTP_ERROR_MIN,
  HTTP_NO_CONTENT,
  JSON_CONTENT_TYPE,
  REQUEST_TIMEOUT_MS,
} from '../constants';
import type { ProblemDetail } from './types';

/** The server answered with an error status and a problem detail. */
export class ApiError extends Error {
  /** The HTTP status. */
  readonly status: number;
  /** The plain-language `detail` of the problem. */
  readonly detail: string;

  /**
   * @param status the HTTP status of the reply
   * @param detail the `detail` of the problem detail
   */
  constructor(status: number, detail: string) {
    super(`HTTP ${status}: ${detail}`);
    this.name = 'ApiError';
    this.status = status;
    this.detail = detail;
  }

  /** Whether the server refused the request itself (a 4xx), as opposed to a fault (a 5xx). */
  get isRejection(): boolean {
    return this.status < 500;
  }
}

/** The server could not be reached, or did not answer in time. */
export class NetworkError extends Error {
  /** The underlying failure. */
  readonly cause: unknown;

  /**
   * @param message what happened, in plain words
   * @param cause the underlying failure
   */
  constructor(message: string, cause?: unknown) {
    super(message);
    this.name = 'NetworkError';
    this.cause = cause;
  }
}

/**
 * A file to upload as a part of a multipart request. It is sent as an `expo-file-system` `File` made
 * from `uri`: the server receives the file's own name and the content type that `File` derives from
 * the file's extension, so the file must keep a `.jpg` or `.png` name. `name` and `type` are not
 * sent.
 */
export interface FilePart {
  uri: string;
  name: string;
  type: string;
}

/**
 * The client's own description of a JSON part of a multipart request (made with {@link jsonPart}).
 * It is not React Native's form-part shape: {@link toFormValue} turns it into a `Blob` of the given
 * type when the request is built.
 */
export interface JsonPart {
  string: string;
  type: string;
}

/** One part of a multipart request: a plain string, a JSON part or a file. */
export interface MultipartPart {
  name: string;
  value: string | JsonPart | FilePart;
}

/**
 * Builds a JSON part of a multipart request.
 *
 * @param value the object to send
 * @returns the part's value, with `application/json` as its type
 */
export function jsonPart(value: unknown): JsonPart {
  return { string: JSON.stringify(value), type: JSON_CONTENT_TYPE };
}

/**
 * Turns the value of a multipart part into what `FormData` can carry in Expo SDK 57, whose global
 * `fetch` (`expo/fetch`) accepts only a string, a `Blob` or an object with `bytes()` as a form part.
 *
 * @param value a plain string, a {@link JsonPart} or a {@link FilePart}
 * @returns the string itself, a `Blob` of the JSON text and its type, or the file as a `File`
 * @throws Error whatever the `File` constructor throws for an unusable `uri`
 */
export function toFormValue(value: string | JsonPart | FilePart): string | Blob {
  if (typeof value === 'string') {
    return value;
  }
  if (isFilePart(value)) {
    // `File` implements the Blob interface and `bytes()`, which expo/fetch reads; the cast only
    // satisfies the typing of `FormData.append`.
    return new File(value.uri) as unknown as Blob;
  }
  return new Blob([value.string], { type: value.type });
}

/** Tells a file part from a JSON part by its `uri` property. */
function isFilePart(value: JsonPart | FilePart): value is FilePart {
  return 'uri' in value;
}

/** The operations of the client; the skeleton's and the use cases' API modules take this interface. */
export interface HttpClient {
  /**
   * @param path the path below the base URL, with its query
   * @returns the parsed JSON body
   */
  getJson<T>(path: string): Promise<T>;
  /**
   * @param path the path below the base URL
   * @param body the object to send as JSON
   * @returns the parsed JSON reply, or `undefined` for a 204
   */
  postJson<T = void>(path: string, body: unknown): Promise<T>;
  /**
   * @param path the path below the base URL
   * @param body the object to send as JSON
   * @returns the parsed JSON reply, or `undefined` for a 204
   */
  putJson<T = void>(path: string, body: unknown): Promise<T>;
  /**
   * @param path the path below the base URL
   * @param parts the parts of the `multipart/form-data` body
   * @returns the parsed JSON reply, or `undefined` for a 204
   */
  postMultipart<T>(path: string, parts: MultipartPart[]): Promise<T>;
}

/** The `fetch` function, injectable for tests. */
export type FetchFunction = (url: string, init: RequestInit) => Promise<Response>;

/** Reads the `detail` of a problem reply; a body that is not a problem gives the status text. */
async function readDetail(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as Partial<ProblemDetail>;
    if (typeof problem.detail === 'string' && problem.detail.length > 0) {
      return problem.detail;
    }
  } catch {
    // The body was not JSON: fall through to the status text.
  }
  return response.statusText || `HTTP ${response.status}`;
}

/**
 * Builds a client over `fetch`.
 *
 * @param baseUrl the server's base URL, without a trailing slash
 * @param fetchFn `fetch`; replaced in tests
 * @param timeoutMs the time one request may take
 * @returns the client
 */
export function createHttpClient(
  baseUrl: string,
  fetchFn: FetchFunction = (url, init) => fetch(url, init),
  timeoutMs: number = REQUEST_TIMEOUT_MS,
): HttpClient {
  async function send<T>(method: string, path: string, init: RequestInit): Promise<T> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetchFn(`${baseUrl}${path}`, {
        ...init,
        method,
        signal: controller.signal,
      });
      if (response.status === HTTP_NO_CONTENT) {
        return undefined as T;
      }
      if (response.status >= HTTP_ERROR_MIN) {
        throw new ApiError(response.status, await readDetail(response));
      }
      return (await response.json()) as T;
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      console.warn(`[httpClient] ${method} ${path} failed:`, error);
      if (controller.signal.aborted) {
        throw new NetworkError(`The server did not answer within ${timeoutMs} ms.`, error);
      }
      throw new NetworkError('The server could not be reached.', error);
    } finally {
      clearTimeout(timer);
    }
  }

  function json(body: unknown): RequestInit {
    return {
      headers: { 'Content-Type': JSON_CONTENT_TYPE, Accept: JSON_CONTENT_TYPE },
      body: JSON.stringify(body),
    };
  }

  return {
    getJson: <T>(path: string) => send<T>('GET', path, { headers: { Accept: JSON_CONTENT_TYPE } }),
    postJson: <T = void>(path: string, body: unknown) => send<T>('POST', path, json(body)),
    putJson: <T = void>(path: string, body: unknown) => send<T>('PUT', path, json(body)),
    postMultipart: async <T>(path: string, parts: MultipartPart[]) => {
      // Standards §12.3 Upload, verified on the iPhone on 14 October 2026: expo/fetch accepts only a
      // string, a Blob or an object with bytes() as a form part, so a JSON part is sent as a Blob of
      // type application/json and a file as an expo-file-system File (see toFormValue). The form is
      // built before send(), so a failure here reaches the caller as itself, not as a NetworkError.
      const form = new FormData();
      for (const part of parts) {
        form.append(part.name, toFormValue(part.value));
      }
      return send<T>('POST', path, { headers: { Accept: JSON_CONTENT_TYPE }, body: form });
    },
  };
}
