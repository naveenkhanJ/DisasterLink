package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

/**
 * A place on the wire, as the server sends it inside a report.
 *
 * <p>Implementation-only wire record.
 *
 * @param latitude the latitude in decimal degrees, north positive
 * @param longitude the longitude in decimal degrees, east positive
 * @param accuracyMetres the accuracy of the fix in metres, or {@code null} if it is not known
 */
public record GeoPointResponse(double latitude, double longitude, Double accuracyMetres) {}
