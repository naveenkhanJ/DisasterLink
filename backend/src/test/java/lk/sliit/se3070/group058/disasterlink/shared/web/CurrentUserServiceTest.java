package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.config.DemoUserProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** Tests of {@link CurrentUserService} against H2 with the configured demo users. */
@DataJpaTest(
    properties = {
      "demo.users.dmc-officer-id=DMC-0001",
      "demo.users.dmc-duty-officer-id=DMC-0002",
      "demo.users.district-officer-id=DO-COL-01",
      "demo.users.organization-id=ORG-0001",
      "demo.users.citizen-id=CIT-KLT-0001"
    })
@Import({CurrentUserService.class, CurrentUserServiceTest.PropertiesConfiguration.class})
class CurrentUserServiceTest {

  /** Binds the demo user properties, which the slice does not scan. */
  @TestConfiguration
  @EnableConfigurationProperties(DemoUserProperties.class)
  static class PropertiesConfiguration {}

  @Autowired private CurrentUserService service;
  @Autowired private TestEntityManager entityManager;

  private void saveDemoUsers() {
    entityManager.persist(
        new DMCOfficer(
            "DMC-0001", "N. Jayasinghe", "0112222222", "n@dmc.lk", "Director", Set.of()));
    entityManager.persist(
        new DMCDutyOfficer(
            "DMC-0002", "Amali Perera", "0113333333", "a@dmc.lk", "Duty Officer", Set.of()));
    entityManager.persist(
        new DistrictOfficer(
            "DO-COL-01",
            "S. Fernando",
            "0765551111",
            "s@dmc.lk",
            District.COLOMBO,
            Language.ENGLISH));
    entityManager.persist(
        new Organization(
            "ORG-0001",
            "Sri Lanka Red Cross",
            OrganizationType.NGO,
            "0112691095",
            "Colombo 07",
            List.of(District.COLOMBO),
            Language.ENGLISH));
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("userId gives the configured identifier of each role")
  void userIds() {
    assertThat(service.userId(DemoRole.DMC_OFFICER)).isEqualTo("DMC-0001");
    assertThat(service.userId(DemoRole.DMC_DUTY_OFFICER)).isEqualTo("DMC-0002");
    assertThat(service.userId(DemoRole.DISTRICT_OFFICER)).isEqualTo("DO-COL-01");
    assertThat(service.userId(DemoRole.ORGANIZATION)).isEqualTo("ORG-0001");
  }

  @Test
  @DisplayName("currentUser gives initials, name and the prototype's role label for each role")
  void currentUsers() {
    saveDemoUsers();

    assertThat(service.currentUser(DemoRole.DMC_OFFICER))
        .isEqualTo(new CurrentUserView("NJ", "N. Jayasinghe", "DMC Officer"));
    assertThat(service.currentUser(DemoRole.DMC_DUTY_OFFICER))
        .isEqualTo(new CurrentUserView("AP", "Amali Perera", "DMC Duty Officer"));
    assertThat(service.currentUser(DemoRole.DISTRICT_OFFICER))
        .isEqualTo(new CurrentUserView("SF", "S. Fernando", "Colombo District"));
    assertThat(service.currentUser(DemoRole.ORGANIZATION))
        .isEqualTo(
            new CurrentUserView("SC", "Sri Lanka Red Cross", "Resource-Owning Organisation"));
  }

  @Test
  @DisplayName("A district with two words is written in title case")
  void districtLabelInTitleCase() {
    entityManager.persist(
        new DistrictOfficer(
            "DO-COL-01",
            "K. Silva",
            "0765551111",
            "k@dmc.lk",
            District.NUWARA_ELIYA,
            Language.TAMIL));
    entityManager.flush();
    entityManager.clear();

    assertThat(service.currentUser(DemoRole.DISTRICT_OFFICER).role())
        .isEqualTo("Nuwara Eliya District");
  }

  @Test
  @DisplayName("A configured user that does not exist is a NoSuchElementException for every role")
  void unknownUser() {
    for (DemoRole role : DemoRole.values()) {
      assertThatThrownBy(() -> service.currentUser(role))
          .isInstanceOf(NoSuchElementException.class);
    }
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "N. Jayasinghe|NJ",
        "Amali Perera|AP",
        "Sri Lanka Red Cross|SC",
        "  Kamala   de   Silva |KS",
        "Colombo|C"
      })
  @DisplayName("Initials are the first letters of the first and last words, in upper case")
  void initials(String name, String expected) {
    assertThat(CurrentUserService.initials(name)).isEqualTo(expected.toUpperCase());
  }
}
