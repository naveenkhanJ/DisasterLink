/**
 * ShowLocation.tsx — `showLocation(location)`: the report's location as text, "6.5888 N, 79.9640 E"
 * with "GPS · accuracy ±8 m" or "Entered manually"; no map on the phone (standards section 10)
 * (SC2 step 14, A1 12d; WF2P-S03).
 *
 * Implements CH-27 (FD-12).
 */
import React from 'react';
import { Text, View } from 'react-native';
import type { GeoPoint } from '../api/reportApi';
import { Icon, styles as ui } from '../components/ui';
import { screenStyles as s } from './Screen';
import { formatCoordinates, locationSource } from './texts';

/**
 * Draws the location block.
 *
 * @param props the location and whether it is a GPS fix
 * @returns the block
 */
export function ShowLocation({ location, fromGps }: { location: GeoPoint; fromGps: boolean }) {
  return (
    <View style={s.rowCenter}>
      <Icon name="pin" size={22} />
      <View>
        <Text style={ui.label}>Location *</Text>
        <Text style={s.strong}>{formatCoordinates(location)}</Text>
        <Text style={ui.muted}>{locationSource(location, fromGps)}</Text>
      </View>
    </View>
  );
}
