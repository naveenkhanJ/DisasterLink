package lk.sliit.se3070.group058.dews.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link GeoDistance}. */
class GeoDistanceTest {

  private static final GeoPoint COLOMBO = new GeoPoint(6.9271, 79.8612, null);
  private static final GeoPoint KANDY = new GeoPoint(7.2906, 80.6337, null);

  @Test
  @DisplayName("The distance from a point to itself is zero")
  void samePointHasZeroDistance() {
    assertThat(GeoDistance.haversineKm(COLOMBO, COLOMBO)).isZero();
  }

  @Test
  @DisplayName("One degree of longitude on the equator is about 111.195 km")
  void oneDegreeOfLongitudeOnTheEquator() {
    GeoPoint origin = new GeoPoint(0.0, 0.0, null);
    GeoPoint oneDegreeEast = new GeoPoint(0.0, 1.0, null);

    assertThat(GeoDistance.haversineKm(origin, oneDegreeEast)).isCloseTo(111.195, within(0.01));
  }

  @Test
  @DisplayName("Colombo to Kandy is about 94.34 km")
  void colomboToKandy() {
    // Computed independently with Python's math module (haversine, radius 6371.0088 km):
    // 94.33556 km for 6.9271 N 79.8612 E and 7.2906 N 80.6337 E.
    assertThat(GeoDistance.haversineKm(COLOMBO, KANDY)).isCloseTo(94.3356, within(0.001));
  }

  @Test
  @DisplayName("The distance is the same in both directions")
  void distanceIsSymmetric() {
    assertThat(GeoDistance.haversineKm(COLOMBO, KANDY))
        .isEqualTo(GeoDistance.haversineKm(KANDY, COLOMBO));
  }

  @Test
  @DisplayName("A null point is rejected")
  void nullPointIsRejected() {
    assertThatNullPointerException().isThrownBy(() -> GeoDistance.haversineKm(null, KANDY));
    assertThatNullPointerException().isThrownBy(() -> GeoDistance.haversineKm(COLOMBO, null));
  }

  @Test
  @DisplayName("The utility class cannot be instantiated")
  void constructorThrowsAssertionError() throws ReflectiveOperationException {
    Constructor<GeoDistance> constructor = GeoDistance.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(AssertionError.class);
  }
}
