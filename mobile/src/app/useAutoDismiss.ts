/**
 * useAutoDismiss.ts — makes a notice that needs no action fade after a time (implementation-only;
 * standards §10 item 12, P222, UC2-7e G7 and D3; serves CH-25 and CH-27). There is one timer per
 * notice: it is cleared when the notice closes, is replaced (a new `restartKey`) or the component
 * unmounts. With a screen reader on, nothing fades, so a blind citizen does not miss a notice: the
 * hook reads `AccessibilityInfo.isScreenReaderEnabled()` once and follows `screenReaderChanged`.
 */
import { useEffect, useRef, useState } from 'react';
import { AccessibilityInfo } from 'react-native';

/**
 * Tells whether a screen reader is on, now and when it changes.
 *
 * @returns whether a screen reader is on
 */
function useScreenReader(): boolean {
  const [on, setOn] = useState(false);
  useEffect(() => {
    let cancelled = false;
    void AccessibilityInfo.isScreenReaderEnabled().then((enabled) => {
      if (!cancelled) {
        setOn(enabled);
      }
    });
    const subscription = AccessibilityInfo.addEventListener('screenReaderChanged', setOn);
    return () => {
      cancelled = true;
      subscription.remove();
    };
  }, []);
  return on;
}

/**
 * Calls `onDismiss` once `ms` milliseconds after the notice appeared, unless it closed first.
 *
 * @param active whether the notice is shown and may fade
 * @param ms how long it stays, in milliseconds
 * @param onDismiss closes the notice
 * @param restartKey changes when a new notice of the same kind replaces the old one: the timer restarts
 */
export function useAutoDismiss(
  active: boolean,
  ms: number,
  onDismiss: () => void,
  restartKey?: unknown,
): void {
  const screenReader = useScreenReader();
  const latest = useRef(onDismiss);
  useEffect(() => {
    latest.current = onDismiss;
  });
  useEffect(() => {
    if (!active || screenReader) {
      return undefined;
    }
    const timer = setTimeout(() => latest.current(), ms);
    return () => clearTimeout(timer);
  }, [active, ms, restartKey, screenReader]);
}
