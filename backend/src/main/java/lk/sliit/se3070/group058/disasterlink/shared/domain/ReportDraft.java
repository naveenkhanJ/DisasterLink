package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A hazard report as the citizen app sends it; the client-only members of the design record are not
 * part of the Java type.
 *
 * <p>Implements CH-26.
 *
 * @param draftId the identifier made on the device, sent as the submission identifier
 * @param hazardType the type of the hazard
 * @param description the citizen's description
 * @param location the place of the hazard
 * @param locationFromGps whether the location came from the phone's GPS
 * @param district the district of the hazard
 * @param capturedAt when the report was captured on the phone
 */
public record ReportDraft(
    String draftId,
    HazardType hazardType,
    String description,
    GeoPoint location,
    boolean locationFromGps,
    District district,
    LocalDateTime capturedAt) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ReportDraft(
      String draftId,
      HazardType hazardType,
      String description,
      GeoPoint location,
      boolean locationFromGps,
      District district,
      LocalDateTime capturedAt) {
    this.draftId = Objects.requireNonNull(draftId, "draftId");
    this.hazardType = Objects.requireNonNull(hazardType, "hazardType");
    this.description = Objects.requireNonNull(description, "description");
    this.location = Objects.requireNonNull(location, "location");
    this.locationFromGps = locationFromGps;
    this.district = Objects.requireNonNull(district, "district");
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
  }
}
