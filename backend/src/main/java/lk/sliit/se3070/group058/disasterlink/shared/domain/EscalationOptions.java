package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * The levels a warning can be escalated to.
 *
 * <p>Implements CH-06.
 *
 * @param warning the warning to escalate
 * @param allowedLevels the levels above the current one; empty at the highest level
 */
public record EscalationOptions(Warning warning, List<WarningLevel> allowedLevels) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public EscalationOptions(Warning warning, List<WarningLevel> allowedLevels) {
    this.warning = Objects.requireNonNull(warning, "warning");
    this.allowedLevels = List.copyOf(allowedLevels);
  }
}
