package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.config.PhotoStorageProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Gives every seeded report its photo: SC2 step 24 shows the photo of a report, so a file must
 * exist under the name each report stores. The one placeholder JPEG of the resources is copied to
 * {@code <storage-dir>/<HazardReport.photo>} for every report; the directory is the one of the
 * photo endpoint and {@code receiveReport} ({@link PhotoStorageProperties}). An existing file is
 * overwritten (P155 G9).
 */
@Component
@Profile("!test")
class PhotoSeed {

  /** The placeholder photograph (a Group_058 stand-in; every seeded report shows it). */
  static final String PLACEHOLDER = "seed/photo-placeholder.jpg";

  private final PhotoStorageProperties properties;

  private final HazardReportRepository reports;

  PhotoSeed(PhotoStorageProperties properties, HazardReportRepository reports) {
    this.properties = properties;
    this.reports = reports;
  }

  /**
   * Copies the placeholder under the photo name of every saved report.
   *
   * @return how many files were written
   * @throws UncheckedIOException if the directory or a file cannot be written
   */
  int seed() {
    Path directory = Path.of(properties.storageDir()).toAbsolutePath().normalize();
    List<HazardReport> saved = reports.findAll();
    try {
      Files.createDirectories(directory);
      for (HazardReport report : saved) {
        copyPlaceholder(directory.resolve(report.getPhoto()).normalize(), directory);
      }
    } catch (IOException exception) {
      throw new UncheckedIOException(
          "Cannot write the seed photos to " + directory + ": " + exception.getMessage(),
          exception);
    }
    return saved.size();
  }

  private static void copyPlaceholder(Path target, Path directory) throws IOException {
    if (!target.startsWith(directory)) {
      throw new IOException("A photo name leaves the photo directory: " + target);
    }
    try (InputStream placeholder = new ClassPathResource(PLACEHOLDER).getInputStream()) {
      Files.copy(placeholder, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
