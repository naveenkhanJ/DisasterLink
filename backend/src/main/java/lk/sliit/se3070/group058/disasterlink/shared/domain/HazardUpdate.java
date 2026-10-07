package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.List;

/**
 * An officer's update of a hazard's description, severity and districts.
 *
 * <p>Implements CH-22.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a field the officer has not entered is
 * {@code null}, so that the update reaches the check of {@code WarningController.updateHazard},
 * which refuses an invalid update with {@code InvalidHazardUpdateException} (MD section 6,
 * exceptions table; SC1 A1, E4).
 *
 * @param description the new description, or {@code null} if not entered — checked by {@code
 *     WarningController.updateHazard}, which refuses with {@code InvalidHazardUpdateException}
 * @param severity the new severity, or {@code null} if not entered — checked by {@code
 *     WarningController.updateHazard}, which refuses with {@code InvalidHazardUpdateException}
 * @param districts the new affected districts, empty if none entered
 */
public record HazardUpdate(String description, HazardSeverity severity, List<District> districts) {

  /** Creates the record, replacing a {@code null} collection by an empty one and copying it. */
  public HazardUpdate(String description, HazardSeverity severity, List<District> districts) {
    this.description = description;
    this.severity = severity;
    this.districts = districts == null ? List.of() : List.copyOf(districts);
  }
}
