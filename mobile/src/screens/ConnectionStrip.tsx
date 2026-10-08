/**
 * ConnectionStrip.tsx — the slim strip under the app header on every screen that says whether the
 * phone is online: "Online", "No internet connection" (the phone's network listener) or "Server not
 * reachable" (the push poll); it replaces the home's own Online/Offline tag (standards §10 item 11,
 * P213; the strip is not a dictionary display).
 *
 * Serves CH-27 (SC2 A8, E4).
 */
import React from 'react';
import { StyleSheet, View } from 'react-native';
import type { ConnectionState } from '../app/useCitizenAppUI';
import { Icon, Tag } from '../components/ui';
import { colors, space } from '../theme';
import { CONNECTION_TEXTS } from './texts';

/**
 * Draws the strip.
 *
 * @param props the connection state
 * @returns the strip
 */
export function ConnectionStrip({ state }: { state: ConnectionState }) {
  const { label, icon, tone } = CONNECTION_TEXTS[state];
  return (
    <View style={styles.strip} accessibilityRole="summary">
      <Icon name={icon} size={16} />
      <Tag label={label} tone={tone} />
    </View>
  );
}

const styles = StyleSheet.create({
  strip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: space.sm,
    paddingHorizontal: space.lg,
    paddingVertical: space.xs,
    backgroundColor: colors.pageBg,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
});
