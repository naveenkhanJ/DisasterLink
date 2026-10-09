package lk.sliit.se3070.group058.disasterlink.analytics.web;

import java.util.List;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ReportNotSharedException;
import lk.sliit.se3070.group058.disasterlink.analytics.service.AnalyticsController;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportHeader;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ScopedReport;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Screen controller for resource-owning organisations viewing shared disaster reports.
 *
 * <p>Implements CH-10, CH-20, CH-31, CH-32 and CH-33. Provides access to shared report headers and
 * organisation-scoped views with restricted distribution lines.
 */
@Controller
@RequestMapping("/organisation")
public class DonorReportView {

  private static final Logger LOG = LoggerFactory.getLogger(DonorReportView.class);

  private final AnalyticsController analyticsController;
  private final CurrentUserService currentUserService;

  /**
   * Constructs the donor report boundary controller with required dependencies.
   *
   * @param analyticsController the analytics application coordinator
   * @param currentUserService the demo user context service
   */
  public DonorReportView(
      AnalyticsController analyticsController, CurrentUserService currentUserService) {
    this.analyticsController =
        Objects.requireNonNull(analyticsController, "analyticsController must not be null");
    this.currentUserService =
        Objects.requireNonNull(currentUserService, "currentUserService must not be null");
  }

  /**
   * Opens the list of reports shared with the organisation (Steps 16-17, WF4-S05).
   *
   * @param organizationId the organisation identifier (optional, defaults to current demo user)
   * @param model the Spring MVC model
   * @return the shared reports list view
   */
  @GetMapping({"", "/", "/reports", "/showSharedReports.html"})
  public String openSharedReports(
      @RequestParam(value = "organizationId", required = false) String organizationId,
      Model model) {
    String orgId = resolveOrganizationId(organizationId);
    LOG.info("Opening shared reports list for organisation {}", orgId);

    List<ReportHeader> reports = analyticsController.getSharedReports(orgId);
    model.addAttribute("reports", reports);
    model.addAttribute("organizationId", orgId);
    populateCurrentUser(model);
    return "organisation/showSharedReports";
  }

  /**
   * Opens a specific shared report with organisation-scoped data (Steps 18-19, WF4-S06).
   *
   * @param reportId the report identifier
   * @param organizationId the organisation identifier (optional, defaults to current demo user)
   * @param model the Spring MVC model
   * @return the scoped report view
   */
  @GetMapping({"/report", "/showScopedReport.html"})
  public String openSharedReport(
      @RequestParam(value = "reportId", required = false) String reportId,
      @RequestParam(value = "organizationId", required = false) String organizationId,
      Model model) {
    String orgId = resolveOrganizationId(organizationId);
    String targetReportId = reportId;
    if (targetReportId == null || targetReportId.isBlank()) {
      List<ReportHeader> reports = analyticsController.getSharedReports(orgId);
      targetReportId = reports.isEmpty() ? null : reports.get(0).reportId();
    }
    LOG.info("Opening scoped report {} for organisation {}", targetReportId, orgId);

    if (targetReportId != null) {
      try {
        ScopedReport scopedReport = analyticsController.getScopedView(targetReportId, orgId);
        model.addAttribute("scopedReport", scopedReport);
      } catch (ReportNotSharedException e) {
        throw e;
      } catch (Exception e) {
        LOG.warn("Could not load scoped report: {}", e.getMessage());
      }
    }
    model.addAttribute("organizationId", orgId);
    populateCurrentUser(model);
    return "organisation/showScopedReport";
  }

  /**
   * Handles refusals when a report has not been shared with the requesting organisation (E6,
   * WF4-S05a).
   *
   * @param ex the report not shared exception
   * @param model the Spring MVC model
   * @return the not shared notice view
   */
  @ExceptionHandler(ReportNotSharedException.class)
  public String showNotShared(ReportNotSharedException ex, Model model) {
    LOG.warn("Refused access to unshared report: {}", ex.getMessage());
    model.addAttribute("notSharedError", ex.getMessage());
    populateCurrentUser(model);
    return "organisation/showNotShared";
  }

  private String resolveOrganizationId(String organizationId) {
    if (organizationId != null && !organizationId.isBlank()) {
      return organizationId;
    }
    return currentUserService.userId(DemoRole.ORGANIZATION);
  }

  private void populateCurrentUser(Model model) {
    CurrentUserView user = currentUserService.currentUser(DemoRole.ORGANIZATION);
    model.addAttribute("currentUser", user);
  }
}
