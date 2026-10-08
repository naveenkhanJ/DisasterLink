package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.VerificationOutcome;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NewHazardDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.NotificationGateway;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCDutyOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests the decisions of {@link ReportVerificationController} against H2 with the real
 * repositories, the real {@code IdGenerator} and the real transaction manager. The test class has
 * no transaction of its own (standards section 4, "Exceptions"), so the controller's transaction
 * boundary is the one exercised and whatever is arranged or decided really commits; the cleanup
 * script empties the tables after each method.
 *
 * <p>UC1's {@code WarningController} and the two gateways are mocks.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ReportVerificationController.class, IdGenerator.class})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ReportVerificationControllerPersistenceTest {

  private static final String CITIZEN_ID = "CIT-KLT-0001";
  private static final String FIRST_OFFICER = "DMC-0001";
  private static final String SECOND_OFFICER = "DMC-0002";
  private static final String REPORT_ID = "RPT-2026-COL-0001";
  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 7, 40);
  private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 5, 21, 7, 42);

  /** A clock fixed at 07:42 in Colombo, and two gateway mocks that say which channel they send. */
  @TestConfiguration
  static class Config {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-05-21T02:12:00Z"), ZoneId.of("Asia/Colombo"));
    }

    @Bean
    NotificationGateway pushGateway() {
      return gatewayFor(NotificationChannel.PUSH);
    }

    @Bean
    NotificationGateway smsGateway() {
      return gatewayFor(NotificationChannel.SMS);
    }

    private static NotificationGateway gatewayFor(NotificationChannel channel) {
      NotificationGateway gateway = mock(NotificationGateway.class);
      when(gateway.supports(channel)).thenReturn(true);
      return gateway;
    }
  }

  @MockitoBean private WarningController warningController;

  @Autowired private ReportVerificationController controller;
  @Autowired private HazardReportRepository reports;
  @Autowired private HazardRepository hazards;
  @Autowired private CitizenRepository citizens;
  @Autowired private DMCDutyOfficerRepository officers;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired
  @Qualifier("pushGateway")
  private NotificationGateway push;

  @Autowired
  @Qualifier("smsGateway")
  private NotificationGateway sms;

  private Citizen citizen;

  @BeforeEach
  void arrange() {
    jdbcTemplate.execute("ALTER SEQUENCE hazard_id_seq RESTART WITH 300");
    clearInvocations(push, sms);
    when(push.sendNotice(any(), any())).thenReturn(true);
    when(sms.sendNotice(any(), any())).thenReturn(true);
    citizen = citizens.save(SampleEntities.citizen(CITIZEN_ID, "token"));
    officers.save(SampleEntities.dutyOfficer(FIRST_OFFICER));
    officers.save(SampleEntities.dutyOfficer(SECOND_OFFICER));
    reports.save(
        FeatureFixtures.report(
            REPORT_ID,
            citizen,
            FeatureFixtures.ORIGIN,
            CAPTURED,
            RECEIVED,
            ReportStatus.PENDING_VERIFICATION));
  }

  private static NewHazardDetails details() {
    return new NewHazardDetails(
        HazardSeverity.HIGH, List.of(District.COLOMBO), "Kelani at Kaduwela", null);
  }

  private <T> T inTransaction(java.util.function.Supplier<T> work) {
    return new TransactionTemplate(transactionManager).execute(status -> work.get());
  }

  @Test
  @DisplayName(
      "SC2 E5: a failure after the hazard was created rolls everything back and sends nothing")
  void failedVerificationLeavesTheRepositoryUnchanged() {
    when(warningController.getActiveWarningForHazard(anyString()))
        .thenThrow(new IllegalStateException("UC1 is unavailable"));

    assertThatThrownBy(() -> controller.verifyAsNewHazard(REPORT_ID, details(), FIRST_OFFICER))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("UC1 is unavailable");

    HazardReport stored = reports.findById(REPORT_ID).orElseThrow();
    assertThat(stored.getStatus()).isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(stored.getVerificationTime()).isEmpty();
    assertThat(hazards.count()).isZero();
    verify(push, never()).sendNotice(any(), any());
    verify(sms, never()).sendNotice(any(), any());
  }

  @Test
  @DisplayName("SC2 A6: a new hazard is stored as HZ-<year>-nnnn, linked, and the citizen told")
  void newHazardIsStoredAndLinked() {
    when(warningController.getActiveWarningForHazard(anyString())).thenReturn(Optional.empty());

    VerificationOutcome outcome = controller.verifyAsNewHazard(REPORT_ID, details(), FIRST_OFFICER);

    assertThat(outcome.activeWarning()).isNull();
    HazardReport stored = reports.findById(REPORT_ID).orElseThrow();
    assertThat(stored.getStatus()).isEqualTo(ReportStatus.VERIFIED);
    assertThat(stored.getVerificationTime()).contains(RECEIVED);
    assertThat(hazards.count()).isEqualTo(1);
    String linked =
        inTransaction(
            () ->
                reports.findById(REPORT_ID).orElseThrow().getHazard().orElseThrow().getHazardId());
    assertThat(linked).isEqualTo("HZ-2026-0300");
    Hazard hazard = hazards.findById(linked).orElseThrow();
    assertThat(hazard.getDescription()).isEqualTo("Water is rising");
    assertThat(hazard.getLocationName()).isEqualTo("Kelani at Kaduwela");
    assertThat(hazard.getStartTime()).isEqualTo(CAPTURED);
    String reviewer =
        inTransaction(
            () ->
                reports.findById(REPORT_ID).orElseThrow().getReviewer().orElseThrow().getUserId());
    assertThat(reviewer).isEqualTo(FIRST_OFFICER);
    ArgumentCaptor<ReportDecisionNotice> notice =
        ArgumentCaptor.forClass(ReportDecisionNotice.class);
    verify(push).sendNotice(any(Citizen.class), notice.capture());
    assertThat(notice.getValue())
        .isEqualTo(new ReportDecisionNotice(REPORT_ID, ReportStatus.VERIFIED, null, RECEIVED));
    verify(sms, never()).sendNotice(any(), any());
  }

  @Test
  @DisplayName("SC2 E10 (FX-07): the decision that commits first wins; the slower one is refused")
  void concurrentDecisionLosesOnTheVersion() {
    TransactionTemplate separateTransaction = new TransactionTemplate(transactionManager);
    separateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    // The first officer's decision is inside its transaction when it asks UC1 for the warning;
    // that answer, in a transaction of its own, lets the second officer reject the same report
    // and commit before the first commit, so the first fails on the report's @Version.
    when(warningController.getActiveWarningForHazard(anyString()))
        .thenAnswer(
            call -> {
              separateTransaction.execute(
                  status ->
                      controller.rejectReport(
                          REPORT_ID, "Second officer's reason", SECOND_OFFICER));
              return Optional.empty();
            });
    hazards.save(SampleEntities.hazard("HZ-2026-0142"));

    assertThatThrownBy(() -> controller.verifyReport(REPORT_ID, "HZ-2026-0142", FIRST_OFFICER))
        .isInstanceOfSatisfying(
            ReportAlreadyDecidedException.class,
            exception -> {
              assertThat(exception.getReport().getStatus()).isEqualTo(ReportStatus.REJECTED);
              assertThat(exception.getReport().getRejectionReason())
                  .contains("Second officer's reason");
            });

    HazardReport stored = reports.findById(REPORT_ID).orElseThrow();
    assertThat(stored.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(stored.getRejectionReason()).contains("Second officer's reason");
    String reviewer =
        inTransaction(
            () ->
                reports.findById(REPORT_ID).orElseThrow().getReviewer().orElseThrow().getUserId());
    assertThat(reviewer).isEqualTo(SECOND_OFFICER);
    assertThat(inTransaction(() -> reports.findById(REPORT_ID).orElseThrow().getHazard()))
        .isEmpty();
    ArgumentCaptor<ReportDecisionNotice> notice =
        ArgumentCaptor.forClass(ReportDecisionNotice.class);
    verify(push, times(1)).sendNotice(any(), notice.capture());
    assertThat(notice.getValue().status()).isEqualTo(ReportStatus.REJECTED);
    assertThat(notice.getValue().reason()).isEqualTo("Second officer's reason");
    verify(sms, never()).sendNotice(any(), any());
  }

  @Test
  @DisplayName("SC2 E10: after a committed decision the same report cannot be decided again")
  void committedDecisionIsFinal() {
    controller.rejectReport(REPORT_ID, "Not a flood", FIRST_OFFICER);

    assertThatThrownBy(() -> controller.rejectReport(REPORT_ID, "Again", SECOND_OFFICER))
        .isInstanceOf(ReportAlreadyDecidedException.class);

    assertThat(reports.findById(REPORT_ID).orElseThrow().getRejectionReason())
        .contains("Not a flood");
    verify(push, times(1)).sendNotice(any(), any());
  }
}
