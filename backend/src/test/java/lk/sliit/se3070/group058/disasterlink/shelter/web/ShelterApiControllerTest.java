package lk.sliit.se3070.group058.disasterlink.shelter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterOutcome;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterView;
import lk.sliit.se3070.group058.disasterlink.shelter.service.ShelterController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests of the UC3 JSON endpoints: each endpoint's JSON in and out, REJECTED replies as HTTP 200, a
 * malformed body as a 400 problem, and the {@code +05:30} times of the client meta.
 */
@WebMvcTest(ShelterApiController.class)
class ShelterApiControllerTest {

  private static final String OFFICER_ID = "DO-COL-01";

  private static final String META_JSON =
      "{\"clientId\":\"c-1\",\"userId\":\"DO-COL-01\",\"submissionId\":\"s-1\","
          + "\"capturedAt\":\"2026-05-21T14:05:00+05:30\",\"pendingCount\":2}";

  private static final String DETAILS_JSON =
      "{\"name\":\"Wellampitiya Community Hall\","
          + "\"address\":\"Avissawella Road, Wellampitiya\",\"district\":\"COLOMBO\","
          + "\"capacity\":250,\"contactNumber\":\"0112534417\",\"operatorId\":null}";

  private static final String SUBMISSION_JSON =
      "{\"details\":" + DETAILS_JSON + ",\"meta\":" + META_JSON + "}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShelterController shelterController;

  @MockitoBean private CurrentUserService currentUserService;

  private Shelter shelter;

  @BeforeEach
  void setUp() {
    when(currentUserService.userId(DemoRole.DISTRICT_OFFICER)).thenReturn(OFFICER_ID);
    DistrictOfficer officer =
        new DistrictOfficer(
            OFFICER_ID, "R. W", "0775551111", "r@example.lk", District.COLOMBO, Language.SINHALA);
    Organization operator =
        new Organization(
            "ORG-0001",
            "CARE Sri Lanka",
            OrganizationType.NGO,
            "0112690000",
            "Colombo",
            List.of(District.COLOMBO),
            Language.ENGLISH);
    shelter =
        new Shelter(
            "SHL-COL-0001",
            new ShelterDetails(
                "Rathmalana Maha Vidyalaya",
                "Rathmalana, Colombo",
                District.COLOMBO,
                500,
                "0712345678",
                "ORG-0001"),
            officer,
            operator);
  }

  private static ShelterOutcome accepted(Shelter shelter, String submissionId) {
    return new ShelterOutcome(
        new SyncResult(submissionId, SyncOutcome.ACCEPTED, null, null), shelter, 500, List.of());
  }

  @Test
  @DisplayName("GET /api/shelters lists the district officer's shelters as wire records")
  void listsDistrictShelters() throws Exception {
    when(shelterController.getDistrictShelters(OFFICER_ID)).thenReturn(List.of(shelter));

    mockMvc
        .perform(get("/api/shelters"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].shelterId").value("SHL-COL-0001"))
        .andExpect(jsonPath("$[0].name").value("Rathmalana Maha Vidyalaya"))
        .andExpect(jsonPath("$[0].district").value("COLOMBO"))
        .andExpect(jsonPath("$[0].capacity").value(500))
        .andExpect(jsonPath("$[0].currentOccupancy").value(0))
        .andExpect(jsonPath("$[0].status").value("OPEN"))
        .andExpect(jsonPath("$[0].operatorName").value("CARE Sri Lanka"))
        .andExpect(jsonPath("$[0].operatorId").value("ORG-0001"));
  }

  @Test
  @DisplayName("A shelter without an operator has a null operatorName")
  void shelterWithoutOperator() throws Exception {
    Shelter alone =
        new Shelter(
            "SHL-COL-0002",
            new ShelterDetails("Hall", "Road", District.COLOMBO, 50, "0112345678", null),
            shelter.getOfficer(),
            null);
    when(shelterController.getDistrictShelters(OFFICER_ID)).thenReturn(List.of(alone));

    mockMvc
        .perform(get("/api/shelters"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].operatorName").doesNotExist())
        .andExpect(jsonPath("$[0].operatorId").doesNotExist());
  }

  @Test
  @DisplayName("GET /api/shelters/{id} returns the shelter with its remaining capacity")
  void retrievesOneShelter() throws Exception {
    when(shelterController.getShelterDetails("SHL-COL-0001"))
        .thenReturn(new ShelterView(shelter, 500));

    mockMvc
        .perform(get("/api/shelters/SHL-COL-0001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shelter.shelterId").value("SHL-COL-0001"))
        .andExpect(jsonPath("$.remainingCapacity").value(500));
  }

  @Test
  @DisplayName("GET /api/shelters/{id} of an unknown shelter is a 404 problem")
  void unknownShelterIs404() throws Exception {
    when(shelterController.getShelterDetails("SHL-X")).thenThrow(new NoSuchElementException());

    mockMvc
        .perform(get("/api/shelters/SHL-X"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  @Test
  @DisplayName("GET /api/shelters/alternatives passes the district and the number needed")
  void findsAlternatives() throws Exception {
    when(shelterController.findAlternativeShelters(District.COLOMBO, 200))
        .thenReturn(List.of(new ShelterView(shelter, 280)));

    mockMvc
        .perform(
            get("/api/shelters/alternatives").param("district", "COLOMBO").param("needed", "200"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].shelter.name").value("Rathmalana Maha Vidyalaya"))
        .andExpect(jsonPath("$[0].remainingCapacity").value(280));
  }

  @Test
  @DisplayName("GET /api/shelters/alternatives with a non-numeric need is a 400 problem")
  void alternativesWithBadNumberIs400() throws Exception {
    mockMvc
        .perform(
            get("/api/shelters/alternatives").param("district", "COLOMBO").param("needed", "many"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("POST /api/shelters/validation answers an invalid result with HTTP 200")
  void invalidDetailsAreAReplyNotAnError() throws Exception {
    when(shelterController.validateShelterDetails(any(ShelterDetails.class), eq("SHL-COL-0001")))
        .thenReturn(new ValidationResult(false, List.of("contactNumber: must have 10 digits")));

    mockMvc
        .perform(
            post("/api/shelters/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"details\":" + DETAILS_JSON + ",\"shelterId\":\"SHL-COL-0001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.errors[0]").value("contactNumber: must have 10 digits"));
  }

  @Test
  @DisplayName("POST /api/shelters/validation of a registration passes a null shelterId")
  void validatesARegistration() throws Exception {
    when(shelterController.validateShelterDetails(any(ShelterDetails.class), eq(null)))
        .thenReturn(new ValidationResult(true, List.of()));

    mockMvc
        .perform(
            post("/api/shelters/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"details\":" + DETAILS_JSON + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  @DisplayName("POST /api/shelters/{id}/occupancy builds the update with the meta in Asia/Colombo")
  void updatesOccupancy() throws Exception {
    when(shelterController.updateOccupancy(any(OccupancyUpdate.class)))
        .thenReturn(accepted(shelter, "s-1"));

    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":350,\"meta\":" + META_JSON + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.submissionId").value("s-1"))
        .andExpect(jsonPath("$.result.outcome").value("ACCEPTED"))
        .andExpect(jsonPath("$.shelter.shelterId").value("SHL-COL-0001"))
        .andExpect(jsonPath("$.remainingCapacity").value(500))
        .andExpect(jsonPath("$.alternatives").isEmpty());

    ArgumentCaptor<OccupancyUpdate> captor = ArgumentCaptor.forClass(OccupancyUpdate.class);
    verify(shelterController).updateOccupancy(captor.capture());
    OccupancyUpdate update = captor.getValue();
    assertThat(update.shelterId()).isEqualTo("SHL-COL-0001");
    assertThat(update.newTotal()).isEqualTo(350);
    assertThat(update.meta())
        .isEqualTo(
            new ClientMeta("c-1", "DO-COL-01", "s-1", LocalDateTime.of(2026, 5, 21, 14, 5), 2));
  }

  @Test
  @DisplayName("A capturedAt in another offset is converted to the same instant in Asia/Colombo")
  void convertsOtherOffsets() throws Exception {
    when(shelterController.updateOccupancy(any(OccupancyUpdate.class)))
        .thenReturn(accepted(shelter, "s-1"));
    String meta = META_JSON.replace("2026-05-21T14:05:00+05:30", "2026-05-21T08:35:00Z");

    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":350,\"meta\":" + meta + "}"))
        .andExpect(status().isOk());

    ArgumentCaptor<OccupancyUpdate> captor = ArgumentCaptor.forClass(OccupancyUpdate.class);
    verify(shelterController).updateOccupancy(captor.capture());
    assertThat(captor.getValue().meta().capturedAt())
        .isEqualTo(LocalDateTime.of(2026, 5, 21, 14, 5));
  }

  @Test
  @DisplayName("An OVER_CAPACITY rejection is HTTP 200 with the typed reason and the shelter")
  void overCapacityIsAReply() throws Exception {
    ShelterOutcome rejected =
        new ShelterOutcome(
            new SyncResult(
                "s-1",
                SyncOutcome.REJECTED,
                RejectionReason.OVER_CAPACITY,
                "Capacity exceeded: 520 is more than the capacity of 500; 180 places remain"),
            shelter,
            180,
            List.of(new ShelterView(shelter, 280)));
    when(shelterController.updateOccupancy(any(OccupancyUpdate.class))).thenReturn(rejected);

    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":520,\"meta\":" + META_JSON + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("REJECTED"))
        .andExpect(jsonPath("$.result.reason").value("OVER_CAPACITY"))
        .andExpect(jsonPath("$.result.message").value(containsString("520")))
        .andExpect(jsonPath("$.shelter.capacity").value(500))
        .andExpect(jsonPath("$.remainingCapacity").value(180))
        .andExpect(jsonPath("$.alternatives[0].remainingCapacity").value(280));
  }

  @Test
  @DisplayName("An INVALID_VALUE rejection has no shelter and is HTTP 200")
  void invalidValueHasNoShelter() throws Exception {
    ShelterOutcome rejected =
        new ShelterOutcome(
            new SyncResult(
                "s-1", SyncOutcome.REJECTED, RejectionReason.INVALID_VALUE, "Not a valid number"),
            null,
            0,
            List.of());
    when(shelterController.updateOccupancy(any(OccupancyUpdate.class))).thenReturn(rejected);

    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":-5,\"meta\":" + META_JSON + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.reason").value("INVALID_VALUE"))
        .andExpect(jsonPath("$.shelter").doesNotExist());
  }

  @Test
  @DisplayName("POST /api/shelters registers a shelter from the submission JSON")
  void registersAShelter() throws Exception {
    when(shelterController.registerShelter(any(ShelterSubmission.class)))
        .thenReturn(accepted(shelter, "s-1"));

    mockMvc
        .perform(
            post("/api/shelters").contentType(MediaType.APPLICATION_JSON).content(SUBMISSION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("ACCEPTED"));

    ArgumentCaptor<ShelterSubmission> captor = ArgumentCaptor.forClass(ShelterSubmission.class);
    verify(shelterController).registerShelter(captor.capture());
    ShelterSubmission submission = captor.getValue();
    assertThat(submission.details())
        .isEqualTo(
            new ShelterDetails(
                "Wellampitiya Community Hall",
                "Avissawella Road, Wellampitiya",
                District.COLOMBO,
                250,
                "0112534417",
                null));
    assertThat(submission.meta().capturedAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 14, 5));
  }

  @Test
  @DisplayName("PUT /api/shelters/{id} changes the details and replies with the outcome")
  void updatesDetails() throws Exception {
    ShelterOutcome duplicate =
        new ShelterOutcome(
            new SyncResult("s-1", SyncOutcome.DUPLICATE, null, null), shelter, 500, List.of());
    when(shelterController.updateShelterDetails(eq("SHL-COL-0001"), any(ShelterSubmission.class)))
        .thenReturn(duplicate);

    mockMvc
        .perform(
            put("/api/shelters/SHL-COL-0001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SUBMISSION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.outcome").value("DUPLICATE"));
  }

  @Test
  @DisplayName("A malformed body is a 400 problem and nothing is called")
  void malformedBodyIs400() throws Exception {
    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\": oops"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty());

    verifyNoInteractions(shelterController);
  }

  @Test
  @DisplayName("A body without its meta is a 400 problem")
  void missingMetaIs400() throws Exception {
    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":350}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("A submission whose meta lacks its capture time is a 400 problem")
  void missingCapturedAtIs400() throws Exception {
    String meta = META_JSON.replace("\"capturedAt\":\"2026-05-21T14:05:00+05:30\",", "");

    mockMvc
        .perform(
            post("/api/shelters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"details\":" + DETAILS_JSON + ",\"meta\":" + meta + "}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("While the use case is not implemented the endpoint answers a 500 problem")
  void unimplementedControlIs500() throws Exception {
    when(shelterController.getDistrictShelters(OFFICER_ID))
        .thenThrow(new UnsupportedOperationException("not implemented yet — Weliwitigoda"));

    mockMvc
        .perform(get("/api/shelters"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value(containsString("nothing was changed")));
  }

  @Test
  @DisplayName("Unentered shelter details reach validateShelterDetails instead of a 400 (SC3 E2)")
  void unenteredDetailsAreValidatedByTheControl() throws Exception {
    when(shelterController.validateShelterDetails(any(ShelterDetails.class), eq(null)))
        .thenReturn(new ValidationResult(false, List.of("name: required", "district: required")));

    mockMvc
        .perform(
            post("/api/shelters/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"details\":{\"address\":\"Road\",\"capacity\":250}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.errors[0]").value("name: required"));

    ArgumentCaptor<ShelterDetails> captor = ArgumentCaptor.forClass(ShelterDetails.class);
    verify(shelterController).validateShelterDetails(captor.capture(), eq(null));
    assertThat(captor.getValue().name()).isNull();
    assertThat(captor.getValue().district()).isNull();
    assertThat(captor.getValue().contactNumber()).isNull();
  }

  @Test
  @DisplayName("A missing capacity is a 400: the page blocks a non-numeric capacity itself")
  void missingCapacityIs400() throws Exception {
    mockMvc
        .perform(
            post("/api/shelters/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"details\":{\"name\":\"Hall\"}}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("A decimal newTotal is a 400 problem, not a truncated whole number (P155 D-18)")
  void decimalNewTotalIs400() throws Exception {
    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":350.5,\"meta\":" + META_JSON + "}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(shelterController);
  }

  @Test
  @DisplayName("A whole number written with a zero decimal is also refused")
  void wholeNumberWithDecimalPointIs400() throws Exception {
    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":350.0,\"meta\":" + META_JSON + "}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("A capturedAt with milliseconds is accepted and bound to Asia/Colombo (P155 D-10)")
  void capturedAtWithMillisecondsIsBound() throws Exception {
    when(shelterController.updateOccupancy(any(OccupancyUpdate.class)))
        .thenReturn(accepted(shelter, "s-1"));
    String meta = META_JSON.replace("2026-05-21T14:05:00+05:30", "2026-05-21T14:05:00.123+05:30");

    mockMvc
        .perform(
            post("/api/shelters/SHL-COL-0001/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTotal\":350,\"meta\":" + meta + "}"))
        .andExpect(status().isOk());

    ArgumentCaptor<OccupancyUpdate> captor = ArgumentCaptor.forClass(OccupancyUpdate.class);
    verify(shelterController).updateOccupancy(captor.capture());
    assertThat(captor.getValue().meta().capturedAt())
        .isEqualTo(LocalDateTime.of(2026, 5, 21, 14, 5, 0, 123_000_000));
  }
}
