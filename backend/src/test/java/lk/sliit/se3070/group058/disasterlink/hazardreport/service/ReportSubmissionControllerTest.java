package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.ReportReceipt;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.config.PhotoStorageProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.PhotoUpload;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import lk.sliit.se3070.group058.disasterlink.shared.service.SubmissionRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests of {@link ReportSubmissionController} with mocked collaborators, a fixed clock and a
 * temporary photo directory. Each display name names the SC2 flow it covers.
 */
@ExtendWith(MockitoExtension.class)
class ReportSubmissionControllerTest {

  private static final String REPORT_ID = "RPT-2026-KLT-0847";
  private static final String CITIZEN_ID = "CIT-KLT-0001";
  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 7, 40);
  private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 5, 21, 7, 42);
  private static final GeoPoint KALUTARA_BRIDGE = new GeoPoint(6.5888, 79.9640, 9.5);
  private static final ClientMeta META =
      new ClientMeta("client-1", CITIZEN_ID, "draft-1", CAPTURED, 0);
  private static final byte[] JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46
  };
  private static final byte[] PNG_BYTES = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00
  };

  @TempDir private Path tempDir;

  @Mock private HazardReportRepository reports;
  @Mock private CitizenRepository citizens;
  @Mock private IdGenerator ids;
  @Mock private SubmissionRecorder recorder;

  private Path photoDirectory;
  private ReportSubmissionController controller;

  @BeforeEach
  void createController() {
    photoDirectory = tempDir.resolve("data").resolve("photos");
    controller = controllerStoringIn(photoDirectory);
  }

  private ReportSubmissionController controllerStoringIn(Path directory) {
    Clock clock = Clock.fixed(Instant.parse("2026-05-21T02:12:00Z"), ZoneId.of("Asia/Colombo"));
    return new ReportSubmissionController(
        reports, citizens, ids, recorder, new PhotoStorageProperties(directory.toString()), clock);
  }

  private static ReportDraft draft(
      HazardType type, String description, GeoPoint location, District district) {
    return new ReportDraft("draft-1", type, description, location, true, district, CAPTURED);
  }

  private static ReportDraft validDraft() {
    return draft(HazardType.FLOOD, "Water is rising", KALUTARA_BRIDGE, District.KALUTARA);
  }

  private static PhotoUpload jpeg() {
    return new PhotoUpload("photo.jpg", "image/jpeg", JPEG_BYTES);
  }

  private static PhotoUpload png() {
    return new PhotoUpload("photo.png", "image/png", PNG_BYTES);
  }

  private static byte[] withSignature(byte[] signature, int totalLength) {
    return Arrays.copyOf(signature, totalLength);
  }

  private void arrangeReporterAndId() {
    Citizen reporter = SampleEntities.citizen(CITIZEN_ID, "token");
    when(citizens.findById(CITIZEN_ID)).thenReturn(Optional.of(reporter));
    when(ids.next(IdKind.HAZARD_REPORT, District.KALUTARA)).thenReturn(REPORT_ID);
  }

  private void arrangeValidSubmission() {
    arrangeReporterAndId();
    when(reports.save(any(HazardReport.class))).thenAnswer(call -> call.getArgument(0));
  }

  private void assertNothingStored() {
    verifyNoInteractions(reports, ids, citizens);
    verify(recorder).check(META);
    verifyNoMoreInteractions(recorder);
    assertThat(photoDirectory).doesNotExist();
  }

  // ---- main flow: SC2 steps 18 to 20 -------------------------------------------------------

  @Test
  @DisplayName("SC2 step 19: a valid JPEG report is ACCEPTED, stored and recorded")
  void acceptsAValidJpegReport() throws IOException {
    arrangeValidSubmission();

    ReportReceipt receipt = controller.receiveReport(validDraft(), jpeg(), META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(receipt.result().reason()).isNull();
    assertThat(receipt.result().submissionId()).isEqualTo("draft-1");
    assertThat(receipt.result().message()).contains(REPORT_ID);
    ArgumentCaptor<HazardReport> saved = ArgumentCaptor.forClass(HazardReport.class);
    verify(reports).save(saved.capture());
    HazardReport report = saved.getValue();
    assertThat(receipt.report()).isSameAs(report);
    assertThat(report.getReportId()).isEqualTo(REPORT_ID);
    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(report.getReportTime()).isEqualTo(RECEIVED);
    assertThat(report.getCapturedAt()).isEqualTo(CAPTURED);
    assertThat(report.getReporter().getUserId()).isEqualTo(CITIZEN_ID);
    assertThat(report.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(report.getPhoto()).isEqualTo(REPORT_ID + ".jpg");
    assertThat(Files.readAllBytes(photoDirectory.resolve(REPORT_ID + ".jpg")))
        .isEqualTo(JPEG_BYTES);
  }

  @Test
  @DisplayName("SC2 step 19: a valid PNG report is stored as a .png file")
  void acceptsAValidPngReport() throws IOException {
    arrangeValidSubmission();

    ReportReceipt receipt = controller.receiveReport(validDraft(), png(), META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(receipt.report().getPhoto()).isEqualTo(REPORT_ID + ".png");
    assertThat(Files.readAllBytes(photoDirectory.resolve(REPORT_ID + ".png"))).isEqualTo(PNG_BYTES);
    assertThat(photoDirectory).isDirectoryNotContaining(path -> path.toString().endsWith(".jpg"));
  }

  @ParameterizedTest(name = "[{index}] content type \"{0}\"")
  @ValueSource(strings = {"IMAGE/JPEG", " image/jpeg ", "Image/Jpeg"})
  @DisplayName("SC2 step 19: the content type is compared without case and white space")
  void acceptsAContentTypeInAnotherCase(String contentType) {
    arrangeValidSubmission();
    PhotoUpload photo = new PhotoUpload("photo", contentType, JPEG_BYTES);

    ReportReceipt receipt = controller.receiveReport(validDraft(), photo, META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(receipt.report().getPhoto()).endsWith(".jpg");
  }

  @Test
  @DisplayName(
      "SD2 9.1.2.2 to 9.1.2.3: the report is saved, then recorded, then the sync is touched")
  void savesThenRecordsThenTouchesSync() {
    arrangeValidSubmission();

    controller.receiveReport(validDraft(), jpeg(), META);

    InOrder order = inOrder(recorder, reports);
    order.verify(recorder).check(META);
    order.verify(reports).save(any(HazardReport.class));
    order.verify(recorder).record(META, REPORT_ID);
    order.verify(recorder).touchSync(META);
    verifyNoMoreInteractions(recorder);
  }

  @Test
  @DisplayName("SC2 step 19: an unknown reporter is a NoSuchElementException and nothing is stored")
  void unknownReporterIsRefused() {
    when(citizens.findById(CITIZEN_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.receiveReport(validDraft(), jpeg(), META))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining(CITIZEN_ID);

    verifyNoInteractions(ids, reports);
    verify(recorder).check(META);
    verifyNoMoreInteractions(recorder);
    assertThat(photoDirectory).doesNotExist();
  }

  @Test
  @DisplayName(
      "SC2 E4 and E6 19d: a photo that cannot be written is an unchecked error, nothing saved")
  void photoWriteFailureIsAnUncheckedException() throws IOException {
    Path blocker = tempDir.resolve("blocker");
    Files.writeString(blocker, "this is a file, not a directory");
    ReportSubmissionController blocked = controllerStoringIn(blocker);
    arrangeReporterAndId();

    assertThatThrownBy(() -> blocked.receiveReport(validDraft(), jpeg(), META))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining(REPORT_ID)
        .hasCauseInstanceOf(IOException.class);

    verify(reports, never()).save(any());
    verify(recorder, never()).record(any(), any());
    verify(recorder, never()).touchSync(any());
  }

  // ---- E1: required information missing or invalid (validateReport, SD2 9.1.2.1) -------------

  static Stream<Arguments> singleFieldFailures() {
    String longDescription = "x".repeat(ReportSubmissionController.MAX_DESCRIPTION_LENGTH + 1);
    return Stream.of(
        Arguments.of(
            "no hazard type",
            draft(null, "Water", KALUTARA_BRIDGE, District.KALUTARA),
            "hazardType: a hazard type is required"),
        Arguments.of(
            "no description",
            draft(HazardType.FLOOD, null, KALUTARA_BRIDGE, District.KALUTARA),
            "description: a description is required"),
        Arguments.of(
            "empty description",
            draft(HazardType.FLOOD, "", KALUTARA_BRIDGE, District.KALUTARA),
            "description: a description is required"),
        Arguments.of(
            "blank description",
            draft(HazardType.FLOOD, " \t ", KALUTARA_BRIDGE, District.KALUTARA),
            "description: a description is required"),
        Arguments.of(
            "description of 501 characters",
            draft(HazardType.FLOOD, longDescription, KALUTARA_BRIDGE, District.KALUTARA),
            "description: at most 500 characters"),
        Arguments.of(
            "no location",
            draft(HazardType.FLOOD, "Water", null, District.KALUTARA),
            "location: a location is required"),
        Arguments.of(
            "a location outside Sri Lanka",
            draft(HazardType.FLOOD, "Water", new GeoPoint(51.5, -0.12, null), District.KALUTARA),
            "location: outside Sri Lanka"),
        Arguments.of(
            "a location that is not a number",
            draft(
                HazardType.FLOOD,
                "Water",
                new GeoPoint(Double.NaN, Double.NaN, null),
                District.KALUTARA),
            "location: outside Sri Lanka"),
        Arguments.of(
            "no district",
            draft(HazardType.FLOOD, "Water", KALUTARA_BRIDGE, null),
            "district: a district is required"));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("singleFieldFailures")
  @DisplayName("SC2 E1 16a: each rule fails on its own with one field-prefixed error")
  void eachRuleFailsOnItsOwn(String ignoredCase, ReportDraft draft, String expectedError) {
    ValidationResult result = controller.validateReport(draft, jpeg());

    assertThat(result.valid()).isFalse();
    assertThat(result.errors()).containsExactly(expectedError);
  }

  @Test
  @DisplayName(
      "SC2 E1 16a: several failures are all listed, each with its field prefix, in form order")
  void severalFailuresAreAllListed() {
    ReportDraft draft = draft(null, "  ", null, null);

    ValidationResult result = controller.validateReport(draft, null);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "hazardType: a hazard type is required",
            "description: a description is required",
            "photo: one photo is required",
            "location: a location is required",
            "district: a district is required");
  }

  @Test
  @DisplayName(
      "SC2 step 16: a complete report with a description of exactly 500 characters is valid")
  void acceptsADescriptionOfExactlyTheLimit() {
    String description = "x".repeat(ReportSubmissionController.MAX_DESCRIPTION_LENGTH);

    ValidationResult result =
        controller.validateReport(
            draft(HazardType.LANDSLIDE, description, KALUTARA_BRIDGE, District.KALUTARA), png());

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  @DisplayName("SC2 step 16: validateReport needs a draft")
  void validateReportNeedsADraft() {
    assertThatNullPointerException()
        .isThrownBy(() -> controller.validateReport(null, jpeg()))
        .withMessageContaining("draft");
  }

  @Test
  @DisplayName("SC2 E1 and E6 19a: a report that fails validation is REJECTED with every error")
  void rejectsAnInvalidReportWithAllErrors() {
    ReportDraft draft = draft(null, "", KALUTARA_BRIDGE, District.KALUTARA);

    ReportReceipt receipt = controller.receiveReport(draft, null, META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.REJECTED);
    assertThat(receipt.result().reason()).isEqualTo(RejectionReason.VALIDATION_FAILED);
    assertThat(receipt.result().submissionId()).isEqualTo("draft-1");
    assertThat(receipt.result().message())
        .isEqualTo(
            "hazardType: a hazard type is required; description: a description is required;"
                + " photo: one photo is required");
    assertThat(receipt.report()).isNull();
    assertNothingStored();
  }

  // ---- E2 and E6: the photo -----------------------------------------------------------------

  static Stream<Arguments> unusablePhotos() {
    return Stream.of(
        Arguments.of("no photo", null, "photo: one photo is required"),
        Arguments.of(
            "an empty photo",
            new PhotoUpload("p.jpg", "image/jpeg", new byte[0]),
            "photo: the file is empty"),
        Arguments.of(
            "a GIF content type",
            new PhotoUpload("p.gif", "image/gif", JPEG_BYTES),
            "photo: not a readable JPEG or PNG image"),
        Arguments.of(
            "a text content type",
            new PhotoUpload("p.txt", "text/plain", JPEG_BYTES),
            "photo: not a readable JPEG or PNG image"),
        Arguments.of(
            "PNG bytes labelled as JPEG",
            new PhotoUpload("p.jpg", "image/jpeg", PNG_BYTES),
            "photo: not a readable JPEG or PNG image"),
        Arguments.of(
            "JPEG bytes labelled as PNG",
            new PhotoUpload("p.png", "image/png", JPEG_BYTES),
            "photo: not a readable JPEG or PNG image"),
        Arguments.of(
            "bytes shorter than the signature",
            new PhotoUpload("p.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8}),
            "photo: not a readable JPEG or PNG image"),
        Arguments.of(
            "a file over 10 MB",
            new PhotoUpload(
                "big.jpg",
                "image/jpeg",
                withSignature(JPEG_BYTES, (int) ReportSubmissionController.MAX_PHOTO_BYTES + 1)),
            "photo: larger than 10 MB"));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("unusablePhotos")
  @DisplayName("SC2 E2 and E6: an unusable photo is a REJECTED reply and nothing is stored")
  void rejectsAnUnusablePhoto(String ignoredCase, PhotoUpload photo, String expectedError) {
    ReportReceipt receipt = controller.receiveReport(validDraft(), photo, META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.REJECTED);
    assertThat(receipt.result().reason()).isEqualTo(RejectionReason.VALIDATION_FAILED);
    assertThat(receipt.result().message()).isEqualTo(expectedError);
    assertThat(receipt.report()).isNull();
    assertNothingStored();
  }

  @Test
  @DisplayName("SC2 step 16: a photo of exactly 10 MB is accepted by validation")
  void acceptsAPhotoOfExactlyTheLimit() {
    PhotoUpload photo =
        new PhotoUpload(
            "big.jpg",
            "image/jpeg",
            withSignature(JPEG_BYTES, (int) ReportSubmissionController.MAX_PHOTO_BYTES));

    assertThat(controller.validateReport(validDraft(), photo).valid()).isTrue();
  }

  // ---- E3: the location lies within Sri Lanka ------------------------------------------------

  static Stream<Arguments> boundaryLocations() {
    return Stream.of(
        Arguments.of("the southern edge", 5.85, 80.0, true),
        Arguments.of("the northern edge", 9.90, 80.0, true),
        Arguments.of("the western edge", 7.0, 79.50, true),
        Arguments.of("the eastern edge", 7.0, 81.95, true),
        Arguments.of("just south of the box", 5.8499, 80.0, false),
        Arguments.of("just north of the box", 9.9001, 80.0, false),
        Arguments.of("just west of the box", 7.0, 79.4999, false),
        Arguments.of("just east of the box", 7.0, 81.9501, false));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("boundaryLocations")
  @DisplayName("SC2 E3 12e: the box of Sri Lanka includes its border and refuses outside it")
  void locationBoxIncludesItsBorder(
      String ignoredCase, double latitude, double longitude, boolean accepted) {
    ReportDraft draft =
        draft(HazardType.FLOOD, "Water", new GeoPoint(latitude, longitude, null), District.GALLE);

    ValidationResult result = controller.validateReport(draft, jpeg());

    assertThat(result.valid()).isEqualTo(accepted);
    if (!accepted) {
      assertThat(result.errors()).containsExactly("location: outside Sri Lanka");
    }
  }

  // ---- E8: a replayed submission --------------------------------------------------------------

  private static ProcessedSubmission processed(String resultId) {
    return new ProcessedSubmission("draft-1", "client-1", CAPTURED, RECEIVED, resultId);
  }

  @Test
  @DisplayName("SC2 E8 18g: a replayed report is a DUPLICATE that carries the earlier report")
  void replayedReportIsADuplicateWithTheEarlierReport() {
    HazardReport earlier =
        new HazardReport(
            REPORT_ID, SampleEntities.citizen(CITIZEN_ID, "t"), validDraft(), "p.jpg", RECEIVED);
    when(recorder.check(META)).thenReturn(Optional.of(processed(REPORT_ID)));
    when(reports.findById(REPORT_ID)).thenReturn(Optional.of(earlier));

    ReportReceipt receipt = controller.receiveReport(validDraft(), jpeg(), META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    assertThat(receipt.result().reason()).isNull();
    assertThat(receipt.result().submissionId()).isEqualTo("draft-1");
    assertThat(receipt.result().message()).isEqualTo("Already received as " + REPORT_ID);
    assertThat(receipt.report()).isSameAs(earlier);
    verify(reports, never()).save(any());
    verifyNoInteractions(ids, citizens);
    verify(recorder, never()).record(any(), any());
    verify(recorder, never()).touchSync(any());
  }

  @Test
  @DisplayName("SC2 E8 18g: a duplicate whose earlier report is gone still names the report id")
  void duplicateWithoutTheEarlierReport() {
    when(recorder.check(META)).thenReturn(Optional.of(processed(REPORT_ID)));
    when(reports.findById(REPORT_ID)).thenReturn(Optional.empty());

    ReportReceipt receipt = controller.receiveReport(validDraft(), jpeg(), META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    assertThat(receipt.result().message()).contains(REPORT_ID);
    assertThat(receipt.report()).isNull();
  }

  @Test
  @DisplayName("SC2 E8 18g: a duplicate with no recorded result id is still a DUPLICATE")
  void duplicateWithoutAResultId() {
    when(recorder.check(META)).thenReturn(Optional.of(processed(null)));

    ReportReceipt receipt = controller.receiveReport(validDraft(), jpeg(), META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    assertThat(receipt.result().message()).isEqualTo("Already received");
    assertThat(receipt.report()).isNull();
    verifyNoInteractions(reports);
  }

  @Test
  @DisplayName("SC2 E8 18g: a duplicate is answered before validation or storage run")
  void duplicateSkipsValidationAndStorage() {
    when(recorder.check(META)).thenReturn(Optional.of(processed(REPORT_ID)));
    ReportDraft invalid = draft(null, null, null, null);

    ReportReceipt receipt = controller.receiveReport(invalid, null, META);

    assertThat(receipt.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    verifyNoInteractions(ids, citizens);
    assertThat(photoDirectory).doesNotExist();
  }

  // ---- reads and the language change ---------------------------------------------------------

  @Test
  @DisplayName("SC2 step 20: getReportStatus returns the report")
  void getReportStatusReturnsTheReport() {
    HazardReport report =
        new HazardReport(
            REPORT_ID, SampleEntities.citizen(CITIZEN_ID, "t"), validDraft(), "p.jpg", RECEIVED);
    when(reports.findById(REPORT_ID)).thenReturn(Optional.of(report));

    assertThat(controller.getReportStatus(REPORT_ID)).isSameAs(report);
  }

  @Test
  @DisplayName("SC2 step 20: getReportStatus of an unknown report is a NoSuchElementException")
  void getReportStatusOfAnUnknownReport() {
    when(reports.findById("RPT-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getReportStatus("RPT-0"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("RPT-0");
  }

  @Test
  @DisplayName("SC2 step 2: getReportsByReporter returns the repository's order, newest first")
  void getReportsByReporterKeepsTheRepositoryOrder() {
    Citizen citizen = SampleEntities.citizen(CITIZEN_ID, "t");
    HazardReport newer = new HazardReport("RPT-2", citizen, validDraft(), "2.jpg", RECEIVED);
    HazardReport older =
        new HazardReport("RPT-1", citizen, validDraft(), "1.jpg", RECEIVED.minusDays(1));
    when(citizens.existsById(CITIZEN_ID)).thenReturn(true);
    when(reports.findByReporter(CITIZEN_ID)).thenReturn(List.of(newer, older));

    assertThat(controller.getReportsByReporter(CITIZEN_ID)).containsExactly(newer, older);
  }

  @Test
  @DisplayName("SC2 step 2: getReportsByReporter of an unknown citizen is a NoSuchElementException")
  void getReportsByReporterOfAnUnknownCitizen() {
    when(citizens.existsById("CIT-0")).thenReturn(false);

    assertThatThrownBy(() -> controller.getReportsByReporter("CIT-0"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("CIT-0");
    verifyNoInteractions(reports);
  }

  @Test
  @DisplayName("SD2 2.1.1: changePreferredLanguage changes the citizen's language")
  void changePreferredLanguageChangesTheCitizen() {
    Citizen citizen = SampleEntities.citizen(CITIZEN_ID, "t");
    when(citizens.findById(CITIZEN_ID)).thenReturn(Optional.of(citizen));

    controller.changePreferredLanguage(CITIZEN_ID, Language.TAMIL);

    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.TAMIL);
  }

  @Test
  @DisplayName(
      "SD2 2.1.1: changePreferredLanguage of an unknown citizen is a NoSuchElementException")
  void changePreferredLanguageOfAnUnknownCitizen() {
    when(citizens.findById("CIT-0")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.changePreferredLanguage("CIT-0", Language.TAMIL))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("CIT-0");
  }

  @Test
  @DisplayName("SD2 2.1.1.1: changePreferredLanguage refuses a missing language")
  void changePreferredLanguageRefusesNull() {
    Citizen citizen = SampleEntities.citizen(CITIZEN_ID, "t");
    when(citizens.findById(CITIZEN_ID)).thenReturn(Optional.of(citizen));

    assertThatNullPointerException()
        .isThrownBy(() -> controller.changePreferredLanguage(CITIZEN_ID, null));
    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.SINHALA);
  }

  @Test
  @DisplayName("The constructor refuses a missing collaborator")
  void constructorRefusesNull() {
    PhotoStorageProperties storage = new PhotoStorageProperties("x");
    Clock clock = Clock.systemUTC();

    assertThatNullPointerException()
        .isThrownBy(
            () -> new ReportSubmissionController(null, citizens, ids, recorder, storage, clock));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new ReportSubmissionController(reports, null, ids, recorder, storage, clock));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new ReportSubmissionController(reports, citizens, null, recorder, storage, clock));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new ReportSubmissionController(reports, citizens, ids, null, storage, clock));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new ReportSubmissionController(reports, citizens, ids, recorder, null, clock));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new ReportSubmissionController(reports, citizens, ids, recorder, storage, null));
  }
}
