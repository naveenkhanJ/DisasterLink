package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lk.sliit.se3070.group058.testprobe.PageProbeController;
import lk.sliit.se3070.group058.testprobe.ProblemProbeController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Tests the error views against a real server on a random port: MockMvc does not perform the
 * servlet container's error dispatch, so the generic 5xx page and the JSON problem are checked over
 * HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PageProbeController.class, ProblemProbeController.class})
class ErrorPageTest {

  @LocalServerPort private int port;

  private HttpResponse<String> get(String path, String accept) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", accept)
            .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  @DisplayName(
      "A page controller that throws gets the generic 5xx page with the common error banner")
  void pageRequestGetsTheErrorPage() throws Exception {
    HttpResponse<String> response = get("/probe-page/fail", "text/html");

    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("text/html"));
    assertThat(response.body())
        .contains("The service is unavailable and nothing was changed. Please try again.")
        .contains("role=\"alert\"")
        .contains("href=\"/\"")
        .doesNotContain("boom");
  }

  @Test
  @DisplayName("A JSON request keeps the JSON problem of ApiExceptionHandler")
  void jsonRequestKeepsTheProblem() throws Exception {
    HttpResponse<String> response = get("/probe/failure", "application/json");

    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).contains("problem+json"));
    assertThat(response.body())
        .contains("\"status\":500")
        .contains("The service is unavailable and nothing was changed.");
  }
}
