package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lk.sliit.se3070.group058.testprobe.ProblemProbeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests of {@link ApiExceptionHandler}, one per status path. The advice is tested with a standalone
 * MockMvc around a test-only controller, so the controller never becomes part of an application
 * context.
 */
@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ProblemProbeController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("A body that Bean Validation rejects is a 400 problem")
  void invalidBody() throws Exception {
    mockMvc
        .perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.BAD_REQUEST_DETAIL));
  }

  @Test
  @DisplayName("A body that is not JSON is a 400 problem")
  void unreadableBody() throws Exception {
    mockMvc
        .perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.BAD_REQUEST_DETAIL));
  }

  @Test
  @DisplayName("A parameter of the wrong type is a 400 problem")
  void typeMismatch() throws Exception {
    mockMvc
        .perform(get("/probe/parameter").param("count", "many"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("A parameter that breaks its constraint is a 400 problem")
  void constraintViolation() throws Exception {
    mockMvc
        .perform(get("/probe/parameter").param("count", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.BAD_REQUEST_DETAIL));
  }

  @Test
  @DisplayName("A missing multipart part is a 400 problem")
  void missingPart() throws Exception {
    mockMvc
        .perform(multipart("/probe/part"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("NoSuchElementException is a 404 problem")
  void unknownIdentifier() throws Exception {
    mockMvc
        .perform(get("/probe/missing"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.NOT_FOUND_DETAIL));
  }

  @Test
  @DisplayName("An exception with @ResponseStatus keeps that status and its message")
  void declaredStatus() throws Exception {
    mockMvc
        .perform(get("/probe/declared"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value("The report was already decided"));
  }

  @Test
  @DisplayName("A framework error keeps its own status and detail")
  void frameworkStatus() throws Exception {
    mockMvc
        .perform(get("/probe/framework"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.detail").value("Cannot process this"));
  }

  @Test
  @DisplayName(
      "Anything else is a 500 problem with a safe detail, and is logged with the exception")
  void unexpectedFailure(CapturedOutput output) throws Exception {
    mockMvc
        .perform(get("/probe/failure"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(
            jsonPath("$.detail").value("The service is unavailable and nothing was changed."))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));

    assertThat(output.getOut())
        .contains("ERROR")
        .contains("Unexpected error in the JSON API")
        .contains("java.lang.IllegalStateException: secret internal detail");
  }
}
