package lk.sliit.se3070.group058.dews.shared.domain;

import jakarta.persistence.Embeddable;

/**
 * A place on the Earth, with the accuracy of the fix when the phone supplied one.
 *
 * <p>Implements CH-26.
 *
 * @param latitude the latitude in decimal degrees, north positive
 * @param longitude the longitude in decimal degrees, east positive
 * @param accuracyMetres the accuracy of the fix in metres, or {@code null} if it is not known
 */
@Embeddable
public record GeoPoint(double latitude, double longitude, Double accuracyMetres) {}
