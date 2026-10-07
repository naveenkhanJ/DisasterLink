package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Persists every shared entity, flushes, clears the persistence context and reads it back, so the
 * mapping (inheritance, embeddables, element collections, associations) is exercised against H2.
 */
@DataJpaTest
class PersistenceRoundTripTest {

  @Autowired private TestEntityManager entityManager;

  private <T> T reload(T entity, Object id) {
    entityManager.persistAndFlush(entity);
    entityManager.clear();
    @SuppressWarnings("unchecked")
    Class<T> type = (Class<T>) entity.getClass();
    T loaded = entityManager.find(type, id);
    assertThat(loaded).isNotSameAs(entity).isEqualTo(entity).hasSameHashCodeAs(entity);
    return loaded;
  }

  @Test
  @DisplayName("A citizen is stored with all its fields, including the optional device token")
  void citizenRoundTrip() {
    Citizen citizen = reload(SampleEntities.citizen("CIT-KLT-0001", "token-1"), "CIT-KLT-0001");

    assertThat(citizen.getName()).isEqualTo("Nimal Perera");
    assertThat(citizen.getNic()).isEqualTo("199012345678");
    assertThat(citizen.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.SINHALA);
    assertThat(citizen.getDeviceToken()).contains("token-1");
  }

  @Test
  @DisplayName("A citizen without a device token comes back with an empty token")
  void citizenWithoutTokenRoundTrip() {
    Citizen citizen = reload(SampleEntities.citizen("CIT-KLT-0002", null), "CIT-KLT-0002");

    assertThat(citizen.getDeviceToken()).isEmpty();
  }

  @Test
  @DisplayName("A volunteer keeps its organisation (A24)")
  void volunteerRoundTrip() {
    Organization group = SampleEntities.volunteerGroup("ORG-0001");
    entityManager.persist(group);

    CommunityVolunteer volunteer =
        reload(SampleEntities.volunteer("CIT-RAT-0001", group), "CIT-RAT-0001");

    assertThat(volunteer.getGnDivision()).isEqualTo("GN-12");
    assertThat(volunteer.getOrganization()).isPresent();
    assertThat(volunteer.getOrganization().orElseThrow().getOrganizationId()).isEqualTo("ORG-0001");
  }

  @Test
  @DisplayName("Officers keep their reporting districts and district")
  void officersRoundTrip() {
    DMCOfficer officer =
        reload(
            SampleEntities.officer("DMC-1", Set.of(District.COLOMBO, District.GAMPAHA)), "DMC-1");

    assertThat(officer.getReportingDistricts())
        .containsExactlyInAnyOrder(District.COLOMBO, District.GAMPAHA);

    DistrictOfficer districtOfficer =
        reload(SampleEntities.districtOfficer("DO-COL-01"), "DO-COL-01");

    assertThat(districtOfficer.getDistrict()).isEqualTo(District.COLOMBO);
    assertThat(districtOfficer.getPreferredLanguage()).isEqualTo(Language.ENGLISH);
  }

  @Test
  @DisplayName("The discriminator loads the right subclass when users are read as User")
  void discriminatorLoadsSubclasses() {
    entityManager.persist(SampleEntities.citizen("U-CIT", "token"));
    entityManager.persist(SampleEntities.dutyOfficer("U-DUTY"));
    entityManager.persist(SampleEntities.districtOfficer("U-DO"));
    entityManager.flush();
    entityManager.clear();

    List<User> users =
        entityManager
            .getEntityManager()
            .createQuery("select u from User u order by u.userId", User.class)
            .getResultList();

    assertThat(users).hasSize(3);
    assertThat(users.get(0)).isInstanceOf(Citizen.class);
    assertThat(users.get(1)).isInstanceOf(DistrictOfficer.class);
    assertThat(users.get(2)).isInstanceOf(DMCDutyOfficer.class);
  }

  @Test
  @DisplayName("An organisation keeps its districts in order")
  void organizationRoundTrip() {
    Organization organization = reload(SampleEntities.organization("ORG-0001"), "ORG-0001");

    assertThat(organization.getType()).isEqualTo(OrganizationType.NGO);
    assertThat(organization.getDistricts()).containsExactly(District.COLOMBO, District.GAMPAHA);
  }

  @Test
  @DisplayName("A hazard keeps its location, districts, basin and optional end time")
  void hazardRoundTrip() {
    Hazard hazard = reload(SampleEntities.hazard("HZ-2026-0201"), "HZ-2026-0201");

    assertThat(hazard.getLocation()).isEqualTo(SampleEntities.RATNAPURA_POINT);
    assertThat(hazard.getDistricts()).containsExactly(District.RATNAPURA, District.KALUTARA);
    assertThat(hazard.getRiverBasin()).contains(RiverBasin.KALU);
    assertThat(hazard.getEndTime()).isEmpty();
    assertThat(hazard.getDescription()).isEqualTo("Kalu Ganga is rising");
    assertThat(hazard.getStartTime()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("A hazard without accuracy and basin comes back without them")
  void hazardWithoutOptionalValues() {
    Hazard hazard =
        reload(
            new Hazard(
                "HZ-2",
                HazardType.LANDSLIDE,
                "Slip",
                HazardSeverity.HIGH,
                NOW,
                NOW.plusDays(1),
                new GeoPoint(6.5, 80.1, null),
                "Hill",
                List.of(District.KEGALLE),
                null,
                HazardSource.VERIFIED_REPORT,
                HazardStatus.CLOSED),
            "HZ-2");

    assertThat(hazard.getLocation().accuracyMetres()).isNull();
    assertThat(hazard.getRiverBasin()).isEmpty();
    assertThat(hazard.getEndTime()).contains(NOW.plusDays(1));
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.CLOSED);
  }

  @Test
  @DisplayName("A warning keeps both texts, its lists, channels, associations and notifications")
  void warningRoundTrip() {
    Hazard hazard = SampleEntities.hazard("HZ-2026-0201");
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    entityManager.persist(hazard);
    entityManager.persist(officer);
    Warning warning =
        SampleEntities.warning(
            "WRN-2026-0101",
            hazard,
            officer,
            List.of(District.COLOMBO),
            List.of(RiverBasin.KALU, RiverBasin.KELANI),
            NOW.plusHours(6));
    entityManager.persist(warning);
    entityManager.flush();
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0001", "token");
    entityManager.persist(citizen);
    entityManager.clear();

    Warning loaded = entityManager.find(Warning.class, "WRN-2026-0101");

    assertThat(loaded.getMessage()).isEqualTo(SampleEntities.TEXT);
    assertThat(loaded.getInstructions().english()).isEqualTo("Move to higher ground");
    assertThat(loaded.getTargetDistricts()).containsExactly(District.COLOMBO);
    assertThat(loaded.getTargetRiverBasins()).containsExactly(RiverBasin.KALU, RiverBasin.KELANI);
    assertThat(loaded.getChannels())
        .containsExactlyInAnyOrder(NotificationChannel.PUSH, NotificationChannel.SMS);
    assertThat(loaded.getHazard().getHazardId()).isEqualTo("HZ-2026-0201");
    assertThat(loaded.getOfficer().getUserId()).isEqualTo("DMC-1");
    assertThat(loaded.getExpiryTime()).isEqualTo(NOW.plusHours(6));
    assertThat(loaded.getWarningLevel()).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    assertThat(loaded.getNotifications()).isEmpty();
  }

  @Test
  @DisplayName("The two texts of a warning are stored in separate, named columns")
  void warningTextColumnNames() {
    @SuppressWarnings("unchecked")
    List<String> columns =
        entityManager
            .getEntityManager()
            .createNativeQuery(
                "select lower(column_name) from information_schema.columns"
                    + " where upper(table_name) = 'WARNING'")
            .getResultList();

    assertThat(columns)
        .contains(
            "message_sinhala",
            "message_tamil",
            "message_english",
            "instructions_sinhala",
            "instructions_tamil",
            "instructions_english");
  }

  @Test
  @DisplayName("A notification keeps its recipient kind and identifier, delivery status and time")
  void notificationRoundTrip() {
    Notification delivered =
        new Notification(
            RecipientKind.CITIZEN,
            "CIT-KLT-0001",
            NotificationChannel.SMS,
            "text",
            1,
            DeliveryStatus.DELIVERED,
            NOW);

    Notification loaded = reload(delivered, delivered.getNotificationId());

    assertThat(loaded.getRecipientKind()).isEqualTo(RecipientKind.CITIZEN);
    assertThat(loaded.getRecipientId()).isEqualTo("CIT-KLT-0001");
    assertThat(loaded.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    assertThat(loaded.getSentTime()).contains(NOW);
    assertThat(loaded.getRound()).isEqualTo(1);
    assertThat(loaded.getRecipient()).isEmpty();
  }

  @Test
  @DisplayName("Supplies and distributions keep their associations")
  void supplyAndDistributionRoundTrip() {
    Organization donor = SampleEntities.organization("ORG-0001");
    DistrictOfficer coordinator = SampleEntities.districtOfficer("DO-COL-01");
    Hazard hazard = SampleEntities.hazard("HZ-1");
    entityManager.persist(donor);
    entityManager.persist(coordinator);
    entityManager.persist(hazard);
    ReliefSupply supply =
        new ReliefSupply(
            "SUP-1",
            "Rice",
            SupplyCategory.FOOD,
            500.5,
            "kg",
            "Colombo store",
            SupplyStatus.AVAILABLE,
            donor,
            null);
    entityManager.persist(supply);
    ResourceDistribution distribution =
        new ResourceDistribution(
            "DIST-1", 40.25, NOW, District.COLOMBO, "Camp A", supply, null, coordinator, hazard);
    entityManager.persistAndFlush(distribution);
    entityManager.clear();

    ResourceDistribution loaded = entityManager.find(ResourceDistribution.class, "DIST-1");

    assertThat(loaded.getQuantity()).isEqualTo(40.25);
    assertThat(loaded.getReliefSupply().getName()).isEqualTo("Rice");
    assertThat(loaded.getReliefSupply().getDonorOrganization()).isPresent();
    assertThat(loaded.getReliefSupply().getDonorCitizen()).isEmpty();
    assertThat(loaded.getCoordinator().getUserId()).isEqualTo("DO-COL-01");
    assertThat(loaded.getHazard()).isPresent();
    assertThat(loaded.getReceivingCitizen()).isEmpty();
  }

  @Test
  @DisplayName("An audit record keeps the user who performed the action and its optional detail")
  void auditRecordRoundTrip() {
    DMCDutyOfficer officer = SampleEntities.dutyOfficer("DMC-2");
    entityManager.persist(officer);
    AuditRecord record =
        new AuditRecord(
            AuditAction.WARNING_ESCALATED, officer, "WRN-1", "LEVEL_2_AMBER→LEVEL_3_RED", NOW);

    AuditRecord loaded = reload(record, record.getAuditId());

    assertThat(loaded.getAction()).isEqualTo(AuditAction.WARNING_ESCALATED);
    assertThat(loaded.getDetail()).contains("LEVEL_2_AMBER→LEVEL_3_RED");
    assertThat(loaded.getPerformedBy().getUserId()).isEqualTo("DMC-2");
    assertThat(loaded.getPerformedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("Client status and processed submissions are stored as given")
  void clientStatusAndProcessedSubmissionRoundTrip() {
    ClientSyncStatus status = reload(new ClientSyncStatus("c-1", District.GALLE, NOW, 3), "c-1");
    ProcessedSubmission submission =
        reload(new ProcessedSubmission("s-1", "c-1", NOW, NOW.plusMinutes(2), "RPT-1"), "s-1");

    assertThat(status.getDistrict()).isEqualTo(District.GALLE);
    assertThat(status.getPendingCount()).isEqualTo(3);
    assertThat(status.getLastSyncAt()).isEqualTo(NOW);
    assertThat(submission.getResultId()).contains("RPT-1");
    assertThat(submission.getClientId()).isEqualTo("c-1");
    assertThat(submission.getCapturedAt()).isEqualTo(NOW);
  }
}
