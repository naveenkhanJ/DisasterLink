/**
 * PhotoService.ts — takes or chooses the report's photo and keeps a copy of it (implementation-only;
 * SC2 steps 10 and 11; FD-10). The picker options are exactly the ones verified on the iPhone
 * (P188): images only, quality 0.7, no EXIF, the Compatible representation (a HEIC photo arrives as
 * JPEG). The picked file is copied into the app's document directory as `<draftId>.jpg|.png`, so a
 * queued draft keeps its photo; the copy is made with the current `expo-file-system` API
 * (`File`, `Paths`), not `expo-file-system/legacy` (§10).
 */
import { File, Paths } from 'expo-file-system';
import * as ImagePicker from 'expo-image-picker';
import { PHOTO_QUALITY } from '../constants';

/**
 * What taking or choosing a photo came to (implementation-only): the copy of the photo, a cancel by
 * the citizen, or a refused permission. The screen needs to tell the last two apart: a refused
 * camera permission shows "Allow camera access to take the required photo" (WF2P-S03d, SC2 E2
 * 11d), a cancel shows nothing.
 */
export type PhotoResult =
  { kind: 'photo'; uri: string } | { kind: 'cancelled' } | { kind: 'permissionDenied' };

/** The calls the screens make to get and drop a photo. */
export interface PhotoService {
  /**
   * Takes a photo with the camera (asks for the permission first).
   *
   * @param draftId the draft the photo belongs to; names the copy
   * @returns the copy's URI, or that the citizen cancelled, or that the permission was refused
   */
  takePhoto(draftId: string): Promise<PhotoResult>;
  /**
   * Chooses a photo from the gallery (asks for the permission first).
   *
   * @param draftId the draft the photo belongs to; names the copy
   * @returns the copy's URI, or that the citizen cancelled, or that the permission was refused
   */
  choosePhoto(draftId: string): Promise<PhotoResult>;
  /**
   * Deletes the copy of a photo (a discarded draft, or one that was accepted).
   *
   * @param uri the URI of the copy; one that does not exist is ignored
   */
  deletePhoto(uri: string): void;
}

const PICKER_OPTIONS: ImagePicker.ImagePickerOptions = {
  mediaTypes: ['images'],
  quality: PHOTO_QUALITY,
  exif: false,
  preferredAssetRepresentationMode:
    ImagePicker.UIImagePickerPreferredAssetRepresentationMode.Compatible,
};

/** Copies the picked file next to the app's documents as `<draftId>.<jpg|png>` and gives its URI. */
async function copyIntoDocuments(
  asset: ImagePicker.ImagePickerAsset,
  draftId: string,
): Promise<string> {
  const extension = asset.mimeType === 'image/png' ? 'png' : 'jpg';
  const copy = new File(Paths.document, `${draftId}.${extension}`);
  await new File(asset.uri).copy(copy, { overwrite: true });
  return copy.uri;
}

/**
 * Builds the photo service over `expo-image-picker` and `expo-file-system`.
 *
 * @returns the service
 */
export function createPhotoService(): PhotoService {
  async function pick(
    permission: () => Promise<ImagePicker.PermissionResponse>,
    launch: () => Promise<ImagePicker.ImagePickerResult>,
    draftId: string,
  ): Promise<PhotoResult> {
    if (!(await permission()).granted) {
      return { kind: 'permissionDenied' };
    }
    const picked = await launch();
    if (picked.canceled || picked.assets.length === 0) {
      return { kind: 'cancelled' };
    }
    return { kind: 'photo', uri: await copyIntoDocuments(picked.assets[0], draftId) };
  }

  return {
    takePhoto: (draftId) =>
      pick(
        ImagePicker.requestCameraPermissionsAsync,
        () => ImagePicker.launchCameraAsync(PICKER_OPTIONS),
        draftId,
      ),
    choosePhoto: (draftId) =>
      pick(
        ImagePicker.requestMediaLibraryPermissionsAsync,
        () => ImagePicker.launchImageLibraryAsync(PICKER_OPTIONS),
        draftId,
      ),
    deletePhoto(uri) {
      const file = new File(uri);
      if (file.exists) {
        file.delete();
      }
    },
  };
}
