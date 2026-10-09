package lk.sliit.se3070.group058.disasterlink.shelter.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.ShelterView;
import lk.sliit.se3070.group058.disasterlink.shelter.service.ShelterController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit and web slice tests for {@link ShelterManagementUI}. Verifies view resolution, model
 * attributes, and controller interactions for all officer screens.
 */
@WebMvcTest(ShelterManagementUI.class)
class ShelterManagementUITest {

  private static final String OFFICER_ID = "DO-COL-01";
  private static final String SHELTER_ID = "SHL-COL-0001";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShelterController shelterController;
  @MockitoBean private CurrentUserService currentUserService;

  private Shelter shelter;
  private Organization organization;
  private ShelterView shelterView;

  @BeforeEach
  void setUp() {
    DistrictOfficer officer =
        new DistrictOfficer(
            OFFICER_ID, "R. W", "0775551111", "r@example.lk", District.COLOMBO, Language.SINHALA);
    organization =
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
            organization);
    shelterView = new ShelterView(shelter, 500);

    when(currentUserService.userId(DemoRole.DISTRICT_OFFICER)).thenReturn(OFFICER_ID);
    when(currentUserService.currentUser(DemoRole.DISTRICT_OFFICER))
        .thenReturn(
            new lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView(
                "SF", "S. Fernando", "Colombo District"));
  }

  @Test
  @DisplayName("openShelters renders shelterManagement template with district shelters")
  void openSheltersRendersList() throws Exception {
    when(shelterController.getDistrictShelters(OFFICER_ID)).thenReturn(List.of(shelter));

    mockMvc
        .perform(get("/shelter"))
        .andExpect(status().isOk())
        .andExpect(view().name("shelter/shelterManagement"))
        .andExpect(model().attribute("shelters", List.of(shelter)));

    verify(shelterController).getDistrictShelters(OFFICER_ID);
  }

  @Test
  @DisplayName("openShelters with trailing slash also renders shelterManagement template")
  void openSheltersTrailingSlash() throws Exception {
    when(shelterController.getDistrictShelters(OFFICER_ID)).thenReturn(List.of(shelter));

    mockMvc
        .perform(get("/shelter/"))
        .andExpect(status().isOk())
        .andExpect(view().name("shelter/shelterManagement"))
        .andExpect(model().attribute("shelters", List.of(shelter)));
  }

  @Test
  @DisplayName("selectShelter renders shelterDetails template with shelter view and capacity")
  void selectShelterRendersDetails() throws Exception {
    when(shelterController.getShelterDetails(SHELTER_ID)).thenReturn(shelterView);

    mockMvc
        .perform(get("/shelter/details").param("shelterId", SHELTER_ID))
        .andExpect(status().isOk())
        .andExpect(view().name("shelter/shelterDetails"))
        .andExpect(model().attribute("view", shelterView))
        .andExpect(model().attribute("shelter", shelter))
        .andExpect(model().attribute("remainingCapacity", 500));

    verify(shelterController).getShelterDetails(SHELTER_ID);
  }

  @Test
  @DisplayName("startRegistration renders registrationForm with organizations pick-list")
  void startRegistrationRendersForm() throws Exception {
    when(shelterController.getOrganizations()).thenReturn(List.of(organization));

    mockMvc
        .perform(get("/shelter/register"))
        .andExpect(status().isOk())
        .andExpect(view().name("shelter/registrationForm"))
        .andExpect(model().attribute("organizations", List.of(organization)));

    verify(shelterController).getOrganizations();
  }

  @Test
  @DisplayName("editShelterDetails renders editShelterDetails with shelter and organizations")
  void editShelterDetailsRendersEdit() throws Exception {
    when(shelterController.getShelterDetails(SHELTER_ID)).thenReturn(shelterView);
    when(shelterController.getOrganizations()).thenReturn(List.of(organization));

    mockMvc
        .perform(get("/shelter/edit").param("shelterId", SHELTER_ID))
        .andExpect(status().isOk())
        .andExpect(view().name("shelter/editShelterDetails"))
        .andExpect(model().attribute("view", shelterView))
        .andExpect(model().attribute("shelter", shelter))
        .andExpect(model().attribute("organizations", List.of(organization)));

    verify(shelterController).getShelterDetails(SHELTER_ID);
    verify(shelterController).getOrganizations();
  }

  @Test
  @DisplayName("findShelterWithSpace renders shelterDetails with alternative shelters")
  void findShelterWithSpaceRendersAlternatives() throws Exception {
    when(shelterController.findAlternativeShelters(District.COLOMBO, 200))
        .thenReturn(List.of(shelterView));

    mockMvc
        .perform(get("/shelter/find-space").param("district", "COLOMBO").param("needed", "200"))
        .andExpect(status().isOk())
        .andExpect(view().name("shelter/shelterDetails"))
        .andExpect(model().attribute("alternatives", List.of(shelterView)));

    verify(shelterController).findAlternativeShelters(District.COLOMBO, 200);
  }

  @Test
  @DisplayName("Constructor enforces non-null parameters")
  void constructorRejectsNull() {
    assertThatThrownBy(() -> new ShelterManagementUI(null, currentUserService))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ShelterManagementUI(shelterController, null))
        .isInstanceOf(NullPointerException.class);
  }
}
