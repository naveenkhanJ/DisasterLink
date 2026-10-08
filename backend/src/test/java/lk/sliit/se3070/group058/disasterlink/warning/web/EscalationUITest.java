package lk.sliit.se3070.group058.disasterlink.warning.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationOptions;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.IssueOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.warning.service.EscalationController;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningNotEscalatableException;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.EscalationForm;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.PreviewedEscalation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Tests of {@link EscalationUI} through MockMvc against the flows of SC Escalate Warning; the
 * control is mocked and the real templates are rendered.
 */
@WebMvcTest(EscalationUI.class)
@Import(EscalationUITest.FixedClockConfiguration.class)
class EscalationUITest {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

  private static final LocalDateTime NOW = SampleEntities.NOW;

  private static final String WARNING_ID = "WRN-2026-0087";

  private static final String DUTY_OFFICER_ID = "DMC-0002";

  private static final String OFFICER_ID = "DMC-0001";

  private static final String BASE = "/warning/" + WARNING_ID + "/escalate";

  private static final String EVACUATE = "Evacuate to the nearest shelter";

  /** A clock fixed at the scenario's time in Colombo. */
  @TestConfiguration
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO);
    }
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EscalationController control;

  @MockitoBean private CurrentUserService currentUserService;

  @MockitoBean private PlatformTransactionManager transactionManager;

  private Warning warning;

  @BeforeEach
  void setUp() {
    when(currentUserService.currentUser(DemoRole.DMC_DUTY_OFFICER))
        .thenReturn(new CurrentUserView("KP", "K. Perera", "DMC Duty Officer"));
    when(currentUserService.currentUser(DemoRole.DMC_OFFICER))
        .thenReturn(new CurrentUserView("NJ", "N. Jayasinghe", "DMC Officer"));
    when(currentUserService.userId(DemoRole.DMC_DUTY_OFFICER)).thenReturn(DUTY_OFFICER_ID);
    when(currentUserService.userId(DemoRole.DMC_OFFICER)).thenReturn(OFFICER_ID);
    warning =
        SampleEntities.warning(
            WARNING_ID,
            SampleEntities.hazard("HZ-2026-0142"),
            SampleEntities.officer(OFFICER_ID, Set.of()),
            List.of(District.RATNAPURA),
            List.of(RiverBasin.KALU),
            NOW.plusDays(1));
    offering(List.of(WarningLevel.LEVEL_3_RED));
  }

  private void offering(List<WarningLevel> levels) {
    when(control.getEscalationOptions(WARNING_ID))
        .thenReturn(new EscalationOptions(warning, levels));
  }

  private static EscalationForm redForm() {
    return new EscalationForm(
        WarningLevel.LEVEL_3_RED,
        "ඉවත් වන්න",
        "வெளியேறவும்",
        EVACUATE,
        "ඉවත් වන්න",
        "வெளியேறவும்",
        EVACUATE,
        NOW.plusDays(1));
  }

  private static MockHttpSession session(EscalationForm form, boolean fromDashboard) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        EscalationUI.PREVIEWED_ESCALATION,
        new PreviewedEscalation(WARNING_ID, form, fromDashboard));
    return session;
  }

  private void detailsAccepted() {
    when(control.validateEscalation(eq(WARNING_ID), any()))
        .thenReturn(new ValidationResult(true, List.of()));
    when(control.previewEscalation(eq(WARNING_ID), any()))
        .thenReturn(
            new WarningPreview(
                HazardType.FLOOD,
                WarningLevel.LEVEL_3_RED,
                Set.of(District.RATNAPURA, District.KALUTARA),
                SampleEntities.TEXT,
                SampleEntities.TEXT,
                Set.of(NotificationChannel.PUSH, NotificationChannel.SMS),
                NOW.plusDays(1),
                Map.of(District.RATNAPURA, 2, District.KALUTARA, 1),
                14,
                2));
  }

  private void detailsRefused(String error) {
    when(control.validateEscalation(eq(WARNING_ID), any()))
        .thenReturn(new ValidationResult(false, List.of(error)));
  }

  private MockHttpServletRequestBuilder postDetails(MockHttpSession session) {
    return post(BASE + "/details")
        .session(session)
        .param("newLevel", "LEVEL_3_RED")
        .param("messageSinhala", "ඉවත් වන්න")
        .param("messageTamil", "வெளியேறவும்")
        .param("messageEnglish", EVACUATE)
        .param("instructionsSinhala", "ඉවත් වන්න")
        .param("instructionsTamil", "வெளியேறவும்")
        .param("instructionsEnglish", EVACUATE)
        .param("expiryTime", "2026-05-22T08:15");
  }

  @Test
  @DisplayName("Escalate Main steps 3–4 — the prompt opens the levels above the current one")
  void startsFromTheEscalationPrompt() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get(BASE))
            .andExpect(status().isOk())
            .andExpect(view().name("warning/escalation/showEscalationOptions"))
            .andExpect(model().attribute("dutyOfficer", true))
            .andExpect(model().attributeExists("escalation"))
            .andExpect(content().string(containsString(WARNING_ID)))
            .andReturn();

    Object stored = result.getRequest().getSession().getAttribute("previewedEscalation");
    assertThat(stored).isInstanceOf(PreviewedEscalation.class);
    assertThat(((PreviewedEscalation) stored).fromDashboard()).isFalse();
  }

  @Test
  @DisplayName("Escalate A1 1e — the warning dashboard opens the escalation for the DMC officer")
  void startsFromTheDashboard() throws Exception {
    mockMvc
        .perform(get(BASE).param("from", "dashboard"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/showEscalationOptions"))
        .andExpect(model().attribute("dutyOfficer", false))
        .andExpect(content().string(containsString("N. Jayasinghe")));
  }

  @Test
  @DisplayName("Escalate A3 4a–4b — a warning at Level 3 – Red shows that no higher level exists")
  void showsNoHigherLevel() throws Exception {
    offering(List.of());

    mockMvc
        .perform(get(BASE))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/showEscalationOptions"))
        .andExpect(model().attributeExists("escalation"));
  }

  @Test
  @DisplayName("Escalate E2 4c–4e — an expired warning is shown as not escalatable")
  void showsAnExpiredWarningAsNotEscalatable() throws Exception {
    when(control.getEscalationOptions(WARNING_ID))
        .thenThrow(new WarningNotEscalatableException(warning));

    mockMvc
        .perform(get(BASE))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/showEscalationOptions"))
        .andExpect(model().attributeExists("refused"))
        .andExpect(model().attribute("dutyOfficer", true));
  }

  @Test
  @DisplayName("Escalate Main step 3 — an unknown warning is answered with 404")
  void answersAnUnknownWarningWith404() throws Exception {
    when(control.getEscalationOptions("WRN-0")).thenThrow(new NoSuchElementException("WRN-0"));

    mockMvc.perform(get("/warning/WRN-0/escalate")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Escalate E4 — a failure outside the confirm shows the error page")
  void showsTheErrorPageOnAnotherFailure() throws Exception {
    when(control.getEscalationOptions(WARNING_ID)).thenThrow(new IllegalStateException("down"));

    mockMvc
        .perform(get(BASE))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("error/5xx"))
        .andExpect(model().attribute("serviceUnavailable", true));
  }

  @Test
  @DisplayName(
      "Escalate Main steps 5–6 — the chosen level opens the form pre-filled from the warning")
  void opensThePreFilledForm() throws Exception {
    mockMvc
        .perform(
            post(BASE + "/level").session(session(null, false)).param("newLevel", "LEVEL_3_RED"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/escalationDetailsForm"))
        .andExpect(model().attribute("errors", List.of()))
        .andExpect(model().attributeExists("escalationForm", "newLevel"));
  }

  @Test
  @DisplayName("Escalate Main step 5 — choosing a level without a started escalation restarts it")
  void restartsALevelChoiceWithoutAnEscalation() throws Exception {
    mockMvc
        .perform(post(BASE + "/level").param("newLevel", "LEVEL_3_RED"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(BASE));
  }

  @Test
  @DisplayName("Escalate Main steps 7–8 — valid details show the preview of the re-broadcast")
  void previewsTheReBroadcast() throws Exception {
    detailsAccepted();

    mockMvc
        .perform(postDetails(session(null, false)))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/showEscalationPreview"))
        .andExpect(model().attribute("currentNewLevel", "LEVEL_3_RED"))
        .andExpect(model().attributeExists("preview"));
  }

  @Test
  @DisplayName("Escalate E3 8a–8c — invalid details return to the form with the errors")
  void showsTheEscalationErrors() throws Exception {
    detailsRefused("Tamil SMS is 230 characters; the limit is 201");

    mockMvc
        .perform(postDetails(session(null, false)))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/escalationDetailsForm"))
        .andExpect(
            model().attribute("errors", List.of("Tamil SMS is 230 characters; the limit is 201")))
        .andExpect(content().string(containsString(EVACUATE)));
    verify(control, never()).previewEscalation(anyString(), any());
  }

  @Test
  @DisplayName("Escalate Main step 7 — details without a started escalation restart it")
  void restartsDetailsWithoutAnEscalation() throws Exception {
    mockMvc
        .perform(postDetails(new MockHttpSession()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(BASE));
  }

  @Test
  @DisplayName("Escalate A4 9a–9b — another level after the preview keeps the entered texts")
  void choosesAnotherLevelAfterThePreview() throws Exception {
    offering(List.of(WarningLevel.LEVEL_2_AMBER, WarningLevel.LEVEL_3_RED));

    mockMvc
        .perform(
            post(BASE + "/level")
                .session(session(redForm(), false))
                .param("newLevel", "LEVEL_2_AMBER"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/escalationDetailsForm"))
        .andExpect(content().string(containsString(EVACUATE)));
  }

  @Test
  @DisplayName("Escalate Main steps 9–12 — the confirmed escalation is re-broadcast and shown")
  void escalatesTheWarning() throws Exception {
    detailsAccepted();
    when(control.escalateWarning(eq(WARNING_ID), any(), eq(DUTY_OFFICER_ID)))
        .thenReturn(new IssueOutcome(warning, new BroadcastSummary(61_520, Map.of(), List.of())));
    MockHttpSession session = session(redForm(), false);

    mockMvc
        .perform(post(BASE + "/confirm").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/showEscalated"))
        .andExpect(model().attributeExists("escalation", "dispatch"))
        .andExpect(content().string(containsString("61,520")));

    ArgumentCaptor<EscalationDetails> details = ArgumentCaptor.forClass(EscalationDetails.class);
    verify(control).validateEscalation(eq(WARNING_ID), details.capture());
    assertThat(details.getValue().newLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(session.getAttribute("previewedEscalation")).isNull();
  }

  @Test
  @DisplayName("Escalate A1 — an escalation from the dashboard is made by the DMC officer")
  void escalatesAsTheDmcOfficerFromTheDashboard() throws Exception {
    detailsAccepted();
    when(control.escalateWarning(eq(WARNING_ID), any(), eq(OFFICER_ID)))
        .thenReturn(new IssueOutcome(warning, new BroadcastSummary(1, Map.of(), List.of())));

    mockMvc
        .perform(post(BASE + "/confirm").session(session(redForm(), true)))
        .andExpect(view().name("warning/escalation/showEscalated"))
        .andExpect(model().attribute("dutyOfficer", false));
    verify(control).escalateWarning(eq(WARNING_ID), any(), eq(OFFICER_ID));
  }

  @Test
  @DisplayName("Escalate E1 11a–11c — the escalated warning shows the SMS delivery failure")
  void showsTheDeliveryFailureOfTheEscalation() throws Exception {
    detailsAccepted();
    BroadcastSummary failed =
        new BroadcastSummary(
            60_905, Map.of(NotificationChannel.SMS, List.of("CIT-RAT-0001")), List.of());
    when(control.escalateWarning(eq(WARNING_ID), any(), eq(DUTY_OFFICER_ID)))
        .thenReturn(new IssueOutcome(warning, failed));

    mockMvc
        .perform(post(BASE + "/confirm").session(session(redForm(), false)))
        .andExpect(view().name("warning/escalation/showEscalated"))
        .andExpect(content().string(containsString("SMS delivery failed for 1 recipients")));
  }

  @Test
  @DisplayName("Escalate E3 10g — details no longer valid at confirm return to the form")
  void revalidatesAtConfirm() throws Exception {
    detailsRefused("The expiry time must be after now");

    mockMvc
        .perform(post(BASE + "/confirm").session(session(redForm(), false)))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/escalationDetailsForm"))
        .andExpect(model().attribute("errors", List.of("The expiry time must be after now")));
    verify(control, never()).escalateWarning(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("Escalate E2 10a–10c — a warning no longer escalatable at confirm is refused")
  void showsNotEscalatableAtConfirm() throws Exception {
    detailsAccepted();
    when(control.escalateWarning(eq(WARNING_ID), any(), eq(OFFICER_ID)))
        .thenThrow(new WarningNotEscalatableException(warning));

    mockMvc
        .perform(post(BASE + "/confirm").session(session(redForm(), true)))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/escalation/showEscalationOptions"))
        .andExpect(model().attributeExists("refused"))
        .andExpect(model().attribute("dutyOfficer", false));
  }

  @Test
  @DisplayName("Escalate E4 10d–10f — a failed confirm shows the error banner on the preview")
  void showsTheServiceUnavailableBannerOnThePreview() throws Exception {
    detailsAccepted();
    when(control.escalateWarning(eq(WARNING_ID), any(), eq(DUTY_OFFICER_ID)))
        .thenThrow(new IllegalStateException("database unavailable"));
    MockHttpSession session = session(redForm(), false);

    mockMvc
        .perform(post(BASE + "/confirm").session(session))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("warning/escalation/showEscalationPreview"))
        .andExpect(model().attribute("serviceUnavailable", true))
        .andExpect(content().string(containsString("nothing was changed")));
    assertThat(session.getAttribute("previewedEscalation")).isNotNull();
  }

  @Test
  @DisplayName("Escalate Main step 9 — a confirm without previewed details restarts it")
  void restartsAConfirmWithoutDetails() throws Exception {
    mockMvc
        .perform(post(BASE + "/confirm").session(session(null, false)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(BASE));
    verify(control, never()).escalateWarning(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("Escalate A5 — cancelling from UC2's prompt returns to the verification console")
  void cancelsBackToTheConsole() throws Exception {
    MockHttpSession session = session(redForm(), false);

    mockMvc
        .perform(post(BASE + "/cancel").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/hazardreport"));
    assertThat(session.getAttribute("previewedEscalation")).isNull();
    verify(control, never()).escalateWarning(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("Escalate A5 — cancelling from the dashboard returns to the warning dashboard")
  void cancelsBackToTheDashboard() throws Exception {
    mockMvc
        .perform(post(BASE + "/cancel").session(session(null, true)))
        .andExpect(redirectedUrl("/warning"));
  }

  @Test
  @DisplayName("Escalate A5 — cancelling without an escalation returns to the warning dashboard")
  void cancelsWithoutAnEscalation() throws Exception {
    mockMvc.perform(post(BASE + "/cancel")).andExpect(redirectedUrl("/warning"));
  }
}
