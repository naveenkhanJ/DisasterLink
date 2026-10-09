package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistributionLine;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReliefSupply;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ResourceDistribution;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyCategory;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyStatus;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ResourceDistributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link DistributionCalculator}.
 *
 * <p>Implements tests for CH-20, CH-31, CH-32, FX-08, FX-17 and SC4 step 7.
 */
@ExtendWith(MockitoExtension.class)
class DistributionCalculatorTest {

  @Mock private ResourceDistributionRepository resourceDistributionRepository;

  private DistributionCalculator calculator;
  private ReportCriteria criteria;

  @BeforeEach
  void setUp() {
    calculator = new DistributionCalculator(resourceDistributionRepository);
    criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.RESOURCE_DISTRIBUTION),
            null);
  }

  @Test
  @DisplayName("Constructor rejects null arguments")
  void constructorRejectsNull() {
    assertThatThrownBy(() -> new DistributionCalculator(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("supports() identifies only RESOURCE_DISTRIBUTION")
  void supportsMetric() {
    assertThat(calculator.supports(ReportMetric.RESOURCE_DISTRIBUTION)).isTrue();
    assertThat(calculator.supports(ReportMetric.ALERT_TIMELINE)).isFalse();
    assertThat(calculator.supports(ReportMetric.CITIZENS_REACHED)).isFalse();
    assertThat(calculator.supports(ReportMetric.SHELTER_OCCUPANCY_OVER_TIME)).isFalse();
  }

  @Test
  @DisplayName("calculate() rejects null criteria")
  void calculateRejectsNull() {
    assertThatThrownBy(() -> calculator.calculate(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("SC4 step 7 / empty results when no matching distributions exist")
  void returnsEmptyResultsWhenNoDistributions() {
    when(resourceDistributionRepository.findMatching(criteria)).thenReturn(List.of());

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isZero();
    assertThat(results.distributionLines()).isEmpty();
    assertThat(results.failedMetrics()).isEmpty();
  }

  @Test
  @DisplayName(
      "SC4 step 7 / FX-08, FX-17 — maps organization and citizen donor distributions to lines")
  void mapsDistributionsToLines() {
    DistrictOfficer officer = SampleEntities.districtOfficer("OFF-1");
    Hazard hazard = SampleEntities.hazard("HZ-1");

    Organization org = SampleEntities.organization("ORG-1");
    ReliefSupply orgSupply =
        new ReliefSupply(
            "SUP-1",
            "Dry rations",
            SupplyCategory.FOOD,
            1200.0,
            "dry-ration packs",
            "Ratnapura Warehouse",
            SupplyStatus.AVAILABLE,
            org,
            null);
    ResourceDistribution distOrg =
        new ResourceDistribution(
            "RD-1",
            1200.0,
            LocalDateTime.of(2026, 5, 21, 10, 0),
            District.RATNAPURA,
            "Elapatha",
            orgSupply,
            null,
            officer,
            hazard);

    Citizen citizen = SampleEntities.citizen("CIT-1", "tok-1");
    ReliefSupply citizenSupply =
        new ReliefSupply(
            "SUP-2",
            "Rice and dhal",
            SupplyCategory.FOOD,
            480.0,
            "dry-ration packs",
            "Community Centre",
            SupplyStatus.AVAILABLE,
            null,
            citizen);
    ResourceDistribution distCitizen =
        new ResourceDistribution(
            "RD-2",
            480.0,
            LocalDateTime.of(2026, 5, 22, 11, 0),
            District.RATNAPURA,
            "Pelmadulla",
            citizenSupply,
            null,
            officer,
            hazard);

    when(resourceDistributionRepository.findMatching(criteria))
        .thenReturn(List.of(distOrg, distCitizen));

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isEqualTo(2);
    assertThat(results.distributionLines()).hasSize(2);

    DistributionLine line1 = results.distributionLines().get(0);
    assertThat(line1.district()).isEqualTo(District.RATNAPURA);
    assertThat(line1.ownerId()).isEqualTo("ORG-1");
    assertThat(line1.ownerName()).isEqualTo(org.getName());
    assertThat(line1.category()).isEqualTo(SupplyCategory.FOOD);
    assertThat(line1.unit()).isEqualTo("dry-ration packs");
    assertThat(line1.quantity()).isEqualTo(1200.0);

    DistributionLine line2 = results.distributionLines().get(1);
    assertThat(line2.district()).isEqualTo(District.RATNAPURA);
    assertThat(line2.ownerId()).isNull();
    assertThat(line2.ownerName()).isEqualTo("Private individual");
    assertThat(line2.category()).isEqualTo(SupplyCategory.FOOD);
    assertThat(line2.unit()).isEqualTo("dry-ration packs");
    assertThat(line2.quantity()).isEqualTo(480.0);
  }
}
