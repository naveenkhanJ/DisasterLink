/**
 * httpClient.ts — the app's HTTP client over `fetch` (implementation standards §12.1).
 *
 * A reply with a status of 400 or more carries a `ProblemDetail`; it is thrown as {@link ApiError}.
 * A failure to reach the server, or a request that takes longer than the timeout, is thrown as
 * {@link NetworkError}. The caller treats a 4xx as a rejection and a 5xx or a network error as
 * "try again later" (§12.1 item 2).
 */
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

/** A file to upload as a part of a multipart request. */
export interface FilePart {
  uri: string;
  name: string;
  type: string;
}

/** A JSON part of a multipart request: React Native's `FormData` needs the content type spelled out. */
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
    getJson: <T>(path: string) =>
      send<T>('GET', path, { headers: { Accept: JSON_CONTENT_TYPE } }),
    postJson: <T = void>(path: string, body: unknown) => send<T>('POST', path, json(body)),
    putJson: <T = void>(path: string, body: unknown) => send<T>('PUT', path, json(body)),
    postMultipart: <T>(path: string, parts: MultipartPart[]) => {
      // Believed, not verified (standards §12.3 Upload): React Native's FormData sends a string part
      // without a Content-Type, which Spring refuses for @RequestPart, so each JSON part is appended
      // as { string, type: 'application/json' } and a file as { uri, name, type }. Test it from the
      // iPhone early.
      const form = new FormData();
      for (const part of parts) {
        form.append(part.name, part.value as unknown as string | Blob);
      }
      return send<T>('POST', path, { headers: { Accept: JSON_CONTENT_TYPE }, body: form });
    },
  };
}
