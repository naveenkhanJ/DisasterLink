package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A hazard report as the citizen app sends it; the client-only members of the design record are not
 * part of the Java type.
 *
 * <p>Implements CH-26.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a field the citizen has not entered is
 * {@code null}, so that {@code ReportSubmissionController.validateReport} and {@code
 * ReportCaptureController.validateDraft} report it. {@code draftId} and {@code capturedAt} are made
 * by the device, not typed by the citizen, and stay required.
 *
 * @param draftId the identifier made on the device, sent as the submission identifier
 * @param hazardType the type of the hazard, or {@code null} if not entered — checked by {@code
 *     ReportSubmissionController.validateReport}
 * @param description the citizen's description, or {@code null} if not entered — checked by {@code
 *     ReportSubmissionController.validateReport}
 * @param location the place of the hazard, or {@code null} if not entered — checked by {@code
 *     ReportSubmissionController.validateReport}
 * @param locationFromGps whether the location came from the phone's GPS
 * @param district the district of the hazard, or {@code null} if not entered — checked by {@code
 *     ReportSubmissionController.validateReport}
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
   * Creates the record, checking the two structural components.
   *
   * @throws NullPointerException if {@code draftId} or {@code capturedAt} is {@code null}
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
    this.hazardType = hazardType;
    this.description = description;
    this.location = location;
    this.locationFromGps = locationFromGps;
    this.district = district;
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
  }
}
