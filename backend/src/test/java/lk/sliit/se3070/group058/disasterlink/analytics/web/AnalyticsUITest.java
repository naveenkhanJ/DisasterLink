package lk.sliit.se3070.group058.disasterlink.analytics.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ExportFailedException;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ShareOutcome;
import lk.sliit.se3070.group058.disasterlink.analytics.service.AnalyticsController;
import lk.sliit.se3070.group058.disasterlink.shared.domain.CriteriaCheck;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFile;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFormat;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSource;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link AnalyticsUI}.
 *
 * <p>Implements scenario flow tests for SC4 steps 1-15, A1, A2, A3, A5 and E4.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsUITest {

  @Mock private AnalyticsController analyticsController;
  @Mock private CurrentUserService currentUserService;

  private MockMvc mockMvc;
  private ReportCriteria sampleCriteria;
  private DMCOfficer sampleOfficer;
  private CurrentUserView officerUserView;

  @BeforeEach
  void setUp() {
    AnalyticsUI controller = new AnalyticsUI(analyticsController, currentUserService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    sampleOfficer =
        new DMCOfficer(
            "OFF-1",
            "A. Perera",
            "0771234567",
            "perera@example.lk",
            "HQ",
            Set.of(District.RATNAPURA, District.KALUTARA));

    officerUserView = new CurrentUserView("AP", "A. Perera", "DMC Officer");

    sampleCriteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE),
            "HZ-2026-0142");

    lenient().when(currentUserService.userId(DemoRole.DMC_OFFICER)).thenReturn("OFF-1");
    lenient()
        .when(currentUserService.currentUser(DemoRole.DMC_OFFICER))
        .thenReturn(officerUserView);
  }

  @Test
  @DisplayName("SC4 Step 1 / WF4-S01 — openAnalytics displays templates and closed hazards")
  void openAnalyticsDisplaysWorkspace() throws Exception {
    CriteriaTemplate template =
        new CriteriaTemplate("TMP-1", "Monsoon Template", sampleCriteria, sampleOfficer);
    Hazard hazard =
        new Hazard(
            "HZ-1",
            HazardType.FLOOD,
            "Flood",
            HazardSeverity.MEDIUM,
            LocalDateTime.of(2026, 5, 1, 10, 0),
            LocalDateTime.of(2026, 5, 6, 18, 0),
            new GeoPoint(6.68, 80.39, 15.0),
            "Ratnapura",
            List.of(District.RATNAPURA),
            RiverBasin.KALU,
            HazardSource.AGENCY_BULLETIN,
            HazardStatus.CLOSED);

    when(analyticsController.getTemplates("OFF-1")).thenReturn(List.of(template));
    when(analyticsController.getClosedEvents()).thenReturn(List.of(hazard));

    mockMvc
        .perform(get("/analytics"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/analyticsWorkspace"))
        .andExpect(model().attribute("templates", List.of(template)))
        .andExpect(model().attribute("hazards", List.of(hazard)))
        .andExpect(model().attribute("currentUser", officerUserView));
  }

  @Test
  @DisplayName("SC4 A2 — selectTemplate pre-fills criteria from chosen template")
  void selectTemplatePreFillsCriteria() throws Exception {
    CriteriaTemplate template =
        new CriteriaTemplate("TMP-1", "Monsoon Template", sampleCriteria, sampleOfficer);
    when(analyticsController.getTemplates("OFF-1")).thenReturn(List.of(template));
    when(analyticsController.getClosedEvents()).thenReturn(List.of());

    mockMvc
        .perform(get("/analytics/template").param("templateId", "TMP-1"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/analyticsWorkspace"))
        .andExpect(model().attribute("selectedTemplateId", "TMP-1"))
        .andExpect(model().attribute("criteria", sampleCriteria));
  }

  @Test
  @DisplayName("SC4 Step 3 / WF4-S01 — selectEvent pre-fills criteria from hazard")
  void selectEventPreFillsCriteria() throws Exception {
    when(analyticsController.getCriteriaForEvent("HZ-1")).thenReturn(sampleCriteria);
    when(analyticsController.getTemplates("OFF-1")).thenReturn(List.of());
    when(analyticsController.getClosedEvents()).thenReturn(List.of());

    mockMvc
        .perform(get("/analytics/event").param("hazardId", "HZ-1"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/analyticsWorkspace"))
        .andExpect(model().attribute("selectedHazardId", "HZ-1"))
        .andExpect(model().attribute("criteria", sampleCriteria));
  }

  @Test
  @DisplayName("SC4 Steps 6-9 / WF4-S02 — enterCriteria generates report when valid")
  void enterCriteriaGeneratesReport() throws Exception {
    CriteriaCheck check = new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null);
    when(analyticsController.validateCriteria(any(ReportCriteria.class), eq("OFF-1")))
        .thenReturn(check);

    ReportResults results =
        new ReportResults(206248, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    DisasterReport report =
        new DisasterReport(
            "DR-2026-0184",
            "Flood response",
            LocalDateTime.of(2026, 10, 8, 20, 30),
            sampleCriteria,
            results,
            sampleOfficer,
            List.of());
    when(analyticsController.generateReport(any(ReportCriteria.class), eq("OFF-1")))
        .thenReturn(report);

    mockMvc
        .perform(
            post("/analytics/criteria")
                .param("districts", "RATNAPURA")
                .param("hazardType", "FLOOD")
                .param("periodStart", "2026-05-20")
                .param("periodEnd", "2026-05-25")
                .param("metrics", "ALERT_TIMELINE")
                .param("hazardId", "HZ-2026-0142"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/showResults"))
        .andExpect(model().attribute("report", report));
  }

  @Test
  @DisplayName("SC4 A1 / P60 N26 — enterCriteria displays no records suggestion when count is 0")
  void enterCriteriaDisplaysNoRecordsSuggestion() throws Exception {
    CriteriaCheck check = new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null);
    when(analyticsController.validateCriteria(any(ReportCriteria.class), eq("OFF-1")))
        .thenReturn(check);

    ReportResults emptyResults =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    DisasterReport emptyReport =
        new DisasterReport(
            "DR-UNSAVED",
            "Empty",
            LocalDateTime.now(),
            sampleCriteria,
            emptyResults,
            sampleOfficer,
            List.of());
    when(analyticsController.generateReport(any(ReportCriteria.class), eq("OFF-1")))
        .thenReturn(emptyReport);

    mockMvc
        .perform(post("/analytics/criteria"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/analyticsWorkspace"))
        .andExpect(model().attributeExists("noRecordsSuggestion"));
  }

  @Test
  @DisplayName("SC4 E4 — enterCriteria displays out of scope notice and offers adjusted criteria")
  void enterCriteriaDisplaysOutOfScopeNotice() throws Exception {
    ReportCriteria adjusted =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE),
            null);
    CriteriaCheck check =
        new CriteriaCheck(
            new ValidationResult(false, List.of("Colombo out of scope")),
            Set.of(District.COLOMBO),
            adjusted);
    when(analyticsController.validateCriteria(any(ReportCriteria.class), eq("OFF-1")))
        .thenReturn(check);

    mockMvc
        .perform(post("/analytics/criteria"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/analyticsWorkspace"))
        .andExpect(model().attribute("outOfScopeDistricts", Set.of(District.COLOMBO)))
        .andExpect(model().attribute("adjustedCriteria", adjusted));
  }

  @Test
  @DisplayName("SC4 Step 6f / FX-05 — confirmAdjustedCriteria re-validates and generates report")
  void confirmAdjustedCriteriaRevalidatesAndGenerates() throws Exception {
    CriteriaCheck check = new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null);
    when(analyticsController.validateCriteria(sampleCriteria, "OFF-1")).thenReturn(check);

    ReportResults results =
        new ReportResults(100, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    DisasterReport report =
        new DisasterReport(
            "DR-2026-0184",
            "Report",
            LocalDateTime.now(),
            sampleCriteria,
            results,
            sampleOfficer,
            List.of());
    when(analyticsController.generateReport(sampleCriteria, "OFF-1")).thenReturn(report);

    mockMvc
        .perform(
            post("/analytics/confirmAdjustedCriteria")
                .sessionAttr("adjustedCriteriaSession", sampleCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/showResults"))
        .andExpect(model().attribute("report", report));
  }

  @Test
  @DisplayName("SC4 Step 10 / WF4-S03 — enterFindings saves findings text and renders view")
  void enterFindingsSavesFindings() throws Exception {
    ReportResults results =
        new ReportResults(10, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    DisasterReport report =
        new DisasterReport(
            "DR-2026-0184",
            "Report",
            LocalDateTime.now(),
            sampleCriteria,
            results,
            sampleOfficer,
            List.of());
    report.recordFindings("Action points agreed with DDMCU");

    when(analyticsController.saveFindings("DR-2026-0184", "Action points agreed with DDMCU"))
        .thenReturn(report);

    mockMvc
        .perform(
            post("/analytics/findings")
                .param("reportId", "DR-2026-0184")
                .param("text", "Action points agreed with DDMCU"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/showResultsWithFindings"))
        .andExpect(model().attribute("report", report));
  }

  @Test
  @DisplayName("SC4 Step 12 / CH-32 — exportReport delivers downloadable file")
  void exportReportDeliversFileDownload() throws Exception {
    byte[] pdfBytes = "%PDF-1.4 test content".getBytes();
    ExportFile exportFile = new ExportFile("DR-2026-0184_report.pdf", ExportFormat.PDF, pdfBytes);

    when(analyticsController.exportReport("DR-2026-0184", ExportFormat.PDF, "OFF-1"))
        .thenReturn(exportFile);

    mockMvc
        .perform(get("/analytics/export").param("reportId", "DR-2026-0184").param("format", "PDF"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"DR-2026-0184_report.pdf\""));
  }

  @Test
  @DisplayName("SC4 Step 14 / CH-32 — shareReport categorises organisations and displays outcome")
  void shareReportDisplaysOutcome() throws Exception {
    Organization care = SampleEntities.organization("ORG-1");
    Organization sarvodaya = SampleEntities.volunteerGroup("ORG-2");
    ReportResults results =
        new ReportResults(1, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    DisasterReport report =
        new DisasterReport(
            "DR-2026-0184",
            "Flood response",
            LocalDateTime.now(),
            sampleCriteria,
            results,
            sampleOfficer,
            List.of());
    ShareOutcome outcome = new ShareOutcome(report, List.of(care), List.of(sarvodaya));
    when(analyticsController.shareReport("DR-2026-0184", List.of("ORG-1", "ORG-2"), "OFF-1"))
        .thenReturn(outcome);

    mockMvc
        .perform(
            post("/analytics/share")
                .param("reportId", "DR-2026-0184")
                .param("organizationIds", "ORG-1", "ORG-2"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/showShared"))
        .andExpect(model().attribute("outcome", outcome))
        .andExpect(model().attribute("reportId", "DR-2026-0184"));
  }

  @Test
  @DisplayName("SC4 E2 / CH-32 — showExportError handles ExportFailedException")
  void exportReportFailureRendersErrorNotice() throws Exception {
    when(analyticsController.exportReport("DR-2026-0184", ExportFormat.PDF, "OFF-1"))
        .thenThrow(new ExportFailedException("Renderer failure"));

    mockMvc
        .perform(get("/analytics/export").param("reportId", "DR-2026-0184").param("format", "PDF"))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/exportShareChosen"))
        .andExpect(model().attribute("exportError", "Renderer failure"));
  }

  @Test
  @DisplayName(
      "SC4 Step 6f / FX-05 — confirmAdjustedCriteria redisplays workspace on validation failure")
  void confirmAdjustedCriteriaRedisplaysWorkspaceOnFailure() throws Exception {
    CriteriaCheck check =
        new CriteriaCheck(new ValidationResult(false, List.of("Invalid dates")), Set.of(), null);
    when(analyticsController.validateCriteria(sampleCriteria, "OFF-1")).thenReturn(check);

    mockMvc
        .perform(
            post("/analytics/confirmAdjustedCriteria")
                .sessionAttr("adjustedCriteriaSession", sampleCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/analyticsWorkspace"))
        .andExpect(model().attributeExists("validationErrors"));
  }

  @Test
  @DisplayName(
      "SC4 Step 6f / FX-05 — confirmAdjustedCriteria displays no records suggestion when count is 0")
  void confirmAdjustedCriteriaDisplaysNoRecordsSuggestion() throws Exception {
    CriteriaCheck check = new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null);
    when(analyticsController.validateCriteria(sampleCriteria, "OFF-1")).thenReturn(check);

    ReportResults emptyResults =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    DisasterReport emptyReport =
        new DisasterReport(
            "DR-EMPTY",
            "Empty",
            LocalDateTime.now(),
            sampleCriteria,
            emptyResults,
            sampleOfficer,
            List.of());
    when(analyticsController.generateReport(sampleCriteria, "OFF-1")).thenReturn(emptyReport);

    mockMvc
        .perform(
            post("/analytics/confirmAdjustedCriteria")
                .sessionAttr("adjustedCriteriaSession", sampleCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("analytics/analyticsWorkspace"))
        .andExpect(model().attributeExists("noRecordsSuggestion"));
  }

  @Test
  @DisplayName("SC4 Step 12 / CH-32 — exportReport delivers Excel and CSV downloads")
  void exportReportDeliversExcelAndCsv() throws Exception {
    ExportFile excelFile =
        new ExportFile("DR-report.xlsx", ExportFormat.EXCEL, "xlsx bytes".getBytes());
    when(analyticsController.exportReport("DR-2026-0184", ExportFormat.EXCEL, "OFF-1"))
        .thenReturn(excelFile);

    mockMvc
        .perform(
            get("/analytics/export").param("reportId", "DR-2026-0184").param("format", "EXCEL"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_TYPE,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    ExportFile csvFile = new ExportFile("DR-report.csv", ExportFormat.CSV, "csv bytes".getBytes());
    when(analyticsController.exportReport("DR-2026-0184", ExportFormat.CSV, "OFF-1"))
        .thenReturn(csvFile);

    mockMvc
        .perform(get("/analytics/export").param("reportId", "DR-2026-0184").param("format", "CSV"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"));
  }
}
