/**
 * ConnectionStrip.tsx — the slim strip under the app header on every screen that says whether the
 * phone is online: "Online", "No internet connection" (the phone's network listener) or "Server not
 * reachable" (the push poll); it replaces the home's own Online/Offline tag (standards §10 item 11,
 * P213; the strip is not a dictionary display). While the home shows the saved copy the strip adds
 * "· showing the saved copy" after the state, in the same row (P229).
 *
 * Serves CH-27 (SC2 A8, E4).
 */
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { ConnectionState } from '../app/useCitizenAppUI';
import { Icon, Tag, styles as ui } from '../components/ui';
import { colors, space } from '../theme';
import { CONNECTION_TEXTS, SAVED_COPY_NOTE } from './texts';

/**
 * Draws the strip.
 *
 * @param props the connection state, and whether the home shows the saved copy
 * @returns the strip
 */
export function ConnectionStrip({
  state,
  savedCopy,
}: {
  state: ConnectionState;
  savedCopy: boolean;
}) {
  const { label, icon, tone } = CONNECTION_TEXTS[state];
  return (
    <View
      style={styles.strip}
      accessibilityRole="summary"
      accessibilityLabel={savedCopy ? `${label} ${SAVED_COPY_NOTE}` : label}
    >
      <Icon name={icon} size={16} />
      <Tag label={label} tone={tone} />
      {savedCopy ? <Text style={[ui.muted, styles.note]}>{SAVED_COPY_NOTE}</Text> : null}
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
  note: { flexShrink: 1 },
});
