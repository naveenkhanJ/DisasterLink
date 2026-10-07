package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.MockGatewayFailureRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Tests of {@link MockGatewayToggleController} in the {@code dev} profile. */
@WebMvcTest(MockGatewayToggleController.class)
@ActiveProfiles("dev")
class MockGatewayToggleControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MockGatewayFailureRule rule;

  @Test
  @DisplayName("POST sms/failures sets the SMS prefixes and replies 204")
  void setSmsPrefixes() throws Exception {
    mockMvc
        .perform(
            post("/dev/mock-gateways/sms/failures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prefixes\": [\"CIT-COL-\", \"CIT-GAM-\"]}"))
        .andExpect(status().isNoContent());

    verify(rule).setSmsPrefixes(List.of("CIT-COL-", "CIT-GAM-"));
  }

  @Test
  @DisplayName("POST push/failures sets the push prefixes")
  void setPushPrefixes() throws Exception {
    mockMvc
        .perform(
            post("/dev/mock-gateways/push/failures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prefixes\": [\"CIT-RAT-\"]}"))
        .andExpect(status().isNoContent());

    verify(rule).setPushPrefixes(List.of("CIT-RAT-"));
  }

  @Test
  @DisplayName("DELETE clears the prefixes of the channel and replies 204")
  void clearPrefixes() throws Exception {
    mockMvc.perform(delete("/dev/mock-gateways/sms/failures")).andExpect(status().isNoContent());
    mockMvc.perform(delete("/dev/mock-gateways/push/failures")).andExpect(status().isNoContent());

    verify(rule).clearSmsPrefixes();
    verify(rule).clearPushPrefixes();
  }

  @Test
  @DisplayName("An unknown channel is a 404 problem and changes nothing")
  void unknownChannel() throws Exception {
    mockMvc
        .perform(
            post("/dev/mock-gateways/fax/failures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prefixes\": []}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    mockMvc.perform(delete("/dev/mock-gateways/fax/failures")).andExpect(status().isNotFound());

    verifyNoInteractions(rule);
  }

  @Test
  @DisplayName("A body without prefixes is a 400 problem")
  void missingPrefixes() throws Exception {
    mockMvc
        .perform(
            post("/dev/mock-gateways/sms/failures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(rule);
  }
}
