package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.NearbyReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.VerificationOutcome;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoDistance;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSource;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NewHazardDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.NotificationGateway;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCDutyOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Tests of {@link ReportVerificationController} with mocked collaborators, a fixed clock and a
 * transaction template built on a stub transaction manager. Each display name names the SC2 flow it
 * covers.
 */
@ExtendWith(MockitoExtension.class)
class ReportVerificationControllerTest {

  private static final String REPORT_ID = "RPT-2026-KLT-0847";
  private static final String OFFICER_ID = "DMC-0002";
  private static final String HAZARD_ID = "HZ-2026-0142";
  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 7, 40);
  private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 5, 21, 7, 42);
  private static final GeoPoint ORIGIN = new GeoPoint(6.9271, 79.8612, null);
  private static final double DEGREES_PER_KM = 0.009;

  @Mock private HazardReportRepository reports;
  @Mock private HazardRepository hazards;
  @Mock private DMCDutyOfficerRepository officers;
  @Mock private WarningController warnings;
  @Mock private IdGenerator ids;
  @Mock private NotificationGateway push;
  @Mock private NotificationGateway sms;
  @Mock private PlatformTransactionManager transactionManager;

  private final DMCDutyOfficer officer = SampleEntities.dutyOfficer(OFFICER_ID);
  private final Citizen reporterWithToken = SampleEntities.citizen("CIT-KLT-0001", "token");
  private final Citizen reporterWithoutToken = SampleEntities.citizen("CIT-KLT-0002", null);
  private ReportVerificationController controller;
  private ListAppender<ILoggingEvent> logs;

  @BeforeEach
  void createController() {
    lenient().when(push.supports(NotificationChannel.PUSH)).thenReturn(true);
    lenient().when(sms.supports(NotificationChannel.SMS)).thenReturn(true);
    lenient()
        .when(transactionManager.getTransaction(any()))
        .thenReturn(new SimpleTransactionStatus());
    lenient().when(officers.findById(OFFICER_ID)).thenReturn(Optional.of(officer));
    lenient().when(hazards.save(any(Hazard.class))).thenAnswer(call -> call.getArgument(0));
    controller = newController(List.of(push, sms));
    logs = new ListAppender<>();
    logs.start();
    ((Logger) LoggerFactory.getLogger(ReportVerificationController.class)).addAppender(logs);
  }

  @AfterEach
  void detachLogAppender() {
    ((Logger) LoggerFactory.getLogger(ReportVerificationController.class)).detachAppender(logs);
  }

  private ReportVerificationController newController(List<NotificationGateway> gateways) {
    Clock clock = Clock.fixed(Instant.parse("2026-05-21T02:12:00Z"), ZoneId.of("Asia/Colombo"));
    return new ReportVerificationController(
        reports, hazards, officers, warnings, ids, gateways, transactionManager, clock);
  }

  private static HazardReport report(
      String reportId, Citizen reporter, HazardType type, ReportStatus status, GeoPoint location) {
    return new HazardReport(
        reportId,
        reporter,
        type,
        "Water is rising",
        reportId + ".jpg",
        location,
        true,
        District.COLOMBO,
        CAPTURED,
        RECEIVED,
        status,
        null,
        null,
        null,
        null);
  }

  private HazardReport pendingReport() {
    return report(
        REPORT_ID, reporterWithToken, HazardType.FLOOD, ReportStatus.PENDING_VERIFICATION, ORIGIN);
  }

  private static GeoPoint northOf(GeoPoint point, double km) {
    return new GeoPoint(point.latitude() + km * DEGREES_PER_KM, point.longitude(), null);
  }

  private static NewHazardDetails details(String description) {
    return new NewHazardDetails(
        HazardSeverity.HIGH, List.of(District.COLOMBO), "Kelani at Kaduwela", description);
  }

  private List<String> loggedMessages(Level level) {
    return logs.list.stream()
        .filter(event -> event.getLevel() == level)
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
  }

  /** No notice was sent on either gateway (the construction asked them {@code supports}). */
  private void assertNoNoticeSent() {
    verify(push, never()).sendNotice(any(), any());
    verify(sms, never()).sendNotice(any(), any());
  }

  private void arrangePending(HazardReport report) {
    when(reports.findById(report.getReportId())).thenReturn(Optional.of(report));
  }

  // ---- reads -----------------------------------------------------------------------------------

  @Test
  @DisplayName("SC2 step 22: the pending queue is the repository's list, oldest received first")
  void pendingReportsArePassedThrough() {
    HazardReport older = pendingReport();
    HazardReport newer =
        report(
            "RPT-2",
            reporterWithToken,
            HazardType.FLOOD,
            ReportStatus.PENDING_VERIFICATION,
            ORIGIN);
    when(reports.findPending()).thenReturn(List.of(older, newer));

    assertThat(controller.getPendingReports()).containsExactly(older, newer);
  }

  @Test
  @DisplayName("SC2 step 23: the details of a report are found by identifier")
  void reportDetailsAreFound() {
    HazardReport report = pendingReport();
    arrangePending(report);

    assertThat(controller.getReportDetails(REPORT_ID)).isSameAs(report);
  }

  @Test
  @DisplayName("SC2 step 23: the details of an unknown report are a NoSuchElementException")
  void unknownReportDetails() {
    when(reports.findById("RPT-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getReportDetails("RPT-0"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("RPT-0");
  }

  @Test
  @DisplayName(
      "SC2 step 24: nearby reports skip the selected, other types and rejected; nearest first")
  void nearbyReportsAreFilteredAndOrdered() {
    HazardReport selected = pendingReport();
    HazardReport twoKm =
        report(
            "RPT-2",
            reporterWithToken,
            HazardType.FLOOD,
            ReportStatus.PENDING_VERIFICATION,
            northOf(ORIGIN, 2));
    HazardReport oneKmVerified =
        report(
            "RPT-3",
            reporterWithToken,
            HazardType.FLOOD,
            ReportStatus.VERIFIED,
            northOf(ORIGIN, 1));
    HazardReport otherType =
        report(
            "RPT-4",
            reporterWithToken,
            HazardType.LANDSLIDE,
            ReportStatus.PENDING_VERIFICATION,
            northOf(ORIGIN, 0.5));
    HazardReport rejected =
        report(
            "RPT-5",
            reporterWithToken,
            HazardType.FLOOD,
            ReportStatus.REJECTED,
            northOf(ORIGIN, 0.8));
    arrangePending(selected);
    when(reports.findNearby(any(), eq(5.0), any()))
        .thenReturn(List.of(selected, twoKm, otherType, rejected, oneKmVerified));

    List<NearbyReport> nearby = controller.findNearbyReports(REPORT_ID);

    assertThat(nearby)
        .extracting(near -> near.report().getReportId())
        .containsExactly("RPT-3", "RPT-2");
    assertThat(nearby.get(0).distanceKm())
        .isEqualTo(GeoDistance.haversineKm(ORIGIN, oneKmVerified.getLocation()));
    assertThat(nearby.get(1).distanceKm())
        .isEqualTo(GeoDistance.haversineKm(ORIGIN, twoKm.getLocation()))
        .isBetween(1.9, 2.1);
  }

  @Test
  @DisplayName(
      "SC2 step 24: the finder is asked for 5 km around the report and 24 hours before now")
  void nearbyReportsAskTheFinderForFiveKmAndTwentyFourHours() {
    HazardReport selected = pendingReport();
    arrangePending(selected);
    when(reports.findNearby(any(), anyDouble(), any())).thenReturn(List.of(selected));

    assertThat(controller.findNearbyReports(REPORT_ID)).isEmpty();

    verify(reports).findNearby(ORIGIN, 5.0, LocalDateTime.of(2026, 5, 20, 7, 42));
  }

  @Test
  @DisplayName("SC2 step 24: nearby reports of an unknown report are a NoSuchElementException")
  void nearbyReportsOfAnUnknownReport() {
    when(reports.findById("RPT-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.findNearbyReports("RPT-0"))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName(
      "SC2 step 24: candidate hazards are asked for with the report's location, type and district")
  void candidateHazardsDelegateToTheFinder() {
    HazardReport report = pendingReport();
    Hazard hazard = SampleEntities.hazard(HAZARD_ID);
    arrangePending(report);
    when(hazards.findActiveNear(ORIGIN, HazardType.FLOOD, District.COLOMBO))
        .thenReturn(List.of(hazard));

    assertThat(controller.findCandidateHazards(REPORT_ID)).containsExactly(hazard);
  }

  @Test
  @DisplayName("SC2 step 24: candidate hazards of an unknown report are a NoSuchElementException")
  void candidateHazardsOfAnUnknownReport() {
    when(reports.findById("RPT-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.findCandidateHazards("RPT-0"))
        .isInstanceOf(NoSuchElementException.class);
  }

  // ---- verifyReport: SC2 steps 25 to 28 --------------------------------------------------------

  @Test
  @DisplayName(
      "SC2 steps 25-28: verify links the hazard, records officer and time, and pushes the notice")
  void verifyRecordsTheDecisionAndPushesTheNotice() {
    HazardReport report = pendingReport();
    Hazard hazard = SampleEntities.hazard(HAZARD_ID);
    Warning warning = mock(Warning.class);
    arrangePending(report);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(hazard));
    when(warnings.getActiveWarningForHazard(HAZARD_ID)).thenReturn(Optional.of(warning));
    when(push.sendNotice(any(), any())).thenReturn(true);

    VerificationOutcome outcome = controller.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID);

    assertThat(outcome.report()).isSameAs(report);
    assertThat(outcome.activeWarning()).isSameAs(warning);
    assertThat(report.getStatus()).isEqualTo(ReportStatus.VERIFIED);
    assertThat(report.getReviewer()).containsSame(officer);
    assertThat(report.getHazard()).containsSame(hazard);
    assertThat(report.getVerificationTime()).contains(RECEIVED);
    verify(reports).save(report);
    ArgumentCaptor<ReportDecisionNotice> notice =
        ArgumentCaptor.forClass(ReportDecisionNotice.class);
    verify(push).sendNotice(eq(reporterWithToken), notice.capture());
    assertThat(notice.getValue())
        .isEqualTo(new ReportDecisionNotice(REPORT_ID, ReportStatus.VERIFIED, null, RECEIVED));
    verify(sms, never()).sendNotice(any(), any());
    assertThat(loggedMessages(Level.INFO))
        .anyMatch(
            message ->
                message.contains(REPORT_ID)
                    && message.contains(OFFICER_ID)
                    && message.contains("PUSH"));
  }

  @Test
  @DisplayName("SC2 A6 25p: a verified hazard without an active warning gives an outcome with none")
  void verifyWithoutAnActiveWarning() {
    HazardReport report = pendingReport();
    arrangePending(report);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(SampleEntities.hazard(HAZARD_ID)));
    when(push.sendNotice(any(), any())).thenReturn(true);

    VerificationOutcome outcome = controller.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID);

    assertThat(outcome.activeWarning()).isNull();
    assertThat(outcome.report().getStatus()).isEqualTo(ReportStatus.VERIFIED);
  }

  @Test
  @DisplayName(
      "SC2 E5: a failure while asking for the warning rolls the decision back and sends nothing")
  void verifyFailureRollsBack() {
    HazardReport report = pendingReport();
    arrangePending(report);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(SampleEntities.hazard(HAZARD_ID)));
    when(warnings.getActiveWarningForHazard(HAZARD_ID))
        .thenThrow(new UnsupportedOperationException("not implemented yet"));

    assertThatThrownBy(() -> controller.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID))
        .isInstanceOf(UnsupportedOperationException.class);

    verify(transactionManager).rollback(any());
    verify(transactionManager, never()).commit(any());
    verify(reports, never()).save(any());
    assertNoNoticeSent();
  }

  // ---- A9: the notice channel -------------------------------------------------------------------

  @Test
  @DisplayName("SC2 A9: a citizen without a device token is told by SMS only")
  void noTokenMeansSmsOnly() {
    HazardReport report =
        report(
            REPORT_ID,
            reporterWithoutToken,
            HazardType.FLOOD,
            ReportStatus.PENDING_VERIFICATION,
            ORIGIN);
    arrangePending(report);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(SampleEntities.hazard(HAZARD_ID)));
    when(sms.sendNotice(any(), any())).thenReturn(true);

    controller.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID);

    verify(sms).sendNotice(eq(reporterWithoutToken), any(ReportDecisionNotice.class));
    verify(push, never()).sendNotice(any(), any());
    assertThat(loggedMessages(Level.INFO)).anyMatch(message -> message.contains("SMS"));
  }

  @Test
  @DisplayName("SC2 A9: when the push fails the notice goes by SMS")
  void failedPushFallsBackToSms() {
    HazardReport report = pendingReport();
    arrangePending(report);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(SampleEntities.hazard(HAZARD_ID)));
    when(push.sendNotice(any(), any())).thenReturn(false);
    when(sms.sendNotice(any(), any())).thenReturn(true);

    controller.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID);

    verify(push).sendNotice(eq(reporterWithToken), any(ReportDecisionNotice.class));
    verify(sms).sendNotice(eq(reporterWithToken), any(ReportDecisionNotice.class));
    assertThat(loggedMessages(Level.WARN)).isEmpty();
  }

  @Test
  @DisplayName(
      "SC2 A9: when neither push nor SMS can be sent the decision stands and a warning is logged")
  void bothChannelsFailingKeepsTheDecision() {
    HazardReport report = pendingReport();
    arrangePending(report);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(SampleEntities.hazard(HAZARD_ID)));
    when(push.sendNotice(any(), any())).thenReturn(false);
    when(sms.sendNotice(any(), any())).thenReturn(false);

    VerificationOutcome outcome = controller.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID);

    assertThat(outcome.report().getStatus()).isEqualTo(ReportStatus.VERIFIED);
    verify(reports).save(report);
    assertThat(loggedMessages(Level.WARN))
        .singleElement()
        .satisfies(message -> assertThat(message).contains(REPORT_ID));
  }

  // ---- A6: verify as a new hazard
  // -----------------------------------------------------------------

  @Test
  @DisplayName(
      "SC2 A6: a new hazard takes the report's type, place and capture time and the details")
  void newHazardIsCreatedFromTheReportAndTheDetails() {
    HazardReport report = pendingReport();
    arrangePending(report);
    when(ids.next(IdKind.HAZARD, null)).thenReturn("HZ-2026-0300");
    when(push.sendNotice(any(), any())).thenReturn(true);

    VerificationOutcome outcome =
        controller.verifyAsNewHazard(REPORT_ID, details("Kelani is over its banks"), OFFICER_ID);

    ArgumentCaptor<Hazard> saved = ArgumentCaptor.forClass(Hazard.class);
    verify(hazards).save(saved.capture());
    Hazard hazard = saved.getValue();
    assertThat(hazard.getHazardId()).isEqualTo("HZ-2026-0300");
    assertThat(hazard.getType()).isEqualTo(HazardType.FLOOD);
    assertThat(hazard.getLocation()).isEqualTo(ORIGIN);
    assertThat(hazard.getStartTime()).isEqualTo(CAPTURED);
    assertThat(hazard.getSeverity()).isEqualTo(HazardSeverity.HIGH);
    assertThat(hazard.getDistricts()).containsExactly(District.COLOMBO);
    assertThat(hazard.getLocationName()).isEqualTo("Kelani at Kaduwela");
    assertThat(hazard.getDescription()).isEqualTo("Kelani is over its banks");
    assertThat(hazard.getSource()).isEqualTo(HazardSource.VERIFIED_REPORT);
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.ACTIVE);
    assertThat(report.getStatus()).isEqualTo(ReportStatus.VERIFIED);
    assertThat(report.getHazard()).containsSame(hazard);
    assertThat(report.getReviewer()).containsSame(officer);
    assertThat(outcome.activeWarning()).isNull();
    verify(warnings).getActiveWarningForHazard("HZ-2026-0300");
    verify(push).sendNotice(eq(reporterWithToken), any(ReportDecisionNotice.class));
  }

  @ParameterizedTest(name = "[{index}] description \"{0}\"")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  @DisplayName("SC2 A6 (MD v0.13): without a description the new hazard gets the report's")
  void newHazardFallsBackToTheReportsDescription(String description) {
    HazardReport report = pendingReport();
    arrangePending(report);
    when(ids.next(IdKind.HAZARD, null)).thenReturn("HZ-2026-0300");
    when(push.sendNotice(any(), any())).thenReturn(true);

    controller.verifyAsNewHazard(REPORT_ID, details(description), OFFICER_ID);

    ArgumentCaptor<Hazard> saved = ArgumentCaptor.forClass(Hazard.class);
    verify(hazards).save(saved.capture());
    assertThat(saved.getValue().getDescription()).isEqualTo("Water is rising");
  }

  static Stream<Arguments> incompleteDetails() {
    List<District> districts = List.of(District.COLOMBO);
    return Stream.of(
        Arguments.of(
            "no severity",
            new NewHazardDetails(null, districts, "Kelani", "d"),
            List.of("Severity is required")),
        Arguments.of(
            "no districts",
            new NewHazardDetails(HazardSeverity.HIGH, List.of(), "Kelani", "d"),
            List.of("Districts are required")),
        Arguments.of(
            "a null place name",
            new NewHazardDetails(HazardSeverity.HIGH, districts, null, "d"),
            List.of("Place name is required")),
        Arguments.of(
            "a blank place name",
            new NewHazardDetails(HazardSeverity.HIGH, districts, "  ", "d"),
            List.of("Place name is required")),
        Arguments.of(
            "everything missing",
            new NewHazardDetails(null, null, null, null),
            List.of("Severity is required", "Districts are required", "Place name is required")));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("incompleteDetails")
  @DisplayName(
      "SC2 E9: missing details are refused with every error listed, nothing is created or sent")
  void incompleteDetailsAreRefused(
      String ignoredCase, NewHazardDetails details, List<String> expectedErrors) {
    HazardReport report = pendingReport();
    arrangePending(report);

    assertThatThrownBy(() -> controller.verifyAsNewHazard(REPORT_ID, details, OFFICER_ID))
        .isInstanceOfSatisfying(
            InvalidHazardDetailsException.class,
            exception -> {
              assertThat(exception.getErrors()).isEqualTo(expectedErrors);
              assertThat(exception).hasMessage(String.join("; ", expectedErrors));
              assertThat(exception.getErrors()).isUnmodifiable();
            });

    assertThat(report.isPending()).isTrue();
    verify(hazards, never()).save(any());
    verify(reports, never()).save(any());
    verify(ids, never()).next(any(), any());
    assertNoNoticeSent();
    verifyNoInteractions(warnings);
  }

  @Test
  @DisplayName("SC2 A6: verifyAsNewHazard needs details")
  void newHazardNeedsDetails() {
    assertThatThrownBy(() -> controller.verifyAsNewHazard(REPORT_ID, null, OFFICER_ID))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("details");
  }

  // ---- A4 and E7: reject
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "SC2 A4: reject records the trimmed reason and time and tells the citizen the reason")
  void rejectRecordsTheReasonAndSendsIt() {
    HazardReport report = pendingReport();
    arrangePending(report);
    when(push.sendNotice(any(), any())).thenReturn(true);

    HazardReport result = controller.rejectReport(REPORT_ID, "  Not a flood  ", OFFICER_ID);

    assertThat(result).isSameAs(report);
    assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(report.getRejectionReason()).contains("Not a flood");
    assertThat(report.getVerificationTime()).contains(RECEIVED);
    assertThat(report.getReviewer()).containsSame(officer);
    verify(reports).save(report);
    ArgumentCaptor<ReportDecisionNotice> notice =
        ArgumentCaptor.forClass(ReportDecisionNotice.class);
    verify(push).sendNotice(eq(reporterWithToken), notice.capture());
    assertThat(notice.getValue())
        .isEqualTo(
            new ReportDecisionNotice(REPORT_ID, ReportStatus.REJECTED, "Not a flood", RECEIVED));
    verifyNoInteractions(warnings);
    assertThat(loggedMessages(Level.INFO)).noneMatch(message -> message.contains("Not a flood"));
  }

  @ParameterizedTest(name = "[{index}] reason \"{0}\"")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t\n"})
  @DisplayName("SC2 E7: an empty reason is refused, the report stays pending and nothing is sent")
  void emptyReasonIsRefused(String reason) {
    HazardReport report = pendingReport();
    arrangePending(report);

    assertThatThrownBy(() -> controller.rejectReport(REPORT_ID, reason, OFFICER_ID))
        .isInstanceOf(ReasonRequiredException.class)
        .hasMessage("A rejection reason is required");

    assertThat(report.isPending()).isTrue();
    verify(reports, never()).save(any());
    assertNoNoticeSent();
  }

  // ---- E10: already decided
  // -----------------------------------------------------------------------

  private HazardReport decidedReport(ReportStatus status) {
    return new HazardReport(
        REPORT_ID,
        reporterWithToken,
        HazardType.FLOOD,
        "Water is rising",
        "p.jpg",
        ORIGIN,
        true,
        District.COLOMBO,
        CAPTURED,
        RECEIVED,
        status,
        RECEIVED.plusMinutes(5),
        status == ReportStatus.REJECTED ? "Not a flood" : null,
        officer,
        null);
  }

  @ParameterizedTest(name = "[{index}] already {0}")
  @ValueSource(strings = {"VERIFIED", "REJECTED"})
  @DisplayName(
      "SC2 E10: a second decision is refused with the report it found, for all three decisions")
  void secondDecisionIsRefused(ReportStatus status) {
    HazardReport decided = decidedReport(status);
    arrangePending(decided);

    assertAlreadyDecided(() -> controller.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID), decided);
    assertAlreadyDecided(
        () -> controller.verifyAsNewHazard(REPORT_ID, details("d"), OFFICER_ID), decided);
    assertAlreadyDecided(() -> controller.rejectReport(REPORT_ID, "late", OFFICER_ID), decided);

    verify(reports, never()).save(any());
    verify(hazards, never()).save(any());
    assertNoNoticeSent();
    verifyNoInteractions(warnings, ids);
    assertThat(decided.getStatus()).isEqualTo(status);
  }

  @Test
  @DisplayName(
      "SC2 E10: the decided check comes before the reason (E7) and before the details (E9)")
  void theDecidedCheckComesFirst() {
    HazardReport decided = decidedReport(ReportStatus.VERIFIED);
    arrangePending(decided);

    assertAlreadyDecided(() -> controller.rejectReport(REPORT_ID, "", OFFICER_ID), decided);
    assertAlreadyDecided(() -> controller.rejectReport(REPORT_ID, null, OFFICER_ID), decided);
    assertAlreadyDecided(
        () ->
            controller.verifyAsNewHazard(
                REPORT_ID, new NewHazardDetails(null, null, null, null), OFFICER_ID),
        decided);
  }

  private static void assertAlreadyDecided(Runnable decision, HazardReport expected) {
    assertThatThrownBy(decision::run)
        .isInstanceOfSatisfying(
            ReportAlreadyDecidedException.class,
            exception -> {
              assertThat(exception.getReport()).isSameAs(expected);
              assertThat(exception).hasMessage("This report has already been decided");
            });
  }

  @Test
  @DisplayName(
      "SC2 E10 (FX-07): a lost optimistic lock is refused as already decided, re-read report")
  void lostOptimisticLockIsMappedToAlreadyDecided() {
    HazardReport pending = pendingReport();
    HazardReport reread = decidedReport(ReportStatus.REJECTED);
    when(reports.findById(REPORT_ID))
        .thenReturn(Optional.of(pending))
        .thenReturn(Optional.of(reread));
    doThrow(new ObjectOptimisticLockingFailureException(HazardReport.class, REPORT_ID))
        .doNothing()
        .when(transactionManager)
        .commit(any());

    assertAlreadyDecided(() -> controller.rejectReport(REPORT_ID, "late", OFFICER_ID), reread);

    verify(reports, times(2)).findById(REPORT_ID);
    assertNoNoticeSent();
    assertThat(loggedMessages(Level.INFO))
        .anyMatch(
            message ->
                message.contains("lost to a concurrent decision") && message.contains(REPORT_ID));
  }

  // ---- unknown identifiers and construction
  // -----------------------------------------------------------

  @Test
  @DisplayName("SC2 steps 25-28: deciding an unknown report is a NoSuchElementException")
  void decidingAnUnknownReport() {
    when(reports.findById("RPT-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.rejectReport("RPT-0", "x", OFFICER_ID))
        .isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(() -> controller.verifyReport("RPT-0", HAZARD_ID, OFFICER_ID))
        .isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(() -> controller.verifyAsNewHazard("RPT-0", details("d"), OFFICER_ID))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("SC2 step 25: verifying against an unknown hazard is a NoSuchElementException")
  void verifyingAgainstAnUnknownHazard() {
    arrangePending(pendingReport());
    when(hazards.findById("HZ-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.verifyReport(REPORT_ID, "HZ-0", OFFICER_ID))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("HZ-0");
    assertNoNoticeSent();
  }

  @Test
  @DisplayName("SC2 steps 25-28: an unknown officer is a NoSuchElementException")
  void decidingWithAnUnknownOfficer() {
    arrangePending(pendingReport());
    when(officers.findById("DMC-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.rejectReport(REPORT_ID, "x", "DMC-0"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("DMC-0");
    assertNoNoticeSent();
  }

  @Test
  @DisplayName("FD-03: a missing push or SMS gateway fails the construction")
  void missingGatewayFailsFast() {
    assertThatIllegalStateException()
        .isThrownBy(() -> newController(List.of(sms)))
        .withMessageContaining("PUSH");
    assertThatIllegalStateException()
        .isThrownBy(() -> newController(List.of(push)))
        .withMessageContaining("SMS");
    assertThatIllegalStateException().isThrownBy(() -> newController(List.of()));
  }

  @Test
  @DisplayName("The exceptions carry their messages and statuses")
  void exceptionsCarryTheirMessages() {
    assertThat(new ReasonRequiredException()).hasMessage("A rejection reason is required");
    assertThat(new InvalidHazardDetailsException(List.of("a", "b"))).hasMessage("a; b");
    assertThat(statusOf(ReportAlreadyDecidedException.class)).isEqualTo(HttpStatus.CONFLICT);
    assertThat(statusOf(ReasonRequiredException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(statusOf(InvalidHazardDetailsException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThatThrownBy(() -> new ReportAlreadyDecidedException(null))
        .isInstanceOf(NullPointerException.class);
  }

  private static HttpStatus statusOf(Class<? extends RuntimeException> exception) {
    return AnnotatedElementUtils.findMergedAnnotation(exception, ResponseStatus.class).code();
  }
}
