package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A place on the wire, as the app sends it inside a report draft. Bean Validation checks only that
 * a place that is present has its two numbers; whether the place lies within Sri Lanka is a rule of
 * {@code ReportSubmissionController.validateReport}, not of the request's shape (standards section
 * 4, "Input validation").
 *
 * <p>Implementation-only wire record.
 *
 * @param latitude the latitude in decimal degrees, north positive
 * @param longitude the longitude in decimal degrees, east positive
 * @param accuracyMetres the accuracy of the fix in metres, or {@code null} if it is not known
 */
public record GeoPointRequest(
    @NotNull Double latitude, @NotNull Double longitude, Double accuracyMetres) {}
