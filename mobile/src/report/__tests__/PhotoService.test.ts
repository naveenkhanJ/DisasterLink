import * as ImagePicker from 'expo-image-picker';
import { createPhotoService } from '../PhotoService';

interface MockFile {
  uri: string;
  exists: boolean;
  copy: jest.Mock;
  delete: jest.Mock;
}

const mockFiles: MockFile[] = [];
let mockExists = true;

jest.mock('expo-file-system', () => {
  class MockFileClass {
    uri: string;
    exists = mockExists;
    copy = jest.fn(async () => undefined);
    delete = jest.fn();

    constructor(...parts: unknown[]) {
      this.uri = parts
        .map((part) => (typeof part === 'string' ? part : (part as { uri: string }).uri))
        .join('/');
      mockFiles.push(this);
    }
  }
  return { File: MockFileClass, Paths: { document: { uri: 'file:///documents' } } };
});

jest.mock('expo-image-picker', () => ({
  requestCameraPermissionsAsync: jest.fn(),
  requestMediaLibraryPermissionsAsync: jest.fn(),
  launchCameraAsync: jest.fn(),
  launchImageLibraryAsync: jest.fn(),
  UIImagePickerPreferredAssetRepresentationMode: { Compatible: 'compatible' },
}));

const picker = ImagePicker as jest.Mocked<typeof ImagePicker>;

const VERIFIED_OPTIONS = {
  mediaTypes: ['images'],
  quality: 0.7,
  exif: false,
  preferredAssetRepresentationMode: 'compatible',
};

const NOW = 1_760_000_000_000;

beforeEach(() => {
  jest.clearAllMocks();
  jest.spyOn(Date, 'now').mockReturnValue(NOW);
  mockFiles.length = 0;
  mockExists = true;
});

describe('takePhoto', () => {
  it('SC2 step 10: takes a photo with the options verified on the iPhone and copies it as <draftId>-<time>.jpg', async () => {
    picker.requestCameraPermissionsAsync.mockResolvedValue({ granted: true } as never);
    picker.launchCameraAsync.mockResolvedValue({
      canceled: false,
      assets: [{ uri: 'file:///cache/picked.jpg', mimeType: 'image/jpeg' }],
    } as never);

    const uri = await createPhotoService().takePhoto('draft-1');

    expect(uri).toEqual({ kind: 'photo', uri: `file:///documents/draft-1-${NOW}.jpg` });
    expect(picker.launchCameraAsync).toHaveBeenCalledWith(VERIFIED_OPTIONS);
    const [destination, source] = mockFiles;
    expect(source.uri).toBe('file:///cache/picked.jpg');
    expect(source.copy).toHaveBeenCalledWith(destination, { overwrite: true });
  });

  it('SC2 E2 11d: a refused camera permission gives no photo and opens no camera', async () => {
    picker.requestCameraPermissionsAsync.mockResolvedValue({ granted: false } as never);

    expect(await createPhotoService().takePhoto('draft-1')).toEqual({ kind: 'permissionDenied' });
    expect(picker.launchCameraAsync).not.toHaveBeenCalled();
  });

  it('SC2 step 10: a cancelled camera gives no photo and copies nothing', async () => {
    picker.requestCameraPermissionsAsync.mockResolvedValue({ granted: true } as never);
    picker.launchCameraAsync.mockResolvedValue({ canceled: true, assets: null } as never);

    expect(await createPhotoService().takePhoto('draft-1')).toEqual({ kind: 'cancelled' });
    expect(mockFiles).toHaveLength(0);
  });

  it('SC2 step 10: a result without an asset gives no photo', async () => {
    picker.requestCameraPermissionsAsync.mockResolvedValue({ granted: true } as never);
    picker.launchCameraAsync.mockResolvedValue({ canceled: false, assets: [] } as never);

    expect(await createPhotoService().takePhoto('draft-1')).toEqual({ kind: 'cancelled' });
  });
});

describe('choosePhoto', () => {
  it('SC2 step 10: chooses from the gallery and keeps a PNG as .png', async () => {
    picker.requestMediaLibraryPermissionsAsync.mockResolvedValue({ granted: true } as never);
    picker.launchImageLibraryAsync.mockResolvedValue({
      canceled: false,
      assets: [{ uri: 'file:///cache/library.png', mimeType: 'image/png' }],
    } as never);

    const uri = await createPhotoService().choosePhoto('draft-2');

    expect(uri).toEqual({ kind: 'photo', uri: `file:///documents/draft-2-${NOW}.png` });
    expect(picker.launchImageLibraryAsync).toHaveBeenCalledWith(VERIFIED_OPTIONS);
  });

  it('SC2 E2 11d: a refused library permission gives no photo', async () => {
    picker.requestMediaLibraryPermissionsAsync.mockResolvedValue({ granted: false } as never);

    expect(await createPhotoService().choosePhoto('draft-2')).toEqual({ kind: 'permissionDenied' });
    expect(picker.launchImageLibraryAsync).not.toHaveBeenCalled();
  });
});

describe('deletePhoto', () => {
  it('SC2 E8 18o: deletes the copy of a discarded draft', () => {
    createPhotoService().deletePhoto('file:///documents/draft-1.jpg');

    expect(mockFiles[0].delete).toHaveBeenCalledTimes(1);
  });

  it('ignores a copy that does not exist any more', () => {
    mockExists = false;

    createPhotoService().deletePhoto('file:///documents/gone.jpg');

    expect(mockFiles[0].delete).not.toHaveBeenCalled();
  });
});

describe('a second pick for the same draft (standards §10 item 9, P213)', () => {
  it('SC2 step 10: each pick gets its own file name, so the screen shows the new picture', async () => {
    picker.requestCameraPermissionsAsync.mockResolvedValue({ granted: true } as never);
    picker.launchCameraAsync.mockResolvedValue({
      canceled: false,
      assets: [{ uri: 'file:///cache/picked.jpg', mimeType: 'image/jpeg' }],
    } as never);
    const service = createPhotoService();

    const first = await service.takePhoto('draft-1');
    jest.spyOn(Date, 'now').mockReturnValue(NOW + 1);
    const second = await service.takePhoto('draft-1');

    expect(first).toEqual({ kind: 'photo', uri: `file:///documents/draft-1-${NOW}.jpg` });
    expect(second).toEqual({ kind: 'photo', uri: `file:///documents/draft-1-${NOW + 1}.jpg` });
  });
});
