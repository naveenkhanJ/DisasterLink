/**
 * Screen.tsx — the shared scrolling wrapper of the app's screens and the language fonts. The common
 * error banner (C1) is drawn once by the app shell above the screens (CitizenApp.tsx). The wrapper
 * keeps the field being typed in above the keyboard (iOS `automaticallyAdjustKeyboardInsets`,
 * Android `KeyboardAvoidingView`), keeps the page scrollable to its end while the keyboard is up
 * (dragging does not close it; a tap outside a field does, `keyboardShouldPersistTaps="handled"`), and draws
 * pull-to-refresh when a screen gives `refreshing` and `onRefresh` (standards §10, "Phone screens
 * beyond the frames" items 3 and 5; P206). It also gives its children `scrollIntoView(target)` through a
 * context, so content that appears in answer to the citizen is brought into view above the keyboard
 * (item 13, P222; serves CH-27).
 */
import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import type { Language } from '../api/types';
import { SCROLL_TOP_MARGIN } from '../constants';
import { colors, fonts, space } from '../theme';

/** The font family that draws a language's script. */
export const FONT_FOR: Record<Language, string> = {
  SINHALA: fonts.sinhala,
  TAMIL: fonts.tamil,
  ENGLISH: fonts.ui,
};

/** What a screen gives its children: bring an element into view. */
export interface ScreenScroll {
  /**
   * Scrolls so that the element's top is `SCROLL_TOP_MARGIN` points below the top of the visible area
   * (animated). With `automaticallyAdjustKeyboardInsets` this keeps it above the keyboard.
   */
  scrollIntoView(target: React.RefObject<View | null>): void;
}

/** The context of the screen's scroll view; without a provider `scrollIntoView` does nothing. */
export const ScreenScrollContext = createContext<ScreenScroll | undefined>(undefined);

const NO_SCROLL: ScreenScroll = { scrollIntoView: () => undefined };

/**
 * Gives the `scrollIntoView` of the screen around the caller.
 *
 * @returns the function; one that does nothing when the caller is not inside a `Screen`
 */
export function useScrollIntoView(): ScreenScroll['scrollIntoView'] {
  return (useContext(ScreenScrollContext) ?? NO_SCROLL).scrollIntoView;
}

/**
 * Scrolls an element into view when `appears` becomes true after the first render (the content shows
 * up in answer to the citizen); content that is there from the start does not move the page.
 *
 * @param appears whether the content is shown
 * @param target the element
 */
export function useScrollWhenShown(appears: boolean, target: React.RefObject<View | null>): void {
  const scrollIntoView = useScrollIntoView();
  const wasShown = useRef(appears);
  useEffect(() => {
    if (appears && !wasShown.current) {
      scrollIntoView(target);
    }
    wasShown.current = appears;
  }, [appears, target, scrollIntoView]);
}

/**
 * A screen's scrolling content area.
 *
 * @param props the screen's content and, for pull-to-refresh, `refreshing` and `onRefresh`
 * @returns the scroll view
 */
export function Screen({
  children,
  refreshing,
  onRefresh,
}: {
  children: React.ReactNode;
  refreshing?: boolean;
  onRefresh?: () => void;
}) {
  const scrollRef = useRef<ScrollView>(null);
  // The content view carries the padding and the gap, so a child's offset in it is its offset in the scroll content.
  const contentRef = useRef<View>(null);
  const scrollIntoView = useCallback((target: React.RefObject<View | null>) => {
    const inner = contentRef.current;
    const element = target.current;
    if (!inner || !element) {
      return;
    }
    element.measureLayout(
      inner as never,
      (_x, y) =>
        scrollRef.current?.scrollTo({ y: Math.max(0, y - SCROLL_TOP_MARGIN), animated: true }),
      () => undefined,
    );
  }, []);
  const context = useMemo(() => ({ scrollIntoView }), [scrollIntoView]);
  const scroll = (
    <ScrollView
      ref={scrollRef}
      testID="screen-scroll"
      style={styles.scroll}
      keyboardShouldPersistTaps="handled"
      keyboardDismissMode="none"
      automaticallyAdjustKeyboardInsets
      refreshControl={
        onRefresh ? (
          <RefreshControl refreshing={refreshing ?? false} onRefresh={onRefresh} />
        ) : undefined
      }
    >
      <View ref={contentRef} collapsable={false} style={styles.content}>
        {children}
      </View>
    </ScrollView>
  );
  return (
    <ScreenScrollContext.Provider value={context}>
      {Platform.OS === 'android' ? (
        <KeyboardAvoidingView testID="keyboard-avoiding" style={styles.scroll} behavior="height">
          {scroll}
        </KeyboardAvoidingView>
      ) : (
        scroll
      )}
    </ScreenScrollContext.Provider>
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
