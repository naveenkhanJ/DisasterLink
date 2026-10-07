package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the input value records (MD section 0.13, P155 X1): an unentered field reaches the
 * control as {@code null}, a {@code null} collection is empty, and the structural fields stay
 * required.
 */
class InputValueRecordsTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 8, 15);

  @Test
  @DisplayName("WarningDetails with nothing but the level entered has null fields and empty lists")
  void warningDetailsNothingEntered() {
    WarningDetails details =
        new WarningDetails(WarningLevel.LEVEL_2_AMBER, null, null, null, null, null, null);

    assertThat(details.warningLevel()).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    assertThat(details.message()).isNull();
    assertThat(details.instructions()).isNull();
    assertThat(details.expiryTime()).isNull();
    assertThat(details.targetDistricts()).isEmpty();
    assertThat(details.targetRiverBasins()).isEmpty();
    assertThat(details.channels()).isEmpty();
  }

  @Test
  @DisplayName("A missing warning level is refused: it is a shape field (standards section 12.2)")
  void warningLevelIsRequired() {
    assertThatNullPointerException()
        .isThrownBy(() -> new WarningDetails(null, null, null, null, null, null, null))
        .withMessageContaining("warningLevel");
  }

  @Test
  @DisplayName("HazardUpdate and NewHazardDetails with nothing entered have empty district lists")
  void hazardRecordsNothingEntered() {
    assertThat(new HazardUpdate(null, null, null).districts()).isEmpty();
    NewHazardDetails details = new NewHazardDetails(null, null, null, null);

    assertThat(details.districts()).isEmpty();
    assertThat(details.severity()).isNull();
    assertThat(details.locationName()).isNull();
    assertThat(details.description()).isNull();
  }

  @Test
  @DisplayName("EscalationDetails and ShelterDetails accept every unentered field")
  void escalationAndShelterNothingEntered() {
    EscalationDetails escalation = new EscalationDetails(null, null, null, null);
    ShelterDetails shelter = new ShelterDetails(null, null, null, 0, null, null);

    assertThat(escalation.newLevel()).isNull();
    assertThat(shelter.name()).isNull();
    assertThat(shelter.district()).isNull();
    assertThat(shelter.contactNumber()).isNull();
  }

  @Test
  @DisplayName("ReportCriteria with nothing entered has empty sets and no period")
  void criteriaNothingEntered() {
    ReportCriteria criteria = new ReportCriteria(null, null, null, null, null, null);

    assertThat(criteria.districts()).isEmpty();
    assertThat(criteria.metrics()).isEmpty();
    assertThat(criteria.periodStart()).isNull();
    assertThat(criteria.periodEnd()).isNull();
  }

  @Test
  @DisplayName("ReportDraft keeps draftId and capturedAt required but not the typed fields")
  void draftStructuralFieldsStayRequired() {
    ReportDraft draft = new ReportDraft("d-1", null, null, null, false, null, NOW);

    assertThat(draft.hazardType()).isNull();
    assertThat(draft.location()).isNull();
    assertThatNullPointerException()
        .isThrownBy(() -> new ReportDraft(null, null, null, null, false, null, NOW));
    assertThatNullPointerException()
        .isThrownBy(() -> new ReportDraft("d-1", null, null, null, false, null, null));
  }

  @Test
  @DisplayName("A non-null collection is still copied, so a later change of the source is not seen")
  void collectionsAreStillCopied() {
    java.util.List<District> source = new java.util.ArrayList<>(java.util.List.of(District.GALLE));

    HazardUpdate update = new HazardUpdate("d", HazardSeverity.LOW, source);
    source.add(District.COLOMBO);

    assertThat(update.districts()).containsExactly(District.GALLE);
  }
}
