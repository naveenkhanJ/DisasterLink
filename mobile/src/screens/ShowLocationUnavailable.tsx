/**
 * ShowLocationUnavailable.tsx — `showLocationUnavailable()`: the notice that there is no GPS fix
 * ("GPS location is unavailable", or the S03g notice when the cause is known), "Enter location
 * manually", and then a map with a pin over its centre and a place search above it. "Use this
 * location" sends the map's centre. The coordinate fields are a fallback: "Enter coordinates"
 * appears only when the phone is offline, a search failed with an error, or the search cannot be
 * used (Android without the location permission). A point outside Sri Lanka is refused with its
 * error under the map (or under the fields) and the typed values stay (SC2 A1 12a to 12d, E3 12e to
 * 12f; WF2P-S03a and S03b; standards §10 "Manual location", P206).
 *
 * Implements CH-27 (FD-12).
 */
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import MapView from 'react-native-maps';
import type { GeoPoint } from '../api/reportApi';
import { Banner, Button, ErrorText, Icon, TextField, styles as ui } from '../components/ui';
import { colors, fonts, radius, space } from '../theme';
import { locationErrorText, NUMBERS_REQUIRED_TEXT, parseCoordinate } from './texts';

/** How much of the map is visible when it opens on the district centre, in degrees. */
const START_DELTA = 0.3;

/** How much of the map is visible after a place search, in degrees. */
const FOUND_DELTA = 0.01;

/** The height of the map, in pixels (frame S03a: 358 × 260). */
const MAP_HEIGHT = 260;

/** The size of the pin drawn over the centre of the map. */
const PIN_SIZE = 32;

/** The message when a search finds nothing. */
const NO_PLACE_TEXT = 'No place found — try another name or move the pin';

/** The message when a search fails with an error. */
const SEARCH_FAILED_TEXT = 'The place search is not available — move the pin, or enter coordinates';

/** The part of the map that is visible. */
interface Region {
  latitude: number;
  longitude: number;
  latitudeDelta: number;
  longitudeDelta: number;
}

/** What the manual-entry block needs. */
export interface ShowLocationUnavailableProps {
  /** The error of the last entry or review, worded for the fields. */
  error: string | undefined;
  /** `enterLocationManually`: takes the chosen point. */
  onEnter: (location: GeoPoint) => void;
  /** Whether the block replaces a missing fix (the notice is drawn) or is offered under a fix. */
  noFix?: boolean;
  /** The notice that replaces "GPS location is unavailable" when the cause is known (S03g). */
  accessNotice?: React.ReactNode;
  /** Where the map starts: the centre of the registered district. */
  start: GeoPoint;
  /** Finds a place by name; throws on an error. */
  searchPlace: (query: string) => Promise<GeoPoint | undefined>;
  /** Whether the phone is online. */
  isOnline: () => Promise<boolean>;
  /** Whether the place search may be used (false on Android without the permission). */
  searchAllowed: boolean;
}

function regionAround(point: GeoPoint, delta: number): Region {
  return {
    latitude: point.latitude,
    longitude: point.longitude,
    latitudeDelta: delta,
    longitudeDelta: delta,
  };
}

/**
 * Draws the manual-entry block.
 *
 * @param props see {@link ShowLocationUnavailableProps}
 * @returns the block
 */
export function ShowLocationUnavailable(props: ShowLocationUnavailableProps) {
  const { error, onEnter, noFix = true, start, searchAllowed, isOnline } = props;
  const [open, setOpen] = useState(false);
  const [region, setRegion] = useState<Region>(() => regionAround(start, START_DELTA));
  const [query, setQuery] = useState('');
  const [searchMessage, setSearchMessage] = useState<string>();
  const [searchFailed, setSearchFailed] = useState(false);
  const [offline, setOffline] = useState(false);
  const [fieldsShown, setFieldsShown] = useState(false);
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [notNumbers, setNotNumbers] = useState(false);
  const [viaPin, setViaPin] = useState(true);

  useEffect(() => {
    if (open) {
      void isOnline().then((online) => setOffline(!online));
    }
  }, [open, isOnline]);

  async function search(): Promise<void> {
    try {
      const found = await props.searchPlace(query);
      if (found) {
        setSearchMessage(undefined);
        setRegion(regionAround(found, FOUND_DELTA));
      } else {
        setSearchMessage(NO_PLACE_TEXT);
      }
    } catch (searchError) {
      console.warn('[ShowLocationUnavailable] the place search failed:', searchError);
      setSearchFailed(true);
      setSearchMessage(SEARCH_FAILED_TEXT);
    }
  }

  function enterPin(): void {
    setViaPin(true);
    setNotNumbers(false);
    onEnter({ latitude: region.latitude, longitude: region.longitude });
  }

  function enterFields(): void {
    setViaPin(false);
    const typedLatitude = parseCoordinate(latitude);
    const typedLongitude = parseCoordinate(longitude);
    if (typedLatitude === undefined || typedLongitude === undefined) {
      setNotNumbers(true);
      return;
    }
    setNotNumbers(false);
    onEnter({ latitude: typedLatitude, longitude: typedLongitude });
  }

  const locationError = error ? locationErrorText(error, viaPin) : undefined;
  const fieldsError = notNumbers ? NUMBERS_REQUIRED_TEXT : viaPin ? undefined : locationError;
  const mapError = viaPin ? locationError : undefined;
  const linkShown = (offline || searchFailed || !searchAllowed) && !fieldsShown;

  return (
    <View style={{ gap: space.sm }}>
      {props.accessNotice ??
        (noFix ? (
          <Banner kind="info" icon="pin">
            <Text
              style={{ fontFamily: fonts.ui, fontSize: 14, fontWeight: '700', color: colors.text }}
            >
              GPS location is unavailable
            </Text>
          </Banner>
        ) : null)}
      {open ? null : (
        <>
          {locationError ? <ErrorText>{locationError}</ErrorText> : null}
          <Button label="Enter location manually" kind="secondary" onPress={() => setOpen(true)} />
        </>
      )}
      {open ? (
        <>
          {searchAllowed ? (
            <View style={{ flexDirection: 'row', gap: space.sm, alignItems: 'flex-end' }}>
              <TextField
                label="Search for a place"
                value={query}
                onChangeText={setQuery}
                returnKeyType="search"
                onSubmitEditing={() => void search()}
              />
              <Button label="Search" kind="secondary" onPress={() => void search()} />
            </View>
          ) : null}
          {searchMessage ? <ErrorText>{searchMessage}</ErrorText> : null}
          <View style={styles.mapBox}>
            <MapView
              style={styles.map}
              region={region}
              onRegionChangeComplete={setRegion}
              pitchEnabled={false}
              rotateEnabled={false}
            />
            <View pointerEvents="none" style={styles.pin}>
              <Icon name="pin" size={PIN_SIZE} color={colors.errorText} />
            </View>
          </View>
          <Text style={ui.muted}>Drag the map to put the pin on the spot you saw</Text>
          {mapError ? <ErrorText>{mapError}</ErrorText> : null}
          <Button label="Use this location" onPress={enterPin} />
          {linkShown ? (
            <Button
              label="Enter coordinates"
              kind="secondary"
              onPress={() => setFieldsShown(true)}
            />
          ) : null}
          {fieldsShown ? (
            <>
              <View style={{ flexDirection: 'row', gap: space.sm }}>
                <TextField
                  label="Latitude (°N)"
                  value={latitude}
                  onChangeText={setLatitude}
                  keyboardType="numbers-and-punctuation"
                  error={Boolean(fieldsError)}
                />
                <TextField
                  label="Longitude (°E)"
                  value={longitude}
                  onChangeText={setLongitude}
                  keyboardType="numbers-and-punctuation"
                  error={Boolean(fieldsError)}
                />
              </View>
              <Text style={ui.muted}>Type the coordinates of the place you saw</Text>
              {fieldsError ? <ErrorText>{fieldsError}</ErrorText> : null}
              <Button label="Use this location" kind="secondary" onPress={enterFields} />
            </>
          ) : null}
        </>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  mapBox: {
    height: MAP_HEIGHT,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: radius.sm,
    overflow: 'hidden',
  },
  map: { flex: 1 },
  pin: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    marginTop: -PIN_SIZE,
    marginLeft: -PIN_SIZE / 2,
  },
});
