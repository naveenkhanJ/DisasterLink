package lk.sliit.se3070.group058.disasterlink.shelter.service;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterOutcome;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterView;
import org.springframework.stereotype.Service;

/**
 * The control of use case UC3, Manage Emergency Shelters.
 *
 * <p>Signature stub from the skeleton; Weliwitigoda replaces the bodies and adds the constructor on
 * his branch. The signatures are those of the model dictionary section 5 (CH-19, CH-28, CH-29 and
 * CH-05). The three writes carry client metadata and act for {@code meta.userId()}; each is
 * idempotent through {@code SubmissionRecorder}.
 */
@Service
public class ShelterController {

  private static final String OWNER_MESSAGE = "not implemented yet — Weliwitigoda";

  /**
   * Lists the shelters of an officer's district; every officer of the district manages them.
   *
   * @param officerId the identifier of the officer
   * @return the shelters of the officer's district
   */
  public List<Shelter> getDistrictShelters(String officerId) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Shows a shelter with its remaining capacity.
   *
   * @param shelterId the identifier of the shelter
   * @return the shelter view
   */
  public ShelterView getShelterDetails(String shelterId) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Finds other shelters of a district that can take a number of people.
   *
   * @param district the district of the selected shelter
   * @param needed the number of people to accommodate
   * @return the shelters with enough space
   */
  public List<ShelterView> findAlternativeShelters(District district, int needed) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Records a new occupancy total; an invalid value or over-capacity is a rejected reply.
   *
   * @param update the new total with the client metadata
   * @return the outcome
   */
  public ShelterOutcome updateOccupancy(OccupancyUpdate update) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Validates the details of a shelter.
   *
   * @param details the details
   * @param shelterId the shelter being edited, or {@code null} when a shelter is being registered
   * @return the validation result
   */
  public ValidationResult validateShelterDetails(ShelterDetails details, String shelterId) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Registers a new shelter.
   *
   * @param submission the details with the client metadata
   * @return the outcome
   */
  public ShelterOutcome registerShelter(ShelterSubmission submission) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Changes the details of a shelter.
   *
   * @param shelterId the identifier of the shelter
   * @param submission the new details with the client metadata
   * @return the outcome
   */
  public ShelterOutcome updateShelterDetails(String shelterId, ShelterSubmission submission) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }
}
