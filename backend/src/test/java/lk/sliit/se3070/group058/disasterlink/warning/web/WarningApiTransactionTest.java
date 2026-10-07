package lk.sliit.se3070.group058.disasterlink.warning.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves that the endpoint maps the warning inside its read-only transaction. A real {@code Hazard}
 * and {@code Warning} are stored; the mocked control loads the warning through {@code
 * WarningRepository.findById} during the call, as the owner's body will, so the warning's hazard is
 * a lazy reference. Open-in-view is off, so the answer with the hazard's type and place name can
 * only come from a transaction that is still open when the mapper reads it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WarningApiTransactionTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private HazardRepository hazards;

  @Autowired private WarningRepository warnings;

  @Autowired private DMCOfficerRepository officers;

  @MockitoBean private WarningController warningController;

  @AfterEach
  void clean() {
    warnings.deleteAll();
    hazards.deleteAll();
    officers.deleteAll();
  }

  @Test
  @DisplayName("The hazard's type and place name are read inside the transaction (no lazy failure)")
  void mapsTheLazyHazardInsideTheTransaction() throws Exception {
    DMCOfficer officer = officers.save(SampleEntities.officer("DMC-0001", Set.of()));
    Hazard hazard = hazards.save(SampleEntities.hazard("HZ-2026-0142"));
    warnings.save(
        SampleEntities.warning(
            "WRN-2026-0087",
            hazard,
            officer,
            List.of(District.RATNAPURA),
            List.of(),
            LocalDateTime.of(2026, 5, 22, 8, 25)));
    when(warningController.getActiveWarnings(District.RATNAPURA))
        .thenAnswer(invocation -> warnings.findById("WRN-2026-0087").stream().toList());

    mockMvc
        .perform(get("/api/warnings/active").param("district", "RATNAPURA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].warningId").value("WRN-2026-0087"))
        .andExpect(jsonPath("$[0].hazardType").value("FLOOD"))
        .andExpect(jsonPath("$[0].locationName").value("Kalu Ganga at Ratnapura"))
        .andExpect(jsonPath("$[0].targetDistricts[0]").value("RATNAPURA"))
        .andExpect(jsonPath("$[0].message.english").value("Flood warning"));
  }
}
