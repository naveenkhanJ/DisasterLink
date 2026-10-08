/**
 * ShowLocation.tsx — `showLocation(location)`: the report's location as text, "6.5888 N, 79.9640 E"
 * with "GPS · accuracy ±8 m" or "Entered manually"; no map beside it (standards section 10). A manual
 * location chosen near a named place reads "near <place>, <lat> N, <lon> E — Entered manually", and a
 * manual location has a "Change" link that reopens the picker (SC2 step 14, A1 12d; WF2P-S03, S03a,
 * P213).
 *
 * Implements CH-27 (FD-12).
 */
import React from 'react';
import { Pressable, Text, View } from 'react-native';
import type { GeoPoint } from '../api/reportApi';
import { Icon, styles as ui } from '../components/ui';
import { screenStyles as s } from './Screen';
import {
  CHANGE_LOCATION_LABEL,
  formatCoordinates,
  locationSource,
  namedLocationText,
} from './texts';

/**
 * Draws the location block.
 *
 * @param props the location, whether it is a GPS fix, the place it is named after (if any) and, for
 *   a manual location, the handler of "Change"
 * @returns the block
 */
export function ShowLocation({
  location,
  fromGps,
  placeName,
  onChange,
}: {
  location: GeoPoint;
  fromGps: boolean;
  placeName?: string;
  onChange?: () => void;
}) {
  const named = !fromGps && placeName !== undefined;
  return (
    <View style={[s.rowCenter, { justifyContent: 'space-between' }]}>
      <View style={[s.rowCenter, { flex: 1 }]}>
        <Icon name="pin" size={22} />
        <View style={{ flex: 1 }}>
          <Text style={ui.label}>Location *</Text>
          {named ? (
            <Text style={s.strong}>{namedLocationText(placeName, location)}</Text>
          ) : (
            <>
              <Text style={s.strong}>{formatCoordinates(location)}</Text>
              <Text style={ui.muted}>{locationSource(location, fromGps)}</Text>
            </>
          )}
        </View>
      </View>
      {!fromGps && onChange ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Change location"
          hitSlop={8}
          onPress={onChange}
        >
          <Text style={s.link}>{CHANGE_LOCATION_LABEL}</Text>
        </Pressable>
      ) : null}
    </View>
  );
}
