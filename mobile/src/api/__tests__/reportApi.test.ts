import type { HttpClient, JsonPart, MultipartPart } from '../httpClient';
import { createReportApi, photoFile, type ClientMeta, type ReportReceipt } from '../reportApi';
import type { ReportDraft } from '../../report/ReportDraft';

const META: ClientMeta = {
  clientId: 'client-1',
  userId: 'CIT-KLT-0001',
  submissionId: 'draft-1',
  capturedAt: '2026-05-21T07:40:00.000Z',
  pendingCount: 2,
};

const DRAFT: ReportDraft = {
  draftId: 'draft-1',
  hazardType: 'FLOOD',
  description: 'Water is rising',
  photoUri: 'file:///documents/draft-1.jpg',
  location: { latitude: 6.5888, longitude: 79.964, accuracyMetres: 9.5 },
  locationFromGps: true,
  district: 'KALUTARA',
  capturedAt: '2026-05-21T07:40:00.000Z',
  status: 'QUEUED',
  attempts: 3,
  rejectionReason: 'earlier reason',
};

function fakeClient(): jest.Mocked<HttpClient> {
  return {
    getJson: jest.fn(),
    postJson: jest.fn(),
    putJson: jest.fn(),
    postMultipart: jest.fn(),
  } as unknown as jest.Mocked<HttpClient>;
}

function jsonOf(part: MultipartPart): unknown {
  return JSON.parse((part.value as JsonPart).string);
}

describe('createReportApi', () => {
  it('SC2 step 18: uploads the draft, the meta and the photo as the three parts of POST /api/reports', async () => {
    const client = fakeClient();
    const receipt: ReportReceipt = {
      result: { submissionId: 'draft-1', outcome: 'ACCEPTED' },
      report: null,
    };
    client.postMultipart.mockResolvedValue(receipt);

    const result = await createReportApi(client).receiveReport(
      DRAFT,
      META,
      DRAFT.photoUri as string,
    );

    expect(result).toBe(receipt);
    const [path, parts] = client.postMultipart.mock.calls[0];
    expect(path).toBe('/api/reports');
    expect(parts.map((part) => part.name)).toEqual(['draft', 'meta', 'photo']);
    expect((parts[0].value as JsonPart).type).toBe('application/json');
    expect((parts[1].value as JsonPart).type).toBe('application/json');
    expect(jsonOf(parts[1])).toEqual(META);
    expect(parts[2].value).toEqual({
      uri: 'file:///documents/draft-1.jpg',
      name: 'draft-1.jpg',
      type: 'image/jpeg',
    });
  });

  it('SD2 9.1.2: the draft part holds only the Java ReportDraft members, no photo URI and no queue fields', async () => {
    const client = fakeClient();
    client.postMultipart.mockResolvedValue({ result: { submissionId: 'x', outcome: 'ACCEPTED' } });

    await createReportApi(client).receiveReport(DRAFT, META, 'file:///documents/draft-1.jpg');

    expect(jsonOf(client.postMultipart.mock.calls[0][1][0])).toEqual({
      draftId: 'draft-1',
      hazardType: 'FLOOD',
      description: 'Water is rising',
      location: { latitude: 6.5888, longitude: 79.964, accuracyMetres: 9.5 },
      locationFromGps: true,
      district: 'KALUTARA',
      capturedAt: '2026-05-21T07:40:00.000Z',
    });
  });

  it('SC2 step 11: a .png photo is sent as image/png, anything else as image/jpeg', () => {
    expect(photoFile('file:///a/b/RPT.PNG')).toEqual({ name: 'RPT.PNG', type: 'image/png' });
    expect(photoFile('file:///a/b/photo.jpeg')).toEqual({ name: 'photo.jpeg', type: 'image/jpeg' });
    expect(photoFile('plain.jpg')).toEqual({ name: 'plain.jpg', type: 'image/jpeg' });
  });

  it("SC2 step 30: reads a report and the citizen's reports with encoded identifiers", async () => {
    const client = fakeClient();
    client.getJson.mockResolvedValue([]);
    const api = createReportApi(client);

    await api.getReportStatus('RPT-2026-KLT-0847');
    await api.getReportsByReporter('CIT KLT/1');

    expect(client.getJson.mock.calls).toEqual([
      ['/api/reports/RPT-2026-KLT-0847'],
      ['/api/citizens/CIT%20KLT%2F1/reports'],
    ]);
  });

  it('SD2 2.1.1: changes the language with PUT {"language": …}', async () => {
    const client = fakeClient();
    client.putJson.mockResolvedValue(undefined);

    await createReportApi(client).changePreferredLanguage('CIT-KLT-0001', 'TAMIL');

    expect(client.putJson).toHaveBeenCalledWith('/api/citizens/CIT-KLT-0001/language', {
      language: 'TAMIL',
    });
  });
});
