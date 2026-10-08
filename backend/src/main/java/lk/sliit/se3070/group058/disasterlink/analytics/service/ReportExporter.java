package lk.sliit.se3070.group058.disasterlink.analytics.service;

import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ExportFailedException;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFile;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFormat;

/**
 * Strategy interface for exporting a disaster-response report into an external file format.
 *
 * <p>Implements CH-32 (Strategy pattern, model dictionary section 4 and section 6).
 */
public interface ReportExporter {

  /**
   * Returns whether this exporter supports the specified file format.
   *
   * @param format the export format to check
   * @return {@code true} if this exporter produces files in the given format, {@code false}
   *     otherwise
   */
  boolean supports(ExportFormat format);

  /**
   * Exports the provided disaster report into an export file.
   *
   * @param report the stored disaster report to export
   * @return the generated {@link ExportFile} containing file name, format and byte payload
   * @throws ExportFailedException if file generation fails
   */
  ExportFile export(DisasterReport report);
}
