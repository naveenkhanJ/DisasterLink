package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * The details an officer enters to issue a warning (the parameter object of the warning
 * operations).
 *
 * <p>Implements CH-22.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a field the officer has not entered is
 * {@code null}, and a {@code null} collection is empty, so that the control's {@code
 * validateWarning} reports it instead of the construction failing.
 *
 * @param warningLevel the level of the warning, or {@code null} if not entered — checked by {@code
 *     WarningController.validateWarning}
 * @param message the warning message in the three languages, or {@code null} if not entered —
 *     checked by {@code WarningController.validateWarning}
 * @param instructions the instructions to the public in the three languages, or {@code null} if not
 *     entered — checked by {@code WarningController.validateWarning}
 * @param targetDistricts the districts the warning is sent to, empty if none entered
 * @param targetRiverBasins the river basins the warning covers, empty if none entered
 * @param channels the channels the warning is sent through, empty if none entered
 * @param expiryTime the end of the warning's validity, or {@code null} if not entered — checked by
 *     {@code WarningController.validateWarning}
 */
public record WarningDetails(
    WarningLevel warningLevel,
    LocalisedText message,
    LocalisedText instructions,
    List<District> targetDistricts,
    List<RiverBasin> targetRiverBasins,
    Set<NotificationChannel> channels,
    LocalDateTime expiryTime) {

  /** Creates the record, replacing a {@code null} collection by an empty one and copying it. */
  public WarningDetails(
      WarningLevel warningLevel,
      LocalisedText message,
      LocalisedText instructions,
      List<District> targetDistricts,
      List<RiverBasin> targetRiverBasins,
      Set<NotificationChannel> channels,
      LocalDateTime expiryTime) {
    this.warningLevel = warningLevel;
    this.message = message;
    this.instructions = instructions;
    this.targetDistricts = targetDistricts == null ? List.of() : List.copyOf(targetDistricts);
    this.targetRiverBasins = targetRiverBasins == null ? List.of() : List.copyOf(targetRiverBasins);
    this.channels = channels == null ? Set.of() : Set.copyOf(channels);
    this.expiryTime = expiryTime;
  }
}
