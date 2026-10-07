package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the operations the skeleton writes on {@link Warning}. */
class WarningTest {

  private static final Hazard HAZARD = SampleEntities.hazard("HZ-2026-0201");
  private static final DMCOfficer OFFICER = SampleEntities.officer("DMC-1", Set.of());

  private static Warning warning(List<District> districts, List<RiverBasin> basins) {
    return SampleEntities.warning(
        "WRN-2026-0101", HAZARD, OFFICER, districts, basins, NOW.plusHours(6));
  }

  @Test
  @DisplayName("coveredDistricts gives the target districts when there is no basin")
  void coveredDistrictsOfDistrictsOnly() {
    Warning warning = warning(List.of(District.COLOMBO, District.GALLE), List.of());

    assertThat(warning.coveredDistricts())
        .containsExactlyInAnyOrder(District.COLOMBO, District.GALLE);
  }

  @Test
  @DisplayName(
      "coveredDistricts gives the districts of the target basins when there is no district")
  void coveredDistrictsOfBasinsOnly() {
    Warning warning = warning(List.of(), List.of(RiverBasin.KALU));

    assertThat(warning.coveredDistricts())
        .containsExactlyInAnyOrder(District.RATNAPURA, District.KALUTARA);
  }

  @Test
  @DisplayName("coveredDistricts joins overlapping districts and basins without repeats")
  void coveredDistrictsOfBoth() {
    Warning warning =
        warning(List.of(District.RATNAPURA, District.GALLE), List.of(RiverBasin.KALU));

    assertThat(warning.coveredDistricts())
        .containsExactlyInAnyOrder(District.RATNAPURA, District.KALUTARA, District.GALLE);
  }

  @Test
  @DisplayName("coveredDistricts cannot be changed")
  void coveredDistrictsIsUnmodifiable() {
    Set<District> covered = warning(List.of(District.GALLE), List.of()).coveredDistricts();

    assertThatThrownBy(() -> covered.add(District.JAFFNA))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("statusAt is ACTIVE before the expiry time")
  void statusBeforeExpiry() {
    Warning warning = warning(List.of(District.GALLE), List.of());

    assertThat(warning.statusAt(NOW.plusHours(6).minusSeconds(1))).isEqualTo(WarningStatus.ACTIVE);
  }

  @Test
  @DisplayName("statusAt is EXPIRED at the expiry time")
  void statusAtExpiry() {
    Warning warning = warning(List.of(District.GALLE), List.of());

    assertThat(warning.statusAt(NOW.plusHours(6))).isEqualTo(WarningStatus.EXPIRED);
  }

  @Test
  @DisplayName("statusAt is EXPIRED after the expiry time")
  void statusAfterExpiry() {
    Warning warning = warning(List.of(District.GALLE), List.of());

    assertThat(warning.statusAt(NOW.plusDays(1))).isEqualTo(WarningStatus.EXPIRED);
  }

  @Test
  @DisplayName("statusAt rejects a null time")
  void statusRejectsNull() {
    Warning warning = warning(List.of(District.GALLE), List.of());

    assertThatNullPointerException().isThrownBy(() -> warning.statusAt(null));
  }

  @Test
  @DisplayName("getNotifications starts empty and cannot be changed")
  void notificationsAreUnmodifiable() {
    Warning warning = warning(List.of(District.GALLE), List.of());
    List<Notification> notifications = warning.getNotifications();
    Notification notification =
        new Notification(
            SampleEntities.citizen("CIT-KLT-0001", null), NotificationChannel.SMS, "t", 1);

    assertThat(notifications).isEmpty();
    assertThatThrownBy(() -> notifications.add(notification))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("The constructor takes level, texts, targets, channels and expiry from the details")
  void constructorReadsTheDetails() {
    Warning warning = warning(List.of(District.GALLE), List.of(RiverBasin.GIN));

    assertThat(warning.getWarningLevel()).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    assertThat(warning.getMessage()).isEqualTo(SampleEntities.TEXT);
    assertThat(warning.getTargetDistricts()).containsExactly(District.GALLE);
    assertThat(warning.getTargetRiverBasins()).containsExactly(RiverBasin.GIN);
    assertThat(warning.getChannels())
        .containsExactlyInAnyOrder(NotificationChannel.PUSH, NotificationChannel.SMS);
    assertThat(warning.getExpiryTime()).isEqualTo(NOW.plusHours(6));
    assertThat(warning.getIssuedTime()).isEqualTo(NOW.plusMinutes(5));
    assertThat(warning.getHazard()).isSameAs(HAZARD);
    assertThat(warning.getOfficer()).isSameAs(OFFICER);
  }

  @Test
  @DisplayName("The constructor rejects a null argument")
  void constructorRejectsNull() {
    WarningDetails details = SampleEntities.details(List.of(), List.of(), NOW);

    assertThatNullPointerException()
        .isThrownBy(() -> new Warning(null, HAZARD, details, OFFICER, NOW));
    assertThatNullPointerException()
        .isThrownBy(() -> new Warning("W", null, details, OFFICER, NOW));
    assertThatNullPointerException().isThrownBy(() -> new Warning("W", HAZARD, null, OFFICER, NOW));
    assertThatNullPointerException().isThrownBy(() -> new Warning("W", HAZARD, details, null, NOW));
    assertThatNullPointerException()
        .isThrownBy(() -> new Warning("W", HAZARD, details, OFFICER, null));
  }

  @Test
  @DisplayName(
      "The seed constructor stores the given notifications and attaches them to the warning")
  void seedConstructorAttachesNotifications() {
    WarningDetails details = SampleEntities.details(List.of(District.GALLE), List.of(), NOW);
    Notification first =
        new Notification(SampleEntities.organization("ORG-1"), NotificationChannel.SMS, "t", 1);
    Notification second =
        new Notification(SampleEntities.organization("ORG-2"), NotificationChannel.SMS, "t", 2);

    Warning warning =
        new Warning("WRN-2026-0101", HAZARD, details, OFFICER, NOW, List.of(first, second));

    assertThat(warning.getNotifications()).containsExactly(first, second);
    assertThat(first.getWarning()).containsSame(warning);
    assertThat(second.getWarning()).containsSame(warning);
    assertThat(warning.getWarningId()).isEqualTo("WRN-2026-0101");
  }

  @Test
  @DisplayName("A warning made without notifications has none")
  void plainConstructorHasNoNotifications() {
    assertThat(warning(List.of(District.GALLE), List.of()).getNotifications()).isEmpty();
  }

  @Test
  @DisplayName("The constructor needs the components validateWarning checks (P155 X1)")
  void constructorRequiresValidatedComponents() {
    LocalisedText text = SampleEntities.TEXT;
    Set<NotificationChannel> channels = Set.of(NotificationChannel.SMS);

    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Warning(
                    "W",
                    HAZARD,
                    new WarningDetails(null, text, text, List.of(), List.of(), channels, NOW),
                    OFFICER,
                    NOW));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Warning(
                    "W",
                    HAZARD,
                    new WarningDetails(
                        WarningLevel.LEVEL_2_AMBER,
                        null,
                        text,
                        List.of(),
                        List.of(),
                        channels,
                        NOW),
                    OFFICER,
                    NOW));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Warning(
                    "W",
                    HAZARD,
                    new WarningDetails(
                        WarningLevel.LEVEL_2_AMBER,
                        text,
                        null,
                        List.of(),
                        List.of(),
                        channels,
                        NOW),
                    OFFICER,
                    NOW));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Warning(
                    "W",
                    HAZARD,
                    new WarningDetails(
                        WarningLevel.LEVEL_2_AMBER,
                        text,
                        text,
                        List.of(),
                        List.of(),
                        channels,
                        null),
                    OFFICER,
                    NOW));
  }
}
