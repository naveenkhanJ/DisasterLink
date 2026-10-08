/**
 * ShowLocationUnavailable.tsx — `showLocationUnavailable()`: "GPS location is unavailable" and the
 * way to type the coordinates; a location outside Sri Lanka is refused with its error under the
 * fields and the typed values stay (SC2 A1 12a to 12c, E3 12e to 12f; WF2P-S03a and S03b).
 *
 * Implements CH-27 (FD-12).
 */
import React, { useState } from 'react';
import { Text, View } from 'react-native';
import type { GeoPoint } from '../api/reportApi';
import { Banner, Button, ErrorText, TextField, styles as ui } from '../components/ui';
import { colors, fonts, space } from '../theme';
import { NUMBERS_REQUIRED_TEXT, parseCoordinate } from './texts';

/**
 * Draws the manual-entry block.
 *
 * @param props the error of the last entry and the handler that takes the typed place
 * @returns the block
 */
export function ShowLocationUnavailable({
  error,
  onEnter,
  noFix = true,
}: {
  error: string | undefined;
  onEnter: (location: GeoPoint) => void;
  noFix?: boolean;
}) {
  const [typing, setTyping] = useState(false);
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [notNumbers, setNotNumbers] = useState(false);
  const shown = notNumbers ? NUMBERS_REQUIRED_TEXT : error;

  function submitTyped(): void {
    const typedLatitude = parseCoordinate(latitude);
    const typedLongitude = parseCoordinate(longitude);
    if (typedLatitude === undefined || typedLongitude === undefined) {
      setNotNumbers(true);
      return;
    }
    setNotNumbers(false);
    onEnter({ latitude: typedLatitude, longitude: typedLongitude });
  }

  return (
    <View style={{ gap: space.sm }}>
      {noFix ? (
        <Banner kind="info" icon="pin">
          <Text
            style={{ fontFamily: fonts.ui, fontSize: 14, fontWeight: '700', color: colors.text }}
          >
            GPS location is unavailable
          </Text>
        </Banner>
      ) : null}
      {typing || error ? null : (
        <Button label="Enter location manually" kind="secondary" onPress={() => setTyping(true)} />
      )}
      {typing || error ? (
        <>
          <View style={{ flexDirection: 'row', gap: space.sm }}>
            <TextField
              label="Latitude (°N)"
              value={latitude}
              onChangeText={setLatitude}
              keyboardType="numbers-and-punctuation"
              error={Boolean(shown)}
            />
            <TextField
              label="Longitude (°E)"
              value={longitude}
              onChangeText={setLongitude}
              keyboardType="numbers-and-punctuation"
              error={Boolean(shown)}
            />
          </View>
          <Text style={ui.muted}>Type the coordinates of the place you saw</Text>
          {shown ? <ErrorText>{shown}</ErrorText> : null}
          <Button label="Use this location" kind="secondary" onPress={submitTyped} />
        </>
      ) : null}
    </View>
  );
}
