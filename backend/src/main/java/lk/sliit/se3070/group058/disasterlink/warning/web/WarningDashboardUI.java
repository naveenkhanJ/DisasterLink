package lk.sliit.se3070.group058.disasterlink.warning.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardDetailsView;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.warning.service.ActiveWarningExistsException;
import lk.sliit.se3070.group058.disasterlink.warning.service.InvalidHazardUpdateException;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.DispatchView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.HazardUpdateForm;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.HazardView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.PreviewedWarning;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.WarningForm;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.WarningView;
import lk.sliit.se3070.group058.disasterlink.warning.web.mapper.WarningDashboardMapper;
import lk.sliit.se3070.group058.disasterlink.warning.web.mapper.WarningFormMapper;
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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.HandlerMapping;

/**
 * The warning dashboard of the DMC officer, the «boundary» {@code WarningDashboardUI} of use case
 * UC1 (wireframes WF1-S01 to S05 and their states; sd Issue Hazard Warning, sd Modify Warning). Its
 * handlers are named after the model dictionary's inputs: {@code openDashboard}, {@code
 * selectHazard}, {@code editHazard}, {@code enterWarningDetails}, {@code requestPreview}, {@code
 * confirmBroadcast}, {@code editWarning} and {@code resendOnChannel}. Each returns the view of the
 * dictionary's {@code show…} display, which is the template of that name in {@code
 * templates/warning} (standards section 2, FE-28): {@code showHazards}, {@code showHazardDetails}
 * (with the fragments {@code showHazardIncomplete} and {@code showActiveWarning}), {@code
 * showWarningForm} (with {@code showValidationErrors}), {@code showPreview} (with {@code
 * showActiveWarningExists}) and {@code showActiveWarning} (with {@code showDeliveryFailure}).
 *
 * <p><b>Two inputs, one submission</b> (standards section 2, R4G-24): the form posts to {@link
 * #enterWarningDetails}, which stores the details in the session under {@code previewedDetails}
 * (not the form's name {@code warningForm}, R4G-09) and redirects; the browser then gets {@link
 * #requestPreview}. {@link #confirmBroadcast} re-calls {@code validateWarning} with the stored
 * details before {@code issueWarning} (P62 FX-05).
 *
 * <p><b>Transactions.</b> The writes ({@code updateHazard}, {@code issueWarning}, {@code
 * resendOnChannel}) run in the control's own transaction, with none around them here. Everything
 * the pages show is mapped to view records inside a read-only transaction, because open-in-view is
 * off and the hazard's and the warning's collections are lazy.
 *
 * <p><b>Where each exception is handled</b> (standards section 4, "Handlers (b)"): {@link
 * InvalidHazardUpdateException} by {@link #showValidationErrors} (SC1 E5), {@link
 * ActiveWarningExistsException} by {@link #showActiveWarningExists} (E6), an unknown identifier by
 * {@link #notFound} as a 404, and any other failure by {@link #showServiceUnavailable}, which logs
 * it and shows the common error banner, "The service is unavailable and nothing was changed" (MD
 * section 0.10, C1; E2).
 *
 * <p>Implements CH-10, CH-22, CH-23 and CH-24.
 */
@Controller
@RequestMapping("/warning")
@SessionAttributes(WarningDashboardUI.PREVIEWED_DETAILS)
public class WarningDashboardUI {

  /** The session name of the entered details; never the form's model name (R4G-09). */
  static final String PREVIEWED_DETAILS = "previewedDetails";

  private static final Logger LOG = LoggerFactory.getLogger(WarningDashboardUI.class);

  private static final String HAZARDS_VIEW = "warning/showHazards";
  private static final String DETAILS_VIEW = "warning/showHazardDetails";
  private static final String FORM_VIEW = "warning/showWarningForm";
  private static final String PREVIEW_VIEW = "warning/showPreview";
  private static final String ACTIVE_WARNING_VIEW = "warning/showActiveWarning";

  private static final String HAZARDS_SUBTITLE = "Hazards that can be assessed for a warning";
  private static final String DETAILS_SUBTITLE = "Hazard details";
  private static final String FORM_SUBTITLE = "Prepare the warning for the selected hazard";
  private static final String PREVIEW_SUBTITLE =
      "Check what will be sent before you confirm the broadcast";

  private static final String HAZARD_ID = "hazardId";
  private static final String RETURN_TO_FORM = "form";
  private static final String REDIRECT_TO_HAZARD = "redirect:/warning/hazards/{hazardId}";

  private final WarningController control;

  private final CurrentUserService currentUserService;

  private final TransactionTemplate readOnlyTransaction;

  private final Clock clock;

  /**
   * Creates the dashboard.
   *
   * @param control the control of use case UC1
   * @param currentUserService supplies the demo DMC officer
   * @param transactionManager runs the read-only transactions in which entities are mapped
   * @param clock judges whether a shown warning is still active
   */
  public WarningDashboardUI(
      WarningController control,
      CurrentUserService currentUserService,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.control = Objects.requireNonNull(control, "control");
    this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
    this.readOnlyTransaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction.setReadOnly(true);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Shows the assessable hazards (SC1 steps 1–3; WF1-S01; sd message 1).
   *
   * @param model the page model
   * @return the {@code showHazards} page
   */
  @GetMapping
  public String openDashboard(Model model) {
    model.addAttribute("hazards", hazardList());
    return page(model, HAZARDS_VIEW, HAZARDS_SUBTITLE);
  }

  /**
   * Shows a hazard (SC1 steps 4–5, 18–19, A1 5a, A5 5d; sd messages 2 and 8). A complete hazard
   * without an active warning opens the Create Hazard Warning form (2.3); one with an active
   * warning shows it with its dispatch status and Escalate (2.4–2.5, WF1-S02c); an incomplete one
   * shows what is missing (2.6, WF1-S02a).
   *
   * @param hazardId the identifier of the hazard
   * @param model the page model
   * @return the {@code showWarningForm} or {@code showHazardDetails} page
   */
  @GetMapping("/hazards/{hazardId}")
  public String selectHazard(@PathVariable String hazardId, Model model) {
    return hazardPage(hazardId, model);
  }

  /**
   * Applies the officer's hazard update (SC1 A1 5b–5c, E4 12f; sd message 3). When it is valid the
   * form opens for the updated hazard (3.2); an invalid update is refused and shown by {@link
   * #showValidationErrors} (3.3, E5).
   *
   * @param hazardId the identifier of the hazard
   * @param form the posted update panel
   * @param model the page model
   * @return the {@code showWarningForm} page, or the details page while the hazard is incomplete
   */
  @PostMapping("/hazards/{hazardId}/update")
  public String editHazard(
      @PathVariable String hazardId, @ModelAttribute HazardUpdateForm form, Model model) {
    control.updateHazard(hazardId, WarningFormMapper.toUpdate(form));
    return hazardPage(hazardId, model);
  }

  /**
   * Stores the details the officer entered (SC1 steps 6–10; sd message 4) and redirects to the
   * preview, the second input of the same submission. A form without a level is malformed (400,
   * standards section 12.2).
   *
   * @param hazardId the identifier of the hazard
   * @param form the posted form
   * @param model the page model, whose {@code previewedDetails} is kept in the session
   * @return a redirect to {@link #requestPreview}
   */
  @PostMapping("/hazards/{hazardId}/details")
  public String enterWarningDetails(
      @PathVariable String hazardId,
      @Valid @ModelAttribute("warningForm") WarningForm form,
      Model model) {
    model.addAttribute(PREVIEWED_DETAILS, new PreviewedWarning(hazardId, form));
    return REDIRECT_TO_HAZARD + "/preview";
  }

  /**
   * Validates the stored details and shows the preview (SC1 step 11; WF1-S04; sd messages 5–5.3),
   * or the form with the errors beside the fields (A2, E3; 5.4).
   *
   * @param hazardId the identifier of the hazard
   * @param model the page model, with the stored details
   * @return the {@code showPreview} or {@code showWarningForm} page
   */
  @GetMapping("/hazards/{hazardId}/preview")
  public String requestPreview(@PathVariable String hazardId, Model model) {
    WarningForm form = storedForm(model.getAttribute(PREVIEWED_DETAILS), hazardId);
    if (form == null) {
      return REDIRECT_TO_HAZARD;
    }
    WarningDetails details = WarningFormMapper.toDetails(form);
    ValidationResult result = control.validateWarning(hazardId, details);
    if (!result.valid()) {
      return formPage(hazardId, model, form, result.errors());
    }
    return previewPage(hazardId, model, details);
  }

  /**
   * Reopens the form with the details already entered (SC1 A4 12a–12b, E4 12f; sd Modify Warning
   * message 1).
   *
   * @param hazardId the identifier of the hazard
   * @param model the page model, with the stored details
   * @return the {@code showWarningForm} page
   */
  @GetMapping("/hazards/{hazardId}/edit")
  public String editWarning(@PathVariable String hazardId, Model model) {
    return hazardPage(hazardId, model);
  }

  /**
   * Issues the previewed warning (SC1 steps 12–17; sd message 6). The details are validated again
   * first (6.1, E3 13g); then the warning is issued and broadcast and its dispatch status shown
   * (6.3, WF1-S05), with a delivery-failure alert per failed channel (6.4, E1, WF1-S05a).
   *
   * @param hazardId the identifier of the hazard
   * @param model the page model, with the stored details
   * @param status cleared once the warning is issued
   * @return the {@code showActiveWarning} page, or the form with the errors
   */
  @PostMapping("/hazards/{hazardId}/confirm")
  public String confirmBroadcast(@PathVariable String hazardId, Model model, SessionStatus status) {
    WarningForm form = storedForm(model.getAttribute(PREVIEWED_DETAILS), hazardId);
    if (form == null) {
      return REDIRECT_TO_HAZARD;
    }
    WarningDetails details = WarningFormMapper.toDetails(form);
    ValidationResult result = control.validateWarning(hazardId, details);
    if (!result.valid()) {
      return formPage(hazardId, model, form, result.errors());
    }
    BroadcastSummary summary =
        control
            .issueWarning(hazardId, details, currentUserService.userId(DemoRole.DMC_OFFICER))
            .summary();
    status.setComplete();
    return activeWarningPage(hazardId, model, summary, null);
  }

  /**
   * Sends the warning again on the chosen channel to the recipients whose notification failed (SC1
   * E1 16d–16e; WF1-S05b; sd message 7).
   *
   * @param hazardId the identifier of the warning's hazard
   * @param warningId the identifier of the warning
   * @param channel the channel the officer chose
   * @param model the page model
   * @return the {@code showActiveWarning} page with the updated dispatch status
   */
  @PostMapping("/hazards/{hazardId}/warnings/{warningId}/resend")
  public String resendOnChannel(
      @PathVariable String hazardId,
      @PathVariable String warningId,
      @RequestParam NotificationChannel channel,
      Model model) {
    BroadcastSummary summary = control.resendOnChannel(warningId, channel);
    String notice =
        summary.failedRecipientIds().isEmpty()
            ? "Resend on " + WarningDashboardMapper.channelLabel(channel) + " complete"
            : null;
    return activeWarningPage(hazardId, model, summary, notice);
  }

  /**
   * Shows a refused hazard update beside the update panel (SC1 E5 5f / 12i; WF1-S02b): the hazard
   * is unchanged and the panel keeps the values entered.
   *
   * @param exception the refusal, with the errors
   * @param request the request, for the hazard and the values entered
   * @param model the page model
   * @return the page the update came from, with the errors
   */
  @ExceptionHandler(InvalidHazardUpdateException.class)
  public String showValidationErrors(
      InvalidHazardUpdateException exception, HttpServletRequest request, Model model) {
    model.addAttribute("updateErrors", exception.getErrors());
    model.addAttribute("updateForm", WarningFormMapper.updateFormOf(request));
    model.addAttribute("updateOpen", true);
    String hazardId = pathVariable(request, HAZARD_ID);
    if (RETURN_TO_FORM.equals(request.getParameter("returnTo"))) {
      return formPage(hazardId, model, storedForm(request.getSession(), hazardId), List.of());
    }
    return detailsPage(hazardId, model, read(() -> control.getHazardDetails(hazardId)));
  }

  /**
   * Shows that the hazard received an active warning in the meantime (SC1 E6 13d–13f; WF1-S04a):
   * the preview again, with a dialog naming the existing warning and offering Escalate. Nothing was
   * recorded or broadcast.
   *
   * @param exception the refusal, with the existing warning
   * @param request the request, for the hazard and the stored details
   * @param model the page model
   * @return the {@code showPreview} page with the dialog
   */
  @ExceptionHandler(ActiveWarningExistsException.class)
  public String showActiveWarningExists(
      ActiveWarningExistsException exception, HttpServletRequest request, Model model) {
    String hazardId = pathVariable(request, HAZARD_ID);
    String existingId = exception.getWarning().getWarningId();
    LOG.info("Hazard {} already has the active warning {}", hazardId, existingId);
    model.addAttribute("existingWarning", activeWarningOf(hazardId));
    WarningForm form = storedForm(request.getSession(), hazardId);
    return previewPage(hazardId, model, WarningFormMapper.toDetails(form));
  }

  /**
   * Answers an unknown hazard or warning with a 404.
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
   * Shows the common error banner, "The service is unavailable and nothing was changed" (MD section
   * 0.10, C1; SC1 E2 13a–13c), with the status 500: on the preview, which keeps the confirmed
   * details, when the confirmation failed, otherwise on the dashboard. The failure is logged with
   * its exception.
   *
   * @param exception the failure
   * @param request the request, for the hazard and the stored details
   * @param model the page model
   * @return the {@code showPreview} or {@code showHazards} page with the banner
   */
  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String showServiceUnavailable(
      RuntimeException exception, HttpServletRequest request, Model model) {
    String path = request.getRequestURI();
    LOG.error("The warning dashboard failed on {}", path, exception);
    model.addAttribute("serviceUnavailable", true);
    String hazardId = pathVariable(request, HAZARD_ID);
    WarningForm form = hazardId == null ? null : storedForm(request.getSession(), hazardId);
    if (form != null && path.endsWith("/confirm")) {
      return previewPage(hazardId, model, WarningFormMapper.toDetails(form));
    }
    return openDashboard(model);
  }

  /**
   * Chooses the page of a hazard as sd message 2 does: the form when the hazard is complete and has
   * no active warning, otherwise the details. The form keeps the details stored for this hazard.
   */
  private String hazardPage(String hazardId, Model model) {
    HazardDetailsView view = read(() -> control.getHazardDetails(hazardId));
    if (view.activeWarning() != null || !view.missingInformation().isEmpty()) {
      return detailsPage(hazardId, model, view);
    }
    WarningForm form = storedForm(model.getAttribute(PREVIEWED_DETAILS), hazardId);
    return formPage(hazardId, model, form, List.of());
  }

  /** WF1-S02 and its states: details, update panel, missing information, active warning. */
  private String detailsPage(String hazardId, Model model, HazardDetailsView view) {
    HazardView hazard = read(() -> WarningDashboardMapper.toHazardView(view.hazard()));
    model.addAttribute("hazards", hazardList());
    model.addAttribute("hazard", hazard);
    model.addAttribute("missing", view.missingInformation());
    if (view.activeWarning() != null) {
      String warningId = view.activeWarning().getWarningId();
      model.addAttribute("activeWarning", activeWarningOf(hazardId));
      model.addAttribute(
          "dispatch", WarningDashboardMapper.toDispatch(control.getBroadcastSummary(warningId)));
    }
    if (!model.containsAttribute("updateForm")) {
      model.addAttribute("updateForm", WarningFormMapper.updateFormOf(hazard));
    }
    addUpdateOptions(model);
    return page(model, DETAILS_VIEW, DETAILS_SUBTITLE);
  }

  /** WF1-S03 and its states: the hazard card, the form, and the errors beside the fields. */
  private String formPage(String hazardId, Model model, WarningForm form, List<String> errors) {
    HazardView hazard =
        read(
            () -> WarningDashboardMapper.toHazardView(control.getHazardDetails(hazardId).hazard()));
    model.addAttribute("hazard", hazard);
    WarningForm shown =
        WarningFormMapper.normalised(form == null ? WarningFormMapper.emptyForm() : form);
    model.addAttribute("warningForm", shown);
    model.addAttribute("selectedDistricts", WarningFormMapper.names(shown.targetDistricts()));
    model.addAttribute("selectedBasins", WarningFormMapper.names(shown.targetRiverBasins()));
    model.addAttribute("errors", errors);
    model.addAttribute("fieldErrors", WarningFormMapper.fieldErrors(errors));
    model.addAttribute("levelOptions", WarningFormMapper.levelOptions());
    model.addAttribute("basinOptions", WarningFormMapper.basinOptions());
    model.addAttribute("channelOptions", WarningFormMapper.channelOptions());
    if (!model.containsAttribute("updateForm")) {
      model.addAttribute("updateForm", WarningFormMapper.updateFormOf(hazard));
    }
    addUpdateOptions(model);
    return page(model, FORM_VIEW, FORM_SUBTITLE);
  }

  /** WF1-S04: the preview of valid details. */
  private String previewPage(String hazardId, Model model, WarningDetails details) {
    model.addAttribute(HAZARD_ID, hazardId);
    model.addAttribute(
        "preview",
        WarningDashboardMapper.toPreview(
            control.previewWarning(hazardId, details), details.targetDistricts()));
    return page(model, PREVIEW_VIEW, PREVIEW_SUBTITLE);
  }

  /** WF1-S05: the hazard's active warning with the dispatch status of its current round. */
  private String activeWarningPage(
      String hazardId, Model model, BroadcastSummary summary, String notice) {
    WarningView warning = activeWarningOf(hazardId);
    DispatchView dispatch = WarningDashboardMapper.toDispatch(summary);
    model.addAttribute(HAZARD_ID, hazardId);
    model.addAttribute("warning", warning);
    model.addAttribute("dispatch", dispatch);
    model.addAttribute("notice", notice);
    model.addAttribute("channelOptions", WarningFormMapper.channelOptions());
    return page(model, ACTIVE_WARNING_VIEW, warning.hazardLabel());
  }

  private WarningView activeWarningOf(String hazardId) {
    LocalDateTime now = LocalDateTime.now(clock);
    return read(
        () ->
            WarningDashboardMapper.toWarningView(
                control.getActiveWarningForHazard(hazardId).orElseThrow(), now));
  }

  private List<HazardView> hazardList() {
    return read(() -> WarningDashboardMapper.toHazardViews(control.getAssessableHazards()));
  }

  private static void addUpdateOptions(Model model) {
    model.addAttribute("districtOptions", WarningFormMapper.districtOptions());
    model.addAttribute("severityOptions", WarningFormMapper.severityOptions());
  }

  private String page(Model model, String view, String subtitle) {
    model.addAttribute("currentUser", currentUserService.currentUser(DemoRole.DMC_OFFICER));
    model.addAttribute("subtitle", subtitle);
    return view;
  }

  private <T> T read(Supplier<T> work) {
    return readOnlyTransaction.execute(status -> work.get());
  }

  /** The stored form, if the session holds details for this hazard. */
  private static WarningForm storedForm(Object stored, String hazardId) {
    return stored instanceof PreviewedWarning previewed && previewed.hazardId().equals(hazardId)
        ? previewed.form()
        : null;
  }

  private static WarningForm storedForm(HttpSession session, String hazardId) {
    return storedForm(session.getAttribute(PREVIEWED_DETAILS), hazardId);
  }

  private static String pathVariable(HttpServletRequest request, String name) {
    @SuppressWarnings("unchecked")
    Map<String, String> variables =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    return variables == null ? null : variables.get(name);
  }
}
