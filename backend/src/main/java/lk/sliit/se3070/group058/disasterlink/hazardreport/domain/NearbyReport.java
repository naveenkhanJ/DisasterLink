package lk.sliit.se3070.group058.disasterlink.hazardreport.domain;

import java.util.Objects;

/**
 * A report found near the selected report, with its distance from it.
 *
 * <p>Implements CH-26 and CH-27 (P62 FX-02); it lives beside the entity it references.
 *
 * @param report the nearby report
 * @param distanceKm the distance from the selected report in kilometres, computed with {@code
 *     GeoDistance.haversineKm}
 */
public record NearbyReport(HazardReport report, double distanceKm) {

  /**
   * Creates the value.
   *
   * @throws NullPointerException if {@code report} is {@code null}
   */
  public NearbyReport(HazardReport report, double distanceKm) {
    this.report = Objects.requireNonNull(report, "report");
    this.distanceKm = distanceKm;
  }
}
