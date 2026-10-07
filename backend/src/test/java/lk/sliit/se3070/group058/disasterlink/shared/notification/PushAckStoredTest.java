package lk.sliit.se3070.group058.disasterlink.shared.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests {@code POST /api/push/{recipientId}/outbox/ack} against the real repository and database
 * (not a mock): the acknowledged entry gets its {@code deliveredAt} stored, and another recipient's
 * entry, even when its identifier is sent, is left alone (P155 D-14).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PushAckStoredTest {

  private static final LocalDateTime CREATED = LocalDateTime.of(2026, 5, 21, 8, 30);

  @Autowired private MockMvc mockMvc;

  @Autowired private PushOutboxRepository outbox;

  @AfterEach
  void clean() {
    outbox.deleteAll();
  }

  private PushOutbox save(String recipientId, String warningId) {
    return outbox.save(
        PushOutbox.forWarningAlert(
            recipientId, warningId, WarningLevel.LEVEL_2_AMBER, "Move now", CREATED));
  }

  @Test
  @DisplayName("Acknowledging stores deliveredAt for the recipient's entry only")
  void acknowledgeStoresDeliveredAtForTheOwnEntryOnly() throws Exception {
    PushOutbox own = save("CIT-KLT-0001", "WRN-1");
    PushOutbox other = save("CIT-KLT-0002", "WRN-1");
    LocalDateTime before = LocalDateTime.now().minusMinutes(1);

    mockMvc
        .perform(
            post("/api/push/CIT-KLT-0001/outbox/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[\"" + own.getId() + "\",\"" + other.getId() + "\"]}"))
        .andExpect(status().isNoContent());

    PushOutbox storedOwn = outbox.findById(own.getId()).orElseThrow();
    PushOutbox storedOther = outbox.findById(other.getId()).orElseThrow();
    assertThat(storedOwn.getDeliveredAt()).hasValueSatisfying(at -> assertThat(at).isAfter(before));
    assertThat(storedOther.getDeliveredAt()).isEmpty();
  }

  @Test
  @DisplayName("After the acknowledgement the entry leaves the poll; the other recipient's stays")
  void acknowledgedEntryLeavesThePoll() throws Exception {
    PushOutbox own = save("CIT-KLT-0001", "WRN-1");
    save("CIT-KLT-0002", "WRN-1");

    mockMvc
        .perform(get("/api/push/CIT-KLT-0001/outbox"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mockMvc
        .perform(
            post("/api/push/CIT-KLT-0001/outbox/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[\"" + own.getId() + "\"]}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/push/CIT-KLT-0001/outbox"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mockMvc
        .perform(get("/api/push/CIT-KLT-0002/outbox"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }
}
