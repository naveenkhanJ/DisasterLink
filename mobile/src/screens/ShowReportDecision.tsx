/**
 * ShowReportDecision.tsx — `showReportDecision(notice)`: the decision card over the screen, with the
 * notice in the selected language and "Open report" (SC2 step 30, A4 25h; WF2P-S01e and S01f). The
 * text is rebuilt from the notice in the selected language, so it follows the selector (standards
 * section 10 item 18, D-UC2-8, P240); the local notification keeps the text it arrived with.
 *
 * Implements CH-26, CH-27 (FD-12).
 */
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { DecisionCard } from '../app/useCitizenAppUI';
import type { Language } from '../api/types';
import { Button, Card, Icon } from '../components/ui';
import { colors, space } from '../theme';
import { FONT_FOR, screenStyles as s } from './Screen';
import { decisionNoticeText } from './texts';

/**
 * Draws the decision card.
 *
 * @param props the decision, the selected language, "Open report" and close
 * @returns the card
 */
export function ShowReportDecision({
  decision,
  language,
  onOpen,
  onClose,
}: {
  decision: DecisionCard;
  language: Language;
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
        <Text style={[s.warningText, { fontFamily: FONT_FOR[language] }]}>
          {decisionNoticeText(decision.notice, language)}
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
