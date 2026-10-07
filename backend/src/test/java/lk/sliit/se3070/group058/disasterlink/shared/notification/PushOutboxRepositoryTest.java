package lk.sliit.se3070.group058.disasterlink.shared.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Tests of {@link PushOutboxRepository} and of the mapping of {@link PushOutbox}. */
@DataJpaTest
class PushOutboxRepositoryTest {

  private static final LocalDateTime T0 = LocalDateTime.of(2026, 5, 21, 8, 0);

  @Autowired private TestEntityManager entityManager;
  @Autowired private PushOutboxRepository outbox;

  private PushOutbox alert(String recipientId, LocalDateTime createdAt) {
    PushOutbox entry =
        PushOutbox.forWarningAlert(
            recipientId, "WRN-1", WarningLevel.LEVEL_2_AMBER, "text", createdAt);
    entityManager.persist(entry);
    return entry;
  }

  @Test
  @DisplayName("findUndelivered gives the recipient's undelivered entries, oldest first")
  void findUndelivered() {
    final PushOutbox late = alert("CIT-1", T0.plusMinutes(5));
    final PushOutbox early = alert("CIT-1", T0);
    PushOutbox delivered = alert("CIT-1", T0.plusMinutes(1));
    delivered.markDelivered(T0.plusMinutes(2));
    alert("CIT-2", T0);
    entityManager.flush();
    entityManager.clear();

    assertThat(outbox.findUndelivered("CIT-1"))
        .extracting(PushOutbox::getId)
        .containsExactly(early.getId(), late.getId());
    assertThat(outbox.findUndelivered("CIT-3")).isEmpty();
  }

  @Test
  @DisplayName("An entry keeps all its fields through the database, including the long text")
  void roundTrip() {
    PushOutbox entry = alert("CIT-1", T0);
    entityManager.flush();
    entityManager.clear();

    PushOutbox loaded = outbox.findById(entry.getId()).orElseThrow();

    assertThat(loaded).isEqualTo(entry);
    assertThat(loaded.getWarningLevel()).contains(WarningLevel.LEVEL_2_AMBER);
    assertThat(loaded.getText()).isEqualTo("text");
    assertThat(loaded.getCreatedAt()).isEqualTo(T0);
  }
}
