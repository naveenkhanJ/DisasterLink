/**
 * ReportForm.tsx — the report form (a screen component, MD section 0.11 C2): hazard type, capture
 * time, description with its counter, the one photo, the location (`showLocation` or
 * `showLocationUnavailable`), the errors of the last review under their fields, and, for a
 * rejected draft reopened from the home list, the reason and "Discard draft". The form keeps every
 * value it holds on every error (C2).
 *
 * Implements CH-25, CH-27 (SC2 steps 7 to 15, A1, A3, E1 to E3, E8; WF2P-S03, S03a to S03f).
 */
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { GeoPoint } from '../api/reportApi';
import type { PhotoSource } from '../app/useCitizenAppUI';
import type { ReportDraft } from '../report/ReportDraft';
import {
  Banner,
  Button,
  ErrorText,
  Icon,
  PhotoTile,
  ScreenTitle,
  TextArea,
  styles as ui,
} from '../components/ui';
import { MAX_DESCRIPTION_LENGTH } from '../constants';
import { colors, space } from '../theme';
import { Screen, screenStyles as s } from './Screen';
import { CameraPermissionNotice, ShowPhotoError } from './ShowPhotoError';
import { ShowLocation } from './ShowLocation';
import { ShowLocationUnavailable } from './ShowLocationUnavailable';
import { ShowValidationErrors } from './ShowValidationErrors';
import {
  formatDateTime,
  hazardTypeIcon,
  hazardTypeLabel,
  rejectionText,
  type FieldErrors,
} from './texts';

/** What the report form needs. */
export interface ReportFormProps {
  draft: ReportDraft;
  errors: FieldErrors;
  locationUnavailable: boolean;
  permissionDenied: PhotoSource | undefined;
  onChangeHazardType: () => void;
  onEnterDescription: (text: string) => void;
  onPickPhoto: (source: PhotoSource) => void;
  onEnterLocation: (location: GeoPoint) => void;
  onReview: () => void;
  onDiscardDraft: (draftId: string) => void;
}

function PhotoBlock({
  draft,
  error,
  permissionDenied,
  onPick,
}: {
  draft: ReportDraft;
  error: string | undefined;
  permissionDenied: PhotoSource | undefined;
  onPick: (source: PhotoSource) => void;
}) {
  const replacing = draft.status === 'REJECTED' && Boolean(draft.photoUri);
  return (
    <View>
      <Text style={ui.label}>Photo * (1 required)</Text>
      <View style={[s.rowCenter, { alignItems: 'flex-end', marginTop: 4 }]}>
        <PhotoTile uri={draft.photoUri} width={150} height={96} error={Boolean(error)} />
        <Text style={ui.muted}>{draft.photoUri ? '1 of 1' : '0 of 1'}</Text>
      </View>
      {error ? <ShowPhotoError message={error} /> : null}
      {permissionDenied ? (
        <CameraPermissionNotice
          source={permissionDenied}
          onChooseFromGallery={() => onPick('gallery')}
        />
      ) : null}
      <View style={[s.rowCenter, { marginTop: space.sm }]}>
        <Button
          label="Take photo"
          kind="secondary"
          icon="camera"
          style={{ flex: 1, paddingHorizontal: 8 }}
          onPress={() => onPick('camera')}
        />
        <Button
          label={replacing ? 'Replace photo' : 'Choose from gallery'}
          kind="secondary"
          style={{ flex: 1.3, paddingHorizontal: 8 }}
          onPress={() => onPick('gallery')}
        />
      </View>
      <Text style={[ui.muted, { marginTop: 4 }]}>A new photo replaces this one</Text>
    </View>
  );
}

/**
 * Draws the report form.
 *
 * @param props the draft, its errors and the handlers
 * @returns the screen
 */
export function ReportForm(props: ReportFormProps) {
  const { draft, errors } = props;
  const description = draft.description ?? '';
  return (
    <Screen>
      <ScreenTitle title="Report a hazard" />
      {draft.status === 'REJECTED' ? (
        <Banner kind="error" icon="error">
          <Text style={[s.strong, { color: colors.errorText }]}>
            {`Not accepted by the DMC: ${rejectionText(draft.rejectionReason)}`}
          </Text>
        </Banner>
      ) : null}
      <View>
        <View style={styles.typeRow}>
          <View style={s.rowCenter}>
            <Text style={ui.label}>Hazard type</Text>
            <Icon name={hazardTypeIcon(draft.hazardType)} size={20} />
            <Text style={s.strong}>{hazardTypeLabel(draft.hazardType)}</Text>
          </View>
          <Pressable accessibilityRole="button" onPress={props.onChangeHazardType}>
            <Text style={s.link}>Change</Text>
          </Pressable>
        </View>
        {errors.hazardType ? <ErrorText>{errors.hazardType}</ErrorText> : null}
      </View>
      <Text style={ui.muted}>
        {`Capture time ${formatDateTime(draft.capturedAt)} — recorded automatically`}
      </Text>
      <TextArea
        label="Description *"
        value={description}
        onChangeText={props.onEnterDescription}
        counter={`${description.length} / ${MAX_DESCRIPTION_LENGTH}`}
        error={errors.description}
      />
      <PhotoBlock
        draft={draft}
        error={errors.photo}
        permissionDenied={props.permissionDenied}
        onPick={props.onPickPhoto}
      />
      {draft.location ? (
        <>
          <ShowLocation location={draft.location} fromGps={draft.locationFromGps} />
          {errors.location ? (
            <ShowLocationUnavailable
              error={errors.location}
              onEnter={props.onEnterLocation}
              noFix={false}
            />
          ) : null}
        </>
      ) : props.locationUnavailable ? (
        <ShowLocationUnavailable error={errors.location} onEnter={props.onEnterLocation} />
      ) : (
        <Text style={ui.muted}>Finding your location…</Text>
      )}
      {errors.location && !draft.location && !props.locationUnavailable ? (
        <ErrorText>{errors.location}</ErrorText>
      ) : null}
      <ShowValidationErrors errors={errors} />
      <Button label="Review Report" big onPress={props.onReview} />
      {draft.status === 'REJECTED' ? (
        <Button
          label="Discard draft"
          kind="danger"
          icon="cross"
          onPress={() => props.onDiscardDraft(draft.draftId)}
        />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  typeRow: {
    height: 44,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
});
