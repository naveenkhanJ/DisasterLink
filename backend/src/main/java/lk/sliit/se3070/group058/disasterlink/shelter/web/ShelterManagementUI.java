package lk.sliit.se3070.group058.disasterlink.shelter.web;

import java.util.List;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterView;
import lk.sliit.se3070.group058.disasterlink.shelter.service.ShelterController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web boundary controller for UC3, Manage Emergency Shelters.
 *
 * <p>Implements CH-10, CH-28, CH-29, CH-30 and PR15. Serves the officer screens: the district
 * shelter list, shelter detail view, registration form, and edit details screen. Client-side script
 * handles offline queues, sync operations, and dynamic UI updates via the REST API.
 */
@Controller
@RequestMapping("/shelter")
public class ShelterManagementUI {

  private static final Logger log = LoggerFactory.getLogger(ShelterManagementUI.class);

  private final ShelterController shelterController;
  private final CurrentUserService currentUserService;

  /**
   * Creates the boundary controller with required control and user services.
   *
   * @param shelterController the UC3 control
   * @param currentUserService source for current demo officer identity
   */
  public ShelterManagementUI(
      ShelterController shelterController, CurrentUserService currentUserService) {
    this.shelterController = Objects.requireNonNull(shelterController, "shelterController");
    this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
  }

  /**
   * Adds the current district officer user view to every model for layout rendering.
   *
   * @return the current user view
   */
  @ModelAttribute("currentUser")
  public CurrentUserView currentUser() {
    return currentUserService.currentUser(DemoRole.DISTRICT_OFFICER);
  }

  /**
   * Opens the shelter management dashboard listing all shelters in the officer's district.
   *
   * @param model Spring MVC model
   * @return view name for the shelter management page
   */
  @GetMapping({"", "/"})
  public String openShelters(Model model) {
    String officerId = currentUserService.userId(DemoRole.DISTRICT_OFFICER);
    log.debug("Opening shelters dashboard for district officer {}", officerId);
    List<Shelter> shelters = shelterController.getDistrictShelters(officerId);
    model.addAttribute("shelters", shelters);
    return "shelter/shelterManagement";
  }

  /**
   * Selects and displays the details and capacity of an individual shelter.
   *
   * @param shelterId the shelter identifier
   * @param model Spring MVC model
   * @return view name for the shelter details page
   */
  @GetMapping("/details")
  public String selectShelter(@RequestParam("shelterId") String shelterId, Model model) {
    Objects.requireNonNull(shelterId, "shelterId");
    log.debug("Selecting shelter details for {}", shelterId);
    ShelterView view = shelterController.getShelterDetails(shelterId);
    model.addAttribute("view", view);
    model.addAttribute("shelter", view.shelter());
    model.addAttribute("remainingCapacity", view.remainingCapacity());
    return "shelter/shelterDetails";
  }

  /**
   * Opens the shelter registration form for registering a new emergency shelter.
   *
   * @param model Spring MVC model
   * @return view name for the registration form
   */
  @GetMapping("/register")
  public String startRegistration(Model model) {
    log.debug("Opening registration form for new shelter");
    List<Organization> organizations = shelterController.getOrganizations();
    model.addAttribute("organizations", organizations);
    return "shelter/registrationForm";
  }

  /**
   * Opens the edit shelter details form for modifying an existing shelter's configuration.
   *
   * @param shelterId the shelter identifier to edit
   * @param model Spring MVC model
   * @return view name for the edit shelter details page
   */
  @GetMapping("/edit")
  public String editShelterDetails(@RequestParam("shelterId") String shelterId, Model model) {
    Objects.requireNonNull(shelterId, "shelterId");
    log.debug("Opening edit form for shelter {}", shelterId);
    ShelterView view = shelterController.getShelterDetails(shelterId);
    List<Organization> organizations = shelterController.getOrganizations();
    model.addAttribute("view", view);
    model.addAttribute("shelter", view.shelter());
    model.addAttribute("organizations", organizations);
    return "shelter/editShelterDetails";
  }

  /**
   * Finds alternative shelters in the specified district that can accommodate the needed capacity.
   *
   * @param district the district of the shelter
   * @param needed the number of spaces needed
   * @param model Spring MVC model
   * @return view name for the shelter details page showing alternatives
   */
  @GetMapping("/find-space")
  public String findShelterWithSpace(
      @RequestParam("district") District district,
      @RequestParam("needed") int needed,
      Model model) {
    Objects.requireNonNull(district, "district");
    log.debug("Searching alternative shelters in district {} for needed {}", district, needed);
    List<ShelterView> alternatives = shelterController.findAlternativeShelters(district, needed);
    model.addAttribute("alternatives", alternatives);
    return "shelter/shelterDetails";
  }
}
