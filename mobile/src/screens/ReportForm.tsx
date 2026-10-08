/**
 * ReportForm.tsx — the report form (a screen component, MD section 0.11 C2): hazard type, capture
 * time, description with its counter, the one photo, the location (`showLocation` or
 * `showLocationUnavailable`), the errors of the last review under their fields, and, for a
 * rejected draft reopened from the home list, the reason and "Discard draft". The form keeps every
 * value it holds on every error (C2).
 *
 * The photo tile is full width; a tap on it, or "View", opens a full-size preview with "Remove photo",
 * and "Remove" clears it; with a photo the buttons read "Retake photo" and "Choose another" (standards
 * §10 "Phone screens beyond the frames" item 9, P206, P213). The location block shows the S03g notice
 * when the cause of a missing fix is known and the place-list and map-pin picker when the citizen
 * enters the place, with "Change" on a manual location; under it the optional "Address or landmark"
 * (standards §10 "The address", P214). Content that appears in answer to the citizen is scrolled into
 * view: the address field, the S03g notice, and the first field with an error after "Review Report"
 * (standards §10 item 13, P222).
 *
 * Implements CH-25, CH-27 (SC2 steps 7 to 15, A1, A3, E1 to E3, E8; WF2P-S03, S03a to S03g).
 */
import React, { useEffect, useRef, useState } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import type { GeoPoint } from '../api/reportApi';
import type { PhotoSource } from '../app/useCitizenAppUI';
import type { LocationAccessState } from '../location/locationAccess';
import type { ReportDraft } from '../report/ReportDraft';
import {
  Banner,
  Button,
  ErrorText,
  Icon,
  ModalCard,
  PhotoTile,
  ScreenTitle,
  TextArea,
  TextField,
  styles as ui,
} from '../components/ui';
import { MAX_ADDRESS_LENGTH, MAX_DESCRIPTION_LENGTH } from '../constants';
import { colors, space } from '../theme';
import { Screen, screenStyles as s, useScrollIntoView, useScrollWhenShown } from './Screen';
import { CameraPermissionNotice, ShowPhotoError } from './ShowPhotoError';
import { ShowLocation } from './ShowLocation';
import { ShowLocationUnavailable } from './ShowLocationUnavailable';
import { hasLocationNotice, LocationAccessNotice } from './LocationAccessNotice';
import { ShowValidationErrors } from './ShowValidationErrors';
import {
  ADDRESS_FIELD_LABEL,
  formatDateTime,
  hasErrors,
  hazardTypeIcon,
  hazardTypeLabel,
  rejectionText,
  type FieldErrors,
} from './texts';

/** The height of the photo tile on the form, in pixels. */
const PHOTO_HEIGHT = 180;

/** The height of the photo in the full-size preview, in pixels. */
const PREVIEW_HEIGHT = 420;

/** What the report form needs. */
export interface ReportFormProps {
  draft: ReportDraft;
  errors: FieldErrors;
  locationUnavailable: boolean;
  permissionDenied: PhotoSource | undefined;
  onChangeHazardType: () => void;
  /** What the Description field shows; the stored description also holds the address line. */
  descriptionText: string;
  addressText: string;
  onEnterDescription: (text: string) => void;
  onEnterAddress: (text: string) => void;
  onPickPhoto: (source: PhotoSource) => void;
  onEnterLocation: (location: GeoPoint) => void;
  onRemovePhoto: () => void;
  /** Why there is no fix, when it is known (WF2P-S03g). */
  locationAccess: LocationAccessState | undefined;
  onAllowLocationAccess: () => void;
  /** Where the map starts: the registered district's centre. */
  mapStart: GeoPoint;
  /** The place the manual location is named after (P213). */
  manualPlaceName: string | undefined;
  onNameManualPlace: (place: string | undefined) => void;
  /** The picker was reopened by "Change" on a manual location. */
  changingLocation: boolean;
  onChangeManualLocation: () => void;
  onKeepManualLocation: () => void;
  onReview: () => void;
  onDiscardDraft: (draftId: string) => void;
}

function PhotoBlock({
  draft,
  error,
  permissionDenied,
  onPick,
  onRemove,
}: {
  draft: ReportDraft;
  error: string | undefined;
  permissionDenied: PhotoSource | undefined;
  onPick: (source: PhotoSource) => void;
  onRemove: () => void;
}) {
  const [previewing, setPreviewing] = useState(false);
  const attached = Boolean(draft.photoUri);
  return (
    <View>
      <Text style={ui.label}>Photo * (1 required)</Text>
      <View style={{ marginTop: 4, gap: 4 }}>
        <PhotoTile
          uri={draft.photoUri}
          width="100%"
          height={PHOTO_HEIGHT}
          error={Boolean(error)}
          onPress={() => setPreviewing(true)}
        />
        <Text style={ui.muted}>{attached ? '1 of 1' : '0 of 1'}</Text>
      </View>
      {attached ? (
        <View style={[s.rowCenter, { marginTop: space.xs }]}>
          <Button
            label="View"
            kind="secondary"
            style={{ flex: 1 }}
            onPress={() => setPreviewing(true)}
          />
          <Button label="Remove" kind="danger" style={{ flex: 1 }} onPress={onRemove} />
        </View>
      ) : null}
      <ModalCard visible={previewing} onRequestClose={() => setPreviewing(false)}>
        <Image
          accessibilityLabel="Photo preview"
          source={{ uri: draft.photoUri }}
          style={styles.preview}
          resizeMode="contain"
        />
        <Button
          label="Remove photo"
          kind="danger"
          onPress={() => {
            setPreviewing(false);
            onRemove();
          }}
        />
        <Button label="Close" kind="secondary" onPress={() => setPreviewing(false)} />
      </ModalCard>
      {error ? <ShowPhotoError message={error} /> : null}
      {permissionDenied ? (
        <CameraPermissionNotice
          source={permissionDenied}
          onChooseFromGallery={() => onPick('gallery')}
        />
      ) : null}
      <View style={[s.rowCenter, { marginTop: space.sm }]}>
        <Button
          label={attached ? 'Retake photo' : 'Take photo'}
          kind="secondary"
          icon="camera"
          style={{ flex: 1, paddingHorizontal: 8 }}
          onPress={() => onPick('camera')}
        />
        <Button
          label={attached ? 'Choose another' : 'Choose from gallery'}
          kind="secondary"
          icon="image"
          style={{ flex: 1, paddingHorizontal: 8 }}
          onPress={() => onPick('gallery')}
        />
      </View>
      {attached ? (
        <Text style={[ui.muted, { marginTop: 4 }]}>A new photo replaces this one</Text>
      ) : null}
    </View>
  );
}

/** The content of the form; it sits inside the `Screen`, so it can use the screen's `scrollIntoView`. */
function ReportFormContent(props: ReportFormProps) {
  const { draft, errors } = props;
  const manual = draft.location !== undefined && !draft.locationFromGps;
  const [pickerOpen, setPickerOpen] = useState(false);
  const picker = (extra: {
    noFix?: boolean;
    accessNotice?: React.ReactNode;
    start?: GeoPoint;
    initiallyOpen?: boolean;
    onKeep?: () => void;
  }) => (
    <ShowLocationUnavailable
      error={errors.location}
      onEnter={props.onEnterLocation}
      onPlaceNamed={props.onNameManualPlace}
      onOpenChange={setPickerOpen}
      start={props.mapStart}
      {...extra}
    />
  );
  const addressShown = manual || pickerOpen || props.changingLocation || props.addressText !== '';
  const noticeShown = props.locationUnavailable && hasLocationNotice(props.locationAccess);
  const scrollIntoView = useScrollIntoView();
  const hazardRef = useRef<View>(null);
  const descriptionRef = useRef<View>(null);
  const photoRef = useRef<View>(null);
  const locationRef = useRef<View>(null);
  const addressRef = useRef<View>(null);
  useScrollWhenShown(addressShown, addressRef);
  useScrollWhenShown(noticeShown, locationRef);
  // A review gives a new `other` list (a photo or location edit keeps the old one), so only a review
  // scrolls to the first field with an error.
  const seenReview = useRef(errors.other);
  useEffect(() => {
    if (errors.other === seenReview.current) {
      return;
    }
    seenReview.current = errors.other;
    if (!hasErrors(errors)) {
      return;
    }
    const first = errors.hazardType
      ? hazardRef
      : errors.description
        ? descriptionRef
        : errors.photo
          ? photoRef
          : locationRef;
    scrollIntoView(first);
  }, [errors, scrollIntoView]);
  return (
    <>
      <ScreenTitle title="Report a hazard" />
      {draft.status === 'REJECTED' ? (
        <Banner kind="error" icon="error">
          <Text style={[s.strong, { color: colors.errorText }]}>
            {`Not accepted by the DMC: ${rejectionText(draft.rejectionReason)}`}
          </Text>
        </Banner>
      ) : null}
      <View ref={hazardRef} testID="hazard-row" collapsable={false}>
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
      <View ref={descriptionRef} testID="description-field" collapsable={false}>
        <TextArea
          label="Description *"
          value={props.descriptionText}
          onChangeText={props.onEnterDescription}
          counter={`${(draft.description ?? '').length} / ${MAX_DESCRIPTION_LENGTH}`}
          error={errors.description}
        />
      </View>
      <View ref={photoRef} testID="photo-block" collapsable={false}>
        <PhotoBlock
          draft={draft}
          error={errors.photo}
          permissionDenied={props.permissionDenied}
          onPick={props.onPickPhoto}
          onRemove={props.onRemovePhoto}
        />
      </View>
      <View ref={locationRef} testID="location-block" collapsable={false} style={{ gap: space.sm }}>
        {draft.location ? (
          <>
            <ShowLocation
              location={draft.location}
              fromGps={draft.locationFromGps}
              placeName={props.manualPlaceName}
              onChange={props.changingLocation ? undefined : props.onChangeManualLocation}
            />
            {props.changingLocation && manual
              ? picker({
                  noFix: false,
                  start: draft.location,
                  initiallyOpen: true,
                  onKeep: props.onKeepManualLocation,
                })
              : errors.location
                ? picker({ noFix: false })
                : null}
          </>
        ) : props.locationUnavailable ? (
          picker({
            accessNotice: hasLocationNotice(props.locationAccess) ? (
              <LocationAccessNotice
                state={props.locationAccess}
                onAllow={props.onAllowLocationAccess}
              />
            ) : undefined,
          })
        ) : (
          <Text style={ui.muted}>Finding your location…</Text>
        )}
      </View>
      {addressShown ? (
        <View ref={addressRef} testID="address-field" collapsable={false}>
          <TextField
            label={ADDRESS_FIELD_LABEL}
            value={props.addressText}
            onChangeText={props.onEnterAddress}
            maxLength={MAX_ADDRESS_LENGTH}
          />
        </View>
      ) : null}
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
    </>
  );
}

/**
 * Draws the report form.
 *
 * @param props the draft, its errors and the handlers
 * @returns the screen
 */
export function ReportForm(props: ReportFormProps) {
  return (
    <Screen>
      <ReportFormContent {...props} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  preview: { width: '100%', height: PREVIEW_HEIGHT },
  typeRow: {
    height: 44,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
});
