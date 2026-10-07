package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests of the {@link Recipient} methods on citizens, volunteers, district officers and
 * organisations.
 */
class RecipientTest {

  @Test
  @DisplayName("A citizen with a device token is identified and reached as the dictionary says")
  void citizenWithToken() {
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0001", "token-1");

    assertThat(citizen.getRecipientId()).isEqualTo("CIT-KLT-0001");
    assertThat(citizen.getRecipientKind()).isEqualTo(RecipientKind.CITIZEN);
    assertThat(citizen.getContactNumber()).isEqualTo("0771234567");
    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.SINHALA);
    assertThat(citizen.getDeviceToken()).contains("token-1");
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(NotificationChannel.class)
  @DisplayName("A citizen with a device token can receive every channel")
  void citizenWithTokenReceivesEverything(NotificationChannel channel) {
    assertThat(SampleEntities.citizen("CIT-KLT-0001", "token-1").canReceive(channel)).isTrue();
  }

  @Test
  @DisplayName("A citizen without a device token has no token and receives SMS only")
  void citizenWithoutToken() {
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0002", null);

    assertThat(citizen.getDeviceToken()).isEmpty();
    assertThat(citizen.canReceive(NotificationChannel.SMS)).isTrue();
    assertThat(citizen.canReceive(NotificationChannel.PUSH)).isFalse();
    assertThat(citizen.canReceive(NotificationChannel.AUDIBLE_ALERT)).isFalse();
  }

  @Test
  @DisplayName("A community volunteer is a recipient of kind CITIZEN")
  void volunteerIsACitizenRecipient() {
    CommunityVolunteer volunteer = SampleEntities.volunteer("CIT-RAT-0001", null);

    assertThat(volunteer.getRecipientKind()).isEqualTo(RecipientKind.CITIZEN);
    assertThat(volunteer.getRecipientId()).isEqualTo("CIT-RAT-0001");
    assertThat(volunteer.getOrganization()).isEmpty();
    assertThat(volunteer.getGnDivision()).isEqualTo("GN-12");
    assertThat(volunteer.canReceive(NotificationChannel.PUSH)).isTrue();
  }

  @Test
  @DisplayName("A community volunteer keeps the organisation it belongs to")
  void volunteerKeepsItsOrganisation() {
    Organization group = SampleEntities.organization("ORG-0001");

    assertThat(SampleEntities.volunteer("CIT-RAT-0002", group).getOrganization())
        .containsSame(group);
  }

  @Test
  @DisplayName("A district officer is reached by SMS only and has no device token")
  void districtOfficer() {
    DistrictOfficer officer = SampleEntities.districtOfficer("DO-COL-01");

    assertThat(officer.getRecipientId()).isEqualTo("DO-COL-01");
    assertThat(officer.getRecipientKind()).isEqualTo(RecipientKind.DISTRICT_OFFICER);
    assertThat(officer.getContactNumber()).isEqualTo("0765551111");
    assertThat(officer.getPreferredLanguage()).isEqualTo(Language.ENGLISH);
    assertThat(officer.getDeviceToken()).isEmpty();
    assertThat(officer.canReceive(NotificationChannel.SMS)).isTrue();
    assertThat(officer.canReceive(NotificationChannel.PUSH)).isFalse();
    assertThat(officer.canReceive(NotificationChannel.AUDIBLE_ALERT)).isFalse();
  }

  @Test
  @DisplayName("An organisation is reached by SMS only and has no device token")
  void organization() {
    Organization organization = SampleEntities.organization("ORG-0001");

    assertThat(organization.getRecipientId()).isEqualTo("ORG-0001");
    assertThat(organization.getRecipientKind()).isEqualTo(RecipientKind.ORGANIZATION);
    assertThat(organization.getContactNumber()).isEqualTo("0112691095");
    assertThat(organization.getPreferredLanguage()).isEqualTo(Language.ENGLISH);
    assertThat(organization.getDeviceToken()).isEmpty();
    assertThat(organization.canReceive(NotificationChannel.SMS)).isTrue();
    assertThat(organization.canReceive(NotificationChannel.PUSH)).isFalse();
    assertThat(organization.canReceive(NotificationChannel.AUDIBLE_ALERT)).isFalse();
  }
}
