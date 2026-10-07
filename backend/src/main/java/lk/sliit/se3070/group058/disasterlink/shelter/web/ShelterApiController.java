package lk.sliit.se3070.group058.disasterlink.shelter.web;

import jakarta.validation.Valid;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.shelter.service.ShelterController;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.OccupancyRequest;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterOutcomeResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterSubmissionRequest;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterViewResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ValidationRequest;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ValidationResultResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.mapper.ShelterMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The UC3 JSON endpoints that the shelter page script calls (standards sections 4 and 10, "UC3
 * pages"): every read and every write of Manage Emergency Shelters, mapped to the operations of
 * {@link ShelterController}. The replies are wire records, never entities, and a validation or
 * capacity result is a normal HTTP 200 reply carrying REJECTED; only a malformed request (400), an
 * unknown identifier (404) or a fault (500) is an error, answered by {@code ApiExceptionHandler} as
 * a problem detail that the page logs and shows in the common error banner.
 *
 * <p>Written by the skeleton and excluded by name from the use-case owner's coverage gate
 * (standards sections 8 and 12.4). Implements CH-28 and CH-05.
 */
@RestController
@RequestMapping("/api/shelters")
public class ShelterApiController {

  private final ShelterController shelterController;

  private final CurrentUserService currentUserService;

  /**
   * Creates the controller.
   *
   * @param shelterController the UC3 control
   * @param currentUserService the source of the demo district officer's identifier
   */
  public ShelterApiController(
      ShelterController shelterController, CurrentUserService currentUserService) {
    this.shelterController = shelterController;
    this.currentUserService = currentUserService;
  }

  /**
   * Lists the shelters of the district officer's district.
   *
   * @return the shelters
   */
  @GetMapping
  public List<ShelterResponse> getDistrictShelters() {
    String officerId = currentUserService.userId(DemoRole.DISTRICT_OFFICER);
    return shelterController.getDistrictShelters(officerId).stream()
        .map(ShelterMapper::toShelterResponse)
        .toList();
  }

  /**
   * Finds the shelters of a district with at least the needed number of free places.
   *
   * @param district the district of the selected shelter
   * @param needed the number of places needed
   * @return the alternatives with their remaining capacity
   */
  @GetMapping("/alternatives")
  public List<ShelterViewResponse> findAlternativeShelters(
      @RequestParam District district, @RequestParam int needed) {
    return ShelterMapper.toViewResponses(
        shelterController.findAlternativeShelters(district, needed));
  }

  /**
   * Retrieves one shelter with its remaining capacity.
   *
   * @param shelterId the identifier of the shelter
   * @return the shelter view
   */
  @GetMapping("/{shelterId}")
  public ShelterViewResponse getShelterDetails(@PathVariable String shelterId) {
    return ShelterMapper.toViewResponse(shelterController.getShelterDetails(shelterId));
  }

  /**
   * Checks shelter details before a registration or a detail edit.
   *
   * @param request the details and, for an edit, the shelter being edited
   * @return the validation result (HTTP 200 whether valid or not)
   */
  @PostMapping("/validation")
  public ValidationResultResponse validateShelterDetails(
      @Valid @RequestBody ValidationRequest request) {
    return ShelterMapper.toValidationResponse(
        shelterController.validateShelterDetails(
            ShelterMapper.toDetails(request.details()), request.shelterId()));
  }

  /**
   * Records a new total occupancy.
   *
   * @param shelterId the shelter
   * @param request the new total and the client meta
   * @return the outcome (HTTP 200 whether accepted or rejected)
   */
  @PostMapping("/{shelterId}/occupancy")
  public ShelterOutcomeResponse updateOccupancy(
      @PathVariable String shelterId, @Valid @RequestBody OccupancyRequest request) {
    OccupancyUpdate update =
        new OccupancyUpdate(shelterId, request.newTotal(), ShelterMapper.toMeta(request.meta()));
    return ShelterMapper.toOutcomeResponse(shelterController.updateOccupancy(update));
  }

  /**
   * Registers a shelter.
   *
   * @param request the details and the client meta
   * @return the outcome (HTTP 200 whether accepted or rejected)
   */
  @PostMapping
  public ShelterOutcomeResponse registerShelter(
      @Valid @RequestBody ShelterSubmissionRequest request) {
    return ShelterMapper.toOutcomeResponse(
        shelterController.registerShelter(ShelterMapper.toSubmission(request)));
  }

  /**
   * Changes the details of a shelter.
   *
   * @param shelterId the shelter
   * @param request the new details and the client meta
   * @return the outcome (HTTP 200 whether accepted or rejected)
   */
  @PutMapping("/{shelterId}")
  public ShelterOutcomeResponse updateShelterDetails(
      @PathVariable String shelterId, @Valid @RequestBody ShelterSubmissionRequest request) {
    return ShelterMapper.toOutcomeResponse(
        shelterController.updateShelterDetails(shelterId, ShelterMapper.toSubmission(request)));
  }
}
