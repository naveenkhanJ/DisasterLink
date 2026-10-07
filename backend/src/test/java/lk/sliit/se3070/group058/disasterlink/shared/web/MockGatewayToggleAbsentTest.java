package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lk.sliit.se3070.group058.disasterlink.shared.gateway.MockGatewayFailureRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shows that {@link MockGatewayToggleController} does not exist without the {@code dev} profile.
 */
@WebMvcTest(MockGatewayToggleController.class)
class MockGatewayToggleAbsentTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ApplicationContext context;

  @MockitoBean private MockGatewayFailureRule rule;

  @Test
  @DisplayName("Without the dev profile there is no toggle controller and its path is not found")
  void controllerIsAbsent() throws Exception {
    assertThat(context.getBeanNamesForType(MockGatewayToggleController.class)).isEmpty();

    mockMvc.perform(delete("/dev/mock-gateways/sms/failures")).andExpect(status().isNotFound());
  }
}
