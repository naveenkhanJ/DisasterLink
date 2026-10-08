package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportSubmissionController;
import lk.sliit.se3070.group058.disasterlink.shared.config.PhotoStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the stored photo of a report, {@code GET /reports/{reportId}/photo}, so that the
 * verification console can show it (SC2 step 24; standards section 12.3, "Photo endpoint"). It is a
 * web adapter and decides nothing: it finds the report through {@link
 * ReportSubmissionController#getReportStatus}, which reads it in a read-only transaction, and reads
 * the file named in {@code HazardReport.photo} from {@link PhotoStorageProperties#storageDir()},
 * the directory {@code receiveReport} writes to and the seed fills.
 *
 * <p>Everything that is not a stored photo is a 404, answered by the shared {@code
 * ApiExceptionHandler} from a {@link NoSuchElementException}: an unknown report; a stored name that
 * is not a plain file name (it contains {@code /}, {@code \} or {@code ..}, or is not its own file
 * name); a name whose extension is not {@code .jpg}, {@code .jpeg} or {@code .png}; a file that is
 * missing or is not a regular file. The content type comes from the extension.
 *
 * <p>Implements CH-26 (SC2 step 24).
 */
@RestController
public class ReportPhotoController {

  private static final Logger LOG = LoggerFactory.getLogger(ReportPhotoController.class);

  private static final Map<String, MediaType> TYPES_BY_EXTENSION =
      Map.of(
          ".jpg", MediaType.IMAGE_JPEG,
          ".jpeg", MediaType.IMAGE_JPEG,
          ".png", MediaType.IMAGE_PNG);

  private final ReportSubmissionController reportSubmissionController;

  private final PhotoStorageProperties photoStorage;

  /**
   * Creates the controller.
   *
   * @param reportSubmissionController the UC2 control of the citizen side, which finds the report
   * @param photoStorage where the photos are stored
   */
  public ReportPhotoController(
      ReportSubmissionController reportSubmissionController, PhotoStorageProperties photoStorage) {
    this.reportSubmissionController = reportSubmissionController;
    this.photoStorage = photoStorage;
  }

  /**
   * Returns the photo of a report.
   *
   * @param reportId the identifier of the report
   * @return the bytes of the photo with the content type of its extension
   * @throws NoSuchElementException if the report or its photo does not exist, or the stored name is
   *     not a plain file name of a JPEG or PNG file (answered as a 404)
   * @throws UncheckedIOException if the file exists but cannot be read (answered as a 500)
   */
  @GetMapping("/reports/{reportId}/photo")
  public ResponseEntity<byte[]> getPhoto(@PathVariable String reportId) {
    String name = reportSubmissionController.getReportStatus(reportId).getPhoto();
    MediaType type = contentTypeOf(name);
    Path file = Path.of(photoStorage.storageDir()).resolve(name);
    if (!Files.isRegularFile(file)) {
      LOG.warn("Photo file of report {} is missing: {}", reportId, name);
      throw new NoSuchElementException("photo of report " + reportId + " not found");
    }
    try {
      return ResponseEntity.ok().contentType(type).body(Files.readAllBytes(file));
    } catch (IOException e) {
      throw new UncheckedIOException("could not read the photo of report " + reportId, e);
    }
  }

  private static MediaType contentTypeOf(String name) {
    if (!isPlainFileName(name)) {
      throw new NoSuchElementException("the stored photo name is not a plain file name");
    }
    int dot = name.lastIndexOf('.');
    MediaType type =
        dot < 0 ? null : TYPES_BY_EXTENSION.get(name.substring(dot).toLowerCase(Locale.ROOT));
    if (type == null) {
      throw new NoSuchElementException("the stored photo is not a JPEG or PNG file");
    }
    return type;
  }

  private static boolean isPlainFileName(String name) {
    if (name.contains("/") || name.contains("\\") || name.contains("..")) {
      return false;
    }
    try {
      Path path = Path.of(name);
      return path.equals(path.getFileName());
    } catch (InvalidPathException e) {
      return false;
    }
  }
}
