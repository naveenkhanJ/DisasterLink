package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the smaller entities: client status, relief supply, audit record, submissions, users.
 */
class SmallEntitiesTest {

  @Test
  @DisplayName("recordSync sets the last contact and the pending count")
  void recordSyncSetsBothValues() {
    ClientSyncStatus status = new ClientSyncStatus("client-1", District.KALUTARA, NOW, 2);

    status.recordSync(NOW.plusHours(1), 0);

    assertThat(status.getLastSyncAt()).isEqualTo(NOW.plusHours(1));
    assertThat(status.getPendingCount()).isZero();
  }

  @Test
  @DisplayName("recordSync rejects a negative pending count and keeps the old state")
  void recordSyncRejectsNegativeCount() {
    ClientSyncStatus status = new ClientSyncStatus("client-1", District.KALUTARA, NOW, 2);

    assertThatIllegalArgumentException().isThrownBy(() -> status.recordSync(NOW.plusHours(1), -1));
    assertThat(status.getPendingCount()).isEqualTo(2);
    assertThat(status.getLastSyncAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("ClientSyncStatus rejects a negative count and a null time")
  void clientSyncStatusChecksItsArguments() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ClientSyncStatus("c", District.GALLE, NOW, -1));
    ClientSyncStatus status = new ClientSyncStatus("c", District.GALLE, NOW, 0);
    assertThatNullPointerException().isThrownBy(() -> status.recordSync(null, 0));
  }

  @Test
  @DisplayName("A relief supply has an organisation donor")
  void supplyWithOrganisationDonor() {
    Organization donor = SampleEntities.organization("ORG-0001");

    ReliefSupply supply = supply(donor, null);

    assertThat(supply.getDonorOrganization()).containsSame(donor);
    assertThat(supply.getDonorCitizen()).isEmpty();
  }

  @Test
  @DisplayName("A relief supply has a citizen donor")
  void supplyWithCitizenDonor() {
    Citizen donor = SampleEntities.citizen("CIT-KLT-0001", null);

    ReliefSupply supply = supply(null, donor);

    assertThat(supply.getDonorCitizen()).containsSame(donor);
    assertThat(supply.getDonorOrganization()).isEmpty();
  }

  @Test
  @DisplayName("A relief supply may have no donor")
  void supplyWithNoDonor() {
    ReliefSupply supply = supply(null, null);

    assertThat(supply.getDonorOrganization()).isEmpty();
    assertThat(supply.getDonorCitizen()).isEmpty();
  }

  @Test
  @DisplayName("A relief supply with two donors is rejected (the xor constraint)")
  void supplyWithTwoDonorsIsRejected() {
    Organization organization = SampleEntities.organization("ORG-0001");
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0001", null);

    assertThatIllegalArgumentException().isThrownBy(() -> supply(organization, citizen));
  }

  @Test
  @DisplayName("A distribution keeps its required and optional associations")
  void distributionKeepsItsAssociations() {
    ReliefSupply supply = supply(null, null);
    DistrictOfficer coordinator = SampleEntities.districtOfficer("DO-COL-01");

    ResourceDistribution distribution =
        new ResourceDistribution(
            "DIST-1", 40.0, NOW, District.COLOMBO, "Camp A", supply, null, coordinator, null);

    assertThat(distribution.getReliefSupply()).isSameAs(supply);
    assertThat(distribution.getCoordinator()).isSameAs(coordinator);
    assertThat(distribution.getReceivingCitizen()).isEmpty();
    assertThat(distribution.getHazard()).isEmpty();
    assertThat(distribution.getQuantity()).isEqualTo(40.0);
    assertThat(distribution.getDestinationDistrict()).isEqualTo(District.COLOMBO);
  }

  @Test
  @DisplayName("An audit record gets its own identifier and keeps the optional detail")
  void auditRecord() {
    DMCDutyOfficer officer = SampleEntities.dutyOfficer("DMC-2");

    AuditRecord record =
        new AuditRecord(
            AuditAction.WARNING_ESCALATED, officer, "WRN-1", "LEVEL_2_AMBER→LEVEL_3_RED", NOW);
    AuditRecord withoutDetail =
        new AuditRecord(AuditAction.REPORT_GENERATED, officer, "DR-1", null, NOW);

    assertThat(record.getAuditId()).hasSize(36).isNotEqualTo(withoutDetail.getAuditId());
    assertThat(record.getDetail()).contains("LEVEL_2_AMBER→LEVEL_3_RED");
    assertThat(withoutDetail.getDetail()).isEmpty();
    assertThat(record.getPerformedBy()).isSameAs(officer);
    assertThat(record.getSubjectId()).isEqualTo("WRN-1");
  }

  @Test
  @DisplayName("A processed submission keeps its optional result identifier")
  void processedSubmission() {
    ProcessedSubmission withResult =
        new ProcessedSubmission("sub-1", "client-1", NOW, NOW.plusMinutes(1), "RPT-2026-KLT-1000");
    ProcessedSubmission withoutResult =
        new ProcessedSubmission("sub-2", "client-1", NOW, NOW.plusMinutes(1), null);

    assertThat(withResult.getResultId()).contains("RPT-2026-KLT-1000");
    assertThat(withoutResult.getResultId()).isEmpty();
    assertThat(withResult.getReceivedAt()).isEqualTo(NOW.plusMinutes(1));
  }

  @Test
  @DisplayName("Users and organisations reject a null required argument")
  void usersRejectNull() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Citizen(null, "n", "p", "e", "nic", "a", District.GALLE, Language.TAMIL, null));
    assertThatNullPointerException()
        .isThrownBy(() -> new DistrictOfficer("id", "n", "p", "e", null, Language.TAMIL));
    assertThatNullPointerException()
        .isThrownBy(() -> new DMCOfficer("id", "n", "p", "e", "pos", null));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Organization(
                    "o", "n", OrganizationType.NGO, "c", "a", java.util.List.of(), null));
  }

  @Test
  @DisplayName("A DMC officer keeps its reporting districts as an unmodifiable view")
  void officerReportingDistricts() {
    DMCOfficer officer =
        SampleEntities.officer("DMC-3", Set.of(District.COLOMBO, District.GAMPAHA));

    assertThat(officer.getReportingDistricts())
        .containsExactlyInAnyOrder(District.COLOMBO, District.GAMPAHA);
    assertThat(officer.getPosition()).isEqualTo("Director");
  }

  private static ReliefSupply supply(Organization organization, Citizen citizen) {
    return new ReliefSupply(
        "SUP-1",
        "Rice",
        SupplyCategory.FOOD,
        500.0,
        "kg",
        "Colombo store",
        SupplyStatus.AVAILABLE,
        organization,
        citizen);
  }
}
