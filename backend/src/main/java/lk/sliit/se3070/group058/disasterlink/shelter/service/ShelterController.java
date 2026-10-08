package lk.sliit.se3070.group058.disasterlink.shelter.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.SubmissionRecorder;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.OccupancyRecord;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterOutcome;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterView;
import lk.sliit.se3070.group058.disasterlink.shelter.repository.ShelterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The control of use case UC3, Manage Emergency Shelters.
 *
 * <p>Implements CH-05, CH-14, CH-19, CH-28, CH-29 and PR8. Handles district shelter queries,
 * capacity checks, occupancy updates, shelter registration, and detail modifications. All writes
 * are idempotent through {@link SubmissionRecorder} using client metadata.
 */
@Service
public class ShelterController {

  private static final Logger log = LoggerFactory.getLogger(ShelterController.class);
  private static final Pattern SRI_LANKAN_PHONE = Pattern.compile("^0\\d{9}$");
  private static final String DUPLICATE_MESSAGE = "already recorded";
  private static final String INVALID_TOTAL_MESSAGE =
      "Enter the total number of people now in the shelter as a whole number of 0 or more.";

  private final ShelterRepository shelterRepository;
  private final DistrictOfficerRepository districtOfficers;
  private final OrganizationRepository organizationRepository;
  private final SubmissionRecorder submissionRecorder;
  private final Clock clock;

  /**
   * Creates the shelter controller with required repositories and services.
   *
   * @param shelterRepository data access for emergency shelters
   * @param districtOfficers data access for district officers
   * @param organizationRepository data access for operating organizations
   * @param submissionRecorder recorder for submission idempotency and sync status
   * @param clock system clock for recording timestamps
   */
  public ShelterController(
      ShelterRepository shelterRepository,
      DistrictOfficerRepository districtOfficers,
      OrganizationRepository organizationRepository,
      SubmissionRecorder submissionRecorder,
      Clock clock) {
    this.shelterRepository = Objects.requireNonNull(shelterRepository, "shelterRepository");
    this.districtOfficers = Objects.requireNonNull(districtOfficers, "districtOfficers");
    this.organizationRepository =
        Objects.requireNonNull(organizationRepository, "organizationRepository");
    this.submissionRecorder = Objects.requireNonNull(submissionRecorder, "submissionRecorder");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Lists the shelters of an officer's district; every officer of the district manages them.
   *
   * @param officerId the identifier of the officer
   * @return the shelters of the officer's district
   * @throws NoSuchElementException if the officer is not found
   */
  @Transactional(readOnly = true)
  public List<Shelter> getDistrictShelters(String officerId) {
    Objects.requireNonNull(officerId, "officerId");
    DistrictOfficer officer =
        districtOfficers
            .findById(officerId)
            .orElseThrow(() -> new NoSuchElementException("Officer not found: " + officerId));
    log.debug("Listing shelters for officer {} in district {}", officerId, officer.getDistrict());
    return shelterRepository.findByDistrict(officer.getDistrict());
  }

  /**
   * Lists the organisations for the operator pick-list of the registration and edit forms.
   *
   * @return the organisations that can operate a shelter
   */
  @Transactional(readOnly = true)
  public List<Organization> getOrganizations() {
    log.debug("Listing all organizations for shelter operator pick-list");
    return organizationRepository.findAll();
  }

  /**
   * Shows a shelter with its remaining capacity.
   *
   * @param shelterId the identifier of the shelter
   * @return the shelter view
   * @throws NoSuchElementException if the shelter is not found
   */
  @Transactional(readOnly = true)
  public ShelterView getShelterDetails(String shelterId) {
    Objects.requireNonNull(shelterId, "shelterId");
    Shelter shelter =
        shelterRepository
            .findById(shelterId)
            .orElseThrow(() -> new NoSuchElementException("Shelter not found: " + shelterId));
    int remaining = shelter.checkAvailability();
    log.debug("Retrieved shelter {} with remaining capacity {}", shelterId, remaining);
    return new ShelterView(shelter, remaining);
  }

  /**
   * Finds other shelters of a district that can take a number of people.
   *
   * @param district the district of the selected shelter
   * @param needed the number of people to accommodate
   * @return the shelters with enough space
   */
  @Transactional(readOnly = true)
  public List<ShelterView> findAlternativeShelters(District district, int needed) {
    Objects.requireNonNull(district, "district");
    log.debug(
        "Searching alternative shelters in district {} for needed capacity {}", district, needed);
    List<Shelter> shelters = shelterRepository.findWithSpace(district, needed);
    return shelters.stream().map(s -> new ShelterView(s, s.checkAvailability())).toList();
  }

  /**
   * Records a new occupancy total; an invalid value or over-capacity is a rejected reply.
   *
   * @param update the new total with the client metadata
   * @return the outcome
   * @throws NoSuchElementException if the shelter or the recording officer is not found
   */
  @Transactional
  public ShelterOutcome updateOccupancy(OccupancyUpdate update) {
    Objects.requireNonNull(update, "update");
    Optional<ProcessedSubmission> duplicate = submissionRecorder.check(update.meta());
    if (duplicate.isPresent()) {
      log.info("Duplicate updateOccupancy submission detected: {}", update.meta().submissionId());
      submissionRecorder.touchSync(update.meta());
      return new ShelterOutcome(
          new SyncResult(
              update.meta().submissionId(), SyncOutcome.DUPLICATE, null, DUPLICATE_MESSAGE),
          null,
          0,
          List.of());
    }

    Shelter shelter =
        shelterRepository
            .findById(update.shelterId())
            .orElseThrow(
                () -> new NoSuchElementException("Shelter not found: " + update.shelterId()));

    if (update.newTotal() < 0) {
      log.warn(
          "Rejected occupancy update for shelter {}: invalid total {}",
          update.shelterId(),
          update.newTotal());
      return new ShelterOutcome(
          new SyncResult(
              update.meta().submissionId(),
              SyncOutcome.REJECTED,
              RejectionReason.INVALID_VALUE,
              INVALID_TOTAL_MESSAGE),
          null,
          0,
          List.of());
    }

    if (!shelter.canAccommodate(update.newTotal())) {
      int remaining = shelter.checkAvailability();
      String message =
          String.format(
              "Capacity exceeded: %d is more than the capacity of %d; %d places remain",
              update.newTotal(), shelter.getCapacity(), remaining);
      log.warn("Rejected occupancy update for shelter {}: {}", update.shelterId(), message);
      int excess = Math.max(1, update.newTotal() - shelter.getCapacity());
      List<ShelterView> alternatives = findAlternativeShelters(shelter.getDistrict(), excess);
      return new ShelterOutcome(
          new SyncResult(
              update.meta().submissionId(),
              SyncOutcome.REJECTED,
              RejectionReason.OVER_CAPACITY,
              message),
          shelter,
          remaining,
          alternatives);
    }

    DistrictOfficer officer =
        districtOfficers
            .findById(update.meta().userId())
            .orElseThrow(
                () -> new NoSuchElementException("Officer not found: " + update.meta().userId()));

    LocalDateTime recordedAt = LocalDateTime.now(clock);
    OccupancyRecord record =
        new OccupancyRecord(update.newTotal(), update.meta().capturedAt(), recordedAt, officer);
    shelter.updateOccupancy(record);
    shelterRepository.save(shelter);

    submissionRecorder.record(update.meta(), shelter.getShelterId());
    submissionRecorder.touchSync(update.meta());

    int remainingCapacity = shelter.checkAvailability();
    log.info(
        "Successfully updated occupancy of shelter {} to {} (remaining {})",
        shelter.getShelterId(),
        shelter.getCurrentOccupancy(),
        remainingCapacity);
    return new ShelterOutcome(
        new SyncResult(update.meta().submissionId(), SyncOutcome.ACCEPTED, null, null),
        shelter,
        remainingCapacity,
        List.of());
  }

  /**
   * Validates the details of a shelter.
   *
   * @param details the details
   * @param shelterId the shelter being edited, or {@code null} when a shelter is being registered
   * @return the validation result
   */
  @Transactional(readOnly = true)
  public ValidationResult validateShelterDetails(ShelterDetails details, String shelterId) {
    Objects.requireNonNull(details, "details");
    List<String> errors = new ArrayList<>();

    if (details.name() == null || details.name().isBlank()) {
      errors.add("name: required");
    }
    if (details.address() == null || details.address().isBlank()) {
      errors.add("address: required");
    }
    if (details.district() == null) {
      errors.add("district: required");
    }
    if (details.capacity() < 1) {
      errors.add("capacity: must be a whole number of at least 1");
    }
    if (details.contactNumber() == null || details.contactNumber().isBlank()) {
      errors.add("contactNumber: required");
    } else if (!SRI_LANKAN_PHONE.matcher(details.contactNumber().trim()).matches()) {
      errors.add("contactNumber: must be a 10-digit Sri Lankan number starting with 0");
    }
    if (details.operatorId() != null && !details.operatorId().isBlank()) {
      if (!organizationRepository.existsById(details.operatorId())) {
        errors.add("operatorId: operating organisation not found");
      }
    }

    if (shelterId != null) {
      Optional<Shelter> existing = shelterRepository.findById(shelterId);
      if (existing.isPresent()) {
        Shelter s = existing.get();
        if (details.capacity() < s.getCurrentOccupancy()) {
          errors.add(
              String.format(
                  "capacity: %d is below the current occupancy of %d",
                  details.capacity(), s.getCurrentOccupancy()));
        }
      }
    }

    return new ValidationResult(errors.isEmpty(), errors);
  }

  /**
   * Registers a new shelter.
   *
   * @param submission the details with the client metadata
   * @return the outcome
   * @throws NoSuchElementException if the recording officer is not found
   */
  @Transactional
  public ShelterOutcome registerShelter(ShelterSubmission submission) {
    Objects.requireNonNull(submission, "submission");
    Optional<ProcessedSubmission> duplicate = submissionRecorder.check(submission.meta());
    if (duplicate.isPresent()) {
      log.info(
          "Duplicate registerShelter submission detected: {}", submission.meta().submissionId());
      submissionRecorder.touchSync(submission.meta());
      return new ShelterOutcome(
          new SyncResult(
              submission.meta().submissionId(), SyncOutcome.DUPLICATE, null, DUPLICATE_MESSAGE),
          null,
          0,
          List.of());
    }

    ValidationResult validation = validateShelterDetails(submission.details(), null);
    if (!validation.valid()) {
      log.warn("Shelter registration validation failed: {}", validation.errors());
      return new ShelterOutcome(
          new SyncResult(
              submission.meta().submissionId(),
              SyncOutcome.REJECTED,
              RejectionReason.VALIDATION_FAILED,
              String.join("; ", validation.errors())),
          null,
          0,
          List.of());
    }

    DistrictOfficer officer =
        districtOfficers
            .findById(submission.meta().userId())
            .orElseThrow(
                () ->
                    new NoSuchElementException("Officer not found: " + submission.meta().userId()));

    Organization operator = null;
    if (submission.details().operatorId() != null && !submission.details().operatorId().isBlank()) {
      operator = organizationRepository.findById(submission.details().operatorId()).orElse(null);
    }

    String shelterId = UUID.randomUUID().toString();
    Shelter newShelter = new Shelter(shelterId, submission.details(), officer, operator);
    shelterRepository.save(newShelter);

    submissionRecorder.record(submission.meta(), shelterId);
    submissionRecorder.touchSync(submission.meta());

    int remainingCapacity = newShelter.checkAvailability();
    log.info(
        "Successfully registered shelter {} with capacity {}", shelterId, newShelter.getCapacity());
    return new ShelterOutcome(
        new SyncResult(submission.meta().submissionId(), SyncOutcome.ACCEPTED, null, null),
        newShelter,
        remainingCapacity,
        List.of());
  }

  /**
   * Changes the details of a shelter.
   *
   * @param shelterId the identifier of the shelter
   * @param submission the new details with the client metadata
   * @return the outcome
   * @throws NoSuchElementException if the shelter is not found
   */
  @Transactional
  public ShelterOutcome updateShelterDetails(String shelterId, ShelterSubmission submission) {
    Objects.requireNonNull(shelterId, "shelterId");
    Objects.requireNonNull(submission, "submission");

    Optional<ProcessedSubmission> duplicate = submissionRecorder.check(submission.meta());
    if (duplicate.isPresent()) {
      log.info(
          "Duplicate updateShelterDetails submission detected: {}",
          submission.meta().submissionId());
      submissionRecorder.touchSync(submission.meta());
      return new ShelterOutcome(
          new SyncResult(
              submission.meta().submissionId(), SyncOutcome.DUPLICATE, null, DUPLICATE_MESSAGE),
          null,
          0,
          List.of());
    }

    Shelter shelter =
        shelterRepository
            .findById(shelterId)
            .orElseThrow(() -> new NoSuchElementException("Shelter not found: " + shelterId));

    ValidationResult validation = validateShelterDetails(submission.details(), shelterId);
    if (!validation.valid()) {
      log.warn(
          "Shelter update validation failed for shelter {}: {}", shelterId, validation.errors());
      return new ShelterOutcome(
          new SyncResult(
              submission.meta().submissionId(),
              SyncOutcome.REJECTED,
              RejectionReason.VALIDATION_FAILED,
              String.join("; ", validation.errors())),
          shelter,
          shelter.checkAvailability(),
          List.of());
    }

    Organization operator = null;
    if (submission.details().operatorId() != null && !submission.details().operatorId().isBlank()) {
      operator = organizationRepository.findById(submission.details().operatorId()).orElse(null);
    }

    shelter.setOperator(operator);
    shelter.updateDetails(submission.details());
    shelterRepository.save(shelter);

    submissionRecorder.record(submission.meta(), shelterId);
    submissionRecorder.touchSync(submission.meta());

    int remainingCapacity = shelter.checkAvailability();
    log.info(
        "Successfully updated details of shelter {} (new capacity {}, remaining {})",
        shelterId,
        shelter.getCapacity(),
        remainingCapacity);
    return new ShelterOutcome(
        new SyncResult(submission.meta().submissionId(), SyncOutcome.ACCEPTED, null, null),
        shelter,
        remainingCapacity,
        List.of());
  }
}
