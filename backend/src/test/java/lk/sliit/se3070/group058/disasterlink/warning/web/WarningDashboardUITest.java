package lk.sliit.se3070.group058.disasterlink.warning.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardDetailsView;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.IssueOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.warning.service.ActiveWarningExistsException;
import lk.sliit.se3070.group058.disasterlink.warning.service.InvalidHazardUpdateException;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.PreviewedWarning;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.WarningForm;
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
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Tests of {@link WarningDashboardUI} through MockMvc against the flows of SC1 Issue Hazard
 * Warning; the control is mocked and the real templates are rendered.
 */
@WebMvcTest(WarningDashboardUI.class)
@Import(WarningDashboardUITest.FixedClockConfiguration.class)
class WarningDashboardUITest {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

  private static final LocalDateTime NOW = SampleEntities.NOW;

  private static final String HAZARD_ID = "HZ-2026-0142";

  private static final String WARNING_ID = "WRN-2026-0087";

  private static final String OFFICER_ID = "DMC-0001";

  private static final String BASE = "/warning/hazards/" + HAZARD_ID;

  /** A clock fixed at the scenario's time in Colombo. */
  @TestConfiguration
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO);
    }
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WarningController control;

  @MockitoBean private CurrentUserService currentUserService;

  @MockitoBean private PlatformTransactionManager transactionManager;

  private final DMCOfficer officer = SampleEntities.officer(OFFICER_ID, Set.of());

  private final Hazard hazard = SampleEntities.hazard(HAZARD_ID);

  @BeforeEach
  void setUp() {
    when(currentUserService.currentUser(DemoRole.DMC_OFFICER))
        .thenReturn(new CurrentUserView("NJ", "N. Jayasinghe", "DMC Officer"));
    when(currentUserService.userId(DemoRole.DMC_OFFICER)).thenReturn(OFFICER_ID);
    when(control.getAssessableHazards()).thenReturn(List.of(hazard));
    when(control.getHazardDetails(HAZARD_ID))
        .thenReturn(new HazardDetailsView(hazard, null, List.of()));
  }

  private Warning activeWarning() {
    Warning warning =
        SampleEntities.warning(
            WARNING_ID,
            hazard,
            officer,
            List.of(District.RATNAPURA),
            List.of(RiverBasin.KALU),
            NOW.plusDays(1));
    when(control.getActiveWarningForHazard(HAZARD_ID)).thenReturn(Optional.of(warning));
    return warning;
  }

  private static WarningForm scenarioForm() {
    return new WarningForm(
        WarningLevel.LEVEL_2_AMBER,
        "සිං",
        "தமிழ்",
        "Flood warning",
        "උපදෙස්",
        "அறிவுறுத்தல்",
        "Move to higher ground",
        List.of(District.RATNAPURA),
        List.of(RiverBasin.KALU),
        Set.of(NotificationChannel.PUSH, NotificationChannel.SMS),
        NOW.plusDays(1));
  }

  private static MockHttpSession sessionWithDetails() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        WarningDashboardUI.PREVIEWED_DETAILS, new PreviewedWarning(HAZARD_ID, scenarioForm()));
    return session;
  }

  private void previewAccepted() {
    when(control.validateWarning(eq(HAZARD_ID), any()))
        .thenReturn(new ValidationResult(true, List.of()));
    when(control.previewWarning(eq(HAZARD_ID), any()))
        .thenReturn(
            new WarningPreview(
                HazardType.FLOOD,
                WarningLevel.LEVEL_2_AMBER,
                Set.of(District.RATNAPURA, District.KALUTARA),
                SampleEntities.TEXT,
                SampleEntities.TEXT,
                Set.of(NotificationChannel.PUSH, NotificationChannel.SMS),
                NOW.plusDays(1),
                Map.of(District.RATNAPURA, 2, District.KALUTARA, 1),
                14,
                0));
  }

  private void detailsRefused(String error) {
    when(control.validateWarning(eq(HAZARD_ID), any()))
        .thenReturn(new ValidationResult(false, List.of(error)));
  }

  @Test
  @DisplayName("Main steps 1–3 — the dashboard lists the assessable hazards")
  void opensTheDashboard() throws Exception {
    mockMvc
        .perform(get("/warning"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showHazards"))
        .andExpect(model().attributeExists("hazards", "currentUser"))
        .andExpect(content().string(containsString(HAZARD_ID)));
  }

  @Test
  @DisplayName("Main steps 4–5 — a complete hazard without a warning opens the warning form")
  void selectsACompleteHazard() throws Exception {
    mockMvc
        .perform(get(BASE))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showWarningForm"))
        .andExpect(model().attributeExists("hazard", "warningForm", "levelOptions"))
        .andExpect(model().attribute("errors", List.of()));
  }

  @Test
  @DisplayName("A1 5a — a hazard with missing information shows what is missing")
  void showsAnIncompleteHazard() throws Exception {
    when(control.getHazardDetails(HAZARD_ID))
        .thenReturn(new HazardDetailsView(hazard, null, List.of("Affected districts")));

    mockMvc
        .perform(get(BASE))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showHazardDetails"))
        .andExpect(model().attribute("missing", List.of("Affected districts")))
        .andExpect(model().attributeDoesNotExist("activeWarning"))
        .andExpect(content().string(containsString("Affected districts")));
  }

  @Test
  @DisplayName("A5 5d — a hazard with an active warning shows it with its dispatch status")
  void showsTheActiveWarningOfAHazard() throws Exception {
    Warning warning = activeWarning();
    when(control.getHazardDetails(HAZARD_ID))
        .thenReturn(new HazardDetailsView(hazard, warning, List.of()));
    when(control.getBroadcastSummary(WARNING_ID))
        .thenReturn(new BroadcastSummary(42, Map.of(), List.of()));

    mockMvc
        .perform(get(BASE))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showHazardDetails"))
        .andExpect(model().attributeExists("activeWarning", "dispatch"))
        .andExpect(content().string(containsString(WARNING_ID)));
  }

  @Test
  @DisplayName("Main step 4 — an unknown hazard is answered with 404")
  void answersAnUnknownHazardWith404() throws Exception {
    when(control.getHazardDetails("HZ-0")).thenThrow(new NoSuchElementException("HZ-0"));

    mockMvc.perform(get("/warning/hazards/HZ-0")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("A1 5b–5c — a valid hazard update is applied and the form opens")
  void appliesAHazardUpdate() throws Exception {
    mockMvc
        .perform(
            post(BASE + "/update")
                .param("description", "Kalu Ganga over the danger level")
                .param("severity", "HIGH")
                .param("districts", "RATNAPURA", "KALUTARA"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showWarningForm"));

    ArgumentCaptor<HazardUpdate> update = ArgumentCaptor.forClass(HazardUpdate.class);
    verify(control).updateHazard(eq(HAZARD_ID), update.capture());
    assertThat(update.getValue().severity()).isEqualTo(HazardSeverity.HIGH);
    assertThat(update.getValue().districts())
        .containsExactly(District.RATNAPURA, District.KALUTARA);
  }

  @Test
  @DisplayName("E5 5f–5g — a refused hazard update is shown beside the panel with its values")
  void showsARefusedHazardUpdate() throws Exception {
    when(control.updateHazard(eq(HAZARD_ID), any()))
        .thenThrow(new InvalidHazardUpdateException(List.of("Select at least one district")));

    mockMvc
        .perform(post(BASE + "/update").param("description", "Rising").param("severity", "MEDIUM"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showHazardDetails"))
        .andExpect(model().attribute("updateErrors", List.of("Select at least one district")))
        .andExpect(model().attribute("updateOpen", true))
        .andExpect(content().string(containsString("Select at least one district")));
  }

  @Test
  @DisplayName("E5 12i–12j — a refused update from the form returns to the form, details kept")
  void showsARefusedHazardUpdateOnTheForm() throws Exception {
    when(control.updateHazard(eq(HAZARD_ID), any()))
        .thenThrow(new InvalidHazardUpdateException(List.of("Enter a description")));

    mockMvc
        .perform(
            post(BASE + "/update")
                .session(sessionWithDetails())
                .param("returnTo", "form")
                .param("severity", "")
                .param("districts", "RATNAPURA"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showWarningForm"))
        .andExpect(model().attribute("updateErrors", List.of("Enter a description")))
        .andExpect(content().string(containsString("Move to higher ground")));
  }

  @Test
  @DisplayName("E4 12f — the hazard is updated before broadcasting and the form keeps the details")
  void updatesTheHazardBeforeBroadcasting() throws Exception {
    mockMvc
        .perform(
            post(BASE + "/update")
                .session(sessionWithDetails())
                .param("description", "Breach at the sandbag barrier")
                .param("severity", "HIGH")
                .param("districts", "RATNAPURA", "KALUTARA"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showWarningForm"))
        .andExpect(content().string(containsString("Move to higher ground")));

    verify(control).updateHazard(eq(HAZARD_ID), any());
  }

  @Test
  @DisplayName("Main steps 6–10 — the entered details are stored and the preview is requested")
  void storesTheDetailsAndRedirects() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(BASE + "/details")
                    .param("warningLevel", "LEVEL_2_AMBER")
                    .param("messageEnglish", "Flood warning")
                    .param("targetDistricts", "RATNAPURA")
                    .param("targetRiverBasins", "KALU")
                    .param("channels", "PUSH", "SMS")
                    .param("expiryTime", "2026-05-22T08:15"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(BASE + "/preview"))
            .andReturn();

    Object stored = result.getRequest().getSession().getAttribute("previewedDetails");
    assertThat(stored).isInstanceOf(PreviewedWarning.class);
    assertThat(((PreviewedWarning) stored).form().warningLevel())
        .isEqualTo(WarningLevel.LEVEL_2_AMBER);
  }

  @Test
  @DisplayName("Main step 6 — a form without a warning level is malformed (400)")
  void refusesAFormWithoutALevel() throws Exception {
    mockMvc
        .perform(post(BASE + "/details").param("messageEnglish", "Flood warning"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Main step 11 — valid details are previewed with the recipients")
  void previewsValidDetails() throws Exception {
    previewAccepted();

    mockMvc
        .perform(get(BASE + "/preview").session(sessionWithDetails()))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showPreview"))
        .andExpect(model().attributeExists("preview"))
        .andExpect(model().attribute("hazardId", HAZARD_ID));
  }

  @Test
  @DisplayName("A2 11a–11b — details without an area return to the form with the error")
  void showsTheMissingAreaError() throws Exception {
    detailsRefused("Select at least one district or river basin");

    mockMvc
        .perform(get(BASE + "/preview").session(sessionWithDetails()))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showWarningForm"))
        .andExpect(
            model().attribute("errors", List.of("Select at least one district or river basin")));
    verify(control, never()).previewWarning(anyString(), any());
  }

  @Test
  @DisplayName("E3 11d–11f — text rule errors are shown and the form keeps its values")
  void showsTheTextRuleErrors() throws Exception {
    detailsRefused("Tamil SMS is 230 characters; the limit is 201");

    mockMvc
        .perform(get(BASE + "/preview").session(sessionWithDetails()))
        .andExpect(view().name("warning/showWarningForm"))
        .andExpect(content().string(containsString("Move to higher ground")));
  }

  @Test
  @DisplayName("Main step 11 — a preview without entered details goes back to the hazard")
  void redirectsAPreviewWithoutDetails() throws Exception {
    mockMvc
        .perform(get(BASE + "/preview"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(BASE));
  }

  @Test
  @DisplayName("A4 12a–12b — Edit reopens the form with the details already entered")
  void reopensTheFormForModification() throws Exception {
    mockMvc
        .perform(get(BASE + "/edit").session(sessionWithDetails()))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showWarningForm"))
        .andExpect(model().attribute("selectedDistricts", "Ratnapura"))
        .andExpect(content().string(containsString("Move to higher ground")));
  }

  @Test
  @DisplayName("A4 12c–12d — the modified details are previewed again")
  void previewsTheModifiedDetails() throws Exception {
    previewAccepted();
    MockHttpSession session = sessionWithDetails();

    mockMvc
        .perform(
            post(BASE + "/details")
                .session(session)
                .param("warningLevel", "LEVEL_3_RED")
                .param("targetDistricts", "RATNAPURA", "KALUTARA")
                .param("channels", "SMS")
                .param("expiryTime", "2026-05-22T08:15"))
        .andExpect(redirectedUrl(BASE + "/preview"));
    mockMvc
        .perform(get(BASE + "/preview").session(session))
        .andExpect(view().name("warning/showPreview"));

    ArgumentCaptor<WarningDetails> details = ArgumentCaptor.forClass(WarningDetails.class);
    verify(control).previewWarning(eq(HAZARD_ID), details.capture());
    assertThat(details.getValue().warningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
  }

  @Test
  @DisplayName("Main steps 12–17 — the confirmed warning is issued and its dispatch status shown")
  void issuesTheConfirmedWarning() throws Exception {
    previewAccepted();
    Warning warning = activeWarning();
    when(control.issueWarning(eq(HAZARD_ID), any(), eq(OFFICER_ID)))
        .thenReturn(new IssueOutcome(warning, new BroadcastSummary(102_549, Map.of(), List.of())));
    MockHttpSession session = sessionWithDetails();

    mockMvc
        .perform(post(BASE + "/confirm").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showActiveWarning"))
        .andExpect(content().string(containsString("102,549")))
        .andExpect(content().string(containsString("No failures")));

    verify(control).validateWarning(eq(HAZARD_ID), any());
    assertThat(session.getAttribute("previewedDetails")).isNull();
  }

  @Test
  @DisplayName("E1 16a–16c — the dispatch status shows a delivery-failure alert per channel")
  void showsTheDeliveryFailure() throws Exception {
    previewAccepted();
    Warning warning = activeWarning();
    BroadcastSummary failed =
        new BroadcastSummary(
            101_309, Map.of(NotificationChannel.SMS, List.of("CIT-COL-0001")), List.of());
    when(control.issueWarning(eq(HAZARD_ID), any(), eq(OFFICER_ID)))
        .thenReturn(new IssueOutcome(warning, failed));

    mockMvc
        .perform(post(BASE + "/confirm").session(sessionWithDetails()))
        .andExpect(view().name("warning/showActiveWarning"))
        .andExpect(content().string(containsString("SMS delivery failed for 1 recipients")))
        .andExpect(content().string(containsString("Resend to failed recipients")));
  }

  @Test
  @DisplayName("E3 13g — details no longer valid at confirm return to the form, nothing issued")
  void revalidatesAtConfirm() throws Exception {
    detailsRefused("The expiry time must be after now");

    mockMvc
        .perform(post(BASE + "/confirm").session(sessionWithDetails()))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showWarningForm"))
        .andExpect(model().attribute("errors", hasItem("The expiry time must be after now")));
    verify(control, never()).issueWarning(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("E6 13d–13f — a warning issued in the meantime is named and Escalate offered")
  void showsTheActiveWarningThatExists() throws Exception {
    previewAccepted();
    Warning existing = activeWarning();
    when(control.issueWarning(eq(HAZARD_ID), any(), eq(OFFICER_ID)))
        .thenThrow(new ActiveWarningExistsException(existing));

    mockMvc
        .perform(post(BASE + "/confirm").session(sessionWithDetails()))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showPreview"))
        .andExpect(model().attributeExists("existingWarning", "preview"))
        .andExpect(content().string(containsString("/warning/" + WARNING_ID + "/escalate")));
  }

  @Test
  @DisplayName("E2 13a–13c — a failed confirm shows the error banner on the preview, details kept")
  void showsTheServiceUnavailableBannerOnThePreview() throws Exception {
    previewAccepted();
    when(control.issueWarning(eq(HAZARD_ID), any(), eq(OFFICER_ID)))
        .thenThrow(new IllegalStateException("database unavailable"));
    MockHttpSession session = sessionWithDetails();

    mockMvc
        .perform(post(BASE + "/confirm").session(session))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("warning/showPreview"))
        .andExpect(model().attribute("serviceUnavailable", true))
        .andExpect(content().string(containsString("nothing was changed")));
    assertThat(session.getAttribute("previewedDetails")).isNotNull();
  }

  @Test
  @DisplayName("E2 — any other failure shows the error banner on the dashboard")
  void showsTheServiceUnavailableBannerOnTheDashboard() throws Exception {
    when(control.getHazardDetails(HAZARD_ID)).thenThrow(new IllegalStateException("down"));

    mockMvc
        .perform(get(BASE))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("warning/showHazards"))
        .andExpect(model().attribute("serviceUnavailable", true));
  }

  @Test
  @DisplayName("Main step 12 — a confirm without entered details goes back to the hazard")
  void redirectsAConfirmWithoutDetails() throws Exception {
    mockMvc
        .perform(post(BASE + "/confirm"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(BASE));
    verify(control, never()).issueWarning(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("E1 16d–16e — a complete resend on SMS is confirmed with a notice")
  void resendsOnSms() throws Exception {
    activeWarning();
    when(control.resendOnChannel(WARNING_ID, NotificationChannel.SMS))
        .thenReturn(new BroadcastSummary(1, Map.of(), List.of()));

    mockMvc
        .perform(post(BASE + "/warnings/" + WARNING_ID + "/resend").param("channel", "SMS"))
        .andExpect(status().isOk())
        .andExpect(view().name("warning/showActiveWarning"))
        .andExpect(model().attribute("notice", "Resend on SMS complete"))
        .andExpect(request().sessionAttributeDoesNotExist("previewedDetails"));
  }

  @Test
  @DisplayName("E1 16e — a resend that still fails shows the failure and no notice")
  void showsAResendThatStillFails() throws Exception {
    activeWarning();
    when(control.resendOnChannel(WARNING_ID, NotificationChannel.PUSH))
        .thenReturn(
            new BroadcastSummary(
                0, Map.of(NotificationChannel.PUSH, List.of("CIT-COL-0002")), List.of("ORG-1")));

    mockMvc
        .perform(post(BASE + "/warnings/" + WARNING_ID + "/resend").param("channel", "PUSH"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("notice", (Object) null))
        .andExpect(content().string(containsString("Push delivery failed for 1 recipients")));
  }
}
