package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AlertTimelineEntry;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistributionLine;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFile;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFormat;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyCategory;
import lk.sliit.se3070.group058.disasterlink.shared.domain.TimelineEvent;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PdfReportExporter}.
 *
 * <p>Implements tests for CH-32, OpenPDF 3, and SC4 step 13.
 */
class PdfReportExporterTest {

  private PdfReportExporter exporter;
  private DisasterReport sampleReport;

  @BeforeEach
  void setUp() {
    exporter = new PdfReportExporter();
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(
                ReportMetric.ALERT_TIMELINE,
                ReportMetric.CITIZENS_REACHED,
                ReportMetric.SHELTER_OCCUPANCY_OVER_TIME,
                ReportMetric.RESOURCE_DISTRIBUTION),
            "HZ-2026-0142");

    AlertTimelineEntry timelineEntry =
        new AlertTimelineEntry(
            "WRN-87",
            LocalDateTime.of(2026, 5, 20, 10, 45),
            WarningLevel.LEVEL_2_AMBER,
            Set.of(District.RATNAPURA),
            TimelineEvent.ISSUED);

    OccupancyPoint occupancyPoint =
        new OccupancyPoint(
            "SHL-1", District.RATNAPURA, LocalDateTime.of(2026, 5, 21, 12, 0), 5920, 6300);

    DistributionLine distLine =
        new DistributionLine(
            District.RATNAPURA,
            "ORG-1",
            "CARE Sri Lanka",
            SupplyCategory.FOOD,
            "dry-ration packs",
            1200.0);

    ReportResults results =
        new ReportResults(
            15,
            List.of(timelineEntry),
            Map.of(NotificationChannel.PUSH, 30100, NotificationChannel.SMS, 42335),
            42335,
            List.of(occupancyPoint),
            6300,
            List.of(distLine),
            Set.of());

    DMCOfficer officer =
        new DMCOfficer("OFF-1", "A. Perera", "0771234567", "officer@example.lk", "HQ", Set.of());
    sampleReport =
        new DisasterReport(
            "DR-2026-0184",
            "flood-response_2026-05",
            LocalDateTime.of(2026, 5, 26, 9, 30),
            criteria,
            results,
            officer,
            List.of());
    sampleReport.recordFindings("Response operations met critical targets.");
  }

  @Test
  @DisplayName("supports() identifies only PDF format")
  void supportsOnlyPdf() {
    assertThat(exporter.supports(ExportFormat.PDF)).isTrue();
    assertThat(exporter.supports(ExportFormat.EXCEL)).isFalse();
    assertThat(exporter.supports(ExportFormat.CSV)).isFalse();
  }

  @Test
  @DisplayName("export() rejects null report")
  void exportRejectsNull() {
    assertThatThrownBy(() -> exporter.export(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("SC4 step 13 — exports report into tabular PDF document")
  void exportsPdfDocument() {
    ExportFile file = exporter.export(sampleReport);

    assertThat(file.format()).isEqualTo(ExportFormat.PDF);
    assertThat(file.fileName()).isEqualTo("DR-2026-0184_flood-response_2026-05.pdf");
    assertThat(file.content()).isNotEmpty();

    // Verify PDF header magic bytes %PDF-
    String header = new String(file.content(), 0, 5, StandardCharsets.US_ASCII);
    assertThat(header).isEqualTo("%PDF-");
  }
}
