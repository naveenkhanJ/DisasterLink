package lk.sliit.se3070.group058.disasterlink.shared.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Tests of {@link PushOutboxController} through MockMvc. */
@WebMvcTest(PushOutboxController.class)
@Import(PushOutboxControllerTest.FixedClockConfiguration.class)
class PushOutboxControllerTest {

  private static final LocalDateTime CREATED = LocalDateTime.of(2026, 5, 21, 8, 30);

  /** A clock fixed at 09:00 in Colombo. */
  @TestConfiguration
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-05-21T03:30:00Z"), ZoneId.of("Asia/Colombo"));
    }
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PushOutboxRepository outbox;

  @Test
  @DisplayName("GET returns the undelivered entries with +05:30 times and leaves out empty fields")
  void pollShape() throws Exception {
    PushOutbox alert =
        PushOutbox.forWarningAlert(
            "CIT-1", "WRN-1", WarningLevel.LEVEL_2_AMBER, "Move now", CREATED);
    PushOutbox decision =
        PushOutbox.forReportDecision(
            "CIT-1",
            new ReportDecisionNotice(
                "RPT-1", ReportStatus.REJECTED, "Not a flood", LocalDateTime.of(2026, 5, 21, 8, 0)),
            "Sorry",
            CREATED.plusMinutes(1));
    when(outbox.findUndelivered("CIT-1")).thenReturn(List.of(alert, decision));

    mockMvc
        .perform(get("/api/push/CIT-1/outbox"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(alert.getId()))
        .andExpect(jsonPath("$[0].kind").value("WARNING_ALERT"))
        .andExpect(jsonPath("$[0].warningId").value("WRN-1"))
        .andExpect(jsonPath("$[0].warningLevel").value("LEVEL_2_AMBER"))
        .andExpect(jsonPath("$[0].text").value("Move now"))
        .andExpect(jsonPath("$[0].createdAt").value("2026-05-21T08:30:00+05:30"))
        .andExpect(jsonPath("$[0].reportId").doesNotExist())
        .andExpect(jsonPath("$[0].status").doesNotExist())
        .andExpect(jsonPath("$[0].decidedAt").doesNotExist())
        .andExpect(jsonPath("$[1].kind").value("REPORT_DECISION"))
        .andExpect(jsonPath("$[1].reportId").value("RPT-1"))
        .andExpect(jsonPath("$[1].status").value("REJECTED"))
        .andExpect(jsonPath("$[1].reason").value("Not a flood"))
        .andExpect(jsonPath("$[1].decidedAt").value("2026-05-21T08:00:00+05:30"))
        .andExpect(jsonPath("$[1].warningId").doesNotExist());
  }

  @Test
  @DisplayName("GET with no entries returns an empty list")
  void pollEmpty() throws Exception {
    when(outbox.findUndelivered("CIT-2")).thenReturn(List.of());

    mockMvc
        .perform(get("/api/push/CIT-2/outbox"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  @DisplayName(
      "POST ack marks the recipient's own entries delivered and ignores the others, with 204")
  void ackOnlyOwnEntries() throws Exception {
    PushOutbox own =
        PushOutbox.forWarningAlert("CIT-1", "WRN-1", WarningLevel.LEVEL_1_YELLOW, "t", CREATED);
    PushOutbox foreign =
        PushOutbox.forWarningAlert("CIT-9", "WRN-1", WarningLevel.LEVEL_1_YELLOW, "t", CREATED);
    when(outbox.findAllById(List.of(own.getId(), foreign.getId(), "unknown")))
        .thenReturn(List.of(own, foreign));

    mockMvc
        .perform(
            post("/api/push/CIT-1/outbox/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"ids\": [\""
                        + own.getId()
                        + "\", \""
                        + foreign.getId()
                        + "\", \"unknown\"]}"))
        .andExpect(status().isNoContent());

    assertThat(own.getDeliveredAt()).contains(LocalDateTime.of(2026, 5, 21, 9, 0));
    assertThat(foreign.getDeliveredAt()).isEmpty();
  }

  @Test
  @DisplayName("POST ack without the ids is a 400 problem")
  void ackWithoutIds() throws Exception {
    mockMvc
        .perform(
            post("/api/push/CIT-1/outbox/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
