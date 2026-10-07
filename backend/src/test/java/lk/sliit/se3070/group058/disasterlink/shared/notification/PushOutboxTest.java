package lk.sliit.se3070.group058.disasterlink.shared.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link PushOutbox}, its factories and its mapper. */
class PushOutboxTest {

  private static final LocalDateTime CREATED = LocalDateTime.of(2026, 5, 21, 8, 30);
  private static final LocalDateTime DECIDED = LocalDateTime.of(2026, 5, 21, 8, 0);

  @Test
  @DisplayName("forWarningAlert fills the alert fields and leaves the decision fields empty")
  void warningAlertFactory() {
    PushOutbox entry =
        PushOutbox.forWarningAlert("CIT-1", "WRN-1", WarningLevel.LEVEL_3_RED, "Evacuate", CREATED);

    assertThat(entry.getId()).hasSize(36);
    assertThat(entry.getRecipientId()).isEqualTo("CIT-1");
    assertThat(entry.getKind()).isEqualTo(PushKind.WARNING_ALERT);
    assertThat(entry.getWarningId()).contains("WRN-1");
    assertThat(entry.getWarningLevel()).contains(WarningLevel.LEVEL_3_RED);
    assertThat(entry.getText()).isEqualTo("Evacuate");
    assertThat(entry.getCreatedAt()).isEqualTo(CREATED);
    assertThat(entry.getReportId()).isEmpty();
    assertThat(entry.getStatus()).isEmpty();
    assertThat(entry.getReason()).isEmpty();
    assertThat(entry.getDecidedAt()).isEmpty();
    assertThat(entry.getDeliveredAt()).isEmpty();
  }

  @Test
  @DisplayName("forReportDecision fills the decision fields and leaves the alert fields empty")
  void reportDecisionFactory() {
    ReportDecisionNotice notice =
        new ReportDecisionNotice("RPT-1", ReportStatus.REJECTED, "Not a flood", DECIDED);

    PushOutbox entry = PushOutbox.forReportDecision("CIT-1", notice, "Sorry", CREATED);

    assertThat(entry.getKind()).isEqualTo(PushKind.REPORT_DECISION);
    assertThat(entry.getReportId()).contains("RPT-1");
    assertThat(entry.getStatus()).contains(ReportStatus.REJECTED);
    assertThat(entry.getReason()).contains("Not a flood");
    assertThat(entry.getDecidedAt()).contains(DECIDED);
    assertThat(entry.getText()).isEqualTo("Sorry");
    assertThat(entry.getWarningId()).isEmpty();
    assertThat(entry.getWarningLevel()).isEmpty();
  }

  @Test
  @DisplayName("The factories reject null arguments")
  void factoriesRejectNull() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> PushOutbox.forWarningAlert(null, "W", WarningLevel.LEVEL_1_YELLOW, "t", CREATED));
    assertThatNullPointerException()
        .isThrownBy(
            () -> PushOutbox.forWarningAlert("C", null, WarningLevel.LEVEL_1_YELLOW, "t", CREATED));
    assertThatNullPointerException()
        .isThrownBy(() -> PushOutbox.forWarningAlert("C", "W", null, "t", CREATED));
    assertThatNullPointerException()
        .isThrownBy(
            () -> PushOutbox.forWarningAlert("C", "W", WarningLevel.LEVEL_1_YELLOW, null, CREATED));
    assertThatNullPointerException()
        .isThrownBy(
            () -> PushOutbox.forWarningAlert("C", "W", WarningLevel.LEVEL_1_YELLOW, "t", null));
    assertThatNullPointerException()
        .isThrownBy(() -> PushOutbox.forReportDecision("C", null, "t", CREATED));
  }

  @Test
  @DisplayName("markDelivered sets the time once; a second call keeps the first")
  void markDeliveredKeepsTheFirstTime() {
    PushOutbox entry =
        PushOutbox.forWarningAlert("CIT-1", "WRN-1", WarningLevel.LEVEL_2_AMBER, "t", CREATED);

    entry.markDelivered(CREATED.plusMinutes(1));
    entry.markDelivered(CREATED.plusMinutes(9));

    assertThat(entry.getDeliveredAt()).contains(CREATED.plusMinutes(1));
    assertThatNullPointerException().isThrownBy(() -> entry.markDelivered(null));
  }

  @Test
  @DisplayName("Entries are equal when their identifiers are equal")
  void identity() {
    PushOutbox first =
        PushOutbox.forWarningAlert("C", "W", WarningLevel.LEVEL_1_YELLOW, "t", CREATED);
    PushOutbox second =
        PushOutbox.forWarningAlert("C", "W", WarningLevel.LEVEL_1_YELLOW, "t", CREATED);

    assertThat(first.equals(first)).isTrue();
    assertThat(first).isNotEqualTo(second).hasToString("PushOutbox[" + first.getId() + "]");
    assertThat(first.equals("x")).isFalse();
    assertThat(first.hashCode()).isEqualTo(first.getId().hashCode());
  }

  @Test
  @DisplayName("The mapper adds the +05:30 offset and drops nothing it should keep")
  void mapperAddsTheOffset() {
    ReportDecisionNotice notice =
        new ReportDecisionNotice("RPT-1", ReportStatus.VERIFIED, null, DECIDED);

    PushEntryResponse alert =
        PushEntryMapper.toResponse(
            PushOutbox.forWarningAlert("C", "W", WarningLevel.LEVEL_1_YELLOW, "t", CREATED));
    PushEntryResponse decision =
        PushEntryMapper.toResponse(PushOutbox.forReportDecision("C", notice, "ok", CREATED));

    assertThat(alert.createdAt().toString()).isEqualTo("2026-05-21T08:30+05:30");
    assertThat(alert.decidedAt()).isNull();
    assertThat(alert.warningId()).isEqualTo("W");
    assertThat(decision.decidedAt().toString()).isEqualTo("2026-05-21T08:00+05:30");
    assertThat(decision.reportId()).isEqualTo("RPT-1");
    assertThat(decision.reason()).isNull();
  }
}
