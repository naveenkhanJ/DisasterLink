package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ExportFailedException;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AlertTimelineEntry;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistributionLine;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFile;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFormat;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation exporting reports to Microsoft Excel format (XLSX).
 *
 * <p>Implements CH-32. Uses Apache POI to render tabular sheets with styled headers, without
 * charts. Closed using try-with-resources.
 */
@Component
public class ExcelReportExporter implements ReportExporter {

  private static final Logger LOG = LoggerFactory.getLogger(ExcelReportExporter.class);
  private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  @Override
  public boolean supports(ExportFormat format) {
    return format == ExportFormat.EXCEL;
  }

  @Override
  public ExportFile export(DisasterReport report) {
    Objects.requireNonNull(report, "report");
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      CellStyle headerStyle = createHeaderStyle(workbook);

      createSummarySheet(workbook, report, headerStyle);
      createTimelineSheet(workbook, report.getResults(), headerStyle);
      createCitizensSheet(workbook, report.getResults(), headerStyle);
      createOccupancySheet(workbook, report.getResults(), headerStyle);
      createDistributionSheet(workbook, report.getResults(), headerStyle);

      workbook.write(out);
      String fileName = CsvReportExporter.buildFileName(report, "xlsx");
      LOG.info("Excel report exported successfully: {}", fileName);

      return new ExportFile(fileName, ExportFormat.EXCEL, out.toByteArray());
    } catch (Exception e) {
      LOG.error("Failed to export report {} to Excel", report.getReportId(), e);
      throw new ExportFailedException("Failed to export Excel report: " + e.getMessage(), e);
    }
  }

  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  private void createSummarySheet(Workbook workbook, DisasterReport report, CellStyle headerStyle) {
    Sheet sheet = workbook.createSheet("Report Summary");
    int rowNum = 0;

    Row r0 = sheet.createRow(rowNum++);
    r0.createCell(0).setCellValue("Report ID");
    r0.createCell(1).setCellValue(report.getReportId());

    Row r1 = sheet.createRow(rowNum++);
    r1.createCell(0).setCellValue("Title");
    r1.createCell(1).setCellValue(report.getReportTitle());

    Row r2 = sheet.createRow(rowNum++);
    r2.createCell(0).setCellValue("Generated Date");
    r2.createCell(1)
        .setCellValue(
            report.getGeneratedDate() != null
                ? report.getGeneratedDate().format(ISO_FORMATTER)
                : "");

    Row r3 = sheet.createRow(rowNum++);
    r3.createCell(0).setCellValue("Provisional");
    r3.createCell(1).setCellValue(String.valueOf(report.isProvisional()));

    if (report.getFindings().isPresent()) {
      Row r4 = sheet.createRow(rowNum++);
      r4.createCell(0).setCellValue("Findings");
      r4.createCell(1).setCellValue(report.getFindings().get());
    }

    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
  }

  private void createTimelineSheet(
      Workbook workbook, ReportResults results, CellStyle headerStyle) {
    Sheet sheet = workbook.createSheet("Alert Timeline");
    int rowNum = 0;

    Row header = sheet.createRow(rowNum++);
    String[] cols = {"Time", "Event", "Warning ID", "Level", "Districts"};
    for (int i = 0; i < cols.length; i++) {
      Cell c = header.createCell(i);
      c.setCellValue(cols[i]);
      c.setCellStyle(headerStyle);
    }

    for (AlertTimelineEntry entry : results.alertTimeline()) {
      Row r = sheet.createRow(rowNum++);
      r.createCell(0).setCellValue(entry.issuedTime().format(ISO_FORMATTER));
      r.createCell(1).setCellValue(entry.event().name());
      r.createCell(2).setCellValue(entry.warningId());
      r.createCell(3).setCellValue(entry.warningLevel().name());
      r.createCell(4).setCellValue(entry.districts().toString());
    }

    for (int i = 0; i < cols.length; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private void createCitizensSheet(
      Workbook workbook, ReportResults results, CellStyle headerStyle) {
    Sheet sheet = workbook.createSheet("Citizens Reached");
    int rowNum = 0;

    Row header = sheet.createRow(rowNum++);
    Cell c0 = header.createCell(0);
    c0.setCellValue("Channel");
    c0.setCellStyle(headerStyle);
    Cell c1 = header.createCell(1);
    c1.setCellValue("Citizens Reached");
    c1.setCellStyle(headerStyle);

    for (Map.Entry<NotificationChannel, Integer> entry :
        results.citizensReachedByChannel().entrySet()) {
      Row r = sheet.createRow(rowNum++);
      r.createCell(0).setCellValue(entry.getKey().name());
      r.createCell(1).setCellValue(entry.getValue());
    }

    Row totalRow = sheet.createRow(rowNum++);
    Cell tc0 = totalRow.createCell(0);
    tc0.setCellValue("Total (distinct)");
    tc0.setCellStyle(headerStyle);
    Cell tc1 = totalRow.createCell(1);
    tc1.setCellValue(results.citizensReachedTotal());
    tc1.setCellStyle(headerStyle);

    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
  }

  private void createOccupancySheet(
      Workbook workbook, ReportResults results, CellStyle headerStyle) {
    Sheet sheet = workbook.createSheet("Shelter Occupancy");
    int rowNum = 0;

    Row capRow = sheet.createRow(rowNum++);
    capRow.createCell(0).setCellValue("Combined Current Capacity");
    capRow.createCell(1).setCellValue(results.combinedCapacity());

    Row header = sheet.createRow(rowNum++);
    String[] cols = {"Time", "Shelter ID", "District", "Occupants", "Capacity"};
    for (int i = 0; i < cols.length; i++) {
      Cell c = header.createCell(i);
      c.setCellValue(cols[i]);
      c.setCellStyle(headerStyle);
    }

    for (OccupancyPoint point : results.occupancyOverTime()) {
      Row r = sheet.createRow(rowNum++);
      r.createCell(0).setCellValue(point.capturedAt().format(ISO_FORMATTER));
      r.createCell(1).setCellValue(point.shelterId());
      r.createCell(2).setCellValue(point.district().name());
      r.createCell(3).setCellValue(point.occupantCount());
      r.createCell(4).setCellValue(point.capacity());
    }

    for (int i = 0; i < cols.length; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private void createDistributionSheet(
      Workbook workbook, ReportResults results, CellStyle headerStyle) {
    Sheet sheet = workbook.createSheet("Resource Distribution");
    int rowNum = 0;

    Row header = sheet.createRow(rowNum++);
    String[] cols = {"District", "Owner ID", "Owner Name", "Category", "Quantity", "Unit"};
    for (int i = 0; i < cols.length; i++) {
      Cell c = header.createCell(i);
      c.setCellValue(cols[i]);
      c.setCellStyle(headerStyle);
    }

    for (DistributionLine line : results.distributionLines()) {
      Row r = sheet.createRow(rowNum++);
      r.createCell(0).setCellValue(line.district().name());
      r.createCell(1).setCellValue(line.ownerId() != null ? line.ownerId() : "");
      r.createCell(2).setCellValue(line.ownerName());
      r.createCell(3).setCellValue(line.category().name());
      r.createCell(4).setCellValue(line.quantity());
      r.createCell(5).setCellValue(line.unit());
    }

    for (int i = 0; i < cols.length; i++) {
      sheet.autoSizeColumn(i);
    }
  }
}
