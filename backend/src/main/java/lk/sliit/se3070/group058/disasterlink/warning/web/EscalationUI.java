package lk.sliit.se3070.group058.disasterlink.warning.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.warning.service.EscalationController;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningNotEscalatableException;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.EscalationForm;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.EscalationView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.PreviewedEscalation;
import lk.sliit.se3070.group058.disasterlink.warning.web.mapper.EscalationMapper;
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
 * The escalation screen, the «boundary» {@code EscalationUI} of Escalate Warning (wireframes
 * WF1b-S01 to S04 and their states; sd Escalate Warning, messages 3–10). Its handlers are named
 * after the model dictionary's inputs: {@code startEscalation}, {@code chooseEscalationLevel},
 * {@code enterEscalationDetails}, {@code confirmEscalation} and {@code cancelEscalation}. Each
 * returns the view of the dictionary's {@code show…} display in {@code
 * templates/warning/escalation} (standards section 2, FE-28): {@code showEscalationOptions} (with
 * the fragments {@code showNoHigherLevel} and {@code showNotEscalatable}), the details form {@code
 * escalationDetailsForm} (a screen component, MD C2, with {@code showValidationErrors}), {@code
 * showEscalationPreview} and {@code showEscalated} (with {@code showDeliveryFailure}).
 *
 * <p>The escalation start URL {@code GET /warning/{warningId}/escalate} is fixed (FX-20): UC2's
 * escalation prompt opens it for the DMC duty officer; the warning dashboard opens it with {@code
 * from=dashboard} for the DMC officer (A1), and "Cancel" returns to the screen it came from. {@code
 * enterEscalationDetails} is one input (PR3): it validates and, when valid, previews. The entered
 * details stay in the session under {@code previewedEscalation} (not the form's name {@code
 * escalationForm}, R4G-09); {@link #confirmEscalation} re-calls {@code validateEscalation} before
 * {@code escalateWarning} (P62 FX-05). There is no resend here: after a delivery failure the
 * officer resends from the warning dashboard (MD N7).
 *
 * <p><b>Transactions.</b> {@code escalateWarning} runs in the control's own transaction, with none
 * around it here. Everything the pages show is mapped to view records inside a read-only
 * transaction, because open-in-view is off.
 *
 * <p><b>Where each exception is handled</b> (standards section 4, "Handlers (b)"): {@link
 * WarningNotEscalatableException} by {@link #showNotEscalatable} (E2), an unknown warning by {@link
 * #notFound} as a 404, and any other failure by {@link #showServiceUnavailable}, which logs it and
 * shows the common error banner (MD section 0.10, C1; E4).
 *
 * <p>Implements CH-06, CH-10 and CH-24.
 */
@Controller
@RequestMapping("/warning/{warningId}/escalate")
@SessionAttributes(EscalationUI.PREVIEWED_ESCALATION)
public class EscalationUI {

  /** The session name of the escalation in progress; never the form's model name (R4G-09). */
  static final String PREVIEWED_ESCALATION = "previewedEscalation";

  private static final Logger LOG = LoggerFactory.getLogger(EscalationUI.class);

  private static final String OPTIONS_VIEW = "warning/escalation/showEscalationOptions";
  private static final String FORM_VIEW = "warning/escalation/escalationDetailsForm";
  private static final String PREVIEW_VIEW = "warning/escalation/showEscalationPreview";
  private static final String ESCALATED_VIEW = "warning/escalation/showEscalated";
  private static final String ERROR_VIEW = "error/5xx";

  private static final String FROM_DASHBOARD = "dashboard";
  private static final String WARNING_ID = "warningId";
  private static final String REDIRECT_TO_START = "redirect:/warning/{warningId}/escalate";
  private static final String REDIRECT_TO_DASHBOARD = "redirect:/warning";
  private static final String REDIRECT_TO_CONSOLE = "redirect:/hazardreport";
  private static final String LEVEL_ARROW = " → ";

  private final EscalationController control;

  private final CurrentUserService currentUserService;

  private final TransactionTemplate readOnlyTransaction;

  private final Clock clock;

  /**
   * Creates the escalation screen.
   *
   * @param control the control of Escalate Warning
   * @param currentUserService supplies the demo DMC officer or duty officer
   * @param transactionManager runs the read-only transactions in which entities are mapped
   * @param clock judges whether a shown warning is still active
   */
  public EscalationUI(
      EscalationController control,
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
   * Opens the escalation screen for a warning (Escalate steps 3–4, A1 1e, A3; sd message 3): the
   * levels above its current one (WF1b-S01), or the notice that it is already at the highest level
   * (3.3, WF1b-S01a). An expired warning is refused (3.4, E2 4c).
   *
   * @param warningId the identifier of the warning
   * @param from {@code dashboard} when opened from the warning dashboard, otherwise absent
   * @param model the page model, where the escalation in progress starts
   * @return the {@code showEscalationOptions} page
   */
  @GetMapping
  public String startEscalation(
      @PathVariable String warningId, @RequestParam(required = false) String from, Model model) {
    PreviewedEscalation started =
        new PreviewedEscalation(warningId, null, FROM_DASHBOARD.equals(from));
    model.addAttribute(PREVIEWED_ESCALATION, started);
    return optionsPage(warningId, model, started.fromDashboard());
  }

  /**
   * Opens the details form for the chosen level (Escalate steps 5–6, A4 9a–9b; sd messages 5 and
   * 8): pre-filled from the warning, or, when the officer changes the level after a preview, with
   * the texts and expiry time already entered.
   *
   * @param warningId the identifier of the warning
   * @param newLevel the chosen level
   * @param model the page model, with the escalation in progress
   * @return the {@code escalationDetailsForm} page
   */
  @PostMapping("/level")
  public String chooseEscalationLevel(
      @PathVariable String warningId, @RequestParam WarningLevel newLevel, Model model) {
    PreviewedEscalation stored = stored(model.getAttribute(PREVIEWED_ESCALATION), warningId);
    if (stored == null) {
      return REDIRECT_TO_START;
    }
    EscalationForm form =
        stored.form() == null
            ? read(
                () ->
                    EscalationMapper.formOf(
                        control.getEscalationOptions(warningId).warning(), newLevel))
            : EscalationMapper.withLevel(stored.form(), newLevel);
    model.addAttribute(
        PREVIEWED_ESCALATION, new PreviewedEscalation(warningId, form, stored.fromDashboard()));
    return formPage(warningId, model, form, List.of(), stored.fromDashboard());
  }

  /**
   * Validates the entered details and, when they are valid, shows the preview of the re-broadcast
   * (Escalate steps 7–8, E3; sd message 7; PR3), otherwise the form with the errors beside the
   * fields (7.4, WF1b-S02a).
   *
   * @param warningId the identifier of the warning
   * @param form the posted form
   * @param model the page model, with the escalation in progress
   * @return the {@code showEscalationPreview} or {@code escalationDetailsForm} page
   */
  @PostMapping("/details")
  public String enterEscalationDetails(
      @PathVariable String warningId,
      @ModelAttribute("escalationForm") EscalationForm form,
      Model model) {
    PreviewedEscalation stored = stored(model.getAttribute(PREVIEWED_ESCALATION), warningId);
    if (stored == null) {
      return REDIRECT_TO_START;
    }
    model.addAttribute(
        PREVIEWED_ESCALATION, new PreviewedEscalation(warningId, form, stored.fromDashboard()));
    EscalationDetails details = EscalationMapper.toDetails(form);
    ValidationResult result = control.validateEscalation(warningId, details);
    if (!result.valid()) {
      return formPage(warningId, model, form, result.errors(), stored.fromDashboard());
    }
    return previewPage(warningId, model, details, stored.fromDashboard());
  }

  /**
   * Escalates the previewed warning (Escalate steps 9–12; sd message 10). The details are validated
   * again first (10.1, E3 10g); then the warning is escalated and re-broadcast and the result shown
   * (10.3, WF1b-S04), with a delivery-failure alert per failed channel (10.4, E1, WF1b-S04a).
   *
   * @param warningId the identifier of the warning
   * @param model the page model, with the escalation in progress
   * @param status cleared once the warning is escalated
   * @return the {@code showEscalated} page, or the form with the errors
   */
  @PostMapping("/confirm")
  public String confirmEscalation(
      @PathVariable String warningId, Model model, SessionStatus status) {
    PreviewedEscalation stored = stored(model.getAttribute(PREVIEWED_ESCALATION), warningId);
    if (stored == null || stored.form() == null) {
      return REDIRECT_TO_START;
    }
    EscalationDetails details = EscalationMapper.toDetails(stored.form());
    ValidationResult result = control.validateEscalation(warningId, details);
    if (!result.valid()) {
      return formPage(warningId, model, stored.form(), result.errors(), stored.fromDashboard());
    }
    DemoRole role = roleOf(stored.fromDashboard());
    BroadcastSummary summary =
        control.escalateWarning(warningId, details, currentUserService.userId(role)).summary();
    status.setComplete();
    EscalationView escalated = escalationView(warningId);
    model.addAttribute("escalation", escalated);
    model.addAttribute("dispatch", WarningDashboardMapper.toDispatch(summary));
    return page(model, ESCALATED_VIEW, escalated.warning().hazardLabel(), stored.fromDashboard());
  }

  /**
   * Closes the escalation screen without changing anything (Escalate A5; sd messages 4, 6 and 9):
   * back to the warning dashboard or the verification console it was opened from.
   *
   * @param warningId the identifier of the warning
   * @param model the page model, with the escalation in progress
   * @param status cleared, so nothing of the escalation is kept
   * @return a redirect to the screen the escalation was opened from
   */
  @PostMapping("/cancel")
  public String cancelEscalation(
      @PathVariable String warningId, Model model, SessionStatus status) {
    PreviewedEscalation stored = stored(model.getAttribute(PREVIEWED_ESCALATION), warningId);
    status.setComplete();
    LOG.info("Escalation of warning {} cancelled", warningId);
    return stored == null || stored.fromDashboard() ? REDIRECT_TO_DASHBOARD : REDIRECT_TO_CONSOLE;
  }

  /**
   * Shows that the warning cannot be escalated (Escalate E2 4c–4e and 10a–10c; WF1b-S01b): expired,
   * or no longer below the chosen level. Nothing was changed, sent or recorded.
   *
   * @param exception the refusal, with the warning
   * @param request the request, for the escalation in progress
   * @param model the page model
   * @return the {@code showEscalationOptions} page in its not-escalatable state
   */
  @ExceptionHandler(WarningNotEscalatableException.class)
  public String showNotEscalatable(
      WarningNotEscalatableException exception, HttpServletRequest request, Model model) {
    String warningId = exception.getWarning().getWarningId();
    LOG.info("Warning {} cannot be escalated", warningId);
    model.addAttribute(
        "refused",
        EscalationMapper.toRefusedView(exception.getWarning(), LocalDateTime.now(clock)));
    boolean fromDashboard = fromDashboard(request, warningId);
    return page(model, OPTIONS_VIEW, "Escalate Warning", fromDashboard);
  }

  /**
   * Answers an unknown warning with a 404.
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
   * 0.10, C1; Escalate E4 10d–10f), with the status 500: on the preview, which keeps the confirmed
   * details, when the confirmation failed, otherwise on the generic error page. The failure is
   * logged with its exception.
   *
   * @param exception the failure
   * @param request the request, for the warning and the escalation in progress
   * @param model the page model
   * @return the {@code showEscalationPreview} page with the banner, or the error page
   */
  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String showServiceUnavailable(
      RuntimeException exception, HttpServletRequest request, Model model) {
    String path = request.getRequestURI();
    LOG.error("The escalation screen failed on {}", path, exception);
    model.addAttribute("serviceUnavailable", true);
    String warningId = pathVariable(request);
    PreviewedEscalation stored =
        warningId == null
            ? null
            : stored(request.getSession().getAttribute(PREVIEWED_ESCALATION), warningId);
    if (stored != null && stored.form() != null && path.endsWith("/confirm")) {
      return previewPage(
          warningId, model, EscalationMapper.toDetails(stored.form()), stored.fromDashboard());
    }
    return ERROR_VIEW;
  }

  /** WF1b-S01 and S01a: the warning card and the level tiles, or the highest-level notice. */
  private String optionsPage(String warningId, Model model, boolean fromDashboard) {
    EscalationView escalation = escalationView(warningId);
    model.addAttribute("escalation", escalation);
    return page(model, OPTIONS_VIEW, escalation.warning().hazardLabel(), fromDashboard);
  }

  /** WF1b-S02 and S02a: the details form with the warning card and the errors. */
  private String formPage(
      String warningId,
      Model model,
      EscalationForm form,
      List<String> errors,
      boolean fromDashboard) {
    EscalationView escalation = escalationView(warningId);
    model.addAttribute("escalation", escalation);
    model.addAttribute("escalationForm", form);
    model.addAttribute(
        "newLevel",
        form.newLevel() == null ? null : WarningDashboardMapper.levelLabel(form.newLevel()));
    model.addAttribute("newLevelClass", form.newLevel() == null ? "" : levelClass(form.newLevel()));
    model.addAttribute("errors", errors);
    model.addAttribute("fieldErrors", WarningFormMapper.fieldErrors(errors));
    String subtitle = escalation.warning().hazardLabel() + " · " + escalation.warning().warningId();
    if (form.newLevel() != null) {
      subtitle +=
          " · "
              + escalation.warning().levelLabel()
              + LEVEL_ARROW
              + WarningDashboardMapper.levelLabel(form.newLevel());
    }
    return page(model, FORM_VIEW, subtitle, fromDashboard);
  }

  /** WF1b-S03: the preview of the re-broadcast, with the other offered levels (A4). */
  private String previewPage(
      String warningId, Model model, EscalationDetails details, boolean fromDashboard) {
    EscalationView escalation = escalationView(warningId);
    model.addAttribute("escalation", escalation);
    model.addAttribute(WARNING_ID, warningId);
    model.addAttribute("currentNewLevel", details.newLevel().name());
    model.addAttribute(
        "preview",
        WarningDashboardMapper.toPreview(control.previewEscalation(warningId, details), List.of()));
    return page(
        model,
        PREVIEW_VIEW,
        escalation.warning().hazardLabel() + " · preview of the re-broadcast",
        fromDashboard);
  }

  private EscalationView escalationView(String warningId) {
    LocalDateTime now = LocalDateTime.now(clock);
    return read(
        () -> EscalationMapper.toEscalationView(control.getEscalationOptions(warningId), now));
  }

  private String page(Model model, String view, String subtitle, boolean fromDashboard) {
    model.addAttribute("currentUser", currentUserService.currentUser(roleOf(fromDashboard)));
    model.addAttribute("dutyOfficer", !fromDashboard);
    model.addAttribute("subtitle", subtitle);
    return view;
  }

  private <T> T read(Supplier<T> work) {
    return readOnlyTransaction.execute(status -> work.get());
  }

  private static DemoRole roleOf(boolean fromDashboard) {
    return fromDashboard ? DemoRole.DMC_OFFICER : DemoRole.DMC_DUTY_OFFICER;
  }

  private static String levelClass(WarningLevel level) {
    return "tag--l" + (level.ordinal() + 1);
  }

  /** The escalation in progress, if the session holds one for this warning. */
  private static PreviewedEscalation stored(Object stored, String warningId) {
    return stored instanceof PreviewedEscalation escalation
            && escalation.warningId().equals(warningId)
        ? escalation
        : null;
  }

  private static boolean fromDashboard(HttpServletRequest request, String warningId) {
    PreviewedEscalation stored =
        stored(request.getSession().getAttribute(PREVIEWED_ESCALATION), warningId);
    return stored == null
        ? FROM_DASHBOARD.equals(request.getParameter("from"))
        : stored.fromDashboard();
  }

  private static String pathVariable(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Map<String, String> variables =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    return variables == null ? null : variables.get(WARNING_ID);
  }
}
