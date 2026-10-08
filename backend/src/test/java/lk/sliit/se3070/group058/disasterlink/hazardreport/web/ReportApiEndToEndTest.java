package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.util.FileSystemUtils;

/**
 * The UC2 endpoints end to end against H2, with the real control, repositories, id generator and
 * transactions (the test profile seeds nothing): a real multipart upload, then the report, the
 * citizen's list and the photo read back. Open-in-view is off, so the answers with the report's
 * lazy links can only come from the transactions of the endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "photo.storage-dir=target/test-photos/uc2-web")
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ReportApiEndToEndTest {

  private static final Path PHOTO_DIRECTORY = Path.of("target", "test-photos", "uc2-web");
  private static final String CITIZEN_ID = "CIT-KLT-0001";
  private static final byte[] JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46
  };

  private static final String DRAFT_JSON =
      "{\"draftId\":\"draft-1\",\"hazardType\":\"FLOOD\",\"description\":\"Water is rising\","
          + "\"location\":{\"latitude\":6.5888,\"longitude\":79.9640,\"accuracyMetres\":9.5},"
          + "\"locationFromGps\":true,\"district\":\"KALUTARA\","
          + "\"capturedAt\":\"2026-05-21T07:40:00+05:30\"}";

  private static final String META_JSON =
      "{\"clientId\":\"client-1\",\"userId\":\"CIT-KLT-0001\",\"submissionId\":\"draft-1\","
          + "\"capturedAt\":\"2026-05-21T07:40:00+05:30\",\"pendingCount\":0}";

  @Autowired private MockMvc mockMvc;
  @Autowired private CitizenRepository citizens;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void prepare() {
    jdbcTemplate.execute("ALTER SEQUENCE hazard_report_id_seq RESTART WITH 847");
    citizens.save(FeatureFixtures.citizenIn(CITIZEN_ID, District.KALUTARA));
  }

  @AfterEach
  void removePhotos() {
    FileSystemUtils.deleteRecursively(PHOTO_DIRECTORY.toFile());
  }

  private static MockPart jsonPart(String name, String json) {
    MockPart part = new MockPart(name, name, json.getBytes(StandardCharsets.UTF_8));
    part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return part;
  }

  private static MockMultipartHttpServletRequestBuilder upload() {
    return multipart("/api/reports")
        .part(jsonPart("draft", DRAFT_JSON))
        .part(jsonPart("meta", META_JSON));
  }

  private static MockMultipartFile photo() {
    return new MockMultipartFile("photo", "flood.jpg", "image/jpeg", JPEG_BYTES);
  }

  @Test
  @DisplayName("SC2 steps 18 to 20: upload, then read the report, the list and the photo back")
  void uploadsAReportAndReadsItBack() throws Exception {
    MvcResult accepted =
        mockMvc
            .perform(upload().file(photo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.outcome").value("ACCEPTED"))
            .andExpect(jsonPath("$.report.status").value("PENDING_VERIFICATION"))
            .andExpect(jsonPath("$.report.district").value("KALUTARA"))
            .andExpect(jsonPath("$.report.capturedAt").value("2026-05-21T07:40:00+05:30"))
            .andReturn();
    String reportId =
        JsonPath.read(
            accepted.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.report.reportId");
    assertThat(reportId).matches("RPT-\\d{4}-KLT-0847");

    mockMvc
        .perform(get("/api/reports/{id}", reportId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reportId").value(reportId))
        .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
        .andExpect(jsonPath("$.hazardId").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.reportTime").value(org.hamcrest.Matchers.endsWith("+05:30")));
    mockMvc
        .perform(get("/api/citizens/{id}/reports", CITIZEN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].reportId").value(reportId));
    mockMvc
        .perform(get("/reports/{id}/photo", reportId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(content().bytes(JPEG_BYTES));
  }

  @Test
  @DisplayName("SC2 E8 18g: the same upload again is a DUPLICATE carrying the earlier report")
  void secondIdenticalUploadIsDuplicate() throws Exception {
    mockMvc.perform(upload().file(photo())).andExpect(status().isOk());

    mockMvc
        .perform(upload().file(photo()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("DUPLICATE"))
        .andExpect(jsonPath("$.report.reportId").value(org.hamcrest.Matchers.startsWith("RPT-")))
        .andExpect(jsonPath("$.report.status").value("PENDING_VERIFICATION"));
    mockMvc
        .perform(get("/api/citizens/{id}/reports", CITIZEN_ID))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @DisplayName(
      "SC2 E2 and E6: an upload without a photo is a 200 REJECTED reply and stores nothing")
  void anUploadWithoutAPhotoIsRejected() throws Exception {
    mockMvc
        .perform(upload())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("REJECTED"))
        .andExpect(jsonPath("$.result.reason").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.result.message").value("photo: one photo is required"))
        .andExpect(jsonPath("$.report").value(org.hamcrest.Matchers.nullValue()));
    mockMvc
        .perform(get("/api/citizens/{id}/reports", CITIZEN_ID))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("SC2 E6: an upload from an unknown reporter is a 404 problem detail")
  void anUploadFromAnUnknownReporterIsNotFound() throws Exception {
    MockMultipartHttpServletRequestBuilder stranger =
        multipart("/api/reports")
            .part(jsonPart("draft", DRAFT_JSON))
            .part(jsonPart("meta", META_JSON.replace(CITIZEN_ID, "CIT-0000")));

    mockMvc
        .perform(stranger.file(photo()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @DisplayName("SD2 2.1.1: PUT the language is stored; an unknown citizen is a 404")
  void changesTheLanguageOfAStoredCitizen() throws Exception {
    mockMvc
        .perform(
            put("/api/citizens/{id}/language", CITIZEN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"TAMIL\"}"))
        .andExpect(status().isNoContent());
    assertThat(citizens.findById(CITIZEN_ID).orElseThrow().getPreferredLanguage())
        .isEqualTo(Language.TAMIL);

    mockMvc
        .perform(
            put("/api/citizens/{id}/language", "CIT-0000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"TAMIL\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("SC2 step 20: an unknown report and its photo are 404 problem details")
  void unknownReportIsNotFound() throws Exception {
    mockMvc.perform(get("/api/reports/{id}", "RPT-0")).andExpect(status().isNotFound());
    mockMvc.perform(get("/reports/{id}/photo", "RPT-0")).andExpect(status().isNotFound());
  }
}
