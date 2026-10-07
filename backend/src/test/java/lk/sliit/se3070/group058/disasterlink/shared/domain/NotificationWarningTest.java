package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests of the back-reference from {@link Notification} to its {@link Warning}. */
@DataJpaTest
class NotificationWarningTest {

  @Autowired private TestEntityManager entityManager;

  private Warning warning() {
    Hazard hazard = SampleEntities.hazard("HZ-1");
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    entityManager.persist(hazard);
    entityManager.persist(officer);
    return SampleEntities.warning(
        "WRN-1",
        hazard,
        officer,
        List.of(District.COLOMBO),
        List.of(),
        SampleEntities.NOW.plusDays(1));
  }

  private static Notification notification() {
    return new Notification(SampleEntities.organization("ORG-1"), NotificationChannel.SMS, "t", 1);
  }

  @Test
  @DisplayName("A new notification has no warning until it is attached")
  void noWarningBeforeAttach() {
    assertThat(notification().getWarning()).isEmpty();
  }

  @Test
  @DisplayName("attachTo sets the warning that getWarning returns")
  void attachToSetsTheWarning() {
    Notification notification = notification();
    Warning warning = warning();

    notification.attachTo(warning);

    assertThat(notification.getWarning()).containsSame(warning);
  }

  @Test
  @DisplayName("attachTo rejects a null warning")
  void attachToRejectsNull() {
    Notification notification = notification();

    assertThatNullPointerException().isThrownBy(() -> notification.attachTo(null));
  }

  @Test
  @DisplayName("A reloaded notification knows its warning, and warning_id is written once")
  @SuppressWarnings("unchecked")
  void reloadedNotificationKnowsItsWarning() {
    Warning warning = warning();
    Notification notification = notification();
    // Warning.addNotification is Lakmal's placeholder, so the fixture does what it will do:
    // add to the list and call attachTo.
    ((List<Notification>) ReflectionTestUtils.getField(warning, "notifications")).add(notification);
    notification.attachTo(warning);
    entityManager.persistAndFlush(warning);
    entityManager.clear();

    Notification loaded = entityManager.find(Notification.class, notification.getNotificationId());

    assertThat(loaded.getWarning()).isPresent();
    assertThat(loaded.getWarning().orElseThrow().getWarningId()).isEqualTo("WRN-1");
    Object stored =
        entityManager
            .getEntityManager()
            .createNativeQuery("select warning_id from notification where notification_id = :id")
            .setParameter("id", notification.getNotificationId())
            .getSingleResult();
    assertThat(stored).isEqualTo("WRN-1");
  }
}
