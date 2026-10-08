package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
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
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExcelReportExporter}.
 *
 * <p>Implements tests for CH-32, FQ2, FE-25 and SC4 step 13.
 */
class ExcelReportExporterTest {

  private ExcelReportExporter exporter;
  private DisasterReport sampleReport;

  @BeforeEach
  void setUp() {
    exporter = new ExcelReportExporter();
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
  @DisplayName("supports() identifies only EXCEL format")
  void supportsOnlyExcel() {
    assertThat(exporter.supports(ExportFormat.EXCEL)).isTrue();
    assertThat(exporter.supports(ExportFormat.PDF)).isFalse();
    assertThat(exporter.supports(ExportFormat.CSV)).isFalse();
  }

  @Test
  @DisplayName("export() rejects null report")
  void exportRejectsNull() {
    assertThatThrownBy(() -> exporter.export(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("SC4 step 13 / FQ2 — exports report into multi-sheet Excel workbook")
  void exportsExcelWorkbook() throws Exception {
    ExportFile file = exporter.export(sampleReport);

    assertThat(file.format()).isEqualTo(ExportFormat.EXCEL);
    assertThat(file.fileName()).isEqualTo("DR-2026-0184_flood-response_2026-05.xlsx");
    assertThat(file.content()).isNotEmpty();

    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
      Sheet summarySheet = workbook.getSheet("Report Summary");
      assertThat(summarySheet).isNotNull();
      assertThat(summarySheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("DR-2026-0184");

      Sheet timelineSheet = workbook.getSheet("Alert Timeline");
      assertThat(timelineSheet).isNotNull();
      assertThat(timelineSheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("WRN-87");

      Sheet citizensSheet = workbook.getSheet("Citizens Reached");
      assertThat(citizensSheet).isNotNull();
      assertThat(citizensSheet.getRow(1).getCell(0).getStringCellValue()).isNotEmpty();

      Sheet occupancySheet = workbook.getSheet("Shelter Occupancy");
      assertThat(occupancySheet).isNotNull();
      assertThat(occupancySheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("SHL-1");

      Sheet distSheet = workbook.getSheet("Resource Distribution");
      assertThat(distSheet).isNotNull();
      assertThat(distSheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("CARE Sri Lanka");
    }
  }
}
