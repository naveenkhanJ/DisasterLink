/**
 * ShowAlert.tsx — `showAlert(warningId, warningLevel, text)`: the in-app banner of one merged warning
 * alert (SC1 step 15; standards section 10, "One alert per warning"). The local notification with
 * sound is presented by `receiveAlert`.
 *
 * Implements CH-25 (FD-12).
 */
import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { Card, Icon, Tag } from '../components/ui';
import type { AlertNotice } from '../app/useCitizenAppUI';
import { colors } from '../theme';
import { screenStyles as s } from './Screen';
import { LEVEL_LABELS, LEVEL_TONES } from './texts';

/**
 * Draws one alert banner.
 *
 * @param props the alert and the handler of its close button
 * @returns the banner
 */
export function ShowAlert({ alert, onClose }: { alert: AlertNotice; onClose: () => void }) {
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
      <Text style={s.warningText}>{alert.text}</Text>
    </Card>
  );
}
