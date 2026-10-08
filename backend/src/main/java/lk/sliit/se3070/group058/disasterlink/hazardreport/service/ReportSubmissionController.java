package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.ReportReceipt;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.config.PhotoStorageProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.PhotoUpload;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import lk.sliit.se3070.group058.disasterlink.shared.service.SubmissionRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The server side of the citizen's half of use case UC2, Submit and Verify Hazard Report: it
 * receives and validates a report with its photo, answers a status or list question and changes a
 * citizen's preferred language.
 *
 * <p>Implements CH-19, CH-25 and CH-26. The operations and their order follow sequence diagram SD2
 * (messages 2.1.1 and 9.1.2 to 9.1.2.3) and scenario SC2 (steps 16 to 20, exceptions E1, E2, E3, E6
 * and E8).
 *
 * <p>Refusals follow the standards (section 4): a report that fails {@link #validateReport} is a
 * {@code REJECTED} {@link ReportReceipt}, never an exception; an unknown identifier is a {@link
 * NoSuchElementException}, which the shared JSON handler answers with 404.
 *
 * <p><b>Group_058 assumptions</b> (the scenario sets none of them): a photo is "readable" when its
 * content type is {@code image/jpeg} or {@code image/png} and its first bytes are that format's
 * signature (SC2 step 11); a photo is at most 10 MB, the multipart limit of the standards, section
 * 12.3; "within Sri Lanka" is the box of latitudes 5.85 to 9.90 and longitudes 79.50 to 81.95,
 * borders included. The app applies the same box, so the server and the app have one rule.
 */
@Service
public class ReportSubmissionController {

  private static final Logger LOG = LoggerFactory.getLogger(ReportSubmissionController.class);

  /** The longest description SC2 step 16 accepts, in characters (assumed from the storyboard). */
  static final int MAX_DESCRIPTION_LENGTH = 500;

  /** The number of bytes in one megabyte. */
  static final long BYTES_PER_MEGABYTE = 1_048_576L;

  /** The largest photo in megabytes: the multipart limit of the standards, section 12.3. */
  static final long MAX_PHOTO_MEGABYTES = 10L;

  /** The largest photo in bytes. */
  static final long MAX_PHOTO_BYTES = MAX_PHOTO_MEGABYTES * BYTES_PER_MEGABYTE;

  /** The southern edge of the box that counts as within Sri Lanka, in degrees of latitude. */
  static final double MIN_LATITUDE = 5.85;

  /** The northern edge of the box that counts as within Sri Lanka, in degrees of latitude. */
  static final double MAX_LATITUDE = 9.90;

  /** The western edge of the box that counts as within Sri Lanka, in degrees of longitude. */
  static final double MIN_LONGITUDE = 79.50;

  /** The eastern edge of the box that counts as within Sri Lanka, in degrees of longitude. */
  static final double MAX_LONGITUDE = 81.95;

  static final String JPEG_TYPE = "image/jpeg";

  static final String PNG_TYPE = "image/png";

  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };

  private static final Map<String, byte[]> SIGNATURES =
      Map.of(JPEG_TYPE, JPEG_SIGNATURE, PNG_TYPE, PNG_SIGNATURE);

  private static final Map<String, String> EXTENSIONS = Map.of(JPEG_TYPE, ".jpg", PNG_TYPE, ".png");

  private static final String ERROR_SEPARATOR = "; ";

  private final HazardReportRepository hazardReportRepository;

  private final CitizenRepository citizenRepository;

  private final IdGenerator idGenerator;

  private final SubmissionRecorder submissionRecorder;

  private final PhotoStorageProperties photoStorage;

  private final Clock clock;

  /**
   * Creates the control.
   *
   * @param hazardReportRepository stores and finds the reports
   * @param citizenRepository finds the reporter
   * @param idGenerator makes the identifier of a new report
   * @param submissionRecorder checks and records the idempotency key and the sync status
   * @param photoStorage where the photos are stored
   * @param clock supplies the time a report is received
   */
  public ReportSubmissionController(
      HazardReportRepository hazardReportRepository,
      CitizenRepository citizenRepository,
      IdGenerator idGenerator,
      SubmissionRecorder submissionRecorder,
      PhotoStorageProperties photoStorage,
      Clock clock) {
    this.hazardReportRepository =
        Objects.requireNonNull(hazardReportRepository, "hazardReportRepository");
    this.citizenRepository = Objects.requireNonNull(citizenRepository, "citizenRepository");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    this.submissionRecorder = Objects.requireNonNull(submissionRecorder, "submissionRecorder");
    this.photoStorage = Objects.requireNonNull(photoStorage, "photoStorage");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Receives a report with its photo (SD2 9.1.2; SC2 steps 18 and 19, and the replay of E8).
   *
   * <p>In this order: a submission that was accepted before is a {@code DUPLICATE} reply carrying
   * the earlier report, and nothing else is done (K-12; SC2 18g); a report that fails {@link
   * #validateReport} is a {@code REJECTED} reply with the reason {@code VALIDATION_FAILED} and
   * nothing is stored or recorded, so a corrected resubmission with the same identifier is
   * processed again (SC2 E6 and E8); a valid report is stored with its photo and recorded, the
   * client's sync status is updated, and the reply is {@code ACCEPTED}.
   *
   * <p>The sync status is updated only for an accepted report ({@code recordSync} is drawn in the
   * valid branch of SD2 only). A rejected or duplicate upload therefore does not touch it; this is
   * the Group_058 reading of SD2.
   *
   * <p><b>Known limitation:</b> the photo is a file, which a database rollback does not remove. If
   * saving the report fails after the photo was written, the file stays in the photo directory; its
   * name contains the report identifier, so it can never clash with a later report.
   *
   * @param draft the report as the app sent it
   * @param photo the photo, or {@code null} if the upload had none (answered as a rejection)
   * @param meta the client's metadata; {@code meta.userId()} is the reporter
   * @return the receipt: {@code ACCEPTED} with the stored report, {@code DUPLICATE} with the
   *     earlier report (or {@code null} if it is gone), or {@code REJECTED} without a report
   * @throws NoSuchElementException if the reporter is not a known citizen
   * @throws UncheckedIOException if the photo cannot be written (the transaction rolls back)
   */
  @Transactional
  public ReportReceipt receiveReport(ReportDraft draft, PhotoUpload photo, ClientMeta meta) {
    Optional<ProcessedSubmission> processed = submissionRecorder.check(meta);
    if (processed.isPresent()) {
      return duplicateReceipt(meta, processed.get());
    }
    ValidationResult validation = validateReport(draft, photo);
    if (!validation.valid()) {
      return rejectedReceipt(meta, validation);
    }
    Citizen reporter = findCitizen(meta.userId());
    String reportId = idGenerator.next(IdKind.HAZARD_REPORT, draft.district());
    String fileName = storePhoto(reportId, photo);
    HazardReport report =
        hazardReportRepository.save(
            new HazardReport(reportId, reporter, draft, fileName, LocalDateTime.now(clock)));
    submissionRecorder.record(meta, reportId);
    submissionRecorder.touchSync(meta);
    return acceptedReceipt(meta, report);
  }

  /**
   * Checks a report and its photo against the rules of SC2 step 16, again on the server (FX-01,
   * R4G-01; SD2 9.1.2.1). Every error is collected and starts with the name of its field, as the
   * app's errors do: {@code hazardType}, {@code description}, {@code photo}, {@code location} or
   * {@code district}.
   *
   * <p>The rules: a hazard type; a description that is not blank and has at most 500 characters;
   * exactly one photo that is not empty, is a JPEG or PNG by content type and by its first bytes,
   * and is at most 10 MB; a location within Sri Lanka (the box in the class description); a
   * district. A failure is a result, never an exception (standards section 4).
   *
   * @param draft the report as the app sent it
   * @param photo the photo, or {@code null} if there is none
   * @return the result; valid when there is no error
   * @throws NullPointerException if {@code draft} is {@code null}
   */
  public ValidationResult validateReport(ReportDraft draft, PhotoUpload photo) {
    Objects.requireNonNull(draft, "draft");
    List<String> errors = new ArrayList<>();
    if (draft.hazardType() == null) {
      errors.add("hazardType: a hazard type is required");
    }
    checkDescription(draft.description(), errors);
    checkPhoto(photo, errors);
    checkLocation(draft.location(), errors);
    if (draft.district() == null) {
      errors.add("district: a district is required");
    }
    return new ValidationResult(errors.isEmpty(), errors);
  }

  /**
   * Finds a report, for the app's status screen.
   *
   * @param reportId the identifier of the report
   * @return the report
   * @throws NoSuchElementException if there is no such report
   */
  @Transactional(readOnly = true)
  public HazardReport getReportStatus(String reportId) {
    return hazardReportRepository
        .findById(reportId)
        .orElseThrow(() -> new NoSuchElementException("report " + reportId + " not found"));
  }

  /**
   * Lists the reports a citizen submitted, newest first, for the app's "My reports".
   *
   * @param citizenId the identifier of the citizen
   * @return the citizen's reports
   * @throws NoSuchElementException if there is no such citizen
   */
  @Transactional(readOnly = true)
  public List<HazardReport> getReportsByReporter(String citizenId) {
    if (!citizenRepository.existsById(citizenId)) {
      throw new NoSuchElementException(citizenNotFound(citizenId));
    }
    return hazardReportRepository.findByReporter(citizenId);
  }

  /**
   * Changes a citizen's preferred language (SD2 2.1.1 and 2.1.1.1).
   *
   * @param citizenId the identifier of the citizen
   * @param language the new language
   * @throws NoSuchElementException if there is no such citizen
   * @throws NullPointerException if {@code language} is {@code null}
   */
  @Transactional
  public void changePreferredLanguage(String citizenId, Language language) {
    findCitizen(citizenId).changePreferredLanguage(language);
  }

  private ReportReceipt acceptedReceipt(ClientMeta meta, HazardReport report) {
    String reportId = report.getReportId();
    String submissionId = meta.submissionId();
    LOG.info("Report {} accepted from submission {}", reportId, submissionId);
    return new ReportReceipt(
        new SyncResult(
            submissionId,
            SyncOutcome.ACCEPTED,
            null,
            "Report " + reportId + " received and waiting for verification"),
        report);
  }

  private ReportReceipt rejectedReceipt(ClientMeta meta, ValidationResult validation) {
    String submissionId = meta.submissionId();
    int errorCount = validation.errors().size();
    LOG.info("Report submission {} rejected with {} validation error(s)", submissionId, errorCount);
    return new ReportReceipt(
        new SyncResult(
            submissionId,
            SyncOutcome.REJECTED,
            RejectionReason.VALIDATION_FAILED,
            String.join(ERROR_SEPARATOR, validation.errors())),
        null);
  }

  private ReportReceipt duplicateReceipt(ClientMeta meta, ProcessedSubmission processed) {
    String submissionId = meta.submissionId();
    String earlierId = processed.getResultId().orElse(null);
    HazardReport earlier =
        processed.getResultId().flatMap(hazardReportRepository::findById).orElse(null);
    LOG.info("Report submission {} is a duplicate of {}", submissionId, earlierId);
    String message = earlierId == null ? "Already received" : "Already received as " + earlierId;
    return new ReportReceipt(
        new SyncResult(submissionId, SyncOutcome.DUPLICATE, null, message), earlier);
  }

  private static void checkDescription(String description, List<String> errors) {
    if (description == null || description.isBlank()) {
      errors.add("description: a description is required");
    } else if (description.length() > MAX_DESCRIPTION_LENGTH) {
      errors.add("description: at most " + MAX_DESCRIPTION_LENGTH + " characters");
    }
  }

  private static void checkPhoto(PhotoUpload photo, List<String> errors) {
    if (photo == null) {
      errors.add("photo: one photo is required");
      return;
    }
    byte[] content = photo.content();
    if (content.length == 0) {
      errors.add("photo: the file is empty");
    } else if (!isReadableImage(normalisedType(photo), content)) {
      errors.add("photo: not a readable JPEG or PNG image");
    } else if (content.length > MAX_PHOTO_BYTES) {
      errors.add("photo: larger than " + MAX_PHOTO_MEGABYTES + " MB");
    }
  }

  private static void checkLocation(GeoPoint location, List<String> errors) {
    if (location == null) {
      errors.add("location: a location is required");
    } else if (!isWithinSriLanka(location)) {
      errors.add("location: outside Sri Lanka");
    }
  }

  private static boolean isWithinSriLanka(GeoPoint location) {
    return location.latitude() >= MIN_LATITUDE
        && location.latitude() <= MAX_LATITUDE
        && location.longitude() >= MIN_LONGITUDE
        && location.longitude() <= MAX_LONGITUDE;
  }

  private static boolean isReadableImage(String contentType, byte[] content) {
    byte[] signature = SIGNATURES.get(contentType);
    return signature != null
        && content.length >= signature.length
        && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
  }

  private static String normalisedType(PhotoUpload photo) {
    return photo.contentType().strip().toLowerCase(Locale.ROOT);
  }

  /** Writes the photo as {@code <storage-dir>/<reportId>.jpg|.png} and returns the file name. */
  private String storePhoto(String reportId, PhotoUpload photo) {
    String fileName = reportId + EXTENSIONS.get(normalisedType(photo));
    Path directory = Path.of(photoStorage.storageDir());
    try {
      Files.createDirectories(directory);
      Files.write(directory.resolve(fileName), photo.content());
    } catch (IOException e) {
      throw new UncheckedIOException("could not store the photo of report " + reportId, e);
    }
    return fileName;
  }

  private Citizen findCitizen(String citizenId) {
    return citizenRepository
        .findById(citizenId)
        .orElseThrow(() -> new NoSuchElementException(citizenNotFound(citizenId)));
  }

  private static String citizenNotFound(String citizenId) {
    return "citizen " + citizenId + " not found";
  }
}
