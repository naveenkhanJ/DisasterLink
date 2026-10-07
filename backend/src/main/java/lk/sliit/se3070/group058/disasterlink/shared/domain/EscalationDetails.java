package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;

/**
 * The new level, message, instructions and validity end of an escalation.
 *
 * <p>Implements CH-06.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a field the officer has not entered is
 * {@code null}, so that {@code EscalationController.validateEscalation} reports it.
 *
 * @param newLevel the level the warning is escalated to, or {@code null} if not entered — checked
 *     by {@code EscalationController.validateEscalation}
 * @param message the new message in the three languages, or {@code null} if not entered — checked
 *     by {@code EscalationController.validateEscalation}
 * @param instructions the new instructions in the three languages, or {@code null} if not entered —
 *     checked by {@code EscalationController.validateEscalation}
 * @param expiryTime the end of the escalated warning's validity, or {@code null} if not entered —
 *     checked by {@code EscalationController.validateEscalation}
 */
public record EscalationDetails(
    WarningLevel newLevel,
    LocalisedText message,
    LocalisedText instructions,
    LocalDateTime expiryTime) {

  /** Creates the record; every component may be {@code null} (not entered). */
  public EscalationDetails(
      WarningLevel newLevel,
      LocalisedText message,
      LocalisedText instructions,
      LocalDateTime expiryTime) {
    this.newLevel = newLevel;
    this.message = message;
    this.instructions = instructions;
    this.expiryTime = expiryTime;
  }
}
