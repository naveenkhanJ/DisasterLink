package lk.sliit.se3070.group058.disasterlink.shelter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.SubmissionRecorder;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.OccupancyRecord;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterOutcome;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterView;
import lk.sliit.se3070.group058.disasterlink.shelter.repository.ShelterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ShelterController}. Covers all scenario flows of UC3 (Main Flow, A1, A2,
 * A4, A5, A6, E1, E2, E3, E4) and verifies idempotency through {@link SubmissionRecorder}.
 */
@ExtendWith(MockitoExtension.class)
class ShelterControllerTest {

  private static final String OFFICER_ID = "DO-COL-01";
  private static final String SHELTER_ID = "SHL-COL-0001";
  private static final String SUBMISSION_ID = "sub-101";
  private static final String CLIENT_ID = "client-01";
  private static final LocalDateTime CAPTURED_AT = LocalDateTime.of(2026, 5, 21, 14, 5);

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-05-21T08:35:00Z"), ZoneId.of("Asia/Colombo"));

  @Mock private ShelterRepository shelterRepository;
  @Mock private DistrictOfficerRepository districtOfficers;
  @Mock private OrganizationRepository organizationRepository;
  @Mock private SubmissionRecorder submissionRecorder;

  private ShelterController controller;
  private DistrictOfficer officer;
  private Organization operator;
  private Shelter shelter;
  private ClientMeta meta;

  @BeforeEach
  void setUp() {
    controller =
        new ShelterController(
            shelterRepository, districtOfficers, organizationRepository, submissionRecorder, clock);

    officer =
        new DistrictOfficer(
            OFFICER_ID, "R. W", "0775551111", "r@example.lk", District.COLOMBO, Language.SINHALA);
    operator =
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
            SHELTER_ID,
            new ShelterDetails(
                "Rathmalana Maha Vidyalaya",
                "Rathmalana, Colombo",
                District.COLOMBO,
                500,
                "0712345678",
                "ORG-0001"),
            officer,
            operator);

    meta = new ClientMeta(CLIENT_ID, OFFICER_ID, SUBMISSION_ID, CAPTURED_AT, 0);
  }

  @Test
  @DisplayName("Main flow — step 2: getDistrictShelters returns shelters of officer's district")
  void getDistrictSheltersSuccess() {
    when(districtOfficers.findById(OFFICER_ID)).thenReturn(Optional.of(officer));
    when(shelterRepository.findByDistrict(District.COLOMBO)).thenReturn(List.of(shelter));

    List<Shelter> result = controller.getDistrictShelters(OFFICER_ID);

    assertThat(result).containsExactly(shelter);
    verify(districtOfficers).findById(OFFICER_ID);
    verify(shelterRepository).findByDistrict(District.COLOMBO);
  }

  @Test
  @DisplayName("getDistrictShelters throws NoSuchElementException when officer not found")
  void getDistrictSheltersOfficerNotFound() {
    when(districtOfficers.findById("DO-UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getDistrictShelters("DO-UNKNOWN"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("Officer not found");
  }

  @Test
  @DisplayName("getOrganizations returns all registered organizations for pick-list")
  void getOrganizationsReturnsAll() {
    when(organizationRepository.findAll()).thenReturn(List.of(operator));

    List<Organization> result = controller.getOrganizations();

    assertThat(result).containsExactly(operator);
    verify(organizationRepository).findAll();
  }

  @Test
  @DisplayName("Main flow — step 4: getShelterDetails returns shelter view with remaining capacity")
  void getShelterDetailsSuccess() {
    when(shelterRepository.findById(SHELTER_ID)).thenReturn(Optional.of(shelter));

    ShelterView view = controller.getShelterDetails(SHELTER_ID);

    assertThat(view.shelter()).isEqualTo(shelter);
    assertThat(view.remainingCapacity()).isEqualTo(500);
  }

  @Test
  @DisplayName("getShelterDetails throws NoSuchElementException when shelter is missing")
  void getShelterDetailsNotFound() {
    when(shelterRepository.findById("SHL-UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getShelterDetails("SHL-UNKNOWN"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("Shelter not found");
  }

  @Test
  @DisplayName("A1 — findAlternativeShelters returns shelters with enough space")
  void findAlternativeSheltersWithSpace() {
    when(shelterRepository.findWithSpace(District.COLOMBO, 200)).thenReturn(List.of(shelter));

    List<ShelterView> views = controller.findAlternativeShelters(District.COLOMBO, 200);

    assertThat(views).hasSize(1);
    assertThat(views.get(0).shelter()).isEqualTo(shelter);
    assertThat(views.get(0).remainingCapacity()).isEqualTo(500);
  }

  @Test
  @DisplayName("Main flow — steps 7-8: updateOccupancy records new total below capacity")
  void updateOccupancySuccess() {
    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(shelterRepository.findById(SHELTER_ID)).thenReturn(Optional.of(shelter));
    when(districtOfficers.findById(OFFICER_ID)).thenReturn(Optional.of(officer));

    OccupancyUpdate update = new OccupancyUpdate(SHELTER_ID, 350, meta);
    ShelterOutcome outcome = controller.updateOccupancy(update);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(outcome.result().submissionId()).isEqualTo(SUBMISSION_ID);
    assertThat(outcome.shelter()).isEqualTo(shelter);
    assertThat(outcome.remainingCapacity()).isEqualTo(150);
    assertThat(shelter.getCurrentOccupancy()).isEqualTo(350);
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.OPEN);

    verify(shelterRepository).save(shelter);
    verify(submissionRecorder).record(meta, SHELTER_ID);
    verify(submissionRecorder).touchSync(meta);
  }

  @Test
  @DisplayName(
      "A2 — steps 8a-8c: updateOccupancy marks shelter full when occupancy equals capacity")
  void updateOccupancyReachesCapacity() {
    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(shelterRepository.findById(SHELTER_ID)).thenReturn(Optional.of(shelter));
    when(districtOfficers.findById(OFFICER_ID)).thenReturn(Optional.of(officer));

    OccupancyUpdate update = new OccupancyUpdate(SHELTER_ID, 500, meta);
    ShelterOutcome outcome = controller.updateOccupancy(update);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(outcome.shelter().getStatus()).isEqualTo(ShelterStatus.FULL);
    assertThat(outcome.remainingCapacity()).isEqualTo(0);
    assertThat(shelter.getCurrentOccupancy()).isEqualTo(500);

    verify(shelterRepository).save(shelter);
    verify(submissionRecorder).record(meta, SHELTER_ID);
    verify(submissionRecorder).touchSync(meta);
  }

  @Test
  @DisplayName("A6 — updateOccupancy recognizes duplicate submission and returns DUPLICATE")
  void updateOccupancyDuplicate() {
    ProcessedSubmission processed =
        new ProcessedSubmission(
            SUBMISSION_ID, CLIENT_ID, CAPTURED_AT, LocalDateTime.now(clock), SHELTER_ID);
    when(submissionRecorder.check(meta)).thenReturn(Optional.of(processed));

    OccupancyUpdate update = new OccupancyUpdate(SHELTER_ID, 350, meta);
    ShelterOutcome outcome = controller.updateOccupancy(update);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    assertThat(outcome.result().message()).isEqualTo("already recorded");
    assertThat(outcome.shelter()).isNull();
    assertThat(outcome.remainingCapacity()).isEqualTo(0);

    verify(submissionRecorder).touchSync(meta);
    verifyNoMoreInteractions(shelterRepository);
  }

  @Test
  @DisplayName("E1 — 7a-7b: updateOccupancy rejects total exceeding capacity with OVER_CAPACITY")
  void updateOccupancyExceedsCapacity() {
    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(shelterRepository.findById(SHELTER_ID)).thenReturn(Optional.of(shelter));
    when(shelterRepository.findWithSpace(District.COLOMBO, 20)).thenReturn(List.of());

    OccupancyUpdate update = new OccupancyUpdate(SHELTER_ID, 520, meta);
    ShelterOutcome outcome = controller.updateOccupancy(update);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.REJECTED);
    assertThat(outcome.result().reason()).isEqualTo(RejectionReason.OVER_CAPACITY);
    assertThat(outcome.result().message()).contains("520 is more than the capacity of 500");
    assertThat(outcome.shelter()).isEqualTo(shelter);
    assertThat(outcome.remainingCapacity()).isEqualTo(500);
    assertThat(outcome.alternatives()).isEmpty();

    verify(shelterRepository).findWithSpace(District.COLOMBO, 20);
    verifyNoMoreInteractions(submissionRecorder);
  }

  @Test
  @DisplayName("E3 — 7d-7e: updateOccupancy rejects negative total with INVALID_VALUE")
  void updateOccupancyNegativeTotal() {
    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(shelterRepository.findById(SHELTER_ID)).thenReturn(Optional.of(shelter));

    OccupancyUpdate update = new OccupancyUpdate(SHELTER_ID, -10, meta);
    ShelterOutcome outcome = controller.updateOccupancy(update);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.REJECTED);
    assertThat(outcome.result().reason()).isEqualTo(RejectionReason.INVALID_VALUE);
    assertThat(outcome.result().message())
        .isEqualTo(
            "Enter the total number of people now in the shelter as a whole number of 0 or more.");
    assertThat(outcome.shelter()).isNull();
    assertThat(outcome.remainingCapacity()).isEqualTo(0);

    verifyNoMoreInteractions(submissionRecorder);
  }

  @Test
  @DisplayName("A5 — steps 2a-2f: registerShelter registers new shelter when valid")
  void registerShelterSuccess() {
    ShelterDetails details =
        new ShelterDetails(
            "Wellampitiya Community Hall",
            "Avissawella Road, Wellampitiya",
            District.COLOMBO,
            250,
            "0112534417",
            "ORG-0001");
    ShelterSubmission submission = new ShelterSubmission(details, meta);

    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(organizationRepository.existsById("ORG-0001")).thenReturn(true);
    when(districtOfficers.findById(OFFICER_ID)).thenReturn(Optional.of(officer));
    when(organizationRepository.findById("ORG-0001")).thenReturn(Optional.of(operator));

    ShelterOutcome outcome = controller.registerShelter(submission);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(outcome.shelter()).isNotNull();
    assertThat(outcome.shelter().getName()).isEqualTo("Wellampitiya Community Hall");
    assertThat(outcome.shelter().getCapacity()).isEqualTo(250);
    assertThat(outcome.shelter().getCurrentOccupancy()).isEqualTo(0);
    assertThat(outcome.shelter().getStatus()).isEqualTo(ShelterStatus.OPEN);
    assertThat(outcome.remainingCapacity()).isEqualTo(250);

    ArgumentCaptor<Shelter> captor = ArgumentCaptor.forClass(Shelter.class);
    verify(shelterRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("Wellampitiya Community Hall");
    verify(submissionRecorder).record(eq(meta), any(String.class));
    verify(submissionRecorder).touchSync(meta);
  }

  @Test
  @DisplayName("A5 — registerShelter returns DUPLICATE on duplicate submissionId")
  void registerShelterDuplicate() {
    ShelterDetails details =
        new ShelterDetails("Hall", "Road", District.COLOMBO, 200, "0112534417", null);
    ShelterSubmission submission = new ShelterSubmission(details, meta);

    when(submissionRecorder.check(meta))
        .thenReturn(
            Optional.of(
                new ProcessedSubmission(
                    SUBMISSION_ID, CLIENT_ID, CAPTURED_AT, LocalDateTime.now(clock), "SHL-NEW")));

    ShelterOutcome outcome = controller.registerShelter(submission);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    assertThat(outcome.shelter()).isNull();
    verify(submissionRecorder).touchSync(meta);
    verifyNoMoreInteractions(shelterRepository);
  }

  @Test
  @DisplayName("E2 — registerShelter rejects submission when validation fails")
  void registerShelterValidationFailure() {
    ShelterDetails invalidDetails = new ShelterDetails("", "", null, 0, "123", "NON-EXISTENT");
    ShelterSubmission submission = new ShelterSubmission(invalidDetails, meta);

    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(organizationRepository.existsById("NON-EXISTENT")).thenReturn(false);

    ShelterOutcome outcome = controller.registerShelter(submission);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.REJECTED);
    assertThat(outcome.result().reason()).isEqualTo(RejectionReason.VALIDATION_FAILED);
    assertThat(outcome.result().message())
        .contains("name: required")
        .contains("address: required")
        .contains("district: required")
        .contains("capacity: must be a whole number of at least 1")
        .contains("contactNumber: must be a 10-digit Sri Lankan number starting with 0")
        .contains("operatorId: operating organisation not found");
    assertThat(outcome.shelter()).isNull();
  }

  @Test
  @DisplayName("A4 — steps 4e-4h: updateShelterDetails updates details and recomputes status")
  void updateShelterDetailsSuccess() {
    ShelterDetails newDetails =
        new ShelterDetails(
            "Rathmalana Maha Vidyalaya",
            "Rathmalana, Colombo",
            District.COLOMBO,
            550,
            "0712345679",
            "ORG-0001");
    ShelterSubmission submission = new ShelterSubmission(newDetails, meta);

    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(shelterRepository.findById(SHELTER_ID)).thenReturn(Optional.of(shelter));
    when(organizationRepository.existsById("ORG-0001")).thenReturn(true);
    when(organizationRepository.findById("ORG-0001")).thenReturn(Optional.of(operator));

    ShelterOutcome outcome = controller.updateShelterDetails(SHELTER_ID, submission);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.ACCEPTED);
    assertThat(outcome.shelter().getCapacity()).isEqualTo(550);
    assertThat(outcome.shelter().getContactNumber()).isEqualTo("0712345679");
    assertThat(outcome.remainingCapacity()).isEqualTo(550);

    verify(shelterRepository).save(shelter);
    verify(submissionRecorder).record(meta, SHELTER_ID);
    verify(submissionRecorder).touchSync(meta);
  }

  @Test
  @DisplayName("A4 — updateShelterDetails returns DUPLICATE on replay")
  void updateShelterDetailsDuplicate() {
    ShelterDetails details =
        new ShelterDetails("Hall", "Road", District.COLOMBO, 500, "0712345678", null);
    ShelterSubmission submission = new ShelterSubmission(details, meta);

    when(submissionRecorder.check(meta))
        .thenReturn(
            Optional.of(
                new ProcessedSubmission(
                    SUBMISSION_ID, CLIENT_ID, CAPTURED_AT, LocalDateTime.now(clock), SHELTER_ID)));

    ShelterOutcome outcome = controller.updateShelterDetails(SHELTER_ID, submission);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.DUPLICATE);
    assertThat(outcome.shelter()).isNull();
    verify(submissionRecorder).touchSync(meta);
    verifyNoMoreInteractions(shelterRepository);
  }

  @Test
  @DisplayName("E4 — 4k-4l: updateShelterDetails rejects capacity below current occupancy")
  void updateShelterDetailsCapacityBelowOccupancy() {
    shelter.updateOccupancy(
        new OccupancyRecord(320, CAPTURED_AT, LocalDateTime.now(clock), officer));

    ShelterDetails newDetails =
        new ShelterDetails(
            "Rathmalana Maha Vidyalaya",
            "Rathmalana, Colombo",
            District.COLOMBO,
            300,
            "0712345678",
            null);
    ShelterSubmission submission = new ShelterSubmission(newDetails, meta);

    when(submissionRecorder.check(meta)).thenReturn(Optional.empty());
    when(shelterRepository.findById(SHELTER_ID)).thenReturn(Optional.of(shelter));

    ShelterOutcome outcome = controller.updateShelterDetails(SHELTER_ID, submission);

    assertThat(outcome.result().outcome()).isEqualTo(SyncOutcome.REJECTED);
    assertThat(outcome.result().reason()).isEqualTo(RejectionReason.VALIDATION_FAILED);
    assertThat(outcome.result().message())
        .contains("capacity: 300 is below the current occupancy of 320");
    assertThat(outcome.shelter()).isEqualTo(shelter);
    assertThat(outcome.remainingCapacity()).isEqualTo(180);
  }

  @Test
  @DisplayName("validateShelterDetails returns valid result for valid registration details")
  void validateShelterDetailsRegistrationValid() {
    ShelterDetails details =
        new ShelterDetails("Hall", "Address", District.COLOMBO, 250, "0112345678", null);

    ValidationResult result = controller.validateShelterDetails(details, null);

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  @DisplayName("validateShelterDetails catches all missing fields and invalid phone")
  void validateShelterDetailsInvalid() {
    ShelterDetails details =
        new ShelterDetails(null, "   ", null, -5, "not-a-number", "ORG-INVALID");
    when(organizationRepository.existsById("ORG-INVALID")).thenReturn(false);

    ValidationResult result = controller.validateShelterDetails(details, null);

    assertThat(result.valid()).isFalse();
    assertThat(result.errors())
        .contains("name: required")
        .contains("address: required")
        .contains("district: required")
        .contains("capacity: must be a whole number of at least 1")
        .contains("contactNumber: must be a 10-digit Sri Lankan number starting with 0")
        .contains("operatorId: operating organisation not found");
  }

  @Test
  @DisplayName("Constructor enforces non-null dependencies")
  void constructorRejectsNull() {
    assertThatThrownBy(
            () ->
                new ShelterController(
                    null, districtOfficers, organizationRepository, submissionRecorder, clock))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new ShelterController(
                    shelterRepository, null, organizationRepository, submissionRecorder, clock))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new ShelterController(
                    shelterRepository, districtOfficers, null, submissionRecorder, clock))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new ShelterController(
                    shelterRepository, districtOfficers, organizationRepository, null, clock))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new ShelterController(
                    shelterRepository,
                    districtOfficers,
                    organizationRepository,
                    submissionRecorder,
                    null))
        .isInstanceOf(NullPointerException.class);
  }
}
