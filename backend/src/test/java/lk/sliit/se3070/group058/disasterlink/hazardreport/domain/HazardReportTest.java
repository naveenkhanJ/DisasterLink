package lk.sliit.se3070.group058.disasterlink.hazardreport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDateTime;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link HazardReport} and of the value types that live beside it. */
class HazardReportTest {

  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 8, 0);
  private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 5, 21, 8, 5);
  private static final Citizen REPORTER = SampleEntities.citizen("CIT-KLT-0001", "token");
  private static final GeoPoint LOCATION = new GeoPoint(6.5888, 79.9640, 9.5);

  private static HazardReport pendingReport() {
    ReportDraft draft =
        new ReportDraft(
            "draft-1",
            HazardType.LANDSLIDE,
            "Slip on the road",
            LOCATION,
            false,
            District.KALUTARA,
            CAPTURED);
    return new HazardReport("RPT-2026-KLT-1000", REPORTER, draft, "photo-1.jpg", RECEIVED);
  }

  @Test
  @DisplayName("The drawn constructor reads the draft and starts the report as pending")
  void drawnConstructorReadsTheDraft() {
    HazardReport report = pendingReport();

    assertThat(report.getReportId()).isEqualTo("RPT-2026-KLT-1000");
    assertThat(report.getReporter()).isSameAs(REPORTER);
    assertThat(report.getHazardType()).isEqualTo(HazardType.LANDSLIDE);
    assertThat(report.getDescription()).isEqualTo("Slip on the road");
    assertThat(report.getLocation()).isEqualTo(LOCATION);
    assertThat(report.isLocationFromGps()).isFalse();
    assertThat(report.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(report.getCapturedAt()).isEqualTo(CAPTURED);
    assertThat(report.getReportTime()).isEqualTo(RECEIVED);
    assertThat(report.getPhoto()).isEqualTo("photo-1.jpg");
    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(report.getVerificationTime()).isEmpty();
    assertThat(report.getRejectionReason()).isEmpty();
    assertThat(report.getReviewer()).isEmpty();
    assertThat(report.getHazard()).isEmpty();
  }

  @Test
  @DisplayName("The seed constructor keeps a decided report with its reviewer and hazard")
  void seedConstructorKeepsTheStoredState() {
    var officer = SampleEntities.dutyOfficer("DMC-9");
    var hazard = SampleEntities.hazard("HZ-2026-0142");

    HazardReport report =
        new HazardReport(
            "RPT-1",
            REPORTER,
            HazardType.FLOOD,
            "d",
            "p.jpg",
            LOCATION,
            true,
            District.COLOMBO,
            CAPTURED,
            RECEIVED,
            ReportStatus.REJECTED,
            RECEIVED.plusHours(1),
            "Not a flood",
            officer,
            hazard);

    assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(report.getVerificationTime()).contains(RECEIVED.plusHours(1));
    assertThat(report.getRejectionReason()).contains("Not a flood");
    assertThat(report.getReviewer()).containsSame(officer);
    assertThat(report.getHazard()).containsSame(hazard);
    assertThat(report.isLocationFromGps()).isTrue();
  }

  @Test
  @DisplayName("The constructors reject a null required argument")
  void constructorsRejectNull() {
    ReportDraft draft =
        new ReportDraft("d", HazardType.FLOOD, "x", LOCATION, true, District.GALLE, CAPTURED);

    assertThatNullPointerException()
        .isThrownBy(() -> new HazardReport(null, REPORTER, draft, "p", RECEIVED));
    assertThatNullPointerException()
        .isThrownBy(() -> new HazardReport("r", null, draft, "p", RECEIVED));
    assertThatNullPointerException()
        .isThrownBy(() -> new HazardReport("r", REPORTER, null, "p", RECEIVED));
    assertThatNullPointerException()
        .isThrownBy(() -> new HazardReport("r", REPORTER, draft, null, RECEIVED));
    assertThatNullPointerException()
        .isThrownBy(() -> new HazardReport("r", REPORTER, draft, "p", null));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new HazardReport(
                    "r",
                    REPORTER,
                    null,
                    "d",
                    "p",
                    LOCATION,
                    true,
                    District.GALLE,
                    CAPTURED,
                    RECEIVED,
                    ReportStatus.VERIFIED,
                    null,
                    null,
                    null,
                    null));
  }

  @Test
  @DisplayName("Reports are equal when their identifiers are equal")
  void equalsFollowsTheIdentifier() {
    HazardReport report = pendingReport();

    assertThat(report).isEqualTo(pendingReport()).hasSameHashCodeAs(pendingReport());
    assertThat(report)
        .isNotEqualTo(
            FeatureFixtures.report(
                "RPT-2", REPORTER, LOCATION, CAPTURED, RECEIVED, ReportStatus.VERIFIED));
    assertThat(report.equals(report)).isTrue();
    assertThat(report.equals("x")).isFalse();
    assertThat(report).hasToString("HazardReport[RPT-2026-KLT-1000]");
  }

  @Test
  @DisplayName("ReportReceipt keeps the result and the optional report")
  void reportReceipt() {
    SyncResult rejected =
        new SyncResult(
            "draft-1", SyncOutcome.REJECTED, RejectionReason.VALIDATION_FAILED, "photo: missing");

    ReportReceipt withoutReport = new ReportReceipt(rejected, null);
    ReportReceipt withReport = new ReportReceipt(rejected, pendingReport());

    assertThat(withoutReport.report()).isNull();
    assertThat(withReport.report()).isNotNull();
    assertThat(withReport.result()).isSameAs(rejected);
    assertThatNullPointerException().isThrownBy(() -> new ReportReceipt(null, null));
  }

  @Test
  @DisplayName("VerificationOutcome keeps the report and the optional active warning")
  void verificationOutcome() {
    VerificationOutcome outcome = new VerificationOutcome(pendingReport(), null);

    assertThat(outcome.activeWarning()).isNull();
    assertThat(outcome.report().getReportId()).isEqualTo("RPT-2026-KLT-1000");
    assertThatNullPointerException().isThrownBy(() -> new VerificationOutcome(null, null));
  }

  @Test
  @DisplayName("NearbyReport keeps the report and its distance")
  void nearbyReport() {
    NearbyReport nearby = new NearbyReport(pendingReport(), 1.2);

    assertThat(nearby.distanceKm()).isEqualTo(1.2);
    assertThat(nearby.report().getDistrict()).isEqualTo(District.KALUTARA);
    assertThatNullPointerException().isThrownBy(() -> new NearbyReport(null, 1.0));
  }

  @Test
  @DisplayName("A report needs the draft fields validateReport checks (P155 X1)")
  void constructorRequiresValidatedDraft() {
    ReportDraft noType =
        new ReportDraft("d", null, "text", LOCATION, false, District.KALUTARA, CAPTURED);
    ReportDraft noText =
        new ReportDraft("d", HazardType.FLOOD, null, LOCATION, false, District.KALUTARA, CAPTURED);
    ReportDraft noLocation =
        new ReportDraft("d", HazardType.FLOOD, "text", null, false, District.KALUTARA, CAPTURED);
    ReportDraft noDistrict =
        new ReportDraft("d", HazardType.FLOOD, "text", LOCATION, false, null, CAPTURED);

    for (ReportDraft draft : List.of(noType, noText, noLocation, noDistrict)) {
      assertThatNullPointerException()
          .isThrownBy(() -> new HazardReport("RPT-1", REPORTER, draft, "p.jpg", RECEIVED));
    }
  }
}
