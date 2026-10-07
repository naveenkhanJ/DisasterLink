package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;

/**
 * The people the seed has saved, as the managed entities the later groups refer to.
 *
 * @param officer the DMC officer DMC-0001
 * @param dutyOfficer the DMC duty officer DMC-0002
 * @param districtOfficers the district officers, in order of identifier
 * @param organizations the 14 organisations, ORG-0001 first
 * @param citizens the citizens of each seeded district, in order of number
 */
record SeedPeople(
    DMCOfficer officer,
    DMCDutyOfficer dutyOfficer,
    List<DistrictOfficer> districtOfficers,
    List<Organization> organizations,
    Map<District, List<Citizen>> citizens) {

  /** The organisation with an identifier. */
  Organization organization(String organizationId) {
    return organizations.stream()
        .filter(organization -> organization.getOrganizationId().equals(organizationId))
        .findFirst()
        .orElseThrow();
  }

  /** The district officer with an identifier. */
  DistrictOfficer districtOfficer(String userId) {
    return districtOfficers.stream()
        .filter(officer -> officer.getUserId().equals(userId))
        .findFirst()
        .orElseThrow();
  }

  /** The citizen with an identifier. */
  Citizen citizen(String userId) {
    return allCitizens()
        .filter(citizen -> citizen.getUserId().equals(userId))
        .findFirst()
        .orElseThrow();
  }

  /** Every seeded citizen. */
  Stream<Citizen> allCitizens() {
    return citizens.values().stream().flatMap(List::stream);
  }

  /** The district officers of a district. */
  List<DistrictOfficer> districtOfficersOf(District district) {
    return districtOfficers.stream().filter(officer -> officer.getDistrict() == district).toList();
  }
}
