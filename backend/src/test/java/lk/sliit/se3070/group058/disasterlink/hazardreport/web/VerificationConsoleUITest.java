package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.NearbyReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.VerificationOutcome;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.InvalidHazardDetailsException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReasonRequiredException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportAlreadyDecidedException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportVerificationController;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper.VerificationConsoleMapper;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NewHazardDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tests of the verification console's pages with the real mapper and the real templates: the
 * rendered HTML of every state of WF2C-S01 to S02h, that the decisions call the control with the
 * demo officer and outside any transaction, and the refusals and the banner.
 */
@WebMvcTest(VerificationConsoleUI.class)
@Import(VerificationConsoleMapper.class)
class VerificationConsoleUITest {

  private static final String REPORT_ID = "RPT-2026-KLT-0847";
  private static final String HAZARD_ID = "HZ-2026-0142";
  private static final String OFFICER_ID = "DMC-0002";
  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 7, 40);
  private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 5, 21, 7, 42);
  private static final LocalDateTime DECIDED = LocalDateTime.of(2026, 5, 21, 8, 15);

  /** A transaction manager that really opens (empty) transactions, so a test can see them. */
  @TestConfiguration
  static class TransactionConfig {
    @Bean
    PlatformTransactionManager transactionManager() {
      return new AbstractPlatformTransactionManager() {
        private static final long serialVersionUID = 1L;

        @Override
        protected Object doGetTransaction() {
          return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
          // Nothing to open: the synchronisation of the base class is what the tests look at.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
          // Nothing to commit.
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
          // Nothing to roll back.
        }
      };
    }
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReportVerificationController control;

  @MockitoBean private CurrentUserService currentUserService;

  private final Citizen reporter = SampleEntities.citizen("CIT-KLT-0001", "token");
  private final DMCDutyOfficer officer = SampleEntities.dutyOfficer(OFFICER_ID);
  private final Hazard hazard = SampleEntities.hazard(HAZARD_ID);

  @BeforeEach
  void arrangeTheDemoOfficer() {
    when(currentUserService.userId(DemoRole.DMC_DUTY_OFFICER)).thenReturn(OFFICER_ID);
    when(currentUserService.currentUser(DemoRole.DMC_DUTY_OFFICER))
        .thenReturn(new CurrentUserView("KP", "K. Perera", "DMC Duty Officer"));
  }

  private HazardReport report(
      String id, Citizen by, HazardType type, ReportStatus status, GeoPoint location, boolean gps) {
    boolean decided = status != ReportStatus.PENDING_VERIFICATION;
    return new HazardReport(
        id,
        by,
        type,
        "Kalu Ganga water has breached the sandbag barriers",
        id + ".jpg",
        location,
        gps,
        District.KALUTARA,
        CAPTURED,
        RECEIVED,
        status,
        decided ? DECIDED : null,
        status == ReportStatus.REJECTED ? "Duplicate of RPT-2026-KLT-0843" : null,
        decided ? officer : null,
        status == ReportStatus.VERIFIED ? hazard : null);
  }

  private HazardReport report(ReportStatus status) {
    return report(
        REPORT_ID, reporter, HazardType.FLOOD, status, new GeoPoint(6.5888, 79.9640, 8.0), true);
  }

  private void arrangeReads(
      HazardReport report, List<NearbyReport> nearby, List<Hazard> candidates) {
    when(control.getReportDetails(report.getReportId())).thenReturn(report);
    when(control.findNearbyReports(report.getReportId())).thenReturn(nearby);
    when(control.findCandidateHazards(report.getReportId())).thenReturn(candidates);
  }

  private void arrangePending() {
    arrangeReads(report(ReportStatus.PENDING_VERIFICATION), List.of(), List.of(hazard));
  }

  private static String html(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  private static void assertContains(String html, String... texts) {
    for (String text : texts) {
      assertThat(html).as("the page shows \"%s\"", text).contains(text);
    }
  }

  private static void assertLacks(String html, String... texts) {
    for (String text : texts) {
      assertThat(html).as("the page does not show \"%s\"", text).doesNotContain(text);
    }
  }

  // ---- S01: the queue -----------------------------------------------------------------------

  @Test
  @DisplayName("WF2C-S01: the queue shows the rows oldest first, the count and a View link per row")
  void queueShowsTheRows() throws Exception {
    HazardReport older =
        report(
            "RPT-2026-KLT-0843",
            reporter,
            HazardType.FLOOD,
            ReportStatus.PENDING_VERIFICATION,
            new GeoPoint(6.58, 79.96, null),
            true);
    HazardReport newer =
        report(
            "RPT-2026-KLT-0845",
            SampleEntities.volunteer("CIT-RAT-0002", null),
            HazardType.BLOCKED_ROAD,
            ReportStatus.PENDING_VERIFICATION,
            new GeoPoint(6.58, 79.96, null),
            true);
    when(control.getPendingReports()).thenReturn(List.of(older, newer));

    ResultActions result =
        mockMvc
            .perform(get("/hazardreport"))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showPendingReports"));

    String html = html(result);
    assertContains(
        html,
        "Verification queue",
        "Reports with the status Pending verification, oldest first",
        "2 reports",
        "RPT-2026-KLT-0843",
        "RPT-2026-KLT-0845",
        "21 May 2026, 07:40",
        "Blocked road",
        "Kalutara",
        "Citizen",
        "Community Volunteer",
        "Pending verification",
        "href=\"/hazardreport/RPT-2026-KLT-0843\"",
        "href=\"/hazardreport/RPT-2026-KLT-0845\"",
        "K. Perera",
        "DMC Duty Officer");
    assertThat(html.indexOf("RPT-2026-KLT-0843")).isLessThan(html.indexOf("RPT-2026-KLT-0845"));
  }

  @Test
  @DisplayName("WF2C-S01: an empty queue says that no reports are waiting")
  void emptyQueue() throws Exception {
    when(control.getPendingReports()).thenReturn(List.of());

    String html = html(mockMvc.perform(get("/hazardreport")).andExpect(status().isOk()));

    assertContains(html, "0 reports", "No reports are waiting for verification");
    assertLacks(html, "<table");
  }

  @Test
  @DisplayName("WF2C-S01: one pending report is worded as 1 report")
  void oneReportIsSingular() throws Exception {
    when(control.getPendingReports())
        .thenReturn(List.of(report(ReportStatus.PENDING_VERIFICATION)));

    assertContains(html(mockMvc.perform(get("/hazardreport"))), "1 report<");
  }

  @Test
  @DisplayName(
      "WF2C-S01 (P213, P217): the queue reloads itself every 15 seconds (header Refresh: 15)")
  void queueReloadsItself() throws Exception {
    when(control.getPendingReports()).thenReturn(List.of());

    mockMvc.perform(get("/hazardreport")).andExpect(header().string("Refresh", "15"));
  }

  @Test
  @DisplayName("WF2C-S02 (P213): the report page and the rejection form never reload by themselves")
  void reportPageAndRejectionFormDoNotReload() throws Exception {
    arrangePending();

    mockMvc
        .perform(get("/hazardreport/{id}", REPORT_ID))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Refresh"));
    mockMvc
        .perform(get("/hazardreport/{id}/reject", REPORT_ID))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Refresh"));
  }

  // ---- S02: the details -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "WF2C-S02: the details show the report, photo, map, nearby and a preselected candidate")
  void detailsShowTheEvidence() throws Exception {
    HazardReport nearby =
        report(
            "RPT-2026-KLT-0843",
            reporter,
            HazardType.FLOOD,
            ReportStatus.PENDING_VERIFICATION,
            new GeoPoint(6.58, 79.96, null),
            true);
    Hazard second = SampleEntities.hazard("HZ-2026-0150");
    arrangeReads(
        report(ReportStatus.PENDING_VERIFICATION),
        List.of(new NearbyReport(nearby, 1.2)),
        List.of(hazard, second));

    ResultActions result =
        mockMvc
            .perform(get("/hazardreport/{id}", REPORT_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showReportDetails"));

    String html = html(result);
    assertContains(
        html,
        "Report RPT-2026-KLT-0847",
        "← Back to queue",
        "Pending verification — review the evidence and decide",
        "Nimal Perera",
        "Citizen",
        "Flood",
        "Kalu Ganga water has breached the sandbag barriers",
        "6.5888 N, 79.9640 E · GPS · accuracy ±8 m",
        "Kalutara",
        "21 May 2026, 07:40",
        "21 May 2026, 07:42",
        "src=\"/reports/RPT-2026-KLT-0847/photo\"",
        "data-selected=\"Kalutara\"",
        "data-dot=\"6.5888,79.964\"",
        "Kalutara District — report location 6.5888 N, 79.9640 E",
        "RPT-2026-KLT-0843",
        "1.2 km",
        "07:40",
        "Flood — Kalu Ganga at Ratnapura",
        "HZ-2026-0142",
        "Severity Medium",
        "Ratnapura, Kalutara",
        "River basin Kalu",
        "Source Agency bulletin",
        "Verify and Link",
        "Verify as New Hazard",
        "Reject",
        "Verification never issues or changes a warning.",
        "action=\"/hazardreport/RPT-2026-KLT-0847/verify\"");
    assertThat(html).containsPattern("value=\"HZ-2026-0142\"\\s+checked=\"checked\"");
    assertThat(html).doesNotContainPattern("value=\"HZ-2026-0150\"\\s+checked");
  }

  @Test
  @DisplayName("WF2C-S02: a manual location is marked Manual and an unknown accuracy is left out")
  void manualLocationWithoutAccuracy() throws Exception {
    arrangeReads(
        report(
            REPORT_ID,
            reporter,
            HazardType.FLOOD,
            ReportStatus.PENDING_VERIFICATION,
            new GeoPoint(6.5888, 79.9640, null),
            false),
        List.of(),
        List.of());

    String html = html(mockMvc.perform(get("/hazardreport/{id}", REPORT_ID)));

    assertContains(html, "6.5888 N, 79.9640 E · Manual", "No nearby reports");
    assertLacks(html, "accuracy");
  }

  @Test
  @DisplayName(
      "WF2C-S02c: without a candidate there is no Verify and Link, and the page says none fits")
  void detailsWithoutCandidates() throws Exception {
    arrangeReads(
        report(
            REPORT_ID,
            reporter,
            HazardType.BLOCKED_ROAD,
            ReportStatus.PENDING_VERIFICATION,
            new GeoPoint(6.5888, 79.9640, 8.0),
            true),
        List.of(),
        List.of());

    String html =
        html(mockMvc.perform(get("/hazardreport/{id}", REPORT_ID)).andExpect(status().isOk()));

    assertContains(
        html, "No active blocked road hazard fits this report", "Verify as New Hazard", "Reject");
    assertLacks(html, "Verify and Link");
  }

  @Test
  @DisplayName("WF2C-S02: an unknown report is a 404")
  void unknownReportIsNotFound() throws Exception {
    when(control.getReportDetails("RPT-0"))
        .thenThrow(new NoSuchElementException("report RPT-0 not found"));

    mockMvc.perform(get("/hazardreport/{id}", "RPT-0")).andExpect(status().isNotFound());
  }

  // ---- S02a and S02b: reject
  // ------------------------------------------------------------------------

  @Test
  @DisplayName("WF2C-S02a: the rejection form has the field, the helper text and the buttons")
  void rejectionFormIsShown() throws Exception {
    arrangePending();

    ResultActions result =
        mockMvc
            .perform(get("/hazardreport/{id}/reject", REPORT_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/rejectionReason"));

    String html = html(result);
    assertContains(
        html,
        "Reject report",
        "Rejection reason *",
        "it is sent to the reporter as typed",
        "action=\"/hazardreport/RPT-2026-KLT-0847/reject\"",
        ">Reject<",
        "Cancel",
        "Pending verification");
    assertLacks(html, "Confirm rejection", "A rejection reason is required");
  }

  @Test
  @DisplayName("WF2C-S02a: Cancel of the confirmation returns to the form with the reason kept")
  void rejectionFormKeepsTheReason() throws Exception {
    arrangePending();

    String html =
        html(
            mockMvc.perform(
                get("/hazardreport/{id}/reject", REPORT_ID).param("reason", "Not a flood")));

    assertContains(html, ">Not a flood</textarea>");
  }

  @Test
  @DisplayName(
      "WF2C-S02a: Reject asks for confirmation with the reason repeated and decides nothing")
  void rejectionAsksForConfirmation() throws Exception {
    arrangePending();

    String html =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/{id}/reject", REPORT_ID)
                        .param("reason", "Duplicate of RPT-2026-KLT-0843"))
                .andExpect(status().isOk())
                .andExpect(view().name("hazardreport/rejectionReason")));

    assertContains(
        html,
        "Reject RPT-2026-KLT-0847 with this reason?",
        "Duplicate of RPT-2026-KLT-0843",
        "Confirm rejection",
        "action=\"/hazardreport/RPT-2026-KLT-0847/reject/confirm\"",
        "name=\"reason\"",
        "href=\"/hazardreport/RPT-2026-KLT-0847/reject?reason=Duplicate%20of%20"
            + "RPT-2026-KLT-0843\"");
    verify(control, never()).rejectReport(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("WF2C-S02b (SC2 E7): an empty reason shows the error under the field, still pending")
  void emptyReasonShowsTheError() throws Exception {
    arrangePending();
    when(control.rejectReport(REPORT_ID, "", OFFICER_ID)).thenThrow(new ReasonRequiredException());

    ResultActions result =
        mockMvc
            .perform(post("/hazardreport/{id}/reject", REPORT_ID).param("reason", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/rejectionReason"));

    String html = html(result);
    assertContains(html, "A rejection reason is required", "field--error", "Pending verification");
    assertLacks(html, "Confirm rejection");
  }

  @Test
  @DisplayName("WF2C-S02b (SC2 E7): confirming an empty reason shows the same error")
  void confirmingAnEmptyReasonShowsTheError() throws Exception {
    arrangePending();
    when(control.rejectReport(REPORT_ID, "  ", OFFICER_ID))
        .thenThrow(new ReasonRequiredException());

    String html =
        html(
            mockMvc.perform(
                post("/hazardreport/{id}/reject/confirm", REPORT_ID).param("reason", "  ")));

    assertContains(html, "A rejection reason is required", "Reject report");
  }

  // ---- S02f: the rejection is recorded
  // --------------------------------------------------------------

  @Test
  @DisplayName("WF2C-S02f: a confirmed rejection shows Report rejected with the reason as typed")
  void rejectionIsRecorded() throws Exception {
    HazardReport rejected = report(ReportStatus.REJECTED);
    when(control.rejectReport(REPORT_ID, "Duplicate of RPT-2026-KLT-0843", OFFICER_ID))
        .thenReturn(rejected);
    arrangeReads(rejected, List.of(), List.of());

    ResultActions result =
        mockMvc
            .perform(
                post("/hazardreport/{id}/reject/confirm", REPORT_ID)
                    .param("reason", "Duplicate of RPT-2026-KLT-0843"))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showDecisionRecorded"));

    String html = html(result);
    assertContains(
        html,
        "Report rejected",
        "Report ID",
        REPORT_ID,
        "Reason",
        "Duplicate of RPT-2026-KLT-0843",
        "Rejected by",
        "Decision time",
        "21 May 2026, 08:15",
        "Back to queue",
        "Rejected");
    assertLacks(html, "scrim", "Report verified");
    verify(control).rejectReport(REPORT_ID, "Duplicate of RPT-2026-KLT-0843", OFFICER_ID);
  }

  // ---- S02c and S02d: the new hazard
  // ---------------------------------------------------------------

  @Test
  @DisplayName("WF2C-S02c: the new-hazard form has the 25 districts, the report's one ticked")
  void newHazardFormIsShown() throws Exception {
    arrangeReads(
        report(
            REPORT_ID,
            reporter,
            HazardType.BLOCKED_ROAD,
            ReportStatus.PENDING_VERIFICATION,
            new GeoPoint(6.5888, 79.9640, 8.0),
            true),
        List.of(),
        List.of());

    ResultActions result =
        mockMvc
            .perform(get("/hazardreport/{id}/new-hazard", REPORT_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showReportDetails"));

    String html = html(result);
    assertContains(
        html,
        "Verify as new hazard",
        "Severity *",
        "Districts *",
        "Place name *",
        "Description",
        "Hazard type <strong>Blocked road</strong> (from the report)",
        "Source Verified report",
        "No active blocked road hazard fits this report",
        "action=\"/hazardreport/RPT-2026-KLT-0847/new-hazard\"",
        "Verify as New Hazard",
        "Cancel");
    assertThat(html.split("name=\"districts\"", -1).length - 1).isEqualTo(25);
    assertThat(html).containsPattern("value=\"KALUTARA\"\\s+checked=\"checked\"");
    assertThat(html).containsPattern("value=\"MEDIUM\"\\s+selected=\"selected\"");
    assertThat(html.indexOf(">Ampara<")).isLessThan(html.indexOf(">Vavuniya<"));
    assertLacks(html, "Districts are required", "Verify and Link");
  }

  @Test
  @DisplayName("WF2C-S02e: the new hazard is verified with the details, blank text becoming null")
  void newHazardIsVerified() throws Exception {
    HazardReport verified = report(ReportStatus.VERIFIED);
    arrangeReads(verified, List.of(), List.of());
    when(control.verifyAsNewHazard(eq(REPORT_ID), any(NewHazardDetails.class), eq(OFFICER_ID)))
        .thenReturn(new VerificationOutcome(verified, null));

    ResultActions result =
        mockMvc
            .perform(
                post("/hazardreport/{id}/new-hazard", REPORT_ID)
                    .param("severity", "HIGH")
                    .param("districts", "KALUTARA", "RATNAPURA")
                    .param("locationName", "Horana–Ratnapura road at Ingiriya")
                    .param("description", "   "))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showDecisionRecorded"));

    assertContains(html(result), "Report verified");
    ArgumentCaptor<NewHazardDetails> details = ArgumentCaptor.forClass(NewHazardDetails.class);
    verify(control).verifyAsNewHazard(eq(REPORT_ID), details.capture(), eq(OFFICER_ID));
    assertThat(details.getValue().severity()).isNotNull();
    assertThat(details.getValue().districts())
        .containsExactly(District.KALUTARA, District.RATNAPURA);
    assertThat(details.getValue().locationName()).isEqualTo("Horana–Ratnapura road at Ingiriya");
    assertThat(details.getValue().description()).isNull();
  }

  @Test
  @DisplayName("WF2C-S02d (SC2 E9): missing districts show under the field, typed values are kept")
  void newHazardErrorsAreShownUnderTheirFields() throws Exception {
    arrangePending();
    when(control.verifyAsNewHazard(eq(REPORT_ID), any(), eq(OFFICER_ID)))
        .thenThrow(new InvalidHazardDetailsException(List.of("Districts are required")));

    ResultActions result =
        mockMvc
            .perform(
                post("/hazardreport/{id}/new-hazard", REPORT_ID)
                    .param("severity", "HIGH")
                    .param("locationName", "Horana–Ratnapura road at Ingiriya")
                    .param("description", "Road is cut"))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showReportDetails"));

    String html = html(result);
    assertContains(
        html,
        "Districts are required",
        "field--error",
        "value=\"Horana–Ratnapura road at Ingiriya\"",
        ">Road is cut</textarea>",
        "Pending verification");
    assertThat(html).containsPattern("value=\"HIGH\"\\s+selected=\"selected\"");
    assertThat(html).doesNotContainPattern("value=\"KALUTARA\"\\s+checked");
    assertLacks(html, "Severity is required", "Place name is required");
  }

  @Test
  @DisplayName("WF2C-S02d (SC2 E9): all three errors show, each under its own field")
  void allNewHazardErrorsAreShown() throws Exception {
    arrangePending();
    when(control.verifyAsNewHazard(eq(REPORT_ID), any(), eq(OFFICER_ID)))
        .thenThrow(
            new InvalidHazardDetailsException(
                new ArrayList<>(
                    List.of(
                        "Severity is required",
                        "Districts are required",
                        "Place name is required",
                        "Something else"))));

    String html =
        html(
            mockMvc.perform(
                post("/hazardreport/{id}/new-hazard", REPORT_ID)
                    .param("severity", "")
                    .param("districts", "KALUTARA")
                    .param("locationName", " ")));

    assertContains(
        html, "Severity is required", "Districts are required", "Place name is required");
    assertThat(html).containsPattern("value=\"KALUTARA\"\\s+checked=\"checked\"");
  }

  @Test
  @DisplayName("WF2C-S02b (SC2 E7): a rejection posted without a reason field is the same error")
  void missingReasonFieldShowsTheError() throws Exception {
    arrangePending();
    when(control.rejectReport(REPORT_ID, null, OFFICER_ID))
        .thenThrow(new ReasonRequiredException());

    String html =
        html(
            mockMvc
                .perform(post("/hazardreport/{id}/reject", REPORT_ID))
                .andExpect(status().isOk()));

    assertContains(html, "A rejection reason is required", "field--error");
  }

  @Test
  @DisplayName("WF2C-S02d (SC2 E9): a missing severity is shown under its field and none is kept")
  void missingSeverityShowsTheError() throws Exception {
    arrangePending();
    when(control.verifyAsNewHazard(eq(REPORT_ID), any(), eq(OFFICER_ID)))
        .thenThrow(new InvalidHazardDetailsException(List.of("Severity is required")));

    String html =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/{id}/new-hazard", REPORT_ID)
                        .param("districts", "KALUTARA")
                        .param("locationName", "Kelani"))
                .andExpect(status().isOk()));

    assertContains(html, "Severity is required", "field--error");
    assertThat(html).doesNotContainPattern("value=\"MEDIUM\"\\s+selected");
  }

  // ---- S02e and S02g: verified, with and without a warning
  // ---------------------------------------------

  @Test
  @DisplayName(
      "WF2C-S02e: verifying shows Report verified with the linked hazard, the officer and the time")
  void verifyShowsTheDecision() throws Exception {
    HazardReport verified = report(ReportStatus.VERIFIED);
    arrangeReads(verified, List.of(), List.of());
    when(control.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID))
        .thenReturn(new VerificationOutcome(verified, null));

    ResultActions result =
        mockMvc
            .perform(post("/hazardreport/{id}/verify", REPORT_ID).param("hazardId", HAZARD_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showDecisionRecorded"));

    String html = html(result);
    assertContains(
        html,
        "Report verified",
        "Linked hazard",
        "Flood — Kalu Ganga at Ratnapura (HZ-2026-0142)",
        "Verified by",
        "Verification time",
        "21 May 2026, 08:15",
        "Back to queue",
        "Verified",
        "Report RPT-2026-KLT-0847",
        "decision recorded");
    assertLacks(html, "scrim", "Escalate", "Verify and Link");
  }

  @Test
  @DisplayName("WF2C-S02g: a verified hazard with an active warning shows the escalation prompt")
  void verifyWithAnActiveWarningShowsThePrompt() throws Exception {
    HazardReport verified = report(ReportStatus.VERIFIED);
    Warning warning =
        SampleEntities.warning(
            "WRN-2026-0087",
            hazard,
            SampleEntities.officer("DMC-0001", java.util.Set.of()),
            List.of(District.RATNAPURA, District.KALUTARA),
            List.of(),
            LocalDateTime.of(2026, 5, 21, 10, 45));
    arrangeReads(verified, List.of(), List.of());
    when(control.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID))
        .thenReturn(new VerificationOutcome(verified, warning));

    ResultActions result =
        mockMvc
            .perform(post("/hazardreport/{id}/verify", REPORT_ID).param("hazardId", HAZARD_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showEscalationPrompt"));

    String html = html(result);
    assertContains(
        html,
        "Report verified",
        "scrim",
        "Flood — Kalu Ganga at Ratnapura has an active warning — WRN-2026-0087,"
            + " Level 2 – Amber (Be Alert), Ratnapura and Kalutara,"
            + " valid until 21 May 2026 10:45. Escalate this warning?",
        "tag tag--l2",
        ">Level 2 – Amber (Be Alert)<",
        "href=\"/warning/WRN-2026-0087/escalate\"",
        "Escalate",
        "Not now",
        "action=\"/hazardreport/RPT-2026-KLT-0847/dismiss-escalation\"");
  }

  @Test
  @DisplayName("WF2C-S02g (SC2 A7 29d): Not now shows the verified page without the dialog")
  void dismissingThePromptChangesNothing() throws Exception {
    arrangeReads(report(ReportStatus.VERIFIED), List.of(), List.of());

    ResultActions result =
        mockMvc
            .perform(post("/hazardreport/{id}/dismiss-escalation", REPORT_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("hazardreport/showDecisionRecorded"));

    assertContains(html(result), "Report verified", "Back to queue");
    assertLacks(html(result), "scrim", "has an active warning");
    verify(control, never()).verifyReport(anyString(), anyString(), anyString());
    verify(control, never()).verifyAsNewHazard(anyString(), any(), anyString());
    verify(control, never()).rejectReport(anyString(), any(), anyString());
  }

  // ---- S02h: already decided
  // -------------------------------------------------------------------------------

  @Test
  @DisplayName("WF2C-S02h (SC2 E10): each of the three decisions on a decided report is refused")
  void eachDecisionShowsTheAlreadyDecidedPage() throws Exception {
    HazardReport decided = report(ReportStatus.VERIFIED);
    arrangeReads(decided, List.of(), List.of());
    when(control.verifyReport(anyString(), anyString(), anyString()))
        .thenThrow(new ReportAlreadyDecidedException(decided));
    when(control.verifyAsNewHazard(anyString(), any(), anyString()))
        .thenThrow(new ReportAlreadyDecidedException(decided));
    when(control.rejectReport(anyString(), any(), anyString()))
        .thenThrow(new ReportAlreadyDecidedException(decided));

    List<ResultActions> attempts =
        List.of(
            mockMvc.perform(
                post("/hazardreport/{id}/verify", REPORT_ID).param("hazardId", HAZARD_ID)),
            mockMvc.perform(
                post("/hazardreport/{id}/new-hazard", REPORT_ID).param("severity", "LOW")),
            mockMvc.perform(
                post("/hazardreport/{id}/reject/confirm", REPORT_ID).param("reason", "late")));

    for (ResultActions attempt : attempts) {
      String html =
          html(
              attempt
                  .andExpect(status().isOk())
                  .andExpect(view().name("hazardreport/showReportDetails")));
      assertContains(
          html,
          "This report has already been decided",
          "Recorded status",
          "Verified",
          "Decision time",
          "21 May 2026, 08:15",
          "Back to queue");
      assertLacks(html, "Verify and Link", "Verify as New Hazard", ">Reject<", "Report verified");
    }
  }

  @Test
  @DisplayName("WF2C-S02h (SC2 E10): an empty reason on a decided report is E10, not E7")
  void anEmptyReasonOnADecidedReportIsAlreadyDecided() throws Exception {
    HazardReport decided = report(ReportStatus.REJECTED);
    arrangeReads(decided, List.of(), List.of());
    when(control.rejectReport(REPORT_ID, "", OFFICER_ID))
        .thenThrow(new ReportAlreadyDecidedException(decided));

    String html =
        html(mockMvc.perform(post("/hazardreport/{id}/reject", REPORT_ID).param("reason", "")));

    assertContains(html, "This report has already been decided", "Rejected");
    assertLacks(html, "A rejection reason is required");
  }

  @Test
  @DisplayName("WF2C-S02h: opening a decided report or its forms shows the decided page")
  void openingADecidedReportShowsTheDecidedPage() throws Exception {
    arrangeReads(report(ReportStatus.REJECTED), List.of(), List.of());

    for (String path : List.of("", "/reject", "/new-hazard")) {
      String html =
          html(
              mockMvc
                  .perform(get("/hazardreport/{id}" + path, REPORT_ID))
                  .andExpect(status().isOk()));
      assertContains(html, "This report has already been decided", "Rejected", "Back to queue");
      assertLacks(html, "Confirm rejection", "Verify as new hazard");
    }
  }

  // ---- C1: the common error banner
  // ------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "SC2 E5 / C1: a failed decision shows the details page with the banner and the status 500")
  void failedDecisionShowsTheBanner() throws Exception {
    arrangePending();
    when(control.verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID))
        .thenThrow(new UnsupportedOperationException("not implemented yet"));

    ResultActions result =
        mockMvc
            .perform(post("/hazardreport/{id}/verify", REPORT_ID).param("hazardId", HAZARD_ID))
            .andExpect(status().isInternalServerError())
            .andExpect(view().name("hazardreport/showReportDetails"));

    String html = html(result);
    assertContains(
        html,
        "The service is unavailable and nothing was changed",
        "Report RPT-2026-KLT-0847",
        "Pending verification",
        "Verify and Link");
  }

  @Test
  @DisplayName("SC2 E5 / C1: a failed rejection and a failed new hazard show the banner too")
  void otherFailedDecisionsShowTheBanner() throws Exception {
    arrangePending();
    when(control.rejectReport(anyString(), any(), anyString()))
        .thenThrow(new IllegalStateException("database down"));
    when(control.verifyAsNewHazard(anyString(), any(), anyString()))
        .thenThrow(new IllegalStateException("database down"));

    mockMvc
        .perform(post("/hazardreport/{id}/reject/confirm", REPORT_ID).param("reason", "x"))
        .andExpect(status().isInternalServerError());
    mockMvc
        .perform(post("/hazardreport/{id}/new-hazard", REPORT_ID).param("severity", "LOW"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @DisplayName("WF2C-S02: a decision on an unknown report is a 404")
  void decisionOnAnUnknownReportIsNotFound() throws Exception {
    when(control.rejectReport(anyString(), any(), anyString()))
        .thenThrow(new NoSuchElementException("report RPT-0 not found"));

    mockMvc
        .perform(post("/hazardreport/{id}/reject/confirm", "RPT-0").param("reason", "x"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("WF2C-S02: verifying without a hazard is a malformed request, a 400")
  void verifyWithoutAHazardIsABadRequest() throws Exception {
    mockMvc
        .perform(post("/hazardreport/{id}/verify", REPORT_ID))
        .andExpect(status().isBadRequest());
  }

  // ---- transactions (UC2-3 review G1)
  // -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "UC2-3 G1: the three decisions run outside any transaction, the reads inside a read-only one")
  void decisionsRunOutsideATransactionAndReadsInside() throws Exception {
    HazardReport verified = report(ReportStatus.VERIFIED);
    AtomicBoolean decisionsInTransaction = new AtomicBoolean();
    List<Boolean> readsReadOnly = new ArrayList<>();
    when(control.getReportDetails(REPORT_ID))
        .thenAnswer(
            call -> {
              readsReadOnly.add(
                  TransactionSynchronizationManager.isActualTransactionActive()
                      && TransactionSynchronizationManager.isCurrentTransactionReadOnly());
              return verified;
            });
    when(control.findNearbyReports(REPORT_ID)).thenReturn(List.of());
    when(control.findCandidateHazards(REPORT_ID)).thenReturn(List.of());
    when(control.verifyReport(anyString(), anyString(), anyString()))
        .thenAnswer(
            call -> {
              decisionsInTransaction.compareAndSet(
                  false, TransactionSynchronizationManager.isActualTransactionActive());
              return new VerificationOutcome(verified, null);
            });
    when(control.verifyAsNewHazard(anyString(), any(), anyString()))
        .thenAnswer(
            call -> {
              decisionsInTransaction.compareAndSet(
                  false, TransactionSynchronizationManager.isActualTransactionActive());
              return new VerificationOutcome(verified, null);
            });
    when(control.rejectReport(anyString(), any(), anyString()))
        .thenAnswer(
            call -> {
              decisionsInTransaction.compareAndSet(
                  false, TransactionSynchronizationManager.isActualTransactionActive());
              return verified;
            });

    mockMvc.perform(post("/hazardreport/{id}/verify", REPORT_ID).param("hazardId", HAZARD_ID));
    mockMvc.perform(post("/hazardreport/{id}/new-hazard", REPORT_ID).param("severity", "LOW"));
    mockMvc.perform(post("/hazardreport/{id}/reject/confirm", REPORT_ID).param("reason", "x"));

    verify(control).verifyReport(REPORT_ID, HAZARD_ID, OFFICER_ID);
    verify(control).verifyAsNewHazard(eq(REPORT_ID), any(), eq(OFFICER_ID));
    verify(control).rejectReport(REPORT_ID, "x", OFFICER_ID);
    assertThat(decisionsInTransaction).isFalse();
    assertThat(readsReadOnly).hasSize(3).containsOnly(true);
  }

  @Test
  @DisplayName("UC2-3 G1: neither the console nor its decision handlers carry @Transactional")
  void noDecisionHandlerIsAnnotatedAsTransactional() throws NoSuchMethodException {
    assertThat(
            AnnotatedElementUtils.hasAnnotation(VerificationConsoleUI.class, Transactional.class))
        .isFalse();
    for (String name :
        List.of("verifyReport", "verifyAsNewHazard", "rejectReport", "confirmRejection")) {
      Method handler =
          java.util.Arrays.stream(VerificationConsoleUI.class.getDeclaredMethods())
              .filter(method -> method.getName().equals(name))
              .findFirst()
              .orElseThrow();
      assertThat(AnnotatedElementUtils.hasAnnotation(handler, Transactional.class))
          .as(name)
          .isFalse();
    }
  }
}
