package lk.sliit.se3070.group058.dews.shared.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The new level, message, instructions and validity end of an escalation.
 *
 * <p>Implements CH-06.
 *
 * @param newLevel the level the warning is escalated to
 * @param message the new message in the three languages
 * @param instructions the new instructions in the three languages
 * @param expiryTime the end of the escalated warning's validity
 */
public record EscalationDetails(
    WarningLevel newLevel,
    LocalisedText message,
    LocalisedText instructions,
    LocalDateTime expiryTime) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public EscalationDetails(
      WarningLevel newLevel,
      LocalisedText message,
      LocalisedText instructions,
      LocalDateTime expiryTime) {
    this.newLevel = Objects.requireNonNull(newLevel, "newLevel");
    this.message = Objects.requireNonNull(message, "message");
    this.instructions = Objects.requireNonNull(instructions, "instructions");
    this.expiryTime = Objects.requireNonNull(expiryTime, "expiryTime");
  }
}
