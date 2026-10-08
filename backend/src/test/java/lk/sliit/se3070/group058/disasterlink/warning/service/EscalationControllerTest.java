package lk.sliit.se3070.group058.disasterlink.warning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditAction;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationOptions;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.IssueOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

/** Tests of {@link EscalationController} against the flows of SC Escalate Warning. */
class EscalationControllerTest {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

  private static final LocalDateTime NOW = SampleEntities.NOW;

  private static final String WARNING_ID = "WRN-2026-0087";

  private static final String OFFICER_ID = "DMC-0002";

  private static final LocalisedText EVACUATE =
      new LocalisedText("ඉවත් වන්න", "வெளியேறவும்", "Evacuate to the nearest shelter");

  private final WarningRepository warnings = mock(WarningRepository.class);

  private final DMCOfficerRepository officers = mock(DMCOfficerRepository.class);

  private final CitizenRepository citizens = mock(CitizenRepository.class);

  private final OrganizationRepository organizations = mock(OrganizationRepository.class);

  private final DistrictOfficerRepository districtOfficers = mock(DistrictOfficerRepository.class);

  private final AuditRecordRepository auditRecords = mock(AuditRecordRepository.class);

  private final WarningBroadcaster broadcaster = mock(WarningBroadcaster.class);

  private final WarningTextValidator textValidator = mock(WarningTextValidator.class);

  private final DMCOfficer officer = SampleEntities.officer(OFFICER_ID, Set.of());

  private EscalationController controller;

  @BeforeEach
  void setUp() {
    controller =
        new EscalationController(
            warnings,
            officers,
            citizens,
            organizations,
            districtOfficers,
            auditRecords,
            broadcaster,
            textValidator,
            Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO));
    when(textValidator.validate(any(), any(), any(), any())).thenReturn(List.of());
  }

  /** A stored Kalu Ganga warning (Ratnapura and the Kalu basin) at a level, expiring at a time. */
  private Warning storedWarning(WarningLevel level, LocalDateTime expiryTime) {
    WarningDetails stored =
        new WarningDetails(
            level,
            SampleEntities.TEXT,
            SampleEntities.TEXT,
            List.of(District.RATNAPURA),
            List.of(RiverBasin.KALU),
            Set.of(NotificationChannel.PUSH, NotificationChannel.SMS),
            expiryTime);
    Warning warning =
        new Warning(
            WARNING_ID, SampleEntities.hazard("HZ-2026-0142"), stored, officer, NOW.minusDays(1));
    when(warnings.findById(WARNING_ID)).thenReturn(Optional.of(warning));
    return warning;
  }

  private Warning activeAmberWarning() {
    return storedWarning(WarningLevel.LEVEL_2_AMBER, NOW.plusHours(2));
  }

  private static EscalationDetails toRed() {
    return new EscalationDetails(WarningLevel.LEVEL_3_RED, EVACUATE, EVACUATE, NOW.plusDays(1));
  }

  private static BroadcastSummary deliveredTo(int count) {
    return new BroadcastSummary(count, Map.of(), List.of());
  }

  @Test
  @DisplayName("Escalate Main step 4 — an Amber warning is offered Level 3 – Red only")
  void offersTheLevelsAboveAmber() {
    Warning warning = activeAmberWarning();

    EscalationOptions options = controller.getEscalationOptions(WARNING_ID);

    assertThat(options.warning()).isSameAs(warning);
    assertThat(options.allowedLevels()).containsExactly(WarningLevel.LEVEL_3_RED);
  }

  @Test
  @DisplayName("Escalate A4 — a Yellow warning is offered both Level 2 – Amber and Level 3 – Red")
  void offersBothHigherLevelsToYellow() {
    storedWarning(WarningLevel.LEVEL_1_YELLOW, NOW.plusHours(2));

    EscalationOptions options = controller.getEscalationOptions(WARNING_ID);

    assertThat(options.allowedLevels())
        .containsExactly(WarningLevel.LEVEL_2_AMBER, WarningLevel.LEVEL_3_RED);
  }

  @Test
  @DisplayName("Escalate A3 4a — a Red warning has no higher level")
  void offersNoLevelAboveRed() {
    storedWarning(WarningLevel.LEVEL_3_RED, NOW.plusHours(2));

    assertThat(controller.getEscalationOptions(WARNING_ID).allowedLevels()).isEmpty();
  }

  @Test
  @DisplayName("Escalate E2 4c–4d — an expired warning cannot be escalated")
  void refusesTheOptionsOfAnExpiredWarning() {
    Warning warning = storedWarning(WarningLevel.LEVEL_2_AMBER, NOW);

    assertThatThrownBy(() -> controller.getEscalationOptions(WARNING_ID))
        .isInstanceOf(WarningNotEscalatableException.class)
        .satisfies(
            thrown ->
                assertThat(((WarningNotEscalatableException) thrown).getWarning())
                    .isSameAs(warning));
  }

  @Test
  @DisplayName("Escalate Main step 3 — an unknown warning is not found")
  void refusesAnUnknownWarning() {
    when(warnings.findById(WARNING_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getEscalationOptions(WARNING_ID))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("Escalate Main step 8 — Level 3 – Red with valid texts passes validation")
  void acceptsValidDetails() {
    Warning warning = activeAmberWarning();

    ValidationResult result = controller.validateEscalation(WARNING_ID, toRed());

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).isEmpty();
    verify(textValidator).validate(EVACUATE, EVACUATE, warning.getChannels(), toRed().expiryTime());
  }

  static Stream<Arguments> levelsNotAboveAmber() {
    return Stream.of(
        Arguments.of(WarningLevel.LEVEL_2_AMBER),
        Arguments.of(WarningLevel.LEVEL_1_YELLOW),
        Arguments.of((WarningLevel) null));
  }

  @ParameterizedTest(name = "new level {0}")
  @MethodSource("levelsNotAboveAmber")
  @DisplayName("Escalate E3 8a — a new level that is not above the current one is refused")
  void refusesALevelNotAbove(WarningLevel newLevel) {
    activeAmberWarning();
    EscalationDetails details = new EscalationDetails(newLevel, EVACUATE, EVACUATE, NOW);

    ValidationResult result = controller.validateEscalation(WARNING_ID, details);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors())
        .containsExactly("The new level must be above the warning's current level");
  }

  @Test
  @DisplayName("Escalate E3 8a — the text rule errors are returned beside the form")
  void returnsTheTextRuleErrors() {
    activeAmberWarning();
    when(textValidator.validate(any(), any(), any(), any()))
        .thenReturn(List.of("Tamil SMS is too long"));

    ValidationResult result = controller.validateEscalation(WARNING_ID, toRed());

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).containsExactly("Tamil SMS is too long");
  }

  @Test
  @DisplayName("Escalate E3 10g — at confirm, a level no longer above the current one is refused")
  void refusesAtConfirmALevelOvertaken() {
    storedWarning(WarningLevel.LEVEL_3_RED, NOW.plusHours(2));

    ValidationResult result = controller.validateEscalation(WARNING_ID, toRed());

    assertThat(result.valid()).isFalse();
  }

  @Test
  @DisplayName("Escalate Main step 8 — the preview counts the citizens, organisations and officers")
  void previewsTheReBroadcast() {
    activeAmberWarning();
    Citizen ratnapura = SampleEntities.citizen("CIT-RAT-0001", "token");
    when(citizens.findByDistrictIn(any())).thenReturn(List.of(ratnapura));
    when(organizations.findByDistrictIn(any()))
        .thenReturn(List.of(SampleEntities.organization("ORG-0001")));
    when(districtOfficers.findByDistrictIn(any()))
        .thenReturn(
            List.of(
                SampleEntities.districtOfficer("DO-RAT-01"),
                SampleEntities.districtOfficer("DO-KLT-01")));

    WarningPreview preview = controller.previewEscalation(WARNING_ID, toRed());

    assertThat(preview.hazardType()).isEqualTo(HazardType.FLOOD);
    assertThat(preview.warningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(preview.coveredDistricts())
        .containsExactlyInAnyOrder(District.RATNAPURA, District.KALUTARA);
    assertThat(preview.message()).isEqualTo(EVACUATE);
    assertThat(preview.expiryTime()).isEqualTo(NOW.plusDays(1));
    assertThat(preview.citizensPerDistrict().values().stream().mapToInt(Integer::intValue).sum())
        .isEqualTo(1);
    assertThat(preview.organizationCount()).isEqualTo(1);
    assertThat(preview.districtOfficerCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("Escalate Main steps 10–12 — the warning is escalated, audited and re-broadcast")
  void escalatesAuditsAndReBroadcasts() {
    Warning warning = activeAmberWarning();
    when(officers.findById(OFFICER_ID)).thenReturn(Optional.of(officer));
    BroadcastSummary summary = deliveredTo(7);
    when(broadcaster.broadcastEscalation(warning)).thenReturn(summary);

    IssueOutcome outcome = controller.escalateWarning(WARNING_ID, toRed(), OFFICER_ID);

    assertThat(outcome.warning()).isSameAs(warning);
    assertThat(outcome.summary()).isSameAs(summary);
    assertThat(warning.getWarningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(warning.getMessage()).isEqualTo(EVACUATE);
    assertThat(warning.getExpiryTime()).isEqualTo(NOW.plusDays(1));
    ArgumentCaptor<AuditRecord> audit = ArgumentCaptor.forClass(AuditRecord.class);
    verify(auditRecords).save(audit.capture());
    assertThat(audit.getValue().getAction()).isEqualTo(AuditAction.WARNING_ESCALATED);
    assertThat(audit.getValue().getSubjectId()).isEqualTo(WARNING_ID);
    assertThat(audit.getValue().getPerformedBy()).isSameAs(officer);
    assertThat(audit.getValue().getPerformedAt()).isEqualTo(NOW);
    assertThat(audit.getValue().getDetail()).contains("LEVEL_2_AMBER→LEVEL_3_RED");
  }

  @Test
  @DisplayName("Escalate E2 10a — a warning that expired before the confirm is not escalated")
  void refusesToEscalateAnExpiredWarning() {
    Warning warning = storedWarning(WarningLevel.LEVEL_2_AMBER, NOW.minusMinutes(1));

    assertThatThrownBy(() -> controller.escalateWarning(WARNING_ID, toRed(), OFFICER_ID))
        .isInstanceOf(WarningNotEscalatableException.class);

    assertThat(warning.getWarningLevel()).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    verifyNoInteractions(auditRecords, broadcaster);
  }

  @Test
  @DisplayName("Escalate E2 10a — a warning already at the chosen level is not escalated again")
  void refusesToEscalateAWarningAlreadyAtTheLevel() {
    storedWarning(WarningLevel.LEVEL_3_RED, NOW.plusHours(2));

    assertThatThrownBy(() -> controller.escalateWarning(WARNING_ID, toRed(), OFFICER_ID))
        .isInstanceOf(WarningNotEscalatableException.class);

    verifyNoInteractions(auditRecords, broadcaster);
  }

  @Test
  @DisplayName("Escalate Main step 10 — escalateWarning needs an existing officer")
  void refusesAnUnknownOfficer() {
    activeAmberWarning();
    when(officers.findById(OFFICER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.escalateWarning(WARNING_ID, toRed(), OFFICER_ID))
        .isInstanceOf(NoSuchElementException.class);

    verifyNoInteractions(auditRecords, broadcaster);
  }

  static Stream<Arguments> unvalidatedDetails() {
    return Stream.of(
        Arguments.of(new EscalationDetails(null, EVACUATE, EVACUATE, NOW.plusDays(1))),
        Arguments.of(
            new EscalationDetails(WarningLevel.LEVEL_3_RED, null, EVACUATE, NOW.plusDays(1))),
        Arguments.of(
            new EscalationDetails(WarningLevel.LEVEL_3_RED, EVACUATE, null, NOW.plusDays(1))),
        Arguments.of(new EscalationDetails(WarningLevel.LEVEL_3_RED, EVACUATE, EVACUATE, null)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unvalidatedDetails")
  @DisplayName("Escalate E3 10g — escalateWarning with unvalidated details is a programming error")
  void refusesUnvalidatedDetails(EscalationDetails details) {
    assertThatIllegalStateException()
        .isThrownBy(() -> controller.escalateWarning(WARNING_ID, details, OFFICER_ID));

    verifyNoInteractions(warnings, auditRecords, broadcaster);
  }
}
