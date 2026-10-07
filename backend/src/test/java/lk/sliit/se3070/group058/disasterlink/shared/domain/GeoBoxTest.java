package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link GeoBox} and of the distances of the points the repository tests use. */
class GeoBoxTest {

  private static final double RADIUS_KM = 5.0;

  @Test
  @DisplayName("The fixture points are about 3 km and 7 km from the origin")
  void fixturePointsHaveTheIntendedDistances() {
    assertThat(GeoDistance.haversineKm(FeatureFixtures.ORIGIN, FeatureFixtures.NORTH_3_KM))
        .isCloseTo(3.0, within(0.05));
    assertThat(GeoDistance.haversineKm(FeatureFixtures.ORIGIN, FeatureFixtures.NORTH_7_KM))
        .isCloseTo(7.0, within(0.05));
    assertThat(GeoDistance.haversineKm(FeatureFixtures.ORIGIN, FeatureFixtures.DIAGONAL))
        .isGreaterThan(RADIUS_KM);
  }

  @Test
  @DisplayName("The box is centred on the point and as wide as the radius needs")
  void boxIsCentredOnThePoint() {
    GeoBox box = GeoBox.around(FeatureFixtures.ORIGIN, RADIUS_KM);

    assertThat((box.minLatitude() + box.maxLatitude()) / 2).isCloseTo(6.9271, within(1e-9));
    assertThat((box.minLongitude() + box.maxLongitude()) / 2).isCloseTo(79.8612, within(1e-9));
    assertThat(box.maxLatitude() - FeatureFixtures.ORIGIN.latitude())
        .isCloseTo(RADIUS_KM / 111.195, within(0.001));
  }

  @Test
  @DisplayName("The box contains a point within the radius and the diagonal point outside it")
  void boxContainsInsideAndDiagonalPoints() {
    GeoBox box = GeoBox.around(FeatureFixtures.ORIGIN, RADIUS_KM);

    for (GeoPoint point : new GeoPoint[] {FeatureFixtures.NORTH_3_KM, FeatureFixtures.DIAGONAL}) {
      assertThat(point.latitude()).isBetween(box.minLatitude(), box.maxLatitude());
      assertThat(point.longitude()).isBetween(box.minLongitude(), box.maxLongitude());
    }
    assertThat(FeatureFixtures.NORTH_7_KM.latitude()).isGreaterThan(box.maxLatitude());
  }

  @Test
  @DisplayName("A null centre is rejected")
  void nullCentreIsRejected() {
    assertThatNullPointerException().isThrownBy(() -> GeoBox.around(null, RADIUS_KM));
  }
}
