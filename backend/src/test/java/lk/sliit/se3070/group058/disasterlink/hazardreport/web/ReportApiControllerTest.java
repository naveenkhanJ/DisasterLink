package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.ReportReceipt;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportSubmissionController;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.PhotoUpload;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Tests of the four UC2 endpoints of the citizen app: each endpoint's JSON in and out, the {@code
 * +05:30} times, a REJECTED receipt as HTTP 200, and a malformed request as a 400 problem detail
 * with the control never called.
 */
@WebMvcTest({ReportApiController.class})
class ReportApiControllerTest {

  private static final String REPORT_ID = "RPT-2026-KLT-0847";
  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 7, 40);
  private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 5, 21, 7, 42);
  private static final byte[] JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46
  };

  private static final String DRAFT_JSON =
      "{\"draftId\":\"draft-1\",\"hazardType\":\"FLOOD\",\"description\":\"Water is rising\","
          + "\"location\":{\"latitude\":6.5888,\"longitude\":79.9640,\"accuracyMetres\":9.5},"
          + "\"locationFromGps\":true,\"district\":\"KALUTARA\","
          + "\"capturedAt\":\"2026-05-21T07:40:00+05:30\"}";

  /** The same instant as 07:40 in Colombo, written with a {@code Z}. */
  private static final String META_JSON =
      "{\"clientId\":\"client-1\",\"userId\":\"CIT-KLT-0001\",\"submissionId\":\"draft-1\","
          + "\"capturedAt\":\"2026-05-21T02:10:00Z\",\"pendingCount\":0}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReportSubmissionController reportSubmissionController;

  private static MockPart jsonPart(String name, String json) {
    MockPart part = new MockPart(name, name, json.getBytes(StandardCharsets.UTF_8));
    part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return part;
  }

  private static MockMultipartHttpServletRequestBuilder upload(String draft, String meta) {
    return multipart("/api/reports").part(jsonPart("draft", draft)).part(jsonPart("meta", meta));
  }

  private static MockMultipartFile jpeg() {
    return new MockMultipartFile("photo", "flood.jpg", "image/jpeg", JPEG_BYTES);
  }

  private static HazardReport pendingReport() {
    ReportDraft draft =
        new ReportDraft(
            "draft-1",
            HazardType.FLOOD,
            "Water is rising",
            new GeoPoint(6.5888, 79.9640, 9.5),
            true,
            District.KALUTARA,
            CAPTURED);
    return new HazardReport(
        REPORT_ID,
        SampleEntities.citizen("CIT-KLT-0001", "t"),
        draft,
        REPORT_ID + ".jpg",
        RECEIVED);
  }

  private static HazardReport verifiedReport() {
    return new HazardReport(
        "RPT-2026-KLT-0100",
        SampleEntities.citizen("CIT-KLT-0001", "t"),
        HazardType.FLOOD,
        "Water is rising",
        "RPT-2026-KLT-0100.jpg",
        new GeoPoint(6.5888, 79.9640, null),
        false,
        District.KALUTARA,
        CAPTURED,
        RECEIVED,
        ReportStatus.VERIFIED,
        RECEIVED.plusMinutes(30),
        null,
        SampleEntities.dutyOfficer("DMC-0002"),
        SampleEntities.hazard("HZ-2026-0142"));
  }

  private static HazardReport rejectedReport() {
    return new HazardReport(
        "RPT-2026-KLT-0101",
        SampleEntities.citizen("CIT-KLT-0001", "t"),
        HazardType.LANDSLIDE,
        "Slip on the road",
        "RPT-2026-KLT-0101.png",
        new GeoPoint(6.6, 80.0, 12.0),
        true,
        District.KALUTARA,
        CAPTURED,
        RECEIVED,
        ReportStatus.REJECTED,
        RECEIVED.plusMinutes(45),
        "Not a landslide",
        SampleEntities.dutyOfficer("DMC-0002"),
        null);
  }

  private static ReportReceipt receipt(
      SyncOutcome outcome, RejectionReason reason, String message, HazardReport report) {
    return new ReportReceipt(new SyncResult("draft-1", outcome, reason, message), report);
  }

  // ---- POST /api/reports ----------------------------------------------------------------------

  @Test
  @DisplayName("SC2 step 18: the upload's parts reach the control as a draft, a meta and a photo")
  void mapsTheThreePartsToTheControl() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(receipt(SyncOutcome.ACCEPTED, null, "received", pendingReport()));

    mockMvc.perform(upload(DRAFT_JSON, META_JSON).file(jpeg())).andExpect(status().isOk());

    ArgumentCaptor<ReportDraft> draft = ArgumentCaptor.forClass(ReportDraft.class);
    ArgumentCaptor<PhotoUpload> photo = ArgumentCaptor.forClass(PhotoUpload.class);
    ArgumentCaptor<ClientMeta> meta = ArgumentCaptor.forClass(ClientMeta.class);
    verify(reportSubmissionController)
        .receiveReport(draft.capture(), photo.capture(), meta.capture());
    assertThat(draft.getValue().draftId()).isEqualTo("draft-1");
    assertThat(draft.getValue().hazardType()).isEqualTo(HazardType.FLOOD);
    assertThat(draft.getValue().description()).isEqualTo("Water is rising");
    assertThat(draft.getValue().location()).isEqualTo(new GeoPoint(6.5888, 79.9640, 9.5));
    assertThat(draft.getValue().locationFromGps()).isTrue();
    assertThat(draft.getValue().district()).isEqualTo(District.KALUTARA);
    assertThat(draft.getValue().capturedAt()).isEqualTo(CAPTURED);
    assertThat(meta.getValue())
        .isEqualTo(new ClientMeta("client-1", "CIT-KLT-0001", "draft-1", CAPTURED, 0));
    assertThat(photo.getValue().fileName()).isEqualTo("flood.jpg");
    assertThat(photo.getValue().contentType()).isEqualTo("image/jpeg");
    assertThat(photo.getValue().content()).isEqualTo(JPEG_BYTES);
  }

  @Test
  @DisplayName("SC2 step 19: an ACCEPTED receipt is HTTP 200 with the report and its +05:30 times")
  void answersAnAcceptedReceipt() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(
            receipt(
                SyncOutcome.ACCEPTED, null, "Report " + REPORT_ID + " received", pendingReport()));

    mockMvc
        .perform(upload(DRAFT_JSON, META_JSON).file(jpeg()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.submissionId").value("draft-1"))
        .andExpect(jsonPath("$.result.outcome").value("ACCEPTED"))
        .andExpect(jsonPath("$.result.reason").value(nullValue()))
        .andExpect(jsonPath("$.result.message").value("Report " + REPORT_ID + " received"))
        .andExpect(jsonPath("$.report.reportId").value(REPORT_ID))
        .andExpect(jsonPath("$.report.hazardType").value("FLOOD"))
        .andExpect(jsonPath("$.report.description").value("Water is rising"))
        .andExpect(jsonPath("$.report.location.latitude").value(6.5888))
        .andExpect(jsonPath("$.report.location.longitude").value(79.964))
        .andExpect(jsonPath("$.report.location.accuracyMetres").value(9.5))
        .andExpect(jsonPath("$.report.locationFromGps").value(true))
        .andExpect(jsonPath("$.report.district").value("KALUTARA"))
        .andExpect(jsonPath("$.report.capturedAt").value("2026-05-21T07:40:00+05:30"))
        .andExpect(jsonPath("$.report.reportTime").value("2026-05-21T07:42:00+05:30"))
        .andExpect(jsonPath("$.report.status").value("PENDING_VERIFICATION"))
        .andExpect(jsonPath("$.report.verificationTime").value(nullValue()))
        .andExpect(jsonPath("$.report.rejectionReason").value(nullValue()))
        .andExpect(jsonPath("$.report.hazardId").value(nullValue()))
        .andExpect(jsonPath("$.report.photo").doesNotExist())
        .andExpect(jsonPath("$.report.reporter").doesNotExist());
  }

  @Test
  @DisplayName("SC2 E6 19b: a REJECTED receipt is HTTP 200 with the reason and no report")
  void answersARejectedReceiptAsHttp200() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(
            receipt(
                SyncOutcome.REJECTED,
                RejectionReason.VALIDATION_FAILED,
                "photo: not a readable JPEG or PNG image",
                null));

    mockMvc
        .perform(upload(DRAFT_JSON, META_JSON).file(jpeg()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("REJECTED"))
        .andExpect(jsonPath("$.result.reason").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.result.message").value("photo: not a readable JPEG or PNG image"))
        .andExpect(jsonPath("$.report").value(nullValue()));
  }

  @Test
  @DisplayName("SC2 E8 18g: a DUPLICATE receipt is HTTP 200 with the earlier report")
  void answersADuplicateReceipt() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(
            receipt(
                SyncOutcome.DUPLICATE, null, "Already received as " + REPORT_ID, verifiedReport()));

    mockMvc
        .perform(upload(DRAFT_JSON, META_JSON).file(jpeg()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("DUPLICATE"))
        .andExpect(jsonPath("$.result.message").value("Already received as " + REPORT_ID))
        .andExpect(jsonPath("$.report.reportId").value("RPT-2026-KLT-0100"))
        .andExpect(jsonPath("$.report.status").value("VERIFIED"))
        .andExpect(jsonPath("$.report.hazardId").value("HZ-2026-0142"))
        .andExpect(jsonPath("$.report.verificationTime").value("2026-05-21T08:12:00+05:30"));
  }

  @Test
  @DisplayName("SC2 E2: an upload without the photo part is not a 400, the control gets no photo")
  void passesOnAMissingPhoto() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(
            receipt(
                SyncOutcome.REJECTED,
                RejectionReason.VALIDATION_FAILED,
                "photo: one photo is required",
                null));

    mockMvc
        .perform(upload(DRAFT_JSON, META_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("REJECTED"));

    verify(reportSubmissionController).receiveReport(any(), isNull(), any());
  }

  @Test
  @DisplayName("SC2 E2: an empty photo part is passed on as a photo with empty content")
  void passesOnAnEmptyPhoto() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(
            receipt(SyncOutcome.REJECTED, RejectionReason.VALIDATION_FAILED, "empty", null));
    MockMultipartFile empty =
        new MockMultipartFile("photo", "empty.jpg", "image/jpeg", new byte[0]);

    mockMvc.perform(upload(DRAFT_JSON, META_JSON).file(empty)).andExpect(status().isOk());

    ArgumentCaptor<PhotoUpload> photo = ArgumentCaptor.forClass(PhotoUpload.class);
    verify(reportSubmissionController).receiveReport(any(), photo.capture(), any());
    assertThat(photo.getValue().content()).isEmpty();
    assertThat(photo.getValue().fileName()).isEqualTo("empty.jpg");
  }

  @Test
  @DisplayName(
      "SC2 step 18: a photo part without a name or a content type gets the stated defaults")
  void givesTheDefaultsToAnUnnamedPhoto() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(receipt(SyncOutcome.REJECTED, RejectionReason.VALIDATION_FAILED, "x", null));
    MockMultipartFile bare = new MockMultipartFile("photo", "", null, JPEG_BYTES);

    mockMvc.perform(upload(DRAFT_JSON, META_JSON).file(bare)).andExpect(status().isOk());

    ArgumentCaptor<PhotoUpload> photo = ArgumentCaptor.forClass(PhotoUpload.class);
    verify(reportSubmissionController).receiveReport(any(), photo.capture(), any());
    assertThat(photo.getValue().fileName()).isEqualTo("photo");
    assertThat(photo.getValue().contentType()).isEqualTo("application/octet-stream");
  }

  @Test
  @DisplayName("SC2 E1: a draft with only its structural fields is passed on to validateReport")
  void passesOnAnIncompleteDraft() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(
            receipt(
                SyncOutcome.REJECTED, RejectionReason.VALIDATION_FAILED, "hazardType: …", null));
    String bare =
        "{\"draftId\":\"draft-1\",\"locationFromGps\":false,"
            + "\"capturedAt\":\"2026-05-21T07:40:00+05:30\"}";

    mockMvc
        .perform(upload(bare, META_JSON).file(jpeg()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("REJECTED"));

    ArgumentCaptor<ReportDraft> draft = ArgumentCaptor.forClass(ReportDraft.class);
    verify(reportSubmissionController).receiveReport(draft.capture(), any(), any());
    assertThat(draft.getValue().hazardType()).isNull();
    assertThat(draft.getValue().description()).isNull();
    assertThat(draft.getValue().location()).isNull();
    assertThat(draft.getValue().district()).isNull();
  }

  static Stream<Arguments> malformedUploads() {
    String noCapturedAt = DRAFT_JSON.replace(",\"capturedAt\":\"2026-05-21T07:40:00+05:30\"", "");
    return Stream.of(
        Arguments.of("malformed draft JSON", "{not json", META_JSON),
        Arguments.of("malformed meta JSON", DRAFT_JSON, "[1, 2"),
        Arguments.of("a blank draftId", DRAFT_JSON.replace("draft-1", " "), META_JSON),
        Arguments.of(
            "a missing draftId", DRAFT_JSON.replace("\"draftId\":\"draft-1\",", ""), META_JSON),
        Arguments.of("a missing capturedAt in the draft", noCapturedAt, META_JSON),
        Arguments.of(
            "a capturedAt without an offset",
            DRAFT_JSON.replace("07:40:00+05:30", "07:40:00"),
            META_JSON),
        Arguments.of("an unknown hazard type", DRAFT_JSON.replace("FLOOD", "VOLCANO"), META_JSON),
        Arguments.of("an unknown district", DRAFT_JSON.replace("KALUTARA", "ATLANTIS"), META_JSON),
        Arguments.of(
            "a location without its latitude",
            DRAFT_JSON.replace("\"latitude\":6.5888,", ""),
            META_JSON),
        Arguments.of(
            "a missing locationFromGps",
            DRAFT_JSON.replace("\"locationFromGps\":true,", ""),
            META_JSON),
        Arguments.of("a blank clientId", DRAFT_JSON, META_JSON.replace("client-1", "")),
        Arguments.of("a blank userId", DRAFT_JSON, META_JSON.replace("CIT-KLT-0001", " ")),
        Arguments.of(
            "a missing submissionId",
            DRAFT_JSON,
            META_JSON.replace("\"submissionId\":\"draft-1\",", "")),
        Arguments.of(
            "a missing capturedAt in the meta",
            DRAFT_JSON,
            META_JSON.replace(",\"capturedAt\":\"2026-05-21T02:10:00Z\"", "")),
        Arguments.of("a negative pendingCount", DRAFT_JSON, META_JSON.replace(":0}", ":-1}")),
        Arguments.of(
            "a missing pendingCount", DRAFT_JSON, META_JSON.replace(",\"pendingCount\":0", "")));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("malformedUploads")
  @DisplayName("SC2 E6: a malformed upload is a 400 problem detail and the control is not called")
  void refusesAMalformedUpload(String ignoredCase, String draftJson, String metaJson)
      throws Exception {
    mockMvc
        .perform(upload(draftJson, metaJson).file(jpeg()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty());

    verifyNoInteractions(reportSubmissionController);
  }

  @Test
  @DisplayName("SC2 E6: an upload without the draft part is a 400 and the control is not called")
  void refusesAnUploadWithoutTheDraft() throws Exception {
    mockMvc
        .perform(multipart("/api/reports").part(jsonPart("meta", META_JSON)).file(jpeg()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(reportSubmissionController);
  }

  @Test
  @DisplayName("SC2 E6: an upload without the meta part is a 400 and the control is not called")
  void refusesAnUploadWithoutTheMeta() throws Exception {
    mockMvc
        .perform(multipart("/api/reports").part(jsonPart("draft", DRAFT_JSON)).file(jpeg()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(reportSubmissionController);
  }

  @Test
  @DisplayName(
      "P186 characterisation (MockMvc): a draft part sent without a Content-Type is refused")
  void characterisesADraftPartWithoutAContentType() throws Exception {
    MockPart untyped = new MockPart("draft", "draft", DRAFT_JSON.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/api/reports").part(untyped).part(jsonPart("meta", META_JSON)).file(jpeg()))
        .andExpect(status().isUnsupportedMediaType());

    verifyNoInteractions(reportSubmissionController);
  }

  // ---- GET /api/reports/{reportId} --------------------------------------------------------------

  @Test
  @DisplayName("SC2 step 20: GET a pending report returns its wire record with +05:30 times")
  void returnsAPendingReport() throws Exception {
    when(reportSubmissionController.getReportStatus(REPORT_ID)).thenReturn(pendingReport());

    mockMvc
        .perform(get("/api/reports/{id}", REPORT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reportId").value(REPORT_ID))
        .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
        .andExpect(jsonPath("$.capturedAt").value("2026-05-21T07:40:00+05:30"))
        .andExpect(jsonPath("$.reportTime").value("2026-05-21T07:42:00+05:30"))
        .andExpect(jsonPath("$.verificationTime").value(nullValue()))
        .andExpect(jsonPath("$.hazardId").value(nullValue()));
  }

  @Test
  @DisplayName("SC2 step 30: GET a verified report shows the linked hazard and the decision time")
  void returnsAVerifiedReport() throws Exception {
    when(reportSubmissionController.getReportStatus("RPT-2026-KLT-0100"))
        .thenReturn(verifiedReport());

    mockMvc
        .perform(get("/api/reports/{id}", "RPT-2026-KLT-0100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VERIFIED"))
        .andExpect(jsonPath("$.hazardId").value("HZ-2026-0142"))
        .andExpect(jsonPath("$.verificationTime").value("2026-05-21T08:12:00+05:30"))
        .andExpect(jsonPath("$.location.accuracyMetres").value(nullValue()))
        .andExpect(jsonPath("$.locationFromGps").value(false));
  }

  @Test
  @DisplayName("SC2 step 30: GET a rejected report shows the reason")
  void returnsARejectedReport() throws Exception {
    when(reportSubmissionController.getReportStatus("RPT-2026-KLT-0101"))
        .thenReturn(rejectedReport());

    mockMvc
        .perform(get("/api/reports/{id}", "RPT-2026-KLT-0101"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.rejectionReason").value("Not a landslide"))
        .andExpect(jsonPath("$.hazardId").value(nullValue()));
  }

  @Test
  @DisplayName("SC2 step 20: GET an unknown report is a 404 problem detail")
  void answersNotFoundForAnUnknownReport() throws Exception {
    when(reportSubmissionController.getReportStatus("RPT-0"))
        .thenThrow(new NoSuchElementException());

    mockMvc
        .perform(get("/api/reports/{id}", "RPT-0"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404));
  }

  // ---- GET /api/citizens/{citizenId}/reports
  // ------------------------------------------------------

  @Test
  @DisplayName("SC2 step 2: GET the citizen's reports returns the array in the control's order")
  void returnsTheCitizensReportsInOrder() throws Exception {
    when(reportSubmissionController.getReportsByReporter("CIT-KLT-0001"))
        .thenReturn(List.of(pendingReport(), verifiedReport(), rejectedReport()));

    mockMvc
        .perform(get("/api/citizens/{id}/reports", "CIT-KLT-0001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].reportId").value(REPORT_ID))
        .andExpect(jsonPath("$[1].reportId").value("RPT-2026-KLT-0100"))
        .andExpect(jsonPath("$[2].reportId").value("RPT-2026-KLT-0101"))
        .andExpect(jsonPath("$[1].hazardId").value("HZ-2026-0142"));
  }

  @Test
  @DisplayName("SC2 step 2: GET the reports of a citizen without any is an empty array")
  void returnsAnEmptyArray() throws Exception {
    when(reportSubmissionController.getReportsByReporter("CIT-KLT-0002")).thenReturn(List.of());

    mockMvc
        .perform(get("/api/citizens/{id}/reports", "CIT-KLT-0002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("SC2 step 2: GET the reports of an unknown citizen is a 404 problem detail")
  void answersNotFoundForAnUnknownCitizen() throws Exception {
    when(reportSubmissionController.getReportsByReporter("CIT-0"))
        .thenThrow(new NoSuchElementException());

    mockMvc
        .perform(get("/api/citizens/{id}/reports", "CIT-0"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  // ---- PUT /api/citizens/{citizenId}/language
  // ------------------------------------------------------

  @Test
  @DisplayName("SD2 2.1.1: PUT the language is 204 and the control gets the language")
  void changesTheLanguage() throws Exception {
    mockMvc
        .perform(
            put("/api/citizens/{id}/language", "CIT-KLT-0001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"SINHALA\"}"))
        .andExpect(status().isNoContent());

    verify(reportSubmissionController).changePreferredLanguage("CIT-KLT-0001", Language.SINHALA);
  }

  @ParameterizedTest(name = "[{index}] body {0}")
  @ValueSource(
      strings = {"{\"language\":\"ESPERANTO\"}", "{}", "{\"language\":null}", "not json", ""})
  @DisplayName(
      "SD2 2.1.1: PUT a missing or unknown language is a 400 and the control is not called")
  void refusesAnInvalidLanguageBody(String body) throws Exception {
    mockMvc
        .perform(
            put("/api/citizens/{id}/language", "CIT-KLT-0001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(reportSubmissionController);
  }

  @Test
  @DisplayName("SD2 2.1.1: PUT the language of an unknown citizen is a 404 problem detail")
  void answersNotFoundWhenTheCitizenIsUnknown() throws Exception {
    doThrow(new NoSuchElementException())
        .when(reportSubmissionController)
        .changePreferredLanguage("CIT-0", Language.TAMIL);

    mockMvc
        .perform(
            put("/api/citizens/{id}/language", "CIT-0")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"TAMIL\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
