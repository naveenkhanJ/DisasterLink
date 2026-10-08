package lk.sliit.se3070.group058.disasterlink.analytics.web;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ExportFailedException;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ShareOutcome;
import lk.sliit.se3070.group058.disasterlink.analytics.service.AnalyticsController;
import lk.sliit.se3070.group058.disasterlink.shared.domain.CriteriaCheck;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFile;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFormat;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

/**
 * Screen controller for the DMC Officer analytics workspace.
 *
 * <p>Implements CH-10, CH-31, CH-32 and CH-33. Coordinates criteria templates, event pre-fills,
 * criteria validation and out-of-scope adjustments, report generation, findings, exports and
 * sharing.
 */
@Controller
@RequestMapping("/analytics")
@SessionAttributes("adjustedCriteriaSession")
public class AnalyticsUI {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyticsUI.class);

  private final AnalyticsController analyticsController;
  private final CurrentUserService currentUserService;

  /**
   * Constructs the analytics boundary controller with required dependencies.
   *
   * @param analyticsController the analytics application coordinator
   * @param currentUserService the demo user context service
   */
  public AnalyticsUI(
      AnalyticsController analyticsController, CurrentUserService currentUserService) {
    this.analyticsController =
        Objects.requireNonNull(analyticsController, "analyticsController must not be null");
    this.currentUserService =
        Objects.requireNonNull(currentUserService, "currentUserService must not be null");
  }

  /**
   * Opens the analytics workspace showing criteria templates and closed events (WF4-S01).
   *
   * @param model the Spring MVC model
   * @return the template name
   */
  @GetMapping({"", "/"})
  public String openAnalytics(Model model) {
    String officerId = currentUserService.userId(DemoRole.DMC_OFFICER);
    List<CriteriaTemplate> templates = analyticsController.getTemplates(officerId);
    List<Hazard> hazards = analyticsController.getClosedEvents();

    model.addAttribute("templates", templates);
    model.addAttribute("hazards", hazards);
    populateCurrentUser(model);
    return "analytics/analyticsWorkspace";
  }

  /**
   * Pre-fills criteria from a selected template (A2).
   *
   * @param templateId the selected criteria template identifier
   * @param model the Spring MVC model
   * @return the workspace view
   */
  @GetMapping("/template")
  public String selectTemplate(@RequestParam("templateId") String templateId, Model model) {
    String officerId = currentUserService.userId(DemoRole.DMC_OFFICER);
    List<CriteriaTemplate> templates = analyticsController.getTemplates(officerId);
    List<Hazard> hazards = analyticsController.getClosedEvents();

    Optional<CriteriaTemplate> matched =
        templates.stream().filter(t -> t.getTemplateId().equals(templateId)).findFirst();
    matched.ifPresent(template -> model.addAttribute("criteria", template.getCriteria()));

    model.addAttribute("selectedTemplateId", templateId);
    model.addAttribute("templates", templates);
    model.addAttribute("hazards", hazards);
    populateCurrentUser(model);
    return "analytics/analyticsWorkspace";
  }

  /**
   * Pre-fills criteria from a selected closed event (Step 3).
   *
   * @param hazardId the selected closed hazard identifier
   * @param model the Spring MVC model
   * @return the workspace view
   */
  @GetMapping("/event")
  public String selectEvent(@RequestParam("hazardId") String hazardId, Model model) {
    String officerId = currentUserService.userId(DemoRole.DMC_OFFICER);
    ReportCriteria criteria = analyticsController.getCriteriaForEvent(hazardId);

    model.addAttribute("criteria", criteria);
    model.addAttribute("selectedHazardId", hazardId);
    model.addAttribute("templates", analyticsController.getTemplates(officerId));
    model.addAttribute("hazards", analyticsController.getClosedEvents());
    populateCurrentUser(model);
    return "analytics/analyticsWorkspace";
  }

  /**
   * Validates criteria and generates a report or displays validation notices (Steps 5-9, A1, A3,
   * E4).
   *
   * @param criteria the report criteria entered by the officer
   * @param model the Spring MVC model
   * @return the target view name
   */
  @PostMapping("/criteria")
  public String enterCriteria(
      @ModelAttribute("criteriaInput") ReportCriteria criteria, Model model) {
    String officerId = currentUserService.userId(DemoRole.DMC_OFFICER);
    CriteriaCheck check = analyticsController.validateCriteria(criteria, officerId);

    if (!check.result().valid()) {
      if (!check.outOfScope().isEmpty() && check.adjusted() != null) {
        LOG.info(
            "Criteria contains out-of-scope districts for officer {}: {}",
            officerId,
            check.outOfScope());
        model.addAttribute("adjustedCriteriaSession", check.adjusted());
        model.addAttribute("outOfScopeDistricts", check.outOfScope());
        model.addAttribute("adjustedCriteria", check.adjusted());
      } else {
        LOG.warn(
            "Criteria validation failed for officer {}: {}", officerId, check.result().errors());
        model.addAttribute("validationErrors", check.result().errors());
      }
      model.addAttribute("criteria", criteria);
      model.addAttribute("templates", analyticsController.getTemplates(officerId));
      model.addAttribute("hazards", analyticsController.getClosedEvents());
      populateCurrentUser(model);
      return "analytics/analyticsWorkspace";
    }

    DisasterReport report = analyticsController.generateReport(criteria, officerId);
    if (report.getResults().matchedRecordCount() == 0) {
      LOG.info("Zero matching records for criteria; returning unsaved report notice (A1)");
      String suggestion =
          "No records matched your criteria. Consider widening the selected districts, "
              + "expanding the date range, or selecting all hazard types.";
      model.addAttribute("noRecordsSuggestion", suggestion);
      model.addAttribute("criteria", criteria);
      model.addAttribute("templates", analyticsController.getTemplates(officerId));
      model.addAttribute("hazards", analyticsController.getClosedEvents());
      populateCurrentUser(model);
      return "analytics/analyticsWorkspace";
    }

    model.addAttribute("report", report);
    model.addAttribute("provisionalNotice", report.getProvisionalSince());
    model.addAttribute("failedMetrics", report.getResults().failedMetrics());
    populateCurrentUser(model);
    return "analytics/showResults";
  }

  /**
   * Confirms adjusted criteria following an out-of-scope notice with re-validation (Step 6f,
   * FX-05).
   *
   * @param adjustedCriteria the adjusted criteria held in session
   * @param sessionStatus the session status to complete upon confirmation
   * @param model the Spring MVC model
   * @return the target view name
   */
  @PostMapping("/confirmAdjustedCriteria")
  public String confirmAdjustedCriteria(
      @ModelAttribute(value = "adjustedCriteriaSession", binding = false)
          ReportCriteria adjustedCriteria,
      SessionStatus sessionStatus,
      Model model) {
    String officerId = currentUserService.userId(DemoRole.DMC_OFFICER);
    CriteriaCheck check = analyticsController.validateCriteria(adjustedCriteria, officerId);

    if (!check.result().valid()) {
      sessionStatus.setComplete();
      model.addAttribute("validationErrors", check.result().errors());
      model.addAttribute("criteria", adjustedCriteria);
      model.addAttribute("templates", analyticsController.getTemplates(officerId));
      model.addAttribute("hazards", analyticsController.getClosedEvents());
      populateCurrentUser(model);
      return "analytics/analyticsWorkspace";
    }

    sessionStatus.setComplete();
    DisasterReport report = analyticsController.generateReport(adjustedCriteria, officerId);

    if (report.getResults().matchedRecordCount() == 0) {
      String suggestion =
          "No records matched your adjusted criteria. Try extending the date range or hazard type.";
      model.addAttribute("noRecordsSuggestion", suggestion);
      model.addAttribute("criteria", adjustedCriteria);
      model.addAttribute("templates", analyticsController.getTemplates(officerId));
      model.addAttribute("hazards", analyticsController.getClosedEvents());
      populateCurrentUser(model);
      return "analytics/analyticsWorkspace";
    }

    model.addAttribute("report", report);
    model.addAttribute("provisionalNotice", report.getProvisionalSince());
    model.addAttribute("failedMetrics", report.getResults().failedMetrics());
    populateCurrentUser(model);
    return "analytics/showResults";
  }

  /**
   * Saves findings narrative on an existing generated report (Step 10).
   *
   * @param reportId the report identifier
   * @param text the findings text
   * @param model the Spring MVC model
   * @return the report view with saved findings notice
   */
  @PostMapping("/findings")
  public String enterFindings(
      @RequestParam("reportId") String reportId, @RequestParam("text") String text, Model model) {
    DisasterReport report = analyticsController.saveFindings(reportId, text);
    model.addAttribute("report", report);
    model.addAttribute("provisionalNotice", report.getProvisionalSince());
    model.addAttribute("failedMetrics", report.getResults().failedMetrics());
    populateCurrentUser(model);
    return "analytics/showResultsWithFindings";
  }

  /**
   * Exports a report in the specified format for download (Step 12, CH-32).
   *
   * @param reportId the report identifier
   * @param format the export format (PDF, Excel, or CSV)
   * @return the HTTP entity containing file download bytes and headers
   */
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportReport(
      @RequestParam("reportId") String reportId, @RequestParam("format") ExportFormat format) {
    String officerId = currentUserService.userId(DemoRole.DMC_OFFICER);
    ExportFile file = analyticsController.exportReport(reportId, format, officerId);

    MediaType mediaType =
        switch (file.format()) {
          case PDF -> MediaType.APPLICATION_PDF;
          case EXCEL ->
              MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
          case CSV -> MediaType.parseMediaType("text/csv");
        };

    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
        .body(file.content());
  }

  /**
   * Shares a report with selected resource-owning organisations (Step 14, CH-32).
   *
   * @param reportId the report identifier
   * @param organizationIds the organisation identifiers to share with
   * @param model the Spring MVC model
   * @return the share outcome view
   */
  @PostMapping("/share")
  public String shareReport(
      @RequestParam("reportId") String reportId,
      @RequestParam("organizationIds") List<String> organizationIds,
      Model model) {
    String officerId = currentUserService.userId(DemoRole.DMC_OFFICER);
    ShareOutcome outcome = analyticsController.shareReport(reportId, organizationIds, officerId);

    model.addAttribute("outcome", outcome);
    model.addAttribute("reportId", reportId);
    populateCurrentUser(model);
    return "analytics/showShared";
  }

  /**
   * Handles export failure exceptions by rendering the export error notice (E2).
   *
   * @param ex the export failed exception
   * @param model the Spring MVC model
   * @return the view displaying the export error
   */
  @ExceptionHandler(ExportFailedException.class)
  public String showExportError(ExportFailedException ex, Model model) {
    LOG.error("Report export failed: {}", ex.getMessage(), ex);
    model.addAttribute("exportError", ex.getMessage());
    populateCurrentUser(model);
    return "analytics/exportShareChosen";
  }

  private void populateCurrentUser(Model model) {
    CurrentUserView user = currentUserService.currentUser(DemoRole.DMC_OFFICER);
    model.addAttribute("currentUser", user);
  }
}
