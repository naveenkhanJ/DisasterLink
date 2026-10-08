/**
 * ShowReportDecision.tsx — `showReportDecision(notice)`: the decision card over the screen, with the
 * notice in the citizen's language and "Open report" (SC2 step 30, A4 25h; WF2P-S01e and S01f).
 *
 * Implements CH-26, CH-27 (FD-12).
 */
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { DecisionCard } from '../app/useCitizenAppUI';
import { Button, Card, Icon } from '../components/ui';
import { colors, space } from '../theme';
import { FONT_FOR, screenStyles as s } from './Screen';

/**
 * Draws the decision card.
 *
 * @param props the decision, "Open report" and close
 * @returns the card
 */
export function ShowReportDecision({
  decision,
  onOpen,
  onClose,
}: {
  decision: DecisionCard;
  onOpen: () => void;
  onClose: () => void;
}) {
  const rejected = decision.notice.status === 'REJECTED';
  return (
    <View style={styles.overlay}>
      <Card style={[styles.card, rejected ? styles.rejected : null]}>
        <View style={[s.rowCenter, { justifyContent: 'space-between' }]}>
          <View style={s.rowCenter}>
            <Icon
              name={rejected ? 'cross' : 'check'}
              size={20}
              color={rejected ? colors.errorText : colors.ok.text}
            />
            <Text style={s.cardTitle}>Report decision</Text>
          </View>
          <Pressable accessibilityLabel="Close" accessibilityRole="button" onPress={onClose}>
            <Icon name="cross" size={20} />
          </Pressable>
        </View>
        <Text style={[s.warningText, { fontFamily: FONT_FOR[decision.language] }]}>
          {decision.text}
        </Text>
        <Button
          label="Open report"
          onPress={onOpen}
          style={{ alignSelf: 'flex-start', marginTop: space.sm }}
        />
      </Card>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: { position: 'absolute', top: space.sm, left: space.sm, right: space.sm },
  card: {
    borderColor: colors.ok.border,
    borderWidth: 2,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
    elevation: 6,
  },
  rejected: { borderColor: colors.errorBorder },
});
