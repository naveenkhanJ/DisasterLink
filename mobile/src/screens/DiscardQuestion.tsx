/**
 * DiscardQuestion.tsx — the in-app question "Discard this report?" shown when the citizen leaves the
 * report flow with an unsaved draft that has content (Home tab, Report tab, Back from the hazard
 * list). A Modal-token card over a scrim, not `Alert.alert` (standards §10 "Phone screens beyond the
 * frames" item 2, P206). Not a dictionary display.
 *
 * Serves CH-27 (SC2 A3, C2: forms keep their values).
 */
import React from 'react';
import { Text } from 'react-native';
import { Button, ModalCard } from '../components/ui';
import { colors, fonts } from '../theme';

/**
 * Draws the question.
 *
 * @param props whether it is open and the handlers of its two buttons
 * @returns the modal
 */
export function DiscardQuestion({
  visible,
  onDiscard,
  onKeepEditing,
}: {
  visible: boolean;
  onDiscard: () => void;
  onKeepEditing: () => void;
}) {
  return (
    <ModalCard visible={visible} onRequestClose={onKeepEditing}>
      <Text style={{ fontFamily: fonts.ui, fontSize: 18, fontWeight: '800', color: colors.text }}>
        Discard this report?
      </Text>
      <Text style={{ fontFamily: fonts.ui, fontSize: 15, lineHeight: 22, color: colors.text }}>
        What you entered on this report will be lost.
      </Text>
      <Button label="Discard" kind="danger" onPress={onDiscard} />
      <Button label="Keep editing" kind="secondary" onPress={onKeepEditing} />
    </ModalCard>
  );
}
