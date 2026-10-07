package lk.sliit.se3070.group058.dews.shared.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The details an officer enters to issue a warning (the parameter object of the warning
 * operations).
 *
 * <p>Implements CH-22.
 *
 * @param warningLevel the level of the warning
 * @param message the warning message in the three languages
 * @param instructions the instructions to the public in the three languages
 * @param targetDistricts the districts the warning is sent to
 * @param targetRiverBasins the river basins the warning covers
 * @param channels the channels the warning is sent through
 * @param expiryTime the end of the warning's validity
 */
public record WarningDetails(
    WarningLevel warningLevel,
    LocalisedText message,
    LocalisedText instructions,
    List<District> targetDistricts,
    List<RiverBasin> targetRiverBasins,
    Set<NotificationChannel> channels,
    LocalDateTime expiryTime) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public WarningDetails(
      WarningLevel warningLevel,
      LocalisedText message,
      LocalisedText instructions,
      List<District> targetDistricts,
      List<RiverBasin> targetRiverBasins,
      Set<NotificationChannel> channels,
      LocalDateTime expiryTime) {
    this.warningLevel = Objects.requireNonNull(warningLevel, "warningLevel");
    this.message = Objects.requireNonNull(message, "message");
    this.instructions = Objects.requireNonNull(instructions, "instructions");
    this.targetDistricts = List.copyOf(targetDistricts);
    this.targetRiverBasins = List.copyOf(targetRiverBasins);
    this.channels = Set.copyOf(channels);
    this.expiryTime = Objects.requireNonNull(expiryTime, "expiryTime");
  }
}
