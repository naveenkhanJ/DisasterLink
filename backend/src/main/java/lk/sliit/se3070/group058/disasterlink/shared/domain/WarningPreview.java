package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What the officer sees before confirming a broadcast.
 *
 * <p>Implements CH-23, CH-24 and CH-06.
 *
 * @param hazardType the type of the hazard the warning is about
 * @param warningLevel the level of the warning
 * @param coveredDistricts the districts the warning covers
 * @param message the warning message in the three languages
 * @param instructions the instructions in the three languages
 * @param channels the channels the warning will use
 * @param expiryTime the end of the warning's validity, shown before confirmation
 * @param citizensPerDistrict the number of citizens per covered district
 * @param organizationCount the number of organisations that will be notified
 * @param districtOfficerCount the number of district officers that will be notified
 */
public record WarningPreview(
    HazardType hazardType,
    WarningLevel warningLevel,
    Set<District> coveredDistricts,
    LocalisedText message,
    LocalisedText instructions,
    Set<NotificationChannel> channels,
    LocalDateTime expiryTime,
    Map<District, Integer> citizensPerDistrict,
    int organizationCount,
    int districtOfficerCount) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public WarningPreview(
      HazardType hazardType,
      WarningLevel warningLevel,
      Set<District> coveredDistricts,
      LocalisedText message,
      LocalisedText instructions,
      Set<NotificationChannel> channels,
      LocalDateTime expiryTime,
      Map<District, Integer> citizensPerDistrict,
      int organizationCount,
      int districtOfficerCount) {
    this.hazardType = Objects.requireNonNull(hazardType, "hazardType");
    this.warningLevel = Objects.requireNonNull(warningLevel, "warningLevel");
    this.coveredDistricts = Set.copyOf(coveredDistricts);
    this.message = Objects.requireNonNull(message, "message");
    this.instructions = Objects.requireNonNull(instructions, "instructions");
    this.channels = Set.copyOf(channels);
    this.expiryTime = Objects.requireNonNull(expiryTime, "expiryTime");
    this.citizensPerDistrict = Map.copyOf(citizensPerDistrict);
    this.organizationCount = organizationCount;
    this.districtOfficerCount = districtOfficerCount;
  }
}
