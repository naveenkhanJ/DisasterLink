package lk.sliit.se3070.group058.disasterlink.warning.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests of the app's active-warnings endpoint: every field of a warning with its hazard's type and
 * place name, times with {@code +05:30}, an empty answer, and a missing or unknown district as a
 * 400 problem (standards sections 12.1 and 12.2).
 */
@WebMvcTest(WarningApiController.class)
class WarningApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WarningController warningController;

  private static Warning warning() {
    return SampleEntities.warning(
        "WRN-2026-0087",
        SampleEntities.hazard("HZ-2026-0142"),
        SampleEntities.officer("DMC-0001", Set.of()),
        List.of(District.RATNAPURA),
        List.of(),
        LocalDateTime.of(2026, 5, 22, 8, 25));
  }

  @Test
  @DisplayName("One warning is returned with every field, its hazard's type and place, and +05:30")
  void returnsEveryFieldOfAWarning() throws Exception {
    when(warningController.getActiveWarnings(District.RATNAPURA)).thenReturn(List.of(warning()));

    mockMvc
        .perform(get("/api/warnings/active").param("district", "RATNAPURA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].warningId").value("WRN-2026-0087"))
        .andExpect(jsonPath("$[0].warningLevel").value("LEVEL_2_AMBER"))
        .andExpect(jsonPath("$[0].message.sinhala").value("සිං"))
        .andExpect(jsonPath("$[0].message.tamil").value("தமிழ்"))
        .andExpect(jsonPath("$[0].message.english").value("Flood warning"))
        .andExpect(jsonPath("$[0].instructions.sinhala").value("උපදෙස්"))
        .andExpect(jsonPath("$[0].instructions.tamil").value("அறிவுறுத்தல்"))
        .andExpect(jsonPath("$[0].instructions.english").value("Move to higher ground"))
        .andExpect(jsonPath("$[0].issuedTime").value("2026-05-21T08:20:00+05:30"))
        .andExpect(jsonPath("$[0].expiryTime").value("2026-05-22T08:25:00+05:30"))
        .andExpect(jsonPath("$[0].targetDistricts[0]").value("RATNAPURA"))
        .andExpect(jsonPath("$[0].hazardType").value("FLOOD"))
        .andExpect(jsonPath("$[0].locationName").value("Kalu Ganga at Ratnapura"));
  }

  @Test
  @DisplayName("No active warning is an empty array")
  void noWarningIsAnEmptyArray() throws Exception {
    when(warningController.getActiveWarnings(District.GALLE)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/warnings/active").param("district", "GALLE"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  @DisplayName("A request without a district is a 400 problem and the control is not called")
  void missingDistrictIs400() throws Exception {
    mockMvc
        .perform(get("/api/warnings/active"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty());

    verifyNoInteractions(warningController);
  }

  @Test
  @DisplayName("An unknown district is a 400 problem and the control is not called")
  void unknownDistrictIs400() throws Exception {
    mockMvc
        .perform(get("/api/warnings/active").param("district", "ATLANTIS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty());

    verifyNoInteractions(warningController);
  }

  @Test
  @DisplayName("While the use case is not implemented the endpoint answers a 500 problem")
  void unimplementedControlIs500() throws Exception {
    when(warningController.getActiveWarnings(District.KALUTARA))
        .thenThrow(new UnsupportedOperationException("not implemented yet — Lakmal"));

    mockMvc
        .perform(get("/api/warnings/active").param("district", "KALUTARA"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500));
  }
}
