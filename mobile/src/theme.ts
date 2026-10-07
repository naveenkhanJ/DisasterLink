/**
 * theme.ts — colours, type and spacing of the citizen app (wireframe-spec §2.1 phone shell, §3 colours).
 * Colour is never the only signal: levels and statuses are always written in full (spec §8 rule 1).
 */
export const colors = {
  pageBg: '#F8FAFC',
  text: '#0F172A',
  muted: '#64748B',
  label: '#475569',
  border: '#E5E7EB',
  borderStrong: '#CBD5E1',
  card: '#FFFFFF',
  brand: '#F59E0B',
  primary: '#0F172A',
  selectedBorder: '#EF4444',
  selectedBg: '#FEE2E2',
  selectedText: '#B91C1C',
  errorText: '#B91C1C',
  errorBorder: '#EF4444',
  photoBg: '#E2E8F0',
  l2: { bg: '#FEF3C7', text: '#92400E', border: '#F59E0B' },
  ok: { bg: '#D1FAE5', text: '#065F46', border: '#10B981' },
  warn: { bg: '#FEF3C7', text: '#92400E', border: '#F59E0B' },
  info: { bg: '#E0ECFF', text: '#1E3A8A', border: '#93C5FD' },
} as const;

/** Font family names registered with expo-font in App.tsx. */
export const fonts = {
  ui: 'Manrope',
  sinhala: 'NotoSansSinhala',
  tamil: 'NotoSansTamil',
} as const;

export const space = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24 } as const;
export const radius = { sm: 8, md: 12, pill: 999 } as const;

export type Tone = 'neutral' | 'l2' | 'ok' | 'warn';
