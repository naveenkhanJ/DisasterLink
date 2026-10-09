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
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation exporting reports to PDF format.
 *
 * <p>Implements CH-32. Uses OpenPDF 3 to render tabular PDF reports without charts.
 */
@Component
public class PdfReportExporter implements ReportExporter {

  private static final Logger LOG = LoggerFactory.getLogger(PdfReportExporter.class);
  private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
  private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
  private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);

  @Override
  public boolean supports(ExportFormat format) {
    return format == ExportFormat.PDF;
  }

  @Override
  public ExportFile export(DisasterReport report) {
    Objects.requireNonNull(report, "report");
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document document = new Document();
      PdfWriter.getInstance(document, out);
      document.open();

      Paragraph title = new Paragraph("Disaster Response Report", TITLE_FONT);
      title.setSpacingAfter(8);
      document.add(title);

      Paragraph summary =
          new Paragraph(
              "Report ID: "
                  + report.getReportId()
                  + "\nTitle: "
                  + (report.getReportTitle() != null ? report.getReportTitle() : "")
                  + "\nGenerated: "
                  + (report.getGeneratedDate() != null
                      ? report.getGeneratedDate().format(ISO_FORMATTER)
                      : "")
                  + "\nProvisional: "
                  + report.isProvisional(),
              CELL_FONT);
      summary.setSpacingAfter(14);
      document.add(summary);

      addTimelineSection(document, report.getResults());
      addCitizensSection(document, report.getResults());
      addOccupancySection(document, report.getResults());
      addDistributionSection(document, report.getResults());
      addFindingsSection(document, report);

      document.close();

      String fileName = CsvReportExporter.buildFileName(report, "pdf");
      LOG.info("PDF report exported successfully: {}", fileName);

      return new ExportFile(fileName, ExportFormat.PDF, out.toByteArray());
    } catch (Exception e) {
      LOG.error("Failed to export report {} to PDF", report.getReportId(), e);
      throw new ExportFailedException("Failed to export PDF report: " + e.getMessage(), e);
    }
  }

  private void addTimelineSection(Document document, ReportResults results) throws Exception {
    Paragraph heading = new Paragraph("Alert Timeline", SECTION_FONT);
    heading.setSpacingBefore(10);
    heading.setSpacingAfter(6);
    document.add(heading);

    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);
    addHeaderCell(table, "Time");
    addHeaderCell(table, "Event");
    addHeaderCell(table, "Warning ID");
    addHeaderCell(table, "Level");
    addHeaderCell(table, "Districts");

    for (AlertTimelineEntry entry : results.alertTimeline()) {
      table.addCell(new Phrase(entry.issuedTime().format(ISO_FORMATTER), CELL_FONT));
      table.addCell(new Phrase(entry.event().name(), CELL_FONT));
      table.addCell(new Phrase(entry.warningId(), CELL_FONT));
      table.addCell(new Phrase(entry.warningLevel().name(), CELL_FONT));
      table.addCell(new Phrase(entry.districts().toString(), CELL_FONT));
    }
    document.add(table);
  }

  private void addCitizensSection(Document document, ReportResults results) throws Exception {
    Paragraph heading = new Paragraph("Citizens Reached", SECTION_FONT);
    heading.setSpacingBefore(10);
    heading.setSpacingAfter(6);
    document.add(heading);

    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(60);
    addHeaderCell(table, "Channel");
    addHeaderCell(table, "Reached Count");

    for (Map.Entry<NotificationChannel, Integer> entry :
        results.citizensReachedByChannel().entrySet()) {
      table.addCell(new Phrase(entry.getKey().name(), CELL_FONT));
      table.addCell(new Phrase(String.valueOf(entry.getValue()), CELL_FONT));
    }
    table.addCell(new Phrase("Total (distinct)", HEADER_FONT));
    table.addCell(new Phrase(String.valueOf(results.citizensReachedTotal()), HEADER_FONT));

    document.add(table);
  }

  private void addOccupancySection(Document document, ReportResults results) throws Exception {
    Paragraph heading =
        new Paragraph(
            "Shelter Occupancy (Combined Capacity: " + results.combinedCapacity() + ")",
            SECTION_FONT);
    heading.setSpacingBefore(10);
    heading.setSpacingAfter(6);
    document.add(heading);

    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);
    addHeaderCell(table, "Time");
    addHeaderCell(table, "Shelter ID");
    addHeaderCell(table, "District");
    addHeaderCell(table, "Occupants");
    addHeaderCell(table, "Capacity");

    for (OccupancyPoint point : results.occupancyOverTime()) {
      table.addCell(new Phrase(point.capturedAt().format(ISO_FORMATTER), CELL_FONT));
      table.addCell(new Phrase(point.shelterId(), CELL_FONT));
      table.addCell(new Phrase(point.district().name(), CELL_FONT));
      table.addCell(new Phrase(String.valueOf(point.occupantCount()), CELL_FONT));
      table.addCell(new Phrase(String.valueOf(point.capacity()), CELL_FONT));
    }
    document.add(table);
  }

  private void addDistributionSection(Document document, ReportResults results) throws Exception {
    Paragraph heading = new Paragraph("Resource Distribution by District and Owner", SECTION_FONT);
    heading.setSpacingBefore(10);
    heading.setSpacingAfter(6);
    document.add(heading);

    PdfPTable table = new PdfPTable(6);
    table.setWidthPercentage(100);
    addHeaderCell(table, "District");
    addHeaderCell(table, "Owner ID");
    addHeaderCell(table, "Owner Name");
    addHeaderCell(table, "Category");
    addHeaderCell(table, "Quantity");
    addHeaderCell(table, "Unit");

    for (DistributionLine line : results.distributionLines()) {
      table.addCell(new Phrase(line.district().name(), CELL_FONT));
      table.addCell(new Phrase(line.ownerId() != null ? line.ownerId() : "", CELL_FONT));
      table.addCell(new Phrase(line.ownerName(), CELL_FONT));
      table.addCell(new Phrase(line.category().name(), CELL_FONT));
      table.addCell(new Phrase(String.valueOf(line.quantity()), CELL_FONT));
      table.addCell(new Phrase(line.unit(), CELL_FONT));
    }
    document.add(table);
  }

  private void addFindingsSection(Document document, DisasterReport report) throws Exception {
    if (report.getFindings().isPresent()) {
      Paragraph heading = new Paragraph("Findings", SECTION_FONT);
      heading.setSpacingBefore(10);
      heading.setSpacingAfter(6);
      document.add(heading);
      document.add(new Paragraph(report.getFindings().get(), CELL_FONT));
    }
  }

  private void addHeaderCell(PdfPTable table, String headerText) {
    PdfPCell cell = new PdfPCell(new Phrase(headerText, HEADER_FONT));
    table.addCell(cell);
  }
}
