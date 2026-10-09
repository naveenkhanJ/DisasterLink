package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation exporting reports to CSV format.
 *
 * <p>Implements CH-32. Uses a plain {@link java.io.Writer} with RFC 4180 quoting, rendering one
 * tabular section per metric separated by blank lines (R3F-25).
 */
@Component
public class CsvReportExporter implements ReportExporter {

  private static final Logger LOG = LoggerFactory.getLogger(CsvReportExporter.class);
  private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  @Override
  public boolean supports(ExportFormat format) {
    return format == ExportFormat.CSV;
  }

  @Override
  public ExportFile export(DisasterReport report) {
    Objects.requireNonNull(report, "report");
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (PrintWriter writer =
          new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
        writeSummary(writer, report);
        writeTimeline(writer, report.getResults());
        writeCitizensReached(writer, report.getResults());
        writeOccupancy(writer, report.getResults());
        writeDistributions(writer, report.getResults());
        writeFindings(writer, report);
        writer.flush();
      }

      String fileName = buildFileName(report, "csv");
      LOG.info("CSV report exported successfully: {}", fileName);
      return new ExportFile(fileName, ExportFormat.CSV, out.toByteArray());
    } catch (Exception e) {
      LOG.error("Failed to export report {} to CSV", report.getReportId(), e);
      throw new ExportFailedException("Failed to export CSV report: " + e.getMessage(), e);
    }
  }

  private void writeSummary(PrintWriter writer, DisasterReport report) {
    writeRow(writer, "Report ID", report.getReportId());
    writeRow(writer, "Title", report.getReportTitle());
    writeRow(
        writer,
        "Generated Date",
        report.getGeneratedDate() != null ? report.getGeneratedDate().format(ISO_FORMATTER) : "");
    writeRow(writer, "Provisional", String.valueOf(report.isProvisional()));
    writer.println();
  }

  private void writeTimeline(PrintWriter writer, ReportResults results) {
    writer.println("Alert Timeline");
    writeRow(writer, "Time", "Event", "Warning ID", "Level", "Districts");
    for (AlertTimelineEntry entry : results.alertTimeline()) {
      writeRow(
          writer,
          entry.issuedTime().format(ISO_FORMATTER),
          entry.event().name(),
          entry.warningId(),
          entry.warningLevel().name(),
          entry.districts().toString());
    }
    writer.println();
  }

  private void writeCitizensReached(PrintWriter writer, ReportResults results) {
    writer.println("Citizens Reached");
    writeRow(writer, "Channel", "Citizens Reached");
    for (Map.Entry<NotificationChannel, Integer> entry :
        results.citizensReachedByChannel().entrySet()) {
      writeRow(writer, entry.getKey().name(), String.valueOf(entry.getValue()));
    }
    writeRow(writer, "Total (distinct)", String.valueOf(results.citizensReachedTotal()));
    writer.println();
  }

  private void writeOccupancy(PrintWriter writer, ReportResults results) {
    writer.println("Shelter Occupancy Over Time");
    writeRow(writer, "Combined Current Capacity", String.valueOf(results.combinedCapacity()));
    writeRow(writer, "Time", "Shelter ID", "District", "Occupants", "Capacity");
    for (OccupancyPoint point : results.occupancyOverTime()) {
      writeRow(
          writer,
          point.capturedAt().format(ISO_FORMATTER),
          point.shelterId(),
          point.district().name(),
          String.valueOf(point.occupantCount()),
          String.valueOf(point.capacity()));
    }
    writer.println();
  }

  private void writeDistributions(PrintWriter writer, ReportResults results) {
    writer.println("Resource Distribution by District and Owner");
    writeRow(writer, "District", "Owner ID", "Owner Name", "Category", "Quantity", "Unit");
    for (DistributionLine line : results.distributionLines()) {
      writeRow(
          writer,
          line.district().name(),
          line.ownerId() != null ? line.ownerId() : "",
          line.ownerName(),
          line.category().name(),
          String.valueOf(line.quantity()),
          line.unit());
    }
    writer.println();
  }

  private void writeFindings(PrintWriter writer, DisasterReport report) {
    if (report.getFindings().isPresent()) {
      writer.println("Findings");
      writeRow(writer, report.getFindings().get());
      writer.println();
    }
  }

  private void writeRow(PrintWriter writer, String... fields) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(quoteRfc4180(fields[i]));
    }
    writer.println(sb);
  }

  private String quoteRfc4180(String field) {
    if (field == null) {
      return "";
    }
    if (field.contains(",")
        || field.contains("\"")
        || field.contains("\n")
        || field.contains("\r")) {
      return "\"" + field.replace("\"", "\"\"") + "\"";
    }
    return field;
  }

  static String buildFileName(DisasterReport report, String ext) {
    String base = report.getReportId();
    if (report.getReportTitle() != null && !report.getReportTitle().isBlank()) {
      String slug =
          report
              .getReportTitle()
              .toLowerCase()
              .replaceAll("[^a-z0-9_]+", "-")
              .replaceAll("^-|-$", "");
      if (!slug.isEmpty()) {
        base += "_" + slug;
      }
    }
    return base + "." + ext;
  }
}
