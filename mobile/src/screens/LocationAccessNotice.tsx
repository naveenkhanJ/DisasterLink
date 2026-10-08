/**
 * LocationAccessNotice.tsx — the notice that says why there is no GPS fix when the cause is known:
 * the permission is refused or can be asked again ("Allow location access to use your current
 * location" with "Allow location access"), or Location Services are off ("Turn on Location Services
 * to use your current location"). App-screen handling like S03d, not a dictionary display
 * (WF2P-S03g; standards §10 "Phone screens beyond the frames" item 8; P206).
 *
 * Serves CH-27 (SC2 A1 12a to 12b).
 */
import React from 'react';
import { Text, View } from 'react-native';
import type { LocationAccessState } from '../location/locationAccess';
import { Banner, Button } from '../components/ui';
import { colors, fonts, space } from '../theme';
import { LOCATION_ALLOW_TEXT, LOCATION_SERVICES_OFF_TEXT } from './texts';

/** The states for which the notice is drawn; `granted` has no cause to explain. */
export type LocationNoticeState = Exclude<LocationAccessState, 'granted'>;

/**
 * Tells whether a location access state has a notice.
 *
 * @param state the state, if one was read
 * @returns whether the state is one of the three that are explained
 */
export function hasLocationNotice(
  state: LocationAccessState | undefined,
): state is LocationNoticeState {
  return state === 'askable' || state === 'blocked' || state === 'servicesOff';
}

/**
 * Draws the notice.
 *
 * @param props the state and the handler of "Allow location access"
 * @returns the notice
 */
export function LocationAccessNotice({
  state,
  onAllow,
}: {
  state: LocationNoticeState;
  onAllow: () => void;
}) {
  const services = state === 'servicesOff';
  return (
    <View style={{ gap: space.sm }}>
      <Banner kind="info" icon="pin">
        <Text style={{ fontFamily: fonts.ui, fontSize: 14, fontWeight: '700', color: colors.text }}>
          {services ? LOCATION_SERVICES_OFF_TEXT : LOCATION_ALLOW_TEXT}
        </Text>
      </Banner>
      {services ? null : (
        <Button label="Allow location access" kind="secondary" onPress={onAllow} />
      )}
    </View>
  );
}
