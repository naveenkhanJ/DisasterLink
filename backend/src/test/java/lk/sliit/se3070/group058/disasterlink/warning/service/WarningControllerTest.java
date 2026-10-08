package lk.sliit.se3070.group058.disasterlink.warning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DeliveryStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardDetailsView;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSource;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.IssueOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RecipientKind;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Tests of {@link WarningController} against the flows of SC1 Issue Hazard Warning. */
class WarningControllerTest {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 10, 45);

  private static final String HAZARD_ID = "HZ-2026-0142";

  private static final String OFFICER_ID = "DMC-0001";

  private final HazardRepository hazards = mock(HazardRepository.class);

  private final WarningRepository warnings = mock(WarningRepository.class);

  private final DMCOfficerRepository officers = mock(DMCOfficerRepository.class);

  private final CitizenRepository citizens = mock(CitizenRepository.class);

  private final OrganizationRepository organizations = mock(OrganizationRepository.class);

  private final WarningBroadcaster broadcaster = mock(WarningBroadcaster.class);

  private final WarningTextValidator textValidator = mock(WarningTextValidator.class);

  private final IdGenerator idGenerator = mock(IdGenerator.class);

  private final DMCOfficer officer = SampleEntities.officer(OFFICER_ID, Set.of());

  private WarningController controller;

  @BeforeEach
  void setUp() {
    controller =
        new WarningController(
            hazards,
            warnings,
            officers,
            citizens,
            organizations,
            broadcaster,
            textValidator,
            idGenerator,
            Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO));
    when(textValidator.validate(any(), any(), any(), any())).thenReturn(List.of());
  }

  private static Hazard hazard(String hazardId, List<District> districts, HazardStatus status) {
    return new Hazard(
        hazardId,
        HazardType.FLOOD,
        "Kalu Ganga rising at the Ratnapura gauge",
        HazardSeverity.MEDIUM,
        NOW.minusHours(1),
        null,
        SampleEntities.RATNAPURA_POINT,
        "Kalu Ganga at Ratnapura",
        districts,
        RiverBasin.KALU,
        HazardSource.AGENCY_BULLETIN,
        status);
  }

  private static Hazard completeHazard() {
    return hazard(HAZARD_ID, List.of(District.RATNAPURA, District.KALUTARA), HazardStatus.ACTIVE);
  }

  private static WarningDetails details(List<District> districts, List<RiverBasin> basins) {
    return new WarningDetails(
        WarningLevel.LEVEL_2_AMBER,
        SampleEntities.TEXT,
        SampleEntities.TEXT,
        districts,
        basins,
        Set.of(NotificationChannel.PUSH, NotificationChannel.SMS),
        NOW.plusDays(1));
  }

  private static WarningDetails scenarioDetails() {
    return details(List.of(District.RATNAPURA), List.of(RiverBasin.KALU));
  }

  /** A recorded Level 1 warning of the hazard that expires at a chosen time. */
  private Warning storedWarning(LocalDateTime expiryTime) {
    WarningDetails stored =
        new WarningDetails(
            WarningLevel.LEVEL_1_YELLOW,
            SampleEntities.TEXT,
            SampleEntities.TEXT,
            List.of(District.RATNAPURA),
            List.of(),
            Set.of(NotificationChannel.SMS),
            expiryTime);
    return new Warning("WRN-2026-0081", completeHazard(), stored, officer, NOW.minusHours(1));
  }

  private static Citizen citizenOf(District district) {
    return new Citizen(
        "CIT-" + district.name(),
        "Citizen",
        "0771234567",
        "c@example.lk",
        "199012345678",
        "Address",
        district,
        Language.ENGLISH,
        null);
  }

  @Test
  @DisplayName("Main step 2 — only the hazards that are assessable are offered")
  void offersTheAssessableHazards() {
    Hazard active = completeHazard();
    Hazard closed = hazard("HZ-2026-0100", List.of(District.COLOMBO), HazardStatus.CLOSED);
    when(hazards.findAssessable()).thenReturn(List.of(active, closed));

    assertThat(controller.getAssessableHazards()).containsExactly(active);
  }

  @Test
  @DisplayName("Main step 5 — a complete hazard without a warning shows no missing information")
  void showsACompleteHazard() {
    Hazard hazard = completeHazard();
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(hazard));
    when(warnings.findActiveByHazard(HAZARD_ID, NOW)).thenReturn(Optional.empty());

    HazardDetailsView view = controller.getHazardDetails(HAZARD_ID);

    assertThat(view.hazard()).isSameAs(hazard);
    assertThat(view.activeWarning()).isNull();
    assertThat(view.missingInformation()).isEmpty();
  }

  @Test
  @DisplayName("A1 5a — a hazard without districts reports them as missing")
  void reportsTheMissingDistricts() {
    when(hazards.findById(HAZARD_ID))
        .thenReturn(Optional.of(hazard(HAZARD_ID, List.of(), HazardStatus.ACTIVE)));
    when(warnings.findActiveByHazard(HAZARD_ID, NOW)).thenReturn(Optional.empty());

    assertThat(controller.getHazardDetails(HAZARD_ID).missingInformation())
        .containsExactly("affected districts");
  }

  @Test
  @DisplayName("A5 5d — the hazard's active warning is shown instead of the form")
  void showsTheActiveWarning() {
    Warning active = storedWarning(NOW.plusHours(20));
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));
    when(warnings.findActiveByHazard(HAZARD_ID, NOW)).thenReturn(Optional.of(active));

    assertThat(controller.getHazardDetails(HAZARD_ID).activeWarning()).isSameAs(active);
  }

  @Test
  @DisplayName("Main step 19 — a warning at its expiry time no longer counts as active")
  void expiredWarningIsNotActive() {
    when(warnings.findActiveByHazard(HAZARD_ID, NOW)).thenReturn(Optional.of(storedWarning(NOW)));

    assertThat(controller.getActiveWarningForHazard(HAZARD_ID)).isEmpty();
  }

  @Test
  @DisplayName("Main step 4 — an unknown hazard is not found")
  void unknownHazardIsNotFound() {
    when(hazards.findById("HZ-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getHazardDetails("HZ-0"))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("A1 5c — a valid hazard update is recorded")
  void recordsAValidUpdate() {
    Hazard hazard = hazard(HAZARD_ID, List.of(), HazardStatus.ACTIVE);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(hazard));
    when(hazards.save(hazard)).thenReturn(hazard);
    HazardUpdate update =
        new HazardUpdate(
            "Kalu Ganga rising",
            HazardSeverity.MEDIUM,
            List.of(District.RATNAPURA, District.KALUTARA));

    Hazard updated = controller.updateHazard(HAZARD_ID, update);

    assertThat(updated.getDistricts()).containsExactly(District.RATNAPURA, District.KALUTARA);
    assertThat(updated.missingInformation()).isEmpty();
  }

  @Test
  @DisplayName("E5 5f — an update without districts is refused and the hazard is not changed")
  void refusesAnUpdateWithoutDistricts() {
    Hazard hazard = hazard(HAZARD_ID, List.of(), HazardStatus.ACTIVE);
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(hazard));
    HazardUpdate update = new HazardUpdate("Kalu Ganga rising", HazardSeverity.HIGH, List.of());

    assertThatThrownBy(() -> controller.updateHazard(HAZARD_ID, update))
        .isInstanceOfSatisfying(
            InvalidHazardUpdateException.class,
            refusal ->
                assertThat(refusal.getErrors())
                    .containsExactly("At least one district is required"));
    assertThat(hazard.getSeverity()).isEqualTo(HazardSeverity.MEDIUM);
    verify(hazards, never()).save(any());
  }

  @Test
  @DisplayName("E5 12i — an update without description and severity lists both errors")
  void refusesAnUpdateWithoutDescriptionAndSeverity() {
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));
    HazardUpdate update = new HazardUpdate(" ", null, List.of(District.RATNAPURA));

    assertThatThrownBy(() -> controller.updateHazard(HAZARD_ID, update))
        .isInstanceOfSatisfying(
            InvalidHazardUpdateException.class,
            refusal ->
                assertThat(refusal.getErrors())
                    .containsExactly("A description is required", "A severity is required"));
  }

  @Test
  @DisplayName("Main step 11 — valid details pass validation")
  void acceptsValidDetails() {
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));

    ValidationResult result = controller.validateWarning(HAZARD_ID, scenarioDetails());

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  @DisplayName("A2 11a–11b — details without a district or river basin are refused")
  void refusesDetailsWithoutAnAffectedArea() {
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));

    ValidationResult result = controller.validateWarning(HAZARD_ID, details(List.of(), List.of()));

    assertThat(result.valid()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "An affected area is required: select at least one district or river basin");
  }

  @Test
  @DisplayName("Main step 11 — details without a channel are refused")
  void refusesDetailsWithoutAChannel() {
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));
    WarningDetails noChannel =
        new WarningDetails(
            WarningLevel.LEVEL_2_AMBER,
            SampleEntities.TEXT,
            SampleEntities.TEXT,
            List.of(District.RATNAPURA),
            List.of(),
            Set.of(),
            NOW.plusDays(1));

    assertThat(controller.validateWarning(HAZARD_ID, noChannel).errors())
        .containsExactly("At least one notification channel is required");
  }

  @Test
  @DisplayName("A1 — a hazard with missing information cannot get a warning")
  void refusesDetailsForAnIncompleteHazard() {
    when(hazards.findById(HAZARD_ID))
        .thenReturn(Optional.of(hazard(HAZARD_ID, List.of(), HazardStatus.ACTIVE)));

    assertThat(controller.validateWarning(HAZARD_ID, scenarioDetails()).errors())
        .containsExactly(
            "A warning cannot be issued until the missing information is provided:"
                + " affected districts");
  }

  @Test
  @DisplayName("E3 11d–11e — the text rule errors are returned beside the form")
  void returnsTheTextRuleErrors() {
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));
    WarningDetails details = scenarioDetails();
    when(textValidator.validate(
            details.message(), details.instructions(), details.channels(), details.expiryTime()))
        .thenReturn(
            List.of(
                "Tamil message and instructions: 236 / 201 characters",
                "Sinhala instructions are required"));

    ValidationResult result = controller.validateWarning(HAZARD_ID, details);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).hasSize(2);
  }

  @Test
  @DisplayName("Main step 11 — the preview expands the Kalu basin and counts the recipients")
  void previewsTheRecipients() {
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));
    Set<District> covered = Set.of(District.RATNAPURA, District.KALUTARA);
    when(citizens.findByDistrictIn(covered))
        .thenReturn(
            List.of(
                citizenOf(District.RATNAPURA),
                citizenOf(District.RATNAPURA),
                citizenOf(District.KALUTARA)));
    when(organizations.findByDistrictIn(covered))
        .thenReturn(List.of(SampleEntities.organization("ORG-0001")));

    WarningPreview preview = controller.previewWarning(HAZARD_ID, scenarioDetails());

    assertThat(preview.hazardType()).isEqualTo(HazardType.FLOOD);
    assertThat(preview.coveredDistricts()).isEqualTo(covered);
    assertThat(preview.citizensPerDistrict())
        .isEqualTo(Map.of(District.RATNAPURA, 2, District.KALUTARA, 1));
    assertThat(preview.organizationCount()).isEqualTo(1);
    assertThat(preview.districtOfficerCount()).isZero();
    assertThat(preview.expiryTime()).isEqualTo(NOW.plusDays(1));
  }

  @Test
  @DisplayName("Main step 11 — a covered district without citizens shows zero")
  void previewShowsZeroForAnEmptyDistrict() {
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));
    when(citizens.findByDistrictIn(Set.of(District.COLOMBO))).thenReturn(List.of());

    WarningPreview preview =
        controller.previewWarning(HAZARD_ID, details(List.of(District.COLOMBO), List.of()));

    assertThat(preview.citizensPerDistrict()).isEqualTo(Map.of(District.COLOMBO, 0));
  }

  @Test
  @DisplayName("Main step 13 — the warning is recorded for the hazard and broadcast")
  void issuesAndBroadcastsTheWarning() {
    Hazard hazard = completeHazard();
    BroadcastSummary summary = new BroadcastSummary(5, Map.of(), List.of());
    when(warnings.findActiveByHazard(HAZARD_ID, NOW)).thenReturn(Optional.empty());
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(hazard));
    when(officers.findById(OFFICER_ID)).thenReturn(Optional.of(officer));
    when(idGenerator.next(IdKind.WARNING, null)).thenReturn("WRN-2026-0087");
    when(broadcaster.broadcast(any())).thenReturn(summary);
    when(warnings.save(any())).thenAnswer(call -> call.getArgument(0));

    IssueOutcome outcome = controller.issueWarning(HAZARD_ID, scenarioDetails(), OFFICER_ID);

    ArgumentCaptor<Warning> broadcast = ArgumentCaptor.forClass(Warning.class);
    verify(broadcaster).broadcast(broadcast.capture());
    Warning warning = broadcast.getValue();
    assertThat(warning.getWarningId()).isEqualTo("WRN-2026-0087");
    assertThat(warning.getHazard()).isSameAs(hazard);
    assertThat(warning.getOfficer()).isSameAs(officer);
    assertThat(warning.getIssuedTime()).isEqualTo(NOW);
    assertThat(warning.coveredDistricts()).containsOnly(District.RATNAPURA, District.KALUTARA);
    verify(warnings).save(warning);
    assertThat(outcome.warning()).isSameAs(warning);
    assertThat(outcome.summary()).isSameAs(summary);
  }

  @Test
  @DisplayName("E6 13d–13e — a second warning is refused and nothing is broadcast")
  void refusesASecondActiveWarning() {
    Warning active = storedWarning(NOW.plusHours(20));
    when(warnings.findActiveByHazard(HAZARD_ID, NOW)).thenReturn(Optional.of(active));

    assertThatThrownBy(() -> controller.issueWarning(HAZARD_ID, scenarioDetails(), OFFICER_ID))
        .isInstanceOfSatisfying(
            ActiveWarningExistsException.class,
            refusal -> assertThat(refusal.getWarning()).isSameAs(active));
    verifyNoInteractions(broadcaster, idGenerator);
    verify(warnings, never()).save(any());
  }

  @Test
  @DisplayName("E3 13g — issueWarning with unvalidated details is a programming error")
  void issueWarningNeedsValidatedDetails() {
    WarningDetails unvalidated =
        new WarningDetails(
            WarningLevel.LEVEL_2_AMBER,
            null,
            SampleEntities.TEXT,
            List.of(District.RATNAPURA),
            List.of(),
            Set.of(NotificationChannel.SMS),
            NOW.plusDays(1));

    assertThatIllegalStateException()
        .isThrownBy(() -> controller.issueWarning(HAZARD_ID, unvalidated, OFFICER_ID));
    assertThatIllegalStateException()
        .isThrownBy(
            () -> controller.issueWarning(HAZARD_ID, details(List.of(), List.of()), OFFICER_ID));
    verifyNoInteractions(broadcaster);
  }

  @Test
  @DisplayName("E1 16d — a resend goes to the broadcaster with the stored warning")
  void resendsOnTheChosenChannel() {
    Warning warning = storedWarning(NOW.plusHours(20));
    BroadcastSummary summary = new BroadcastSummary(1240, Map.of(), List.of());
    when(warnings.findById("WRN-2026-0081")).thenReturn(Optional.of(warning));
    when(broadcaster.resend(warning, NotificationChannel.SMS)).thenReturn(summary);

    assertThat(controller.resendOnChannel("WRN-2026-0081", NotificationChannel.SMS))
        .isSameAs(summary);
  }

  @Test
  @DisplayName("Main step 19 — the dispatch status is retrieved from the recorded notifications")
  void summarisesTheRecordedNotifications() {
    Warning warning =
        new Warning(
            "WRN-2026-0087",
            completeHazard(),
            scenarioDetails(),
            officer,
            NOW,
            List.of(
                new Notification(
                    RecipientKind.CITIZEN,
                    "CIT-KLT-0002",
                    NotificationChannel.SMS,
                    "t",
                    1,
                    DeliveryStatus.FAILED,
                    NOW),
                new Notification(
                    RecipientKind.ORGANIZATION,
                    "ORG-0001",
                    NotificationChannel.SMS,
                    "t",
                    1,
                    DeliveryStatus.DELIVERED,
                    NOW)));
    when(warnings.findById("WRN-2026-0087")).thenReturn(Optional.of(warning));

    BroadcastSummary summary = controller.getBroadcastSummary("WRN-2026-0087");

    assertThat(summary.deliveredCount()).isEqualTo(1);
    assertThat(summary.failedRecipientIds())
        .isEqualTo(Map.of(NotificationChannel.SMS, List.of("CIT-KLT-0002")));
  }

  @Test
  @DisplayName("Main step 15 — the app reads the warnings active now in its district")
  void readsTheActiveWarningsOfADistrict() {
    Warning active = storedWarning(NOW.plusHours(1));
    Warning expired = storedWarning(NOW);
    when(warnings.findActiveByDistrict(District.RATNAPURA, NOW))
        .thenReturn(List.of(active, expired));

    assertThat(controller.getActiveWarnings(District.RATNAPURA)).containsExactly(active);
  }

  @Test
  @DisplayName("Main step 13 — issueWarning needs an existing officer")
  void issueWarningNeedsTheOfficer() {
    when(warnings.findActiveByHazard(anyString(), any())).thenReturn(Optional.empty());
    when(hazards.findById(HAZARD_ID)).thenReturn(Optional.of(completeHazard()));
    when(officers.findById(OFFICER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.issueWarning(HAZARD_ID, scenarioDetails(), OFFICER_ID))
        .isInstanceOf(NoSuchElementException.class);
    verifyNoInteractions(broadcaster);
  }
}
