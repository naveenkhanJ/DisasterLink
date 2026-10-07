package lk.sliit.se3070.group058.disasterlink.shared.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ProcessedSubmissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Tests of {@link SubmissionRecorder} against H2. */
@DataJpaTest
@Import(SubmissionRecorder.class)
class SubmissionRecorderTest {

  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 7, 0);

  /** A clock fixed at 09:00 in Colombo. */
  @TestConfiguration
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-05-21T03:30:00Z"), ZoneId.of("Asia/Colombo"));
    }
  }

  @Autowired private SubmissionRecorder recorder;
  @Autowired private ProcessedSubmissionRepository processedSubmissions;
  @Autowired private ClientSyncStatusRepository clientStatuses;
  @Autowired private TestEntityManager entityManager;

  private static ClientMeta meta(String userId, String submissionId, int pendingCount) {
    return new ClientMeta("client-1", userId, submissionId, CAPTURED, pendingCount);
  }

  @Test
  @DisplayName("check finds nothing for a submission that was not recorded")
  void checkNone() {
    assertThat(recorder.check(meta("CIT-1", "sub-1", 0))).isEmpty();
  }

  @Test
  @DisplayName("record stores the metadata and the result; check then finds it")
  void recordThenCheck() {
    recorder.record(meta("CIT-1", "sub-1", 0), "RPT-2026-KLT-1000");
    entityManager.flush();
    entityManager.clear();

    ProcessedSubmission found = recorder.check(meta("CIT-1", "sub-1", 0)).orElseThrow();

    assertThat(found.getClientId()).isEqualTo("client-1");
    assertThat(found.getCapturedAt()).isEqualTo(CAPTURED);
    assertThat(found.getReceivedAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 9, 0));
    assertThat(found.getResultId()).contains("RPT-2026-KLT-1000");
    assertThat(processedSubmissions.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("record accepts a submission that created nothing")
  void recordWithoutResult() {
    recorder.record(meta("CIT-1", "sub-2", 0), null);

    assertThat(recorder.check(meta("CIT-1", "sub-2", 0)).orElseThrow().getResultId()).isEmpty();
  }

  @Test
  @DisplayName("touchSync creates the status with the district of a citizen")
  void touchSyncForACitizen() {
    entityManager.persist(SampleEntities.citizen("CIT-1", null)); // Kalutara

    recorder.touchSync(meta("CIT-1", "sub-1", 3));
    entityManager.flush();
    entityManager.clear();

    ClientSyncStatus status = clientStatuses.findById("client-1").orElseThrow();
    assertThat(status.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(status.getPendingCount()).isEqualTo(3);
    assertThat(status.getLastSyncAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 9, 0));
  }

  @Test
  @DisplayName("touchSync creates the status with the district of a district officer")
  void touchSyncForADistrictOfficer() {
    entityManager.persist(SampleEntities.districtOfficer("DO-COL-01")); // Colombo

    recorder.touchSync(meta("DO-COL-01", "sub-1", 0));
    entityManager.flush();
    entityManager.clear();

    assertThat(clientStatuses.findById("client-1").orElseThrow().getDistrict())
        .isEqualTo(District.COLOMBO);
  }

  @Test
  @DisplayName("touchSync updates an existing status and keeps its district")
  void touchSyncUpdatesAnExistingStatus() {
    entityManager.persist(
        new ClientSyncStatus("client-1", District.GALLE, CAPTURED.minusDays(1), 5));
    entityManager.flush();
    entityManager.clear();

    recorder.touchSync(meta("nobody-known", "sub-1", 1));
    entityManager.flush();
    entityManager.clear();

    ClientSyncStatus status = clientStatuses.findById("client-1").orElseThrow();
    assertThat(status.getDistrict()).isEqualTo(District.GALLE);
    assertThat(status.getPendingCount()).isEqualTo(1);
    assertThat(status.getLastSyncAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 9, 0));
  }

  @Test
  @DisplayName("touchSync for an unknown user and no status is an error")
  void touchSyncForAnUnknownUser() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> recorder.touchSync(meta("nobody", "sub-1", 0)))
        .withMessageContaining("nobody");
  }
}
