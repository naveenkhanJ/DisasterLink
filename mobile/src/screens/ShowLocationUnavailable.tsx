/**
 * ShowLocationUnavailable.tsx — `showLocationUnavailable()`: the notice that there is no GPS fix
 * ("GPS location is unavailable", or the S03g notice when the cause is known), "Enter location
 * manually", and then a field "Type a town or village" whose type-ahead list (from the bundled
 * GeoNames places, so it works offline) sits directly under it, above a map with a pin over its
 * centre. Choosing a place moves the map there; the citizen drags the map to the exact spot and
 * "Use this location" sends the map's centre. A point outside Sri Lanka is refused with its error
 * under the map (SC2 A1 12a to 12d, E3 12e to 12f; WF2P-S03a and S03b; standards §10 "Manual
 * location", P213). The same block, opened at once, is the "Change" of a manual location, with "Keep
 * this location" to close it.
 *
 * There are no coordinate fields and no online geocoder (P213). Place names: GeoNames (CC BY 4.0).
 *
 * Implements CH-27 (FD-12).
 */
import React, { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import MapView from 'react-native-maps';
import type { GeoPoint } from '../api/reportApi';
import { Banner, Button, ErrorText, Icon, TextField, styles as ui } from '../components/ui';
import { MANUAL_PLACE_RADIUS_KM } from '../constants';
import { distanceKm, searchPlaces, type PlaceMatch } from '../location/placeIndex';
import { colors, fonts, radius, space } from '../theme';
import { KEEP_LOCATION_LABEL, PLACE_CREDIT, PLACE_FIELD_LABEL, placeRowText } from './texts';

/** How much of the map is visible when it opens on the district centre, in degrees. */
const START_DELTA = 0.3;

/** How much of the map is visible after a place was chosen, in degrees. */
const FOUND_DELTA = 0.02;

/** The height of the map, in pixels (frame S03a: 358 × 260). */
const MAP_HEIGHT = 260;

/** The size of the pin drawn over the centre of the map. */
const PIN_SIZE = 32;

/** The least height of a row of the place list, in points (the touch-target rule). */
const ROW_HEIGHT = 44;

/** The part of the map that is visible. */
interface Region {
  latitude: number;
  longitude: number;
  latitudeDelta: number;
  longitudeDelta: number;
}

/** What the manual-entry block needs. */
export interface ShowLocationUnavailableProps {
  /** The error of the last entry or review. */
  error: string | undefined;
  /** `enterLocationManually`: takes the chosen point. */
  onEnter: (location: GeoPoint) => void;
  /** Tells the screen which place the point is named after, or `undefined` when none (before `onEnter`). */
  onPlaceNamed: (place: string | undefined) => void;
  /** Whether the block replaces a missing fix (the notice is drawn) or is offered under a location. */
  noFix?: boolean;
  /** The notice that replaces "GPS location is unavailable" when the cause is known (S03g). */
  accessNotice?: React.ReactNode;
  /** Where the map starts: the registered district's centre, or the point being changed. */
  start: GeoPoint;
  /** Opens the picker at once (the "Change" of a manual location). */
  initiallyOpen?: boolean;
  /** Tells the form whether the picker is open (it shows the address field while it is). */
  onOpenChange?: (open: boolean) => void;
  /** Closes the picker and keeps the location (the "Keep this location" link; only for "Change"). */
  onKeep?: () => void;
}

function regionAround(point: GeoPoint, delta: number): Region {
  return {
    latitude: point.latitude,
    longitude: point.longitude,
    latitudeDelta: delta,
    longitudeDelta: delta,
  };
}

/** The inline list of up to eight suggestions under the place field. */
function Suggestions({
  matches,
  onChoose,
}: {
  matches: PlaceMatch[];
  onChoose: (match: PlaceMatch) => void;
}) {
  if (matches.length === 0) {
    return null;
  }
  return (
    <View style={styles.list}>
      {matches.map((match) => (
        <Pressable
          key={`${match.name}|${match.point.latitude}|${match.point.longitude}`}
          accessibilityRole="button"
          accessibilityLabel={placeRowText(match.name, match.district)}
          onPress={() => onChoose(match)}
          style={styles.row}
        >
          <Text style={styles.rowText}>{placeRowText(match.name, match.district)}</Text>
        </Pressable>
      ))}
    </View>
  );
}

/**
 * Draws the manual-entry block.
 *
 * @param props see {@link ShowLocationUnavailableProps}
 * @returns the block
 */
export function ShowLocationUnavailable(props: ShowLocationUnavailableProps) {
  const { error, onEnter, onPlaceNamed, noFix = true, start, onKeep } = props;
  const [open, setOpen] = useState(props.initiallyOpen ?? false);
  const [region, setRegion] = useState<Region>(() =>
    regionAround(start, props.initiallyOpen ? FOUND_DELTA : START_DELTA),
  );
  const [query, setQuery] = useState('');
  const [chosen, setChosen] = useState<PlaceMatch>();
  const matches = useMemo(() => (chosen ? [] : searchPlaces(query)), [query, chosen]);

  function openPicker(): void {
    setOpen(true);
    props.onOpenChange?.(true);
  }

  function choose(match: PlaceMatch): void {
    setChosen(match);
    setQuery(match.name);
    setRegion(regionAround(match.point, FOUND_DELTA));
  }

  function change(text: string): void {
    setQuery(text);
    setChosen(undefined);
  }

  function usePlace(): void {
    const centre = { latitude: region.latitude, longitude: region.longitude };
    const near = chosen !== undefined && distanceKm(chosen.point, centre) <= MANUAL_PLACE_RADIUS_KM;
    onPlaceNamed(near ? chosen.name : undefined);
    onEnter(centre);
  }

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
          {error ? <ErrorText>{error}</ErrorText> : null}
          <Button label="Enter location manually" kind="secondary" onPress={openPicker} />
        </>
      )}
      {open ? (
        <>
          <TextField
            label={PLACE_FIELD_LABEL}
            value={query}
            onChangeText={change}
            returnKeyType="search"
          />
          <Suggestions matches={matches} onChoose={choose} />
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
          {error ? <ErrorText>{error}</ErrorText> : null}
          <Text style={styles.credit}>{PLACE_CREDIT}</Text>
          <Button label="Use this location" onPress={usePlace} />
          {onKeep ? (
            <Pressable accessibilityRole="button" onPress={onKeep} style={styles.keep}>
              <Text style={styles.keepText}>{KEEP_LOCATION_LABEL}</Text>
            </Pressable>
          ) : null}
        </>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: radius.sm,
    backgroundColor: colors.card,
    overflow: 'hidden',
  },
  row: {
    minHeight: ROW_HEIGHT,
    justifyContent: 'center',
    paddingHorizontal: space.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  rowText: { fontFamily: fonts.ui, fontSize: 15, color: colors.text },
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
  credit: { fontFamily: fonts.ui, fontSize: 11, color: colors.muted },
  keep: { minHeight: ROW_HEIGHT, justifyContent: 'center', alignItems: 'center' },
  keepText: {
    fontFamily: fonts.ui,
    fontSize: 14,
    fontWeight: '700',
    textDecorationLine: 'underline',
    color: colors.text,
  },
});
