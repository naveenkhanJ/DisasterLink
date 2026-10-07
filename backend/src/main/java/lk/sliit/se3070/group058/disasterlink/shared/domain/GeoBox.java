package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Objects;

/**
 * A rectangle of latitudes and longitudes that contains the circle of a given radius around a
 * point. The geo finders of the repositories select candidates inside the box with a query and then
 * keep those within the radius with {@link GeoDistance#haversineKm}, so the box is written once.
 *
 * <p>Implementation-only value type of the model dictionary section 6 (P62 FX-02, R3G-29, R4G-18):
 * the "bounding-box query" of {@code findActiveNear} and {@code findNearby}. The kilometres per
 * degree come from {@code GeoDistance}, so the Earth's radius is defined in one place only.
 *
 * @param minLatitude the southern edge in degrees
 * @param maxLatitude the northern edge in degrees
 * @param minLongitude the western edge in degrees
 * @param maxLongitude the eastern edge in degrees
 */
public record GeoBox(
    double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {

  /** Widens the box by one percent, because a degree of longitude shrinks towards the poles. */
  private static final double MARGIN = 1.01;

  /**
   * Computes the box around a point.
   *
   * @param centre the centre of the circle
   * @param radiusKm the radius in kilometres
   * @return a box that contains every point within {@code radiusKm} of {@code centre}
   * @throws NullPointerException if {@code centre} is {@code null}
   */
  public static GeoBox around(GeoPoint centre, double radiusKm) {
    Objects.requireNonNull(centre, "centre");
    double kmPerDegreeLatitude =
        GeoDistance.haversineKm(
            centre, new GeoPoint(centre.latitude() + 1.0, centre.longitude(), null));
    double kmPerDegreeLongitude =
        GeoDistance.haversineKm(
            centre, new GeoPoint(centre.latitude(), centre.longitude() + 1.0, null));
    double latitudeDelta = MARGIN * radiusKm / kmPerDegreeLatitude;
    double longitudeDelta = MARGIN * radiusKm / kmPerDegreeLongitude;
    return new GeoBox(
        centre.latitude() - latitudeDelta,
        centre.latitude() + latitudeDelta,
        centre.longitude() - longitudeDelta,
        centre.longitude() + longitudeDelta);
  }
}
