package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

import java.time.OffsetDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;

/**
 * A hazard report as the citizen app reads it (standards section 4, "No entities on the wire").
 * Times carry their {@code +05:30} offset (section 12.1, item 3). It has no reporter fields and no
 * photo file name: the app does not need them, and the console reads the photo through {@code GET
 * /reports/{reportId}/photo}.
 *
 * <p>Implementation-only wire record.
 *
 * @param reportId the identifier of the report
 * @param hazardType the type of the hazard
 * @param description the citizen's description
 * @param location the place of the hazard
 * @param locationFromGps whether the location came from the phone's GPS
 * @param district the district of the hazard
 * @param capturedAt when the report was captured on the phone
 * @param reportTime when the server received the report
 * @param status the status of the report
 * @param verificationTime the time of the officer's decision, or {@code null} if undecided
 * @param rejectionReason the reason for a rejection, or {@code null}
 * @param hazardId the identifier of the linked hazard, or {@code null} if none
 */
public record HazardReportResponse(
    String reportId,
    HazardType hazardType,
    String description,
    GeoPointResponse location,
    boolean locationFromGps,
    District district,
    OffsetDateTime capturedAt,
    OffsetDateTime reportTime,
    ReportStatus status,
    OffsetDateTime verificationTime,
    String rejectionReason,
    String hazardId) {}
