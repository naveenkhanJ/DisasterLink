package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportSubmissionController;
import lk.sliit.se3070.group058.disasterlink.shared.config.PhotoStorageProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests of {@code GET /reports/{reportId}/photo}: a stored JPEG or PNG is served with the content
 * type of its extension, and everything else is a 404 problem detail.
 */
@WebMvcTest(ReportPhotoController.class)
class ReportPhotoControllerTest {

  private static final byte[] JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x01
  };
  private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

  @TempDir private Path tempDir;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReportSubmissionController reportSubmissionController;

  @MockitoBean private PhotoStorageProperties photoStorage;

  private Path photoDirectory;

  @BeforeEach
  void createPhotoDirectory() throws IOException {
    photoDirectory = Files.createDirectory(tempDir.resolve("photos"));
    when(photoStorage.storageDir()).thenReturn(photoDirectory.toString());
  }

  private void reportWithPhoto(String reportId, String photoName) {
    HazardReport report =
        new HazardReport(
            reportId,
            SampleEntities.citizen("CIT-KLT-0001", "t"),
            HazardType.FLOOD,
            "Water is rising",
            photoName,
            new GeoPoint(6.5888, 79.9640, null),
            true,
            District.KALUTARA,
            LocalDateTime.of(2026, 5, 21, 7, 40),
            LocalDateTime.of(2026, 5, 21, 7, 42),
            ReportStatus.PENDING_VERIFICATION,
            null,
            null,
            null,
            null);
    when(reportSubmissionController.getReportStatus(reportId)).thenReturn(report);
  }

  @Test
  @DisplayName("SC2 step 24: a stored JPEG is served with image/jpeg and its bytes")
  void servesAStoredJpeg() throws Exception {
    Files.write(photoDirectory.resolve("RPT-1.jpg"), JPEG_BYTES);
    reportWithPhoto("RPT-1", "RPT-1.jpg");

    mockMvc
        .perform(get("/reports/{id}/photo", "RPT-1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(content().bytes(JPEG_BYTES));
  }

  @Test
  @DisplayName("SC2 step 24: a stored PNG is served with image/png and its bytes")
  void servesAStoredPng() throws Exception {
    Files.write(photoDirectory.resolve("RPT-2.png"), PNG_BYTES);
    reportWithPhoto("RPT-2", "RPT-2.png");

    mockMvc
        .perform(get("/reports/{id}/photo", "RPT-2"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(PNG_BYTES));
  }

  @ParameterizedTest(name = "[{index}] extension {0}")
  @ValueSource(strings = {"seed.jpeg", "SEED.JPG", "seed.JPEG"})
  @DisplayName("SC2 step 24: .jpeg and upper-case extensions are served as image/jpeg")
  void servesTheOtherJpegSpellings(String name) throws Exception {
    Files.write(photoDirectory.resolve(name), JPEG_BYTES);
    reportWithPhoto("RPT-3", name);

    mockMvc
        .perform(get("/reports/{id}/photo", "RPT-3"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(content().bytes(JPEG_BYTES));
  }

  @Test
  @DisplayName("SC2 step 24: a photo of an unknown report is a 404 problem detail")
  void answersNotFoundForAnUnknownReport() throws Exception {
    when(reportSubmissionController.getReportStatus("RPT-0"))
        .thenThrow(new NoSuchElementException());

    mockMvc
        .perform(get("/reports/{id}/photo", "RPT-0"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @DisplayName("SC2 step 24: a report whose file is missing is a 404 problem detail")
  void answersNotFoundForAMissingFile() throws Exception {
    reportWithPhoto("RPT-4", "RPT-4.jpg");

    mockMvc
        .perform(get("/reports/{id}/photo", "RPT-4"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @DisplayName("SC2 step 24: a stored name that is a directory is a 404, not a regular file")
  void answersNotFoundForADirectory() throws Exception {
    Files.createDirectory(photoDirectory.resolve("folder.jpg"));
    reportWithPhoto("RPT-5", "folder.jpg");

    mockMvc.perform(get("/reports/{id}/photo", "RPT-5")).andExpect(status().isNotFound());
  }

  @ParameterizedTest(name = "[{index}] stored name \"{0}\"")
  @ValueSource(
      strings = {
        "../secret.jpg",
        "..\\secret.jpg",
        "a/b.jpg",
        "a\\b.jpg",
        "/secret.jpg",
        "secret..jpg",
        "..",
        "bad\u0000name.jpg"
      })
  @DisplayName("SC2 step 24: a stored name that is not a plain file name is a 404")
  void refusesANameThatIsNotAPlainFileName(String name) throws Exception {
    Files.write(tempDir.resolve("secret.jpg"), JPEG_BYTES);
    reportWithPhoto("RPT-6", name);

    mockMvc
        .perform(get("/reports/{id}/photo", "RPT-6"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @ParameterizedTest(name = "[{index}] stored name \"{0}\"")
  @ValueSource(strings = {"photo.gif", "photo.txt", "photo", "photo.", ".hidden", ""})
  @DisplayName("SC2 step 24: a name with an extension other than .jpg, .jpeg or .png is a 404")
  void refusesAnUnsupportedExtension(String name) throws Exception {
    if (!name.isEmpty()) {
      Files.write(photoDirectory.resolve(name), JPEG_BYTES);
    }
    reportWithPhoto("RPT-7", name);

    mockMvc
        .perform(get("/reports/{id}/photo", "RPT-7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
