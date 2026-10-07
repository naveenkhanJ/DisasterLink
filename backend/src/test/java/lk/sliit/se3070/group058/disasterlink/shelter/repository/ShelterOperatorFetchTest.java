package lk.sliit.se3070.group058.disasterlink.shelter.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.web.dto.ShelterResponse;
import lk.sliit.se3070.group058.disasterlink.shelter.web.mapper.ShelterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The UC3 endpoints map {@code Shelter.operator} after the control's transaction has ended
 * (open-in-view is off): the finders they read fetch it with an entity graph (standards section
 * 12.4, "Lazy reads"; P155 X2, G6). The test switches off its own transaction, so each finder's
 * transaction has ended when the shelter is mapped, and cleans up after each method.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ShelterOperatorFetchTest {

  @Autowired private ShelterRepository shelters;
  @Autowired private OrganizationRepository organizations;
  @Autowired private DistrictOfficerRepository officers;

  @BeforeEach
  void saveShelterWithOperator() {
    DistrictOfficer officer =
        officers.save(
            new DistrictOfficer(
                "DO-COL-01",
                "R. W",
                "0770000001",
                "do@example.lk",
                District.COLOMBO,
                Language.ENGLISH));
    Organization operator =
        organizations.save(
            new Organization(
                "ORG-0001",
                "CARE Sri Lanka",
                OrganizationType.NGO,
                "0110000001",
                "Colombo",
                List.of(District.COLOMBO),
                Language.ENGLISH));
    shelters.save(
        new Shelter(
            "SHL-COL-0001",
            new ShelterDetails(
                "Rathmalana Maha Vidyalaya",
                "Rathmalana, Colombo",
                District.COLOMBO,
                500,
                "0710000001",
                "ORG-0001"),
            officer,
            operator));
  }

  private static void assertMapped(Shelter shelter) {
    ShelterResponse response = ShelterMapper.toShelterResponse(shelter);

    assertThat(response.operatorName()).isEqualTo("CARE Sri Lanka");
    assertThat(response.operatorId()).isEqualTo("ORG-0001");
  }

  @Test
  @DisplayName("findByDistrict loads the operator, so the mapper works after the transaction")
  void findByDistrictFetchesTheOperator() {
    List<Shelter> found = shelters.findByDistrict(District.COLOMBO);

    assertThat(found).hasSize(1);
    assertMapped(found.get(0));
  }

  @Test
  @DisplayName("findWithSpace loads the operator, so the mapper works after the transaction")
  void findWithSpaceFetchesTheOperator() {
    List<Shelter> found = shelters.findWithSpace(District.COLOMBO, 100);

    assertThat(found).hasSize(1);
    assertMapped(found.get(0));
  }

  @Test
  @DisplayName("findById loads the operator, so the mapper works after the transaction")
  void findByIdFetchesTheOperator() {
    Shelter found = shelters.findById("SHL-COL-0001").orElseThrow();

    assertMapped(found);
  }

  @Test
  @DisplayName("A shelter without an operator maps to null name and null id")
  void shelterWithoutOperator() {
    DistrictOfficer officer = officers.findById("DO-COL-01").orElseThrow();
    shelters.save(
        new Shelter(
            "SHL-COL-0002",
            new ShelterDetails("Hall", "Road", District.COLOMBO, 50, "0710000002", null),
            officer,
            null));

    ShelterResponse response =
        ShelterMapper.toShelterResponse(shelters.findById("SHL-COL-0002").orElseThrow());

    assertThat(response.operatorName()).isNull();
    assertThat(response.operatorId()).isNull();
  }
}
