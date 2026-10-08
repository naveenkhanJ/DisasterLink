package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.ReportReceipt;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.config.PhotoStorageProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.PhotoUpload;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ProcessedSubmissionRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.SubmissionRecorder;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

/**
 * Receives reports end to end against H2 with the real {@link IdGenerator} and {@link
 * SubmissionRecorder}, so the identifier format, the stored rows, the photo file and the
 * idempotency are checked together (SC2 steps 18 to 20, E8).
 */
@DataJpaTest
@Import({ReportSubmissionController.class, IdGenerator.class, SubmissionRecorder.class})
@TestPropertySource(properties = "photo.storage-dir=target/test-photos/uc2-submission")
class ReportSubmissionControllerPersistenceTest {

  private static final Path PHOTO_DIRECTORY = Path.of("target", "test-photos", "uc2-submission");
  private static final String CITIZEN_ID = "CIT-KLT-0001";
  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 7, 40);
  private static final byte[] JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46
  };

  /** A clock fixed at 07:42 in Colombo, and the photo properties the property source names. */
  @TestConfiguration
  @EnableConfigurationProperties(PhotoStorageProperties.class)
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-05-21T02:12:00Z"), ZoneId.of("Asia/Colombo"));
    }
  }

  @Autowired private ReportSubmissionController controller;
  @Autowired private HazardReportRepository reports;
  @Autowired private CitizenRepository citizens;
  @Autowired private ProcessedSubmissionRepository processedSubmissions;
  @Autowired private ClientSyncStatusRepository clientStatuses;
  @Autowired private TestEntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  /** Sequences are not rolled back with the test transaction, so each test restarts them. */
  @BeforeEach
  void prepare() {
    jdbcTemplate.execute("ALTER SEQUENCE hazard_report_id_seq RESTART WITH 847");
    citizens.save(FeatureFixtures.citizenIn(CITIZEN_ID, District.KALUTARA));
  }

  @AfterEach
  void removePhotos() {
    FileSystemUtils.deleteRecursively(PHOTO_DIRECTORY.toFile());
  }

  private static ReportDraft draft(HazardType type) {
    return new ReportDraft(
        "draft-1",
        type,
        "Water is rising",
        new GeoPoint(6.5888, 79.9640, 9.5),
        true,
        District.KALUTARA,
        CAPTURED);
  }

  private static ClientMeta meta(String submissionId) {
    return new ClientMeta("client-1", CITIZEN_ID, submissionId, CAPTURED, 0);
  }

  private static PhotoUpload jpeg() {
    return new PhotoUpload("photo.jpg", "image/jpeg", JPEG_BYTES);
  }

  @Test
  @DisplayName("SC2 step 19: a valid report gets RPT-<year>-KLT-nnnn and every row and the photo")
  void storesAValidReportEndToEnd() throws IOException {
    ReportReceipt receipt =
        controller.receiveReport(draft(HazardType.FLOOD), jpeg(), meta("draft-1"));

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(receipt.result().message()).contains("RPT-2026-KLT-0847");
    entityManager.flush();
    entityManager.clear();
    HazardReport stored = reports.findById("RPT-2026-KLT-0847").orElseThrow();
    assertThat(stored.getStatus()).isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(stored.getReportTime()).isEqualTo(LocalDateTime.of(2026, 5, 21, 7, 42));
    assertThat(stored.getCapturedAt()).isEqualTo(CAPTURED);
    assertThat(stored.getHazardType()).isEqualTo(HazardType.FLOOD);
    assertThat(stored.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(stored.getReporter().getUserId()).isEqualTo(CITIZEN_ID);
    assertThat(stored.getPhoto()).isEqualTo("RPT-2026-KLT-0847.jpg");
    assertThat(Files.readAllBytes(PHOTO_DIRECTORY.resolve(stored.getPhoto())))
        .isEqualTo(JPEG_BYTES);
    assertThat(processedSubmissions.findBySubmissionId("draft-1"))
        .hasValueSatisfying(
            processed -> assertThat(processed.getResultId()).contains("RPT-2026-KLT-0847"));
    assertThat(clientStatuses.findById("client-1")).isPresent();
  }

  @Test
  @DisplayName("SC2 E8 18g: the same submission again is a DUPLICATE and stores nothing new")
  void secondIdenticalSubmissionIsDuplicate() {
    controller.receiveReport(draft(HazardType.FLOOD), jpeg(), meta("draft-1"));
    entityManager.flush();
    entityManager.clear();

    ReportReceipt second =
        controller.receiveReport(draft(HazardType.FLOOD), jpeg(), meta("draft-1"));

    assertThat(second.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    assertThat(second.result().message()).isEqualTo("Already received as RPT-2026-KLT-0847");
    assertThat(second.report()).isNotNull();
    assertThat(second.report().getReportId()).isEqualTo("RPT-2026-KLT-0847");
    assertThat(reports.count()).isEqualTo(1);
    assertThat(processedSubmissions.count()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "SC2 E8 18n: a rejected draft is not recorded, so its corrected resubmission is accepted")
  void correctedResubmissionIsProcessedAgain() {
    ReportReceipt rejected = controller.receiveReport(draft(null), jpeg(), meta("draft-1"));

    assertThat(rejected.result().outcome()).isEqualTo(SyncOutcome.REJECTED);
    assertThat(rejected.result().reason()).isEqualTo(RejectionReason.VALIDATION_FAILED);
    assertThat(reports.count()).isZero();
    assertThat(processedSubmissions.count()).isZero();
    assertThat(clientStatuses.count()).isZero();

    ReportReceipt corrected =
        controller.receiveReport(draft(HazardType.FLOOD), jpeg(), meta("draft-1"));

    assertThat(corrected.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(reports.count()).isEqualTo(1);
    assertThat(processedSubmissions.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("SC2 step 2: getReportsByReporter and getReportStatus read what was stored")
  void readsWhatWasStored() {
    controller.receiveReport(draft(HazardType.FLOOD), jpeg(), meta("draft-1"));
    controller.receiveReport(draft(HazardType.LANDSLIDE), jpeg(), meta("draft-2"));
    entityManager.flush();
    entityManager.clear();

    List<HazardReport> mine = controller.getReportsByReporter(CITIZEN_ID);

    assertThat(mine)
        .extracting(HazardReport::getReportId)
        .containsExactlyInAnyOrder("RPT-2026-KLT-0847", "RPT-2026-KLT-0848");
    assertThat(controller.getReportStatus("RPT-2026-KLT-0848").getHazardType())
        .isEqualTo(HazardType.LANDSLIDE);
  }

  @Test
  @DisplayName("SD2 2.1.1: changePreferredLanguage is stored for the citizen")
  void changesThePreferredLanguage() {
    controller.changePreferredLanguage(CITIZEN_ID, Language.TAMIL);
    entityManager.flush();
    entityManager.clear();

    assertThat(citizens.findById(CITIZEN_ID).orElseThrow().getPreferredLanguage())
        .isEqualTo(Language.TAMIL);
  }
}
