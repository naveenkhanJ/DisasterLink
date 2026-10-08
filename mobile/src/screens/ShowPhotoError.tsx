/**
 * ShowPhotoError.tsx — `showPhotoError(message)`: the error line under the photo block, fed by the
 * "photo:"-prefixed errors of `validateDraft` (SC2 E2 11a to 11c; WF2P-S03c). A refused camera
 * permission is not a dictionary display: `CameraPermissionNotice` is the app screen's own handling
 * of it (E2 11d; WF2P-S03d, MD N10).
 *
 * Implements CH-27 (FD-12).
 */
import React from 'react';
import { Linking, Text, View } from 'react-native';
import { Banner, Button, ErrorText } from '../components/ui';
import { colors, fonts, space } from '../theme';
import type { PhotoSource } from '../app/useCitizenAppUI';

/**
 * Draws the photo error.
 *
 * @param props the message
 * @returns the error line
 */
export function ShowPhotoError({ message }: { message: string }) {
  return <ErrorText>{message}</ErrorText>;
}

/** The notice for a refused permission, with the way to allow it and the gallery as the other way. */
export function CameraPermissionNotice({
  source,
  onChooseFromGallery,
}: {
  source: PhotoSource;
  onChooseFromGallery: () => void;
}) {
  const camera = source === 'camera';
  return (
    <View style={{ gap: space.sm }}>
      <Banner kind="info" icon="camera">
        <Text style={{ fontFamily: fonts.ui, fontSize: 14, fontWeight: '700', color: colors.text }}>
          {camera
            ? 'Allow camera access to take the required photo'
            : 'Allow photo access to choose the required photo'}
        </Text>
      </Banner>
      <View style={{ flexDirection: 'row', gap: space.sm }}>
        <Button
          label={camera ? 'Allow camera access' : 'Allow photo access'}
          kind="secondary"
          style={{ flex: 1, paddingHorizontal: 8 }}
          onPress={() => void Linking.openSettings()}
        />
        {camera ? (
          <Button
            label="Choose from gallery"
            kind="secondary"
            style={{ flex: 1, paddingHorizontal: 8 }}
            onPress={onChooseFromGallery}
          />
        ) : null}
      </View>
    </View>
  );
}
