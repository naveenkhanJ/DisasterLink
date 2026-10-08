/**
 * ChooseHazardType.tsx — the hazard-type list (a screen component, MD section 0.11 C2: no dictionary
 * display) with the capture time recorded when the draft was started (SC2 steps 5 to 7; WF2P-S02).
 *
 * Implements CH-27.
 */
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { HazardType } from '../api/types';
import { Icon, ScreenTitle, styles as ui } from '../components/ui';
import { colors, fonts, space } from '../theme';
import { Screen, screenStyles as s } from './Screen';
import { formatDateTime, HAZARD_OPTIONS } from './texts';

/**
 * Draws the list of hazard types.
 *
 * @param props the draft's capture time and the handler of a tile
 * @returns the screen
 */
export function ChooseHazardType({
  capturedAt,
  onSelect,
}: {
  capturedAt: string | undefined;
  onSelect: (type: HazardType) => void;
}) {
  return (
    <Screen>
      <ScreenTitle title="Report a hazard" subtitle="What did you see?" />
      {capturedAt ? (
        <Text
          style={ui.muted}
        >{`Capture time ${formatDateTime(capturedAt)} — recorded automatically`}</Text>
      ) : null}
      {HAZARD_OPTIONS.map((option) => (
        <Pressable
          key={option.type}
          accessibilityRole="button"
          accessibilityLabel={option.label}
          onPress={() => onSelect(option.type)}
          style={styles.tile}
        >
          <View style={s.rowCenter}>
            <Icon name={option.icon} size={26} />
            <Text style={styles.tileText}>{option.label}</Text>
          </View>
          <Icon name="chevron-right" size={20} color={colors.muted} />
        </Pressable>
      ))}
    </Screen>
  );
}

const styles = StyleSheet.create({
  tile: {
    height: 64,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: space.md,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: 10,
    backgroundColor: colors.card,
  },
  tileText: { fontFamily: fonts.ui, fontSize: 16, fontWeight: '600', color: colors.text },
});
