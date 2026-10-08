package lk.sliit.se3070.group058.disasterlink.warning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DeliveryStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RecipientKind;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.SmsGateway;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves that a failed UC1 operation leaves the stored data unchanged (implementation standards
 * section 4, Exceptions step 3). The test's own transaction is switched off, so the boundary
 * exercised is the service's; whatever the arrange step writes commits, so the cleanup script runs
 * after each test.
 */
@DataJpaTest
@Import({
  WarningController.class,
  EscalationController.class,
  WarningBroadcaster.class,
  WarningTextValidator.class,
  WarningRollbackTest.FixedClock.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WarningRollbackTest {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 10, 45);

  /** The clock of the services, fixed at the scenario's issue time. */
  @TestConfiguration
  static class FixedClock {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO);
    }
  }

  @Autowired private WarningController controller;

  @Autowired private EscalationController escalationController;

  @Autowired private AuditRecordRepository auditRecords;

  @Autowired private HazardRepository hazards;

  @Autowired private WarningRepository warnings;

  @Autowired private DMCOfficerRepository officers;

  @Autowired private CitizenRepository citizens;

  @Autowired private PlatformTransactionManager transactionManager;

  @MockitoBean private SmsGateway smsGateway;

  @MockitoBean private IdGenerator idGenerator;

  private Hazard hazard;

  private DMCOfficer officer;

  @BeforeEach
  void arrange() {
    hazard = hazards.save(SampleEntities.hazard("HZ-2026-0142"));
    officer = officers.save(SampleEntities.officer("DMC-0001", Set.of()));
    citizens.save(citizen("CIT-KLT-0001"));
    citizens.save(citizen("CIT-KLT-0002"));
    when(smsGateway.supports(NotificationChannel.SMS)).thenReturn(true);
    when(idGenerator.next(IdKind.WARNING, null)).thenReturn("WRN-2026-0087");
  }

  private static Citizen citizen(String userId) {
    return new Citizen(
        userId,
        "Citizen " + userId,
        "0771234567",
        userId + "@example.lk",
        "199012345678",
        "Kalutara",
        District.KALUTARA,
        Language.ENGLISH,
        null);
  }

  /** The second send fails with a server error, after the first was recorded. */
  private void secondSendFails() {
    when(smsGateway.send(any()))
        .thenReturn(true)
        .thenThrow(new IllegalStateException("SMS service crashed"));
  }

  @Test
  @DisplayName("E2 13a — a failure during the broadcast records no warning and no notification")
  void failedIssueRecordsNothing() {
    secondSendFails();

    assertThatThrownBy(
            () ->
                controller.issueWarning(
                    "HZ-2026-0142",
                    SampleEntities.details(List.of(District.KALUTARA), List.of(), NOW.plusDays(1)),
                    "DMC-0001"))
        .isInstanceOf(IllegalStateException.class);

    assertThat(warnings.count()).isZero();
  }

  @Test
  @DisplayName("E1 16e — a resend that fails part-way keeps the stored notifications unchanged")
  void failedResendKeepsTheStoredNotifications() {
    warnings.save(
        new Warning(
            "WRN-2026-0087",
            hazard,
            SampleEntities.details(List.of(District.KALUTARA), List.of(), NOW.plusDays(1)),
            officer,
            NOW.minusHours(1),
            List.of(failedSms("CIT-KLT-0001"), failedSms("CIT-KLT-0002"))));
    secondSendFails();

    assertThatThrownBy(() -> controller.resendOnChannel("WRN-2026-0087", NotificationChannel.SMS))
        .isInstanceOf(IllegalStateException.class);

    List<DeliveryStatus> stored =
        new TransactionTemplate(transactionManager)
            .execute(
                status ->
                    warnings.findById("WRN-2026-0087").orElseThrow().getNotifications().stream()
                        .map(Notification::getDeliveryStatus)
                        .toList());
    assertThat(stored).containsExactly(DeliveryStatus.FAILED, DeliveryStatus.FAILED);
  }

  @Test
  @DisplayName("Escalate E4 10d — a failed re-broadcast keeps the level and records no audit")
  void failedEscalationChangesNothing() {
    warnings.save(
        new Warning(
            "WRN-2026-0087",
            hazard,
            SampleEntities.details(List.of(District.KALUTARA), List.of(), NOW.plusDays(1)),
            officer,
            NOW.minusHours(1)));
    secondSendFails();
    LocalisedText evacuate = new LocalisedText("ඉවත් වන්න", "வெளியேறவும்", "Evacuate now");
    EscalationDetails toRed =
        new EscalationDetails(WarningLevel.LEVEL_3_RED, evacuate, evacuate, NOW.plusDays(1));

    assertThatThrownBy(
            () -> escalationController.escalateWarning("WRN-2026-0087", toRed, "DMC-0001"))
        .isInstanceOf(IllegalStateException.class);

    WarningLevel stored =
        new TransactionTemplate(transactionManager)
            .execute(status -> warnings.findById("WRN-2026-0087").orElseThrow().getWarningLevel());
    assertThat(stored).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    assertThat(auditRecords.count()).isZero();
  }

  private static Notification failedSms(String citizenId) {
    return new Notification(
        RecipientKind.CITIZEN,
        citizenId,
        NotificationChannel.SMS,
        "text",
        1,
        DeliveryStatus.FAILED,
        NOW.minusHours(1));
  }
}
