/**
 * ShowAlert.tsx — `showAlert(warningId, warningLevel, text)`: the in-app banner of one merged warning
 * alert (SC1 step 15; standards section 10, "One alert per warning"). The local notification with
 * sound is presented by `receiveAlert`. The banner shows its warning's message in the selected
 * language whenever the home holds that warning at the alert's level, else the text as received:
 * a home copy at another level is an older version, for example before the reload after an
 * escalation, or the saved copy (standards section 10 item 18, D-UC2-8, P240, P241).
 *
 * Implements CH-25 (FD-12).
 */
import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { Card, Icon, Tag } from '../components/ui';
import type { ActiveWarning, Language } from '../api/types';
import type { AlertNotice } from '../app/useCitizenAppUI';
import { colors } from '../theme';
import { FONT_FOR, screenStyles as s } from './Screen';
import { LEVEL_LABELS, LEVEL_TONES, localised } from './texts';

/**
 * Draws one alert banner.
 *
 * @param props the alert, the selected language, the warnings the home holds and the handler of
 *   the close button
 * @returns the banner
 */
export function ShowAlert({
  alert,
  language,
  warnings,
  onClose,
}: {
  alert: AlertNotice;
  language: Language;
  warnings: ActiveWarning[];
  onClose: () => void;
}) {
  const warning = warnings.find(
    (candidate) =>
      candidate.warningId === alert.warningId && candidate.warningLevel === alert.warningLevel,
  );
  return (
    <Card style={{ borderColor: colors[LEVEL_TONES[alert.warningLevel]].border, borderWidth: 2 }}>
      <View style={[s.rowCenter, { justifyContent: 'space-between' }]}>
        <View style={s.rowCenter}>
          <Icon name="warning-triangle" size={20} />
          <Text style={s.cardTitle}>Warning alert</Text>
        </View>
        <Pressable accessibilityLabel="Close alert" accessibilityRole="button" onPress={onClose}>
          <Icon name="cross" size={20} />
        </Pressable>
      </View>
      <View style={{ marginTop: 8 }}>
        <Tag label={LEVEL_LABELS[alert.warningLevel]} tone={LEVEL_TONES[alert.warningLevel]} />
      </View>
      {warning ? (
        <Text style={[s.warningText, { fontFamily: FONT_FOR[language] }]}>
          {localised(warning.message, language)}
        </Text>
      ) : (
        <Text style={s.warningText}>{alert.text}</Text>
      )}
    </Card>
  );
}
