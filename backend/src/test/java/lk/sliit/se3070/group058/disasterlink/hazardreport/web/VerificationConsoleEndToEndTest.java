package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushKind;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushOutbox;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushOutboxRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCDutyOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The verification console end to end against H2: the real control, repositories, transaction
 * manager and mock gateways; only UC1's {@code WarningController} is a mock. The pages are read
 * through MockMvc, so the lazy associations (reporter, reviewer, hazard, the warning's districts)
 * can only be read inside the transactions of the console and the control (open-in-view is off).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class VerificationConsoleEndToEndTest {

  private static final String CITIZEN_ID = "CIT-KLT-0001";
  private static final String OFFICER_ID = "DMC-0002";
  private static final String FIRST = "RPT-2026-KLT-0843";
  private static final String SECOND = "RPT-2026-KLT-0847";

  /**
   * The nearby search looks at the last 24 hours of the real clock, so the reports are made now.
   */
  private static final LocalDateTime CAPTURED = LocalDateTime.now().minusHours(1);

  @Autowired private MockMvc mockMvc;
  @Autowired private CitizenRepository citizens;
  @Autowired private DMCDutyOfficerRepository dutyOfficers;
  @Autowired private DMCOfficerRepository officers;
  @Autowired private HazardReportRepository reports;
  @Autowired private HazardRepository hazards;
  @Autowired private WarningRepository warnings;
  @Autowired private PushOutboxRepository outbox;

  @MockitoBean private WarningController warningController;

  @BeforeEach
  void arrange() {
    Citizen citizen = citizens.save(SampleEntities.citizen(CITIZEN_ID, "token"));
    dutyOfficers.save(SampleEntities.dutyOfficer(OFFICER_ID));
    reports.save(
        pending(FIRST, citizen, new GeoPoint(6.7098, 80.3992, null), CAPTURED.minusMinutes(30)));
    reports.save(pending(SECOND, citizen, SampleEntities.RATNAPURA_POINT, CAPTURED));
    when(warningController.getActiveWarningForHazard(anyString())).thenReturn(Optional.empty());
  }

  private static HazardReport pending(
      String id, Citizen reporter, GeoPoint location, LocalDateTime captured) {
    return new HazardReport(
        id,
        reporter,
        HazardType.FLOOD,
        "Water is rising",
        id + ".jpg",
        location,
        true,
        District.KALUTARA,
        captured,
        captured.plusMinutes(2),
        ReportStatus.PENDING_VERIFICATION,
        null,
        null,
        null,
        null);
  }

  private static String html(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  private ResultActions page(String path) throws Exception {
    return mockMvc.perform(get(path));
  }

  @Test
  @DisplayName(
      "SC2 steps 21-30, A4, E7, E10: queue, details, reject (E7 then success), reject again")
  void queueDetailsRejectAndRejectAgain() throws Exception {
    String queue = html(page("/hazardreport").andExpect(status().isOk()));
    assertThat(queue).contains("2 reports", FIRST, SECOND, "Citizen");
    assertThat(queue.indexOf(FIRST)).isLessThan(queue.indexOf(SECOND));

    String details = html(page("/hazardreport/" + SECOND).andExpect(status().isOk()));
    assertThat(details)
        .contains(
            "Report " + SECOND, "Nimal Perera", "Water is rising", "/reports/" + SECOND + "/photo")
        .contains(FIRST, " km", "Verify as New Hazard", "Reject");

    String emptyReason =
        html(
            mockMvc
                .perform(post("/hazardreport/" + SECOND + "/reject").param("reason", ""))
                .andExpect(status().isOk()));
    assertThat(emptyReason).contains("A rejection reason is required");
    assertThat(reports.findById(SECOND).orElseThrow().getStatus())
        .isEqualTo(ReportStatus.PENDING_VERIFICATION);

    String confirmation =
        html(
            mockMvc
                .perform(post("/hazardreport/" + SECOND + "/reject").param("reason", "Not a flood"))
                .andExpect(status().isOk()));
    assertThat(confirmation)
        .contains("Reject " + SECOND + " with this reason?", "Confirm rejection");
    assertThat(reports.findById(SECOND).orElseThrow().getStatus())
        .isEqualTo(ReportStatus.PENDING_VERIFICATION);

    String rejected =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/" + SECOND + "/reject/confirm")
                        .param("reason", "Not a flood"))
                .andExpect(status().isOk()));
    assertThat(rejected).contains("Report rejected", "Not a flood", "Rejected by", "Decision time");
    HazardReport stored = reports.findById(SECOND).orElseThrow();
    assertThat(stored.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(stored.getRejectionReason()).contains("Not a flood");
    List<PushOutbox> entries = outbox.findUndelivered(CITIZEN_ID);
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getKind()).isEqualTo(PushKind.REPORT_DECISION);
    assertThat(entries.get(0).getReportId()).contains(SECOND);
    assertThat(entries.get(0).getStatus()).contains(ReportStatus.REJECTED);
    assertThat(entries.get(0).getReason()).contains("Not a flood");

    String again =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/" + SECOND + "/reject/confirm").param("reason", "Again"))
                .andExpect(status().isOk()));
    assertThat(again)
        .contains("This report has already been decided", "Recorded status", "Rejected");
    assertThat(outbox.findUndelivered(CITIZEN_ID)).hasSize(1);
    assertThat(html(page("/hazardreport"))).contains("1 report<").doesNotContain(SECOND);
  }

  @Test
  @DisplayName(
      "SC2 A6, E9: a new hazard with no districts is refused with the error, then verified")
  void verifyAsNewHazard() throws Exception {
    String refused =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/" + SECOND + "/new-hazard")
                        .param("severity", "HIGH")
                        .param("locationName", "Kelani at Kaduwela"))
                .andExpect(status().isOk()));
    assertThat(refused).contains("Districts are required", "Kelani at Kaduwela");
    assertThat(hazards.count()).isZero();

    String verified =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/" + SECOND + "/new-hazard")
                        .param("severity", "HIGH")
                        .param("districts", "COLOMBO", "GAMPAHA")
                        .param("locationName", "Kelani at Kaduwela"))
                .andExpect(status().isOk()));
    assertThat(verified)
        .contains("Report verified", "Linked hazard", "Flood — Kelani at Kaduwela (HZ-");
    assertThat(reports.findById(SECOND).orElseThrow().getStatus()).isEqualTo(ReportStatus.VERIFIED);
    List<Hazard> created = hazards.findAll();
    assertThat(created).hasSize(1);
    assertThat(created.get(0).getHazardId()).matches("HZ-\\d{4}-\\d{4}");
    assertThat(outbox.findUndelivered(CITIZEN_ID)).hasSize(1);
  }

  @Test
  @DisplayName("SC2 steps 25-28, A7: verifying against a candidate with a warning shows the prompt")
  void verifyAgainstACandidateWithAnActiveWarning() throws Exception {
    DMCOfficer dmcOfficer = officers.save(SampleEntities.officer("DMC-0001", Set.of()));
    Hazard hazard = hazards.save(SampleEntities.hazard("HZ-2026-0142"));
    warnings.save(
        SampleEntities.warning(
            "WRN-2026-0087",
            hazard,
            dmcOfficer,
            List.of(District.RATNAPURA, District.KALUTARA),
            List.of(),
            LocalDateTime.of(2026, 5, 22, 10, 45)));
    when(warningController.getActiveWarningForHazard("HZ-2026-0142"))
        .thenAnswer(call -> warnings.findById("WRN-2026-0087"));

    String details = html(page("/hazardreport/" + SECOND).andExpect(status().isOk()));
    assertThat(details)
        .contains("Flood — Kalu Ganga at Ratnapura", "HZ-2026-0142", "Verify and Link");

    String prompt =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/" + SECOND + "/verify").param("hazardId", "HZ-2026-0142"))
                .andExpect(status().isOk()));
    assertThat(prompt)
        .contains(
            "Report verified",
            "has an active warning — WRN-2026-0087, Level 2 – Amber (Be Alert),"
                + " Ratnapura and Kalutara, valid until 22 May 2026 10:45. Escalate this warning?",
            "/warning/WRN-2026-0087/escalate");
    assertThat(reports.findById(SECOND).orElseThrow().getStatus()).isEqualTo(ReportStatus.VERIFIED);

    String dismissed =
        html(
            mockMvc
                .perform(post("/hazardreport/" + SECOND + "/dismiss-escalation"))
                .andExpect(status().isOk()));
    assertThat(dismissed).contains("Report verified").doesNotContain("has an active warning");
  }

  @Test
  @DisplayName("SC2 E5, C1: a failing warning lookup shows the banner, status 500, nothing changed")
  void failingWarningLookupChangesNothing() throws Exception {
    hazards.save(SampleEntities.hazard("HZ-2026-0142"));
    when(warningController.getActiveWarningForHazard(anyString()))
        .thenThrow(new UnsupportedOperationException("not implemented yet"));

    String banner =
        html(
            mockMvc
                .perform(
                    post("/hazardreport/" + SECOND + "/verify").param("hazardId", "HZ-2026-0142"))
                .andExpect(status().isInternalServerError()));

    assertThat(banner)
        .contains(
            "The service is unavailable and nothing was changed",
            "Report " + SECOND,
            "Pending verification");
    assertThat(reports.findById(SECOND).orElseThrow().getStatus())
        .isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(outbox.findUndelivered(CITIZEN_ID)).isEmpty();
  }

  @Test
  @DisplayName("SC2 step 23: an unknown report is a 404")
  void anUnknownReportIsNotFound() throws Exception {
    page("/hazardreport/RPT-0").andExpect(status().isNotFound());
  }
}
