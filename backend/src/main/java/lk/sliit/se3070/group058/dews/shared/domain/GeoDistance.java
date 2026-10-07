package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.Objects;

/**
 * The one haversine distance of the system, shared by the verification control and by the geo
 * finders of the repositories so that the formula is written once.
 *
 * <p>Implements P62 FX-02 and R4G-18 (model dictionary section 6).
 */
public final class GeoDistance {

  /**
   * The mean radius of the Earth in kilometres, as recommended by the International Union of
   * Geodesy and Geophysics (IUGG): 6371.0088 km.
   */
  private static final double EARTH_MEAN_RADIUS_KM = 6371.0088;

  private static final double HALF = 0.5;

  private GeoDistance() {
    throw new AssertionError("GeoDistance is a utility class");
  }

  /**
   * Computes the great-circle distance between two points with the haversine formula.
   *
   * @param a the first point
   * @param b the second point
   * @return the distance in kilometres
   * @throws NullPointerException if a point is {@code null}
   */
  public static double haversineKm(GeoPoint a, GeoPoint b) {
    Objects.requireNonNull(a, "a");
    Objects.requireNonNull(b, "b");
    double latitudeA = Math.toRadians(a.latitude());
    double latitudeB = Math.toRadians(b.latitude());
    double latitudeDelta = latitudeB - latitudeA;
    double longitudeDelta = Math.toRadians(b.longitude() - a.longitude());
    double sinHalfLatitude = Math.sin(latitudeDelta * HALF);
    double sinHalfLongitude = Math.sin(longitudeDelta * HALF);
    double haversine =
        sinHalfLatitude * sinHalfLatitude
            + Math.cos(latitudeA) * Math.cos(latitudeB) * sinHalfLongitude * sinHalfLongitude;
    double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return EARTH_MEAN_RADIUS_KM * centralAngle;
  }
}
