package lk.sliit.se3070.group058.disasterlink.shelter.web.mapper;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.web.WireTime;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterOutcome;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterView;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ClientMetaRequest;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterDetailsRequest;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterOutcomeResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterSubmissionRequest;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterViewResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ValidationResultResponse;

/**
 * Maps between the UC3 wire records and the dictionary's types. Times on the wire carry their
 * offset (model dictionary section 0.3); stored and design times are {@code LocalDateTime} in
 * Asia/Colombo.
 *
 * <p>Implementation-only class (standards section 4, "No entities on the wire").
 */
public final class ShelterMapper {

  private ShelterMapper() {
    throw new AssertionError("ShelterMapper is a utility class");
  }

  /**
   * Maps a shelter to its wire record.
   *
   * @param shelter the shelter
   * @return the record
   */
  public static ShelterResponse toShelterResponse(Shelter shelter) {
    return new ShelterResponse(
        shelter.getShelterId(),
        shelter.getName(),
        shelter.getAddress(),
        shelter.getDistrict(),
        shelter.getCapacity(),
        shelter.getCurrentOccupancy(),
        shelter.getContactNumber(),
        shelter.getStatus(),
        shelter.getOperator().map(Organization::getName).orElse(null),
        shelter.getOperator().map(Organization::getOrganizationId).orElse(null));
  }

  /**
   * Maps a shelter view to its wire record.
   *
   * @param view the view
   * @return the record
   */
  public static ShelterViewResponse toViewResponse(ShelterView view) {
    return new ShelterViewResponse(toShelterResponse(view.shelter()), view.remainingCapacity());
  }

  /**
   * Maps the views to their wire records.
   *
   * @param views the views
   * @return the records, in order
   */
  public static List<ShelterViewResponse> toViewResponses(List<ShelterView> views) {
    return views.stream().map(ShelterMapper::toViewResponse).toList();
  }

  /**
   * Maps an outcome to its wire record.
   *
   * @param outcome the outcome of a write
   * @return the record
   */
  public static ShelterOutcomeResponse toOutcomeResponse(ShelterOutcome outcome) {
    return new ShelterOutcomeResponse(
        outcome.result(),
        outcome.shelter() == null ? null : toShelterResponse(outcome.shelter()),
        outcome.remainingCapacity(),
        toViewResponses(outcome.alternatives()));
  }

  /**
   * Maps a validation result to its wire record.
   *
   * @param result the result
   * @return the record
   */
  public static ValidationResultResponse toValidationResponse(ValidationResult result) {
    return new ValidationResultResponse(result.valid(), result.errors());
  }

  /**
   * Maps the wire form of the client meta, converting {@code capturedAt} to Asia/Colombo.
   *
   * @param request the request record
   * @return the client meta
   */
  public static ClientMeta toMeta(ClientMetaRequest request) {
    return new ClientMeta(
        request.clientId(),
        request.userId(),
        request.submissionId(),
        WireTime.fromWire(request.capturedAt()),
        request.pendingCount());
  }

  /**
   * Maps the wire form of the shelter details.
   *
   * @param request the request record
   * @return the details
   */
  public static ShelterDetails toDetails(ShelterDetailsRequest request) {
    return new ShelterDetails(
        request.name(),
        request.address(),
        request.district(),
        request.capacity(),
        request.contactNumber(),
        request.operatorId());
  }

  /**
   * Maps the wire form of a registration or a detail change.
   *
   * @param request the request record
   * @return the submission
   */
  public static ShelterSubmission toSubmission(ShelterSubmissionRequest request) {
    return new ShelterSubmission(toDetails(request.details()), toMeta(request.meta()));
  }
}
