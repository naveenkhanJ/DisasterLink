import type { HttpClient } from '../httpClient';
import { createSkeletonApi } from '../skeletonApi';

function fakeClient(): { client: HttpClient; getJson: jest.Mock; postJson: jest.Mock } {
  const getJson = jest.fn(async () => []);
  const postJson = jest.fn(async () => undefined);
  const client = {
    getJson,
    postJson,
    putJson: jest.fn(),
    postMultipart: jest.fn(),
  } as unknown as HttpClient;
  return { client, getJson, postJson };
}

describe('createSkeletonApi', () => {
  it('asks for the active warnings of a district', async () => {
    const { client, getJson } = fakeClient();

    await createSkeletonApi(client).getActiveWarnings('KALUTARA');

    expect(getJson).toHaveBeenCalledWith('/api/warnings/active?district=KALUTARA');
  });

  it('encodes the district in the query', async () => {
    const { client, getJson } = fakeClient();

    await createSkeletonApi(client).getActiveWarnings('NUWARA_ELIYA');

    expect(getJson).toHaveBeenCalledWith('/api/warnings/active?district=NUWARA_ELIYA');
  });

  it('returns what the client returns for the active warnings', async () => {
    const { client, getJson } = fakeClient();
    getJson.mockResolvedValueOnce([{ warningId: 'WRN-2026-0101' }]);

    const result = await createSkeletonApi(client).getActiveWarnings('RATNAPURA');

    expect(result).toEqual([{ warningId: 'WRN-2026-0101' }]);
  });

  it('polls the outbox of a recipient and encodes the identifier', async () => {
    const { client, getJson } = fakeClient();

    await createSkeletonApi(client).pollOutbox('CIT-KLT-0001');
    await createSkeletonApi(client).pollOutbox('a b/c');

    expect(getJson).toHaveBeenNthCalledWith(1, '/api/push/CIT-KLT-0001/outbox');
    expect(getJson).toHaveBeenNthCalledWith(2, '/api/push/a%20b%2Fc/outbox');
  });

  it('acknowledges entries with {"ids": [...]}', async () => {
    const { client, postJson } = fakeClient();

    await createSkeletonApi(client).acknowledge('CIT-KLT-0001', ['e-1', 'e-2']);

    expect(postJson).toHaveBeenCalledWith('/api/push/CIT-KLT-0001/outbox/ack', {
      ids: ['e-1', 'e-2'],
    });
  });
});
