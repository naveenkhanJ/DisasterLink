/**
 * ui.tsx — the citizen app's shared building blocks (cards, tags, buttons, fields, rows, the tab bar).
 * Presentational only: handlers are passed in; nothing here talks to the server.
 */
import React from 'react';
import {
  ActivityIndicator,
  Image,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
  type KeyboardTypeOptions,
  type ReturnKeyTypeOptions,
  type StyleProp,
  type ViewStyle,
} from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';
import { ICONS, type IconElement, type IconName } from './iconData';

import { colors, fonts, radius, space, type Tone } from '../theme';

/**
 * ICONS keeps each icon's literal type (`as const`), so an element's attributes only list the keys that icon uses and
 * `attrs.fill` did not type-check (TS2339, reported from the Expo run). Reading the set through the wider element type
 * fixes that without changing any icon.
 */
const ICON_SET: Record<IconName, readonly IconElement[]> = ICONS;

/** One icon of the wireframe set, drawn as 24 × 24 line art in the current text colour. */
export function Icon({
  name,
  size = 20,
  color = colors.text,
}: {
  name: IconName;
  size?: number;
  color?: string;
}) {
  return (
    <Svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke={color}
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {ICON_SET[name].map(([tag, attrs], i) =>
        tag === 'circle' ? (
          <Circle key={i} cx={attrs.cx} cy={attrs.cy} r={attrs.r} />
        ) : (
          <Path key={i} d={attrs.d} fill={attrs.fill === 'currentColor' ? color : undefined} />
        ),
      )}
    </Svg>
  );
}

export function Card({
  children,
  style,
}: {
  children: React.ReactNode;
  style?: StyleProp<ViewStyle>;
}) {
  return <View style={[styles.card, style]}>{children}</View>;
}

const ERROR_TONE = { bg: '#FEE2E2', text: colors.errorText, border: colors.errorBorder };

const TONES: Record<Tone, { bg: string; text: string; border: string }> = {
  neutral: { bg: '#F1F5F9', text: '#334155', border: colors.borderStrong },
  l1: colors.l1,
  l2: colors.l2,
  l3: colors.l3,
  ok: colors.ok,
  warn: colors.warn,
};

/** A bordered tag with the level or status written in full. */
export function Tag({
  label,
  tone = 'neutral',
  icon,
}: {
  label: string;
  tone?: Tone;
  icon?: IconName;
}) {
  const t = TONES[tone];
  return (
    <View style={[styles.tag, { backgroundColor: t.bg, borderColor: t.border }]}>
      {icon ? <Icon name={icon} size={14} color={t.text} /> : null}
      <Text style={[styles.tagText, { color: t.text }]}>{label}</Text>
    </View>
  );
}

type ButtonKind = 'primary' | 'secondary' | 'danger';

export function Button({
  label,
  onPress,
  kind = 'primary',
  icon,
  big = false,
  disabled = false,
  style,
}: {
  label: string;
  onPress?: () => void;
  kind?: ButtonKind;
  icon?: IconName;
  big?: boolean;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
}) {
  const primary = kind === 'primary';
  const textColor = primary ? '#FFFFFF' : kind === 'danger' ? colors.errorText : colors.text;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={[
        styles.button,
        primary
          ? styles.buttonPrimary
          : kind === 'danger'
            ? styles.buttonDanger
            : styles.buttonSecondary,
        big && styles.buttonBig,
        disabled && styles.buttonDisabled,
        style,
      ]}
    >
      {icon ? <Icon name={icon} size={18} color={textColor} /> : null}
      <Text style={[styles.buttonText, { color: textColor }, big && { fontSize: 16 }]}>
        {label}
      </Text>
    </Pressable>
  );
}

/** A labelled value row as used on the review, confirmation and status cards. */
export function LabelValue({
  label,
  children,
  last = false,
}: {
  label: string;
  children: React.ReactNode;
  last?: boolean;
}) {
  return (
    <View style={[styles.lv, !last && styles.lvRule]}>
      <Text style={styles.label}>{label}</Text>
      <View style={{ marginTop: 2 }}>
        {typeof children === 'string' ? <Text style={styles.value}>{children}</Text> : children}
      </View>
    </View>
  );
}

export function ErrorText({ children }: { children: string }) {
  return (
    <View style={styles.errorRow}>
      <Icon name="error" size={16} color={colors.errorText} />
      <Text style={styles.errorText}>{children}</Text>
    </View>
  );
}

/** Multi-line text field with an optional counter and error. */
export function TextArea({
  label,
  value,
  counter,
  error,
  onChangeText,
}: {
  label: string;
  value: string;
  counter?: string;
  error?: string;
  onChangeText?: (t: string) => void;
}) {
  return (
    <View>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        multiline
        accessibilityLabel={label}
        value={value}
        onChangeText={onChangeText}
        style={[styles.textArea, error ? styles.inputError : null]}
      />
      {counter ? <Text style={[styles.muted, { textAlign: 'right' }]}>{counter}</Text> : null}
      {error ? <ErrorText>{error}</ErrorText> : null}
    </View>
  );
}

/** The report's photo: the copy on the phone, or an empty tile (with an error border if asked). */
export function PhotoTile({
  uri,
  width,
  height,
  error = false,
  onPress,
}: {
  uri?: string;
  width: number | '100%';
  height: number;
  error?: boolean;
  onPress?: () => void;
}) {
  if (uri) {
    const image = (
      <Image
        accessibilityLabel="Photo of the hazard"
        source={{ uri }}
        style={[styles.photo, { width, height }]}
        resizeMode="cover"
      />
    );
    return onPress ? (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Open photo"
        onPress={onPress}
        style={{ width }}
      >
        {image}
      </Pressable>
    ) : (
      image
    );
  }
  return (
    <View style={[styles.photo, { width, height }, error ? styles.inputError : null]}>
      <Icon name="camera" size={18} color={colors.muted} />
      <Text style={styles.muted}>photo</Text>
    </View>
  );
}

/** A single-line field with a label, an optional hint and an optional error. */
export function TextField({
  label,
  value,
  onChangeText,
  error = false,
  keyboardType,
  returnKeyType,
  onSubmitEditing,
}: {
  label: string;
  value: string;
  onChangeText: (text: string) => void;
  error?: boolean;
  keyboardType?: KeyboardTypeOptions;
  returnKeyType?: ReturnKeyTypeOptions;
  onSubmitEditing?: () => void;
}) {
  return (
    <View style={{ flex: 1 }}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        value={value}
        onChangeText={onChangeText}
        keyboardType={keyboardType}
        returnKeyType={returnKeyType}
        onSubmitEditing={onSubmitEditing}
        style={[styles.textField, error ? styles.inputError : null]}
      />
    </View>
  );
}

type BannerKind = 'ok' | 'error' | 'info' | 'warn';

/** A notice bar: an icon, bold text and an optional close "×". */
export function Banner({
  kind,
  icon,
  children,
  onClose,
}: {
  kind: BannerKind;
  icon: IconName;
  children: React.ReactNode;
  onClose?: () => void;
}) {
  const tone = kind === 'error' ? ERROR_TONE : colors[kind];
  return (
    <View
      accessibilityRole="alert"
      style={[styles.banner, { backgroundColor: tone.bg, borderColor: tone.border }]}
    >
      <Icon name={icon} size={18} color={tone.text} />
      <View style={{ flex: 1 }}>{children}</View>
      {onClose ? (
        <Pressable accessibilityLabel="Close" accessibilityRole="button" onPress={onClose}>
          <Icon name="cross" size={18} color={tone.text} />
        </Pressable>
      ) : null}
    </View>
  );
}

/** The common error banner (C1): the service is unavailable and nothing was changed; Retry. */
export function ServiceUnavailableBanner({ text, onRetry }: { text: string; onRetry: () => void }) {
  return (
    <Banner kind="error" icon="error">
      <Text style={[styles.bannerText, { color: colors.errorText }]}>{text}</Text>
      <Button label="Retry" kind="secondary" onPress={onRetry} style={styles.retry} />
    </Banner>
  );
}

export function ScreenTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <View>
      <Text style={styles.title}>{title}</Text>
      {subtitle ? <Text style={styles.subtitle}>{subtitle}</Text> : null}
    </View>
  );
}

export type Tab = 'Home' | 'Report';

/**
 * Bottom tab bar: Home and Report, plus Profile signposted (P91, wireframe-spec §8 rule 12): the Register use case is
 * designed but not implemented in this version, so the tab is muted and only reports that it is not available.
 */
export function TabBar({
  active,
  onHome,
  onReport,
  onSoon,
}: {
  active: Tab;
  onHome: () => void;
  onReport: () => void;
  onSoon: (label: string) => void;
}) {
  const tab = (name: Tab, icon: IconName, onPress: () => void) => {
    const on = name === active;
    return (
      <Pressable
        accessibilityRole="tab"
        onPress={onPress}
        style={[styles.tab, on && styles.tabActive]}
      >
        <Icon name={icon} size={22} color={on ? colors.text : colors.muted} />
        <Text style={[styles.tabText, on && styles.tabTextActive]}>{name}</Text>
      </Pressable>
    );
  };
  return (
    <View style={styles.tabBar}>
      {tab('Home', 'home', onHome)}
      {tab('Report', 'plus', onReport)}
      <Pressable
        accessibilityRole="tab"
        accessibilityState={{ disabled: true }}
        onPress={() => onSoon('Profile')}
        style={styles.tab}
      >
        <Icon name="user" size={22} color={colors.borderStrong} />
        <Text style={[styles.tabText, { color: colors.muted }]}>Profile</Text>
        <Text style={styles.soonTag}>Not in this version</Text>
      </Pressable>
    </View>
  );
}

/** The "not available in this version" notice shown when a signposted item is pressed. */
export function SoonNotice({ label, onClose }: { label: string; onClose: () => void }) {
  return (
    <View accessibilityRole="alert" style={styles.soonNotice}>
      <Icon name="info" size={18} color={colors.info.text} />
      <Text style={styles.soonText}>{label} — not available in this version.</Text>
      <Pressable accessibilityLabel="Close" onPress={onClose}>
        <Icon name="cross" size={18} color={colors.info.text} />
      </Pressable>
    </View>
  );
}

/**
 * A modal: a centred white card with a 2 px border over a 40 % black scrim (the Modal token). It is
 * drawn in the app, not with `Alert.alert`.
 */
export function ModalCard({
  visible,
  onRequestClose,
  children,
}: {
  visible: boolean;
  onRequestClose: () => void;
  children: React.ReactNode;
}) {
  return (
    <Modal transparent visible={visible} animationType="fade" onRequestClose={onRequestClose}>
      <View style={styles.scrim}>
        <View style={styles.modalCard}>{children}</View>
      </View>
    </Modal>
  );
}

/** The slim "Loading…" row the shell draws under the header while an online action runs. */
export function LoadingRow() {
  return (
    <View style={styles.loadingRow} accessibilityRole="progressbar">
      <ActivityIndicator size="small" color={colors.muted} />
      <Text style={styles.muted}>Loading…</Text>
    </View>
  );
}

/** App header: the DMC badge and the app title; "‹ Back" at the left when `onBack` is given. */
export function AppHeader({ onBack }: { onBack?: () => void }) {
  return (
    <View style={styles.appHeader}>
      {onBack ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Back"
          hitSlop={8}
          onPress={onBack}
        >
          <Text style={styles.backLink}>‹ Back</Text>
        </Pressable>
      ) : null}
      <View style={styles.badge}>
        <Text style={styles.badgeText}>DMC</Text>
      </View>
      <Text style={styles.appTitle}>DMC Sri Lanka</Text>
    </View>
  );
}

export const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: space.md,
  },
  tag: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: 4,
    height: 24,
    paddingHorizontal: 8,
    borderRadius: 6,
    borderWidth: 1,
  },
  tagText: { fontFamily: fonts.ui, fontSize: 13, fontWeight: '700' },
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    height: 44,
    paddingHorizontal: 16,
    borderRadius: radius.sm,
    borderWidth: 1,
  },
  buttonPrimary: { backgroundColor: colors.primary, borderColor: colors.primary },
  buttonSecondary: { backgroundColor: colors.card, borderColor: colors.borderStrong },
  buttonDanger: { backgroundColor: colors.card, borderColor: colors.errorBorder, borderWidth: 2 },
  buttonDisabled: { opacity: 0.5 },
  buttonBig: { height: 52 },
  buttonText: { fontFamily: fonts.ui, fontSize: 15, fontWeight: '700' },
  lv: { paddingVertical: 8 },
  lvRule: { borderBottomWidth: 1, borderBottomColor: colors.border },
  label: {
    fontFamily: fonts.ui,
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '600',
    color: colors.label,
  },
  value: { fontFamily: fonts.ui, fontSize: 15, lineHeight: 21, color: colors.text },
  muted: { fontFamily: fonts.ui, fontSize: 13, lineHeight: 18, color: colors.muted },
  errorRow: { flexDirection: 'row', gap: 6, alignItems: 'flex-start', marginTop: 4 },
  errorText: {
    flex: 1,
    fontFamily: fonts.ui,
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '700',
    color: colors.errorText,
  },
  textArea: {
    minHeight: 104,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: radius.sm,
    padding: space.md,
    marginTop: 4,
    fontFamily: fonts.ui,
    fontSize: 14,
    lineHeight: 20,
    color: colors.text,
    backgroundColor: colors.card,
    textAlignVertical: 'top',
  },
  inputError: { borderWidth: 2, borderColor: colors.errorBorder },
  textField: {
    height: 44,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: radius.sm,
    paddingHorizontal: space.md,
    marginTop: 4,
    fontFamily: fonts.ui,
    fontSize: 15,
    color: colors.text,
    backgroundColor: colors.card,
  },
  banner: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
    padding: space.md,
    borderRadius: radius.sm,
    borderWidth: 2,
  },
  bannerText: {
    fontFamily: fonts.ui,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
    color: colors.text,
  },
  retry: { alignSelf: 'flex-start', marginTop: space.sm, height: 36 },
  photo: {
    backgroundColor: colors.photoBg,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: radius.sm,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
  },
  title: {
    fontFamily: fonts.ui,
    fontSize: 20,
    lineHeight: 28,
    fontWeight: '800',
    color: colors.text,
  },
  subtitle: { fontFamily: fonts.ui, fontSize: 14, lineHeight: 20, color: colors.muted },
  tabBar: {
    flexDirection: 'row',
    height: 64,
    backgroundColor: colors.card,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 2 },
  tabActive: { borderBottomWidth: 2, borderBottomColor: colors.text },
  tabText: { fontFamily: fonts.ui, fontSize: 12, fontWeight: '600', color: colors.muted },
  tabTextActive: { color: colors.text, fontWeight: '800' },
  soonTag: {
    fontFamily: fonts.ui,
    fontSize: 8,
    lineHeight: 11,
    fontWeight: '700',
    color: colors.muted,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    borderRadius: 4,
    paddingHorizontal: 4,
  },
  soonNotice: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    margin: space.sm,
    padding: space.md,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.info.border,
    backgroundColor: colors.info.bg,
  },
  soonText: { flex: 1, fontFamily: fonts.ui, fontSize: 14, color: colors.info.text },
  appHeader: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: space.lg,
    backgroundColor: colors.card,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  badge: {
    width: 28,
    height: 28,
    borderRadius: 6,
    backgroundColor: colors.brand,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { fontFamily: fonts.ui, fontSize: 9, fontWeight: '800', color: colors.text },
  appTitle: { fontFamily: fonts.ui, fontSize: 17, fontWeight: '800', color: colors.text },
  backLink: { fontFamily: fonts.ui, fontSize: 16, fontWeight: '700', color: colors.text },
  scrim: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: space.lg,
  },
  modalCard: {
    alignSelf: 'stretch',
    gap: space.sm,
    padding: space.lg,
    borderRadius: radius.md,
    borderWidth: 2,
    borderColor: colors.text,
    backgroundColor: colors.card,
  },
  loadingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: space.sm,
    paddingHorizontal: space.lg,
    paddingVertical: space.xs,
    backgroundColor: colors.pageBg,
  },
});
