import type { SkeletonApi } from '../../api/skeletonApi';
import { ApiError, NetworkError } from '../../api/httpClient';
import { runFirstRunCheck } from '../firstRunCheck';

function api(pollOutbox: SkeletonApi['pollOutbox']): SkeletonApi {
  return { pollOutbox, getActiveWarnings: jest.fn(), acknowledge: jest.fn() };
}

describe('runFirstRunCheck (removed after the first-run check, S6)', () => {
  it('reports the number of push entries when the server answers', async () => {
    const alert = jest.fn(async () => undefined);
    const poll = jest.fn(async () => [{ id: 'a' }, { id: 'b' }] as never);

    await runFirstRunCheck(api(poll), 'CIT-KLT-0001', alert);

    expect(poll).toHaveBeenCalledWith('CIT-KLT-0001');
    expect(alert).toHaveBeenCalledWith(
      'DisasterLink first-run check',
      'Server reachable: 2 push entries',
    );
  });

  it('reports a network error as "Server NOT reachable"', async () => {
    const alert = jest.fn(async () => undefined);

    await runFirstRunCheck(
      api(async () => {
        throw new NetworkError('The server could not be reached.');
      }),
      'CIT-KLT-0001',
      alert,
    );

    expect(alert).toHaveBeenCalledWith(
      'DisasterLink first-run check',
      'Server NOT reachable: The server could not be reached.',
    );
  });

  it('reports an error status of the server too', async () => {
    const alert = jest.fn(async () => undefined);

    await runFirstRunCheck(
      api(async () => {
        throw new ApiError(500, 'The service is unavailable.');
      }),
      'CIT-KLT-0001',
      alert,
    );

    expect(alert).toHaveBeenCalledWith(
      'DisasterLink first-run check',
      'Server NOT reachable: HTTP 500: The service is unavailable.',
    );
  });

  it('reports a non-Error failure as text', async () => {
    const alert = jest.fn(async () => undefined);

    await runFirstRunCheck(
      api(async () => {
        throw 'boom';
      }),
      'CIT-KLT-0001',
      alert,
    );

    expect(alert).toHaveBeenCalledWith('DisasterLink first-run check', 'Server NOT reachable: boom');
  });
});
