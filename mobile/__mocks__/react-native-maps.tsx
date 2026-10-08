/**
 * react-native-maps.tsx — the Jest stand-in for `react-native-maps` (implementation-only; standards §10, P206;
 * serves CH-27). `MapView` draws a `View` with `testID="map"` that carries its `region` and
 * `onRegionChangeComplete` props, so a test reads the region with `screen.getByTestId('map').props.region` and moves
 * the map with `fireEvent(screen.getByTestId('map'), 'regionChangeComplete', region)`.
 */
import React from 'react';
import { View } from 'react-native';

/** A visible part of the map, as `react-native-maps` describes it. */
export interface Region {
  latitude: number;
  longitude: number;
  latitudeDelta: number;
  longitudeDelta: number;
}

interface MapViewProps {
  region?: Region;
  onRegionChangeComplete?: (region: Region) => void;
  style?: unknown;
  children?: React.ReactNode;
}

/**
 * The stand-in map.
 *
 * @param props the region, its change handler and the children
 * @returns a `View` with `testID="map"`
 */
export default function MapView({ region, onRegionChangeComplete, style, children }: MapViewProps) {
  return (
    <View testID="map" style={style as never} {...({ region, onRegionChangeComplete } as object)}>
      {children}
    </View>
  );
}

/**
 * The stand-in marker.
 *
 * @param props the children, if any
 * @returns a `View`
 */
export function Marker({ children }: { children?: React.ReactNode }) {
  return <View testID="marker">{children}</View>;
}
