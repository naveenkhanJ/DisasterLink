package lk.sliit.se3070.group058.disasterlink.shared.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ProcessedSubmissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place that checks whether a client submission was already accepted ({@code
 * ProcessedSubmission}) and that updates the client's sync status ({@code ClientSyncStatus}). It is
 * called by {@code ReportSubmissionController} (UC2) and {@code ShelterController} (UC3), so
 * neither repeats the logic. It decides nothing else: when to record, and what to do with a
 * duplicate, stay with the calling control.
 *
 * <p>Its methods are transactional and join the caller's transaction.
 *
 * <p>Implementation-only helper of the model dictionary section 6 (P57, V2C N-07; R3C-07; P62
 * FX-11, R4C-06; FC-03).
 */
@Component
public class SubmissionRecorder {

  private final ProcessedSubmissionRepository processedSubmissions;

  private final ClientSyncStatusRepository clientStatuses;

  private final CitizenRepository citizens;

  private final DistrictOfficerRepository districtOfficers;

  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param processedSubmissions the record of accepted submissions
   * @param clientStatuses the sync status of the clients
   * @param citizens finds the district of a citizen user
   * @param districtOfficers finds the district of a district-officer user
   * @param clock supplies the time of a record or a contact
   */
  public SubmissionRecorder(
      ProcessedSubmissionRepository processedSubmissions,
      ClientSyncStatusRepository clientStatuses,
      CitizenRepository citizens,
      DistrictOfficerRepository districtOfficers,
      Clock clock) {
    this.processedSubmissions =
        Objects.requireNonNull(processedSubmissions, "processedSubmissions");
    this.clientStatuses = Objects.requireNonNull(clientStatuses, "clientStatuses");
    this.citizens = Objects.requireNonNull(citizens, "citizens");
    this.districtOfficers = Objects.requireNonNull(districtOfficers, "districtOfficers");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Looks up the record of a submission that was accepted before.
   *
   * @param meta the client metadata of the submission
   * @return the record, or empty if the submission has not been accepted
   */
  @Transactional
  public Optional<ProcessedSubmission> check(ClientMeta meta) {
    return processedSubmissions.findBySubmissionId(meta.submissionId());
  }

  /**
   * Records an accepted submission. The caller decides when: only accepted submissions are
   * recorded, so a corrected resubmission of a rejected draft is processed again.
   *
   * @param meta the client metadata of the submission
   * @param resultId the identifier of what the submission created, or {@code null} if nothing
   */
  @Transactional
  public void record(ClientMeta meta, String resultId) {
    processedSubmissions.save(
        new ProcessedSubmission(
            meta.submissionId(),
            meta.clientId(),
            meta.capturedAt(),
            LocalDateTime.now(clock),
            resultId));
  }

  /**
   * Updates the sync status of the client that sent a submission, creating it at the client's first
   * contact with the district of the sending user.
   *
   * @param meta the client metadata of the submission
   * @throws IllegalArgumentException if the status does not exist yet and {@code meta.userId()} is
   *     neither a citizen nor a district officer
   */
  @Transactional
  public void touchSync(ClientMeta meta) {
    LocalDateTime now = LocalDateTime.now(clock);
    ClientSyncStatus status =
        clientStatuses
            .findById(meta.clientId())
            .orElseGet(
                () ->
                    new ClientSyncStatus(
                        meta.clientId(), districtOf(meta.userId()), now, meta.pendingCount()));
    status.recordSync(now, meta.pendingCount());
    clientStatuses.save(status);
  }

  private District districtOf(String userId) {
    return citizens
        .findById(userId)
        .map(Citizen::getDistrict)
        .or(() -> districtOfficers.findById(userId).map(DistrictOfficer::getDistrict))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "user " + userId + " is neither a citizen nor a district officer"));
  }
}
