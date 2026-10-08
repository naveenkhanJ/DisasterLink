package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.VerificationOutcome;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.InvalidHazardDetailsException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReasonRequiredException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportAlreadyDecidedException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportVerificationController;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.NewHazardForm;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper.VerificationConsoleMapper;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper.VerificationConsoleMapper.PendingReportRow;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper.VerificationConsoleMapper.ReportPage;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerMapping;

/**
 * The verification console of the DMC duty officer, the «boundary» {@code VerificationConsoleUI} of
 * use case UC2: a server-rendered Thymeleaf page for every state of wireframes WF2C-S01 to S02h (no
 * JavaScript of our own; every state is a page). Its handlers are named after the model
 * dictionary's inputs ({@code openQueue}, {@code selectReport}, {@code verifyReport}, {@code
 * verifyAsNewHazard}, {@code rejectReport}, {@code dismissEscalation}); the extra form states (the
 * new-hazard form, the rejection form and its confirmation) are implementation-only screen
 * components (C2). The {@code show…} displays are the templates in {@code templates/hazardreport}
 * (standards section 2, FE-28): {@code showPendingReports}, {@code showReportDetails} (which also
 * shows the new-hazard state, {@code showValidationErrors} and {@code showAlreadyDecided}), {@code
 * rejectionReason} (with {@code showReasonRequired}), {@code showDecisionRecorded} and {@code
 * showEscalationPrompt}.
 *
 * <p><b>Transactions.</b> The three decision calls are made without any surrounding transaction (no
 * {@code @Transactional} here), so the control's own transaction is the outermost one: only then is
 * the citizen told after a real commit and a lost race surfaces as {@link
 * ReportAlreadyDecidedException} (UC2-3 review G1). Everything the pages show is mapped to view
 * records inside a read-only transaction; after a decision the report is read again there, because
 * the entity the control returns, and the one an exception carries, is detached.
 *
 * <p><b>Where each exception is handled</b> (standards section 4, "Handlers (b)": the exceptions
 * are handled here, not thrown upwards): {@link ReasonRequiredException} by {@link
 * #showReasonRequired}, {@link InvalidHazardDetailsException} by {@link #showValidationErrors},
 * {@link ReportAlreadyDecidedException} by {@link #showAlreadyDecided}, an unknown identifier
 * ({@link NoSuchElementException}) by {@link #notFound} as a 404, and any other failure of a page
 * or a decision by {@link #showServiceUnavailable}. That last handler exists for the common error
 * banner (MD section 0.10, C1): a technical failure is not a refusal and has no {@code show…}
 * display of its own; the page the officer was on is shown again with the banner "The service is
 * unavailable and nothing was changed" and the status 500 (SC2 E5, step 26b), and the failure is
 * logged with its exception. It catches {@link RuntimeException} because the control's decisions
 * run in transactions that roll back on any unchecked exception; the checked binding and parameter
 * errors of the framework are not affected and keep their 400.
 *
 * <p>Implements CH-19, CH-25, CH-26 and CH-27. Implementation-only web adapter (standards section
 * 3); the handlers of the three decisions are tested not to run in a transaction.
 */
@Controller
@RequestMapping("/hazardreport")
public class VerificationConsoleUI {

  private static final Logger LOG = LoggerFactory.getLogger(VerificationConsoleUI.class);

  private static final String QUEUE_VIEW = "hazardreport/showPendingReports";
  private static final String DETAILS_VIEW = "hazardreport/showReportDetails";
  private static final String REJECTION_VIEW = "hazardreport/rejectionReason";
  private static final String DECISION_VIEW = "hazardreport/showDecisionRecorded";
  private static final String ESCALATION_VIEW = "hazardreport/showEscalationPrompt";

  private static final String QUEUE_SUBTITLE =
      "Reports with the status Pending verification, oldest first";

  /** The HTTP header that makes the browser load the page again after the given seconds. */
  private static final String REFRESH_HEADER = "Refresh";

  /**
   * How often the queue page reloads itself, in seconds (standards section 12.3, P213 and P217;
   * WF2C-S01 "Behaviour"): new reports appear without a manual reload. Only the queue sets it; the
   * report page (WF2C-S02) and the rejection form never reload by themselves, so a report under
   * review is not disturbed.
   */
  private static final String QUEUE_REFRESH_SECONDS = "15";

  private final ReportVerificationController control;

  private final CurrentUserService currentUserService;

  private final TransactionTemplate readOnlyTransaction;

  private final VerificationConsoleMapper mapper;

  /**
   * Creates the console.
   *
   * @param control the officer's control of use case UC2
   * @param currentUserService supplies the demo duty officer
   * @param transactionManager runs the read-only transactions in which entities are mapped
   * @param mapper maps the entities to the view records of the templates
   */
  public VerificationConsoleUI(
      ReportVerificationController control,
      CurrentUserService currentUserService,
      PlatformTransactionManager transactionManager,
      VerificationConsoleMapper mapper) {
    this.control = Objects.requireNonNull(control, "control");
    this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
    this.readOnlyTransaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction.setReadOnly(true);
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /**
   * Shows the queue of the reports that wait for a decision (SC2 steps 21 and 22; WF2C-S01).
   *
   * @param model the page model
   * @param response the response, on which the page asks the browser to reload it every {@value
   *     #QUEUE_REFRESH_SECONDS} seconds
   * @return the {@code showPendingReports} page
   */
  @GetMapping
  public String openQueue(Model model, HttpServletResponse response) {
    response.setHeader(REFRESH_HEADER, QUEUE_REFRESH_SECONDS);
    List<PendingReportRow> rows = read(() -> mapper.toRows(control.getPendingReports()));
    model.addAttribute("rows", rows);
    model.addAttribute("count", mapper.countLabel(rows.size()));
    return page(model, QUEUE_VIEW, QUEUE_SUBTITLE);
  }

  /**
   * Shows one report with its nearby reports and candidate hazards (SC2 steps 23 to 25; WF2C-S02).
   * A report that is no longer pending is shown in its already-decided state.
   *
   * @param reportId the identifier of the report
   * @param model the page model
   * @return the {@code showReportDetails} page
   */
  @GetMapping("/{reportId}")
  public String selectReport(@PathVariable String reportId, Model model) {
    return render(model, reportId, DETAILS_VIEW);
  }

  /**
   * Verifies the report against the chosen candidate hazard (SC2 steps 25 to 28; WF2C-S02e). When
   * the hazard has an active warning the escalation prompt is shown (A7, WF2C-S02g). Not
   * transactional: the control's transaction is the outermost one.
   *
   * @param reportId the identifier of the report
   * @param hazardId the identifier of the chosen hazard
   * @param model the page model
   * @return the {@code showDecisionRecorded} or {@code showEscalationPrompt} page
   */
  @PostMapping("/{reportId}/verify")
  public String verifyReport(
      @PathVariable String reportId, @RequestParam String hazardId, Model model) {
    VerificationOutcome outcome =
        control.verifyReport(
            reportId, hazardId, currentUserService.userId(DemoRole.DMC_DUTY_OFFICER));
    return decisionPage(model, reportId, outcome);
  }

  /**
   * Shows the new-hazard form (SC2 A6, step 25m; WF2C-S02c): the details page with the form instead
   * of the decision buttons.
   *
   * @param reportId the identifier of the report
   * @param model the page model
   * @return the {@code showReportDetails} page in its new-hazard state
   */
  @GetMapping("/{reportId}/new-hazard")
  public String newHazardForm(@PathVariable String reportId, Model model) {
    return render(model, reportId, DETAILS_VIEW, VerificationConsoleMapper.STATE_NEW_HAZARD, null);
  }

  /**
   * Verifies the report as a new hazard with the details the officer entered (SC2 A6, steps 25n to
   * 25s; WF2C-S02e). Not transactional.
   *
   * @param reportId the identifier of the report
   * @param form the posted new-hazard form
   * @param model the page model
   * @return the {@code showDecisionRecorded} or {@code showEscalationPrompt} page
   */
  @PostMapping("/{reportId}/new-hazard")
  public String verifyAsNewHazard(
      @PathVariable String reportId, @ModelAttribute NewHazardForm form, Model model) {
    VerificationOutcome outcome =
        control.verifyAsNewHazard(
            reportId, mapper.toDetails(form), currentUserService.userId(DemoRole.DMC_DUTY_OFFICER));
    return decisionPage(model, reportId, outcome);
  }

  /**
   * Shows the rejection-reason form (SC2 A4, steps 25a and 25b; WF2C-S02a).
   *
   * @param reportId the identifier of the report
   * @param reason a reason to put back in the field (Cancel of the confirmation returns here), or
   *     {@code null}
   * @param model the page model
   * @return the {@code rejectionReason} page
   */
  @GetMapping("/{reportId}/reject")
  public String rejectionForm(
      @PathVariable String reportId, @RequestParam(required = false) String reason, Model model) {
    return render(model, reportId, REJECTION_VIEW, "REASON", reason);
  }

  /**
   * Asks the officer to confirm the rejection (SC2 A4, steps 25c and 25d; the modal of WF2C-S02a):
   * the form page with the reason repeated in a server-rendered dialog. A blank reason is handed to
   * {@code rejectReport}, so the control refuses it with its own precedence (a decided report is
   * refused before an empty reason) and the form shows the error (E7, WF2C-S02b); nothing is
   * decided here.
   *
   * @param reportId the identifier of the report
   * @param reason the reason as typed
   * @param model the page model
   * @return the {@code rejectionReason} page with the confirmation
   */
  @PostMapping("/{reportId}/reject")
  public String confirmRejection(
      @PathVariable String reportId, @RequestParam(required = false) String reason, Model model) {
    if (reason == null || reason.isBlank()) {
      control.rejectReport(reportId, reason, currentUserService.userId(DemoRole.DMC_DUTY_OFFICER));
    }
    return render(model, reportId, REJECTION_VIEW, "CONFIRM", reason);
  }

  /**
   * Rejects the report with the confirmed reason (SC2 A4, steps 25e to 25g; WF2C-S02f). Not
   * transactional.
   *
   * @param reportId the identifier of the report
   * @param reason the reason as typed
   * @param model the page model
   * @return the {@code showDecisionRecorded} page
   */
  @PostMapping("/{reportId}/reject/confirm")
  public String rejectReport(
      @PathVariable String reportId, @RequestParam(required = false) String reason, Model model) {
    control.rejectReport(reportId, reason, currentUserService.userId(DemoRole.DMC_DUTY_OFFICER));
    return decisionPage(model, reportId, null);
  }

  /**
   * Dismisses the escalation prompt (SC2 A7, step 29d): the verified page without the dialog.
   * Nothing is changed.
   *
   * @param reportId the identifier of the report
   * @param model the page model
   * @return the {@code showDecisionRecorded} page
   */
  @PostMapping("/{reportId}/dismiss-escalation")
  public String dismissEscalation(@PathVariable String reportId, Model model) {
    return decisionPage(model, reportId, null);
  }

  /**
   * Shows the rejection form with "A rejection reason is required" under the empty field (SC2 E7,
   * step 25s; WF2C-S02b). The report stays pending.
   *
   * @param exception the refusal
   * @param request the request, for the report and the reason typed
   * @param model the page model
   * @return the {@code rejectionReason} page with the error
   */
  @ExceptionHandler(ReasonRequiredException.class)
  public String showReasonRequired(
      ReasonRequiredException exception, HttpServletRequest request, Model model) {
    model.addAttribute("reasonError", exception.getMessage());
    return render(
        model, reportIdOf(request), REJECTION_VIEW, "REASON", request.getParameter("reason"));
  }

  /**
   * Shows the new-hazard form with each error under its field and the entered values kept (SC2 E9,
   * step 25u; WF2C-S02d).
   *
   * @param exception the refusal, with the errors
   * @param request the request, for the report and the values typed
   * @param model the page model
   * @return the {@code showReportDetails} page in its new-hazard state
   */
  @ExceptionHandler(InvalidHazardDetailsException.class)
  public String showValidationErrors(
      InvalidHazardDetailsException exception, HttpServletRequest request, Model model) {
    model.addAttribute("fieldErrors", mapper.fieldErrors(exception.getErrors()));
    model.addAttribute("form", mapper.toFormView(formOf(request)));
    return render(
        model,
        reportIdOf(request),
        DETAILS_VIEW,
        VerificationConsoleMapper.STATE_NEW_HAZARD,
        null,
        false);
  }

  /**
   * Shows the report in its already-decided state: no decision buttons, "This report has already
   * been decided", the recorded status and the decision time (SC2 E10, steps 26d to 26f;
   * WF2C-S02h).
   *
   * @param exception the refusal, with the report as it stands
   * @param model the page model
   * @return the {@code showReportDetails} page
   */
  @ExceptionHandler(ReportAlreadyDecidedException.class)
  public String showAlreadyDecided(ReportAlreadyDecidedException exception, Model model) {
    return render(model, exception.getReport().getReportId(), DETAILS_VIEW);
  }

  /**
   * Answers an unknown report, hazard or officer with a 404.
   *
   * @param exception the lookup failure
   * @param response where the status is set
   * @throws IOException if the error cannot be sent
   */
  @ExceptionHandler(NoSuchElementException.class)
  public void notFound(NoSuchElementException exception, HttpServletResponse response)
      throws IOException {
    String summary = exception.toString();
    LOG.debug("Not found: {}", summary);
    response.sendError(HttpStatus.NOT_FOUND.value());
  }

  /**
   * Shows the report's page again with the common error banner and the status 500: the service is
   * unavailable and nothing was changed (MD section 0.10, C1; SC2 E5, step 26b). The failure is
   * logged with its exception.
   *
   * @param exception the failure
   * @param request the request, for the report
   * @param model the page model
   * @return the {@code showReportDetails} page with the banner
   */
  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String showServiceUnavailable(
      RuntimeException exception, HttpServletRequest request, Model model) {
    String reportId = reportIdOf(request);
    LOG.error("The console failed on report {}", reportId, exception);
    model.addAttribute("serviceUnavailable", true);
    return render(model, reportId, DETAILS_VIEW);
  }

  private String render(Model model, String reportId, String view) {
    return render(model, reportId, view, VerificationConsoleMapper.STATE_DECIDE, null);
  }

  private String render(Model model, String reportId, String view, String state, String reason) {
    return render(model, reportId, view, state, reason, true);
  }

  /**
   * Builds the model of a report's page and chooses its template. A report that is no longer
   * pending is shown in its already-decided state, whatever page was asked for, except on the
   * decision pages, which are made for it.
   */
  private String render(
      Model model, String reportId, String view, String state, String reason, boolean freshForm) {
    ReportPage page =
        read(
            () ->
                mapper.toPage(
                    control.getReportDetails(reportId),
                    control.findNearbyReports(reportId),
                    control.findCandidateHazards(reportId)));
    model.addAttribute("page", page);
    boolean decisionPage = DECISION_VIEW.equals(view) || ESCALATION_VIEW.equals(view);
    String shown = state;
    String template = view;
    if (!page.report().pending() && !decisionPage) {
      shown = VerificationConsoleMapper.STATE_DECIDED;
      template = DETAILS_VIEW;
    }
    model.addAttribute("state", shown);
    model.addAttribute("reason", reason == null ? "" : reason);
    model.addAttribute("confirming", "CONFIRM".equals(state));
    if (VerificationConsoleMapper.STATE_NEW_HAZARD.equals(shown)) {
      if (freshForm) {
        model.addAttribute("form", mapper.initialForm(page.report().districtCode()));
      }
      model.addAttribute("districtOptions", mapper.districtOptions());
      model.addAttribute("severityOptions", mapper.severityOptions());
    }
    return page(model, template, mapper.subtitle(page.report()));
  }

  private String decisionPage(Model model, String reportId, VerificationOutcome outcome) {
    if (outcome != null && outcome.activeWarning() != null) {
      model.addAttribute(
          "prompt",
          read(() -> mapper.toPrompt(control.getReportDetails(reportId), outcome.activeWarning())));
      return render(model, reportId, ESCALATION_VIEW);
    }
    return render(model, reportId, DECISION_VIEW);
  }

  private String page(Model model, String view, String subtitle) {
    model.addAttribute("currentUser", currentUserService.currentUser(DemoRole.DMC_DUTY_OFFICER));
    model.addAttribute("subtitle", subtitle);
    return view;
  }

  private <T> T read(Supplier<T> work) {
    return readOnlyTransaction.execute(status -> work.get());
  }

  private static String reportIdOf(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Map<String, String> variables =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    return variables.get("reportId");
  }

  /**
   * Reads the form from the request, because an exception handler cannot bind a model attribute.
   * The request was bound successfully before the control refused it, so its values are valid
   * constants; only a missing or blank severity is {@code null}.
   */
  private static NewHazardForm formOf(HttpServletRequest request) {
    String[] posted = request.getParameterValues("districts");
    List<District> districts =
        posted == null ? List.of() : Arrays.stream(posted).map(District::valueOf).toList();
    String severity = request.getParameter("severity");
    return new NewHazardForm(
        severity == null || severity.isBlank() ? null : HazardSeverity.valueOf(severity),
        districts,
        request.getParameter("locationName"),
        request.getParameter("description"));
  }
}
