package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;

/**
 * The {@code draft} part of a report upload: the Java {@code ReportDraft} on the wire, with {@code
 * capturedAt} as ISO-8601 with its offset (for example {@code 2026-05-21T07:40:00+05:30}).
 *
 * <p>Bean Validation checks the request's shape only: the two structural fields of {@code
 * ReportDraft}, {@code draftId} and {@code capturedAt}, must be present. The fields the citizen
 * enters (type, description, location, district) may be missing; {@code
 * ReportSubmissionController.validateReport} answers a missing one with a REJECTED reply, never a
 * 400 (standards section 4, "Input validation"; R4G-12).
 *
 * <p>Implementation-only wire record.
 *
 * @param draftId the identifier made on the phone
 * @param hazardType the type of the hazard, or {@code null} if not entered
 * @param description the citizen's description, or {@code null} if not entered
 * @param location the place of the hazard, or {@code null} if not entered
 * @param locationFromGps whether the location came from the phone's GPS
 * @param district the district of the hazard, or {@code null} if not entered
 * @param capturedAt when the report was captured on the phone
 */
public record ReportDraftRequest(
    @NotBlank String draftId,
    HazardType hazardType,
    String description,
    @Valid GeoPointRequest location,
    boolean locationFromGps,
    District district,
    @NotNull OffsetDateTime capturedAt) {}
