package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.ReportReceipt;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportSubmissionController;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Characterises, against the real embedded Tomcat, how the upload endpoint treats a JSON part that
 * is sent without a {@code Content-Type}, which is what React Native's {@code FormData} does with a
 * plain string part (standards section 12.3, "Upload": believed, not verified). The control is
 * mocked; the request is a hand-built {@code multipart/form-data} body sent over HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportUploadHttpTest {

  private static final String BOUNDARY = "----DisasterLinkBoundary";
  private static final String CRLF = "\r\n";

  private static final String DRAFT_JSON =
      "{\"draftId\":\"draft-1\",\"hazardType\":\"FLOOD\",\"description\":\"Water is rising\","
          + "\"location\":{\"latitude\":6.5888,\"longitude\":79.9640,\"accuracyMetres\":9.5},"
          + "\"locationFromGps\":true,\"district\":\"KALUTARA\","
          + "\"capturedAt\":\"2026-05-21T07:40:00+05:30\"}";

  private static final String META_JSON =
      "{\"clientId\":\"client-1\",\"userId\":\"CIT-KLT-0001\",\"submissionId\":\"draft-1\","
          + "\"capturedAt\":\"2026-05-21T07:40:00+05:30\",\"pendingCount\":0}";

  @LocalServerPort private int port;

  @MockitoBean private ReportSubmissionController reportSubmissionController;

  /** Builds the body of the upload; {@code draftContentType} is omitted from the part if null. */
  private static byte[] body(String draftContentType) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(textPart("draft", draftContentType, DRAFT_JSON).getBytes(StandardCharsets.UTF_8));
    out.write(textPart("meta", "application/json", META_JSON).getBytes(StandardCharsets.UTF_8));
    out.write(
        ("--"
                + BOUNDARY
                + CRLF
                + "Content-Disposition: form-data; name=\"photo\"; filename=\"flood.jpg\""
                + CRLF
                + "Content-Type: image/jpeg"
                + CRLF
                + CRLF)
            .getBytes(StandardCharsets.UTF_8));
    out.write(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    out.write((CRLF + "--" + BOUNDARY + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
    return out.toByteArray();
  }

  private static String textPart(String name, String contentType, String value) {
    return "--"
        + BOUNDARY
        + CRLF
        + "Content-Disposition: form-data; name=\""
        + name
        + "\""
        + CRLF
        + (contentType == null ? "" : "Content-Type: " + contentType + CRLF)
        + CRLF
        + value
        + CRLF;
  }

  private HttpResponse<String> post(String draftContentType) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/reports"))
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body(draftContentType)))
            .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  @DisplayName(
      "P186 control: over real HTTP a draft part with Content-Type application/json is accepted")
  void jsonTypedDraftPartIsAccepted() throws Exception {
    when(reportSubmissionController.receiveReport(any(), any(), any()))
        .thenReturn(
            new ReportReceipt(
                new SyncResult(
                    "draft-1",
                    SyncOutcome.REJECTED,
                    RejectionReason.VALIDATION_FAILED,
                    "photo: not a readable JPEG or PNG image"),
                null));

    HttpResponse<String> response = post("application/json");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"outcome\":\"REJECTED\"");
    verify(reportSubmissionController).receiveReport(any(), any(), any());
  }

  @Test
  @DisplayName(
      "P186 characterisation: over real HTTP a draft part without a Content-Type is refused")
  void draftPartWithoutContentTypeIsRefused() throws Exception {
    HttpResponse<String> response = post(null);

    assertThat(response.statusCode()).isEqualTo(415);
    verifyNoInteractions(reportSubmissionController);
  }
}
