/**
 * Screen.tsx — the shared scrolling wrapper of the app's screens and the language fonts. The common
 * error banner (C1) is drawn once by the app shell above the screens (CitizenApp.tsx).
 */
import React from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import type { Language } from '../api/types';
import { colors, fonts, space } from '../theme';

/** The font family that draws a language's script. */
export const FONT_FOR: Record<Language, string> = {
  SINHALA: fonts.sinhala,
  TAMIL: fonts.tamil,
  ENGLISH: fonts.ui,
};

/**
 * A screen's scrolling content area.
 *
 * @param props the screen's content
 * @returns the scroll view
 */
export function Screen({ children }: { children: React.ReactNode }) {
  return (
    <ScrollView
      style={styles.scroll}
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
    >
      {children}
    </ScrollView>
  );
}

/** The styles that every screen of the app shares. */
export const screenStyles = StyleSheet.create({
  rowCenter: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
  cardTitle: { fontFamily: fonts.ui, fontSize: 17, fontWeight: '800', color: colors.text },
  strong: {
    fontFamily: fonts.ui,
    fontSize: 15,
    lineHeight: 20,
    fontWeight: '700',
    color: colors.text,
  },
  section: { fontFamily: fonts.ui, fontSize: 17, fontWeight: '700', color: colors.text },
  link: {
    fontFamily: fonts.ui,
    fontSize: 14,
    fontWeight: '700',
    textDecorationLine: 'underline',
    color: colors.text,
  },
  warningText: { fontSize: 14, lineHeight: 21, color: colors.text, marginTop: 6 },
});

const styles = StyleSheet.create({
  scroll: { flex: 1, backgroundColor: colors.pageBg },
  content: { padding: space.lg, gap: 10 },
});
