package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditAction;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationOptions;
import lk.sliit.se3070.group058.disasterlink.shared.domain.IssueOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningStatus;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The control of Escalate Warning (sd Escalate Warning, messages 3–10): it offers the levels above
 * an active warning's current level, validates and previews the escalation details, and escalates
 * the warning — new level, message, instructions and expiry time — records the audit record
 * WARNING_ESCALATED and has {@link WarningBroadcaster} re-broadcast it, district officers included,
 * as a new round.
 *
 * <p>The text rules are delegated to {@link WarningTextValidator} (PD6), as {@code
 * WarningController.validateWarning} does. {@link #escalateWarning} does not validate again: the
 * confirm handler re-calls {@link #validateEscalation} first (P62 FX-05). Split from {@code
 * WarningController} for its own reason to change (SRP; round-2 MD N18).
 *
 * <p>Implements CH-06 (R-06; P60 ED1, N5, N6, N24; PR1, PR7).
 */
@Service
public class EscalationController {

  private static final Logger LOG = LoggerFactory.getLogger(EscalationController.class);

  /** The audit detail joins the old and the new level (PR7): "LEVEL_2_AMBER→LEVEL_3_RED". */
  private static final String LEVEL_CHANGE_ARROW = "→";

  private final WarningRepository warnings;

  private final DMCOfficerRepository officers;

  private final CitizenRepository citizens;

  private final OrganizationRepository organizations;

  private final DistrictOfficerRepository districtOfficers;

  private final AuditRecordRepository auditRecords;

  private final WarningBroadcaster broadcaster;

  private final WarningTextValidator textValidator;

  private final Clock clock;

  /**
   * Creates the control.
   *
   * @param warnings finds and stores warnings with their notifications
   * @param officers finds the escalating officer (a duty officer is a DMC officer)
   * @param citizens counts the citizens of the covered districts for the preview
   * @param organizations counts the organisations of the covered districts for the preview
   * @param districtOfficers counts the district officers of the covered districts for the preview
   * @param auditRecords stores the WARNING_ESCALATED record
   * @param broadcaster re-broadcasts the escalated warning
   * @param textValidator checks the text rules of the details
   * @param clock supplies the current time
   * @throws NullPointerException if an argument is {@code null}
   */
  public EscalationController(
      WarningRepository warnings,
      DMCOfficerRepository officers,
      CitizenRepository citizens,
      OrganizationRepository organizations,
      DistrictOfficerRepository districtOfficers,
      AuditRecordRepository auditRecords,
      WarningBroadcaster broadcaster,
      WarningTextValidator textValidator,
      Clock clock) {
    this.warnings = Objects.requireNonNull(warnings, "warnings");
    this.officers = Objects.requireNonNull(officers, "officers");
    this.citizens = Objects.requireNonNull(citizens, "citizens");
    this.organizations = Objects.requireNonNull(organizations, "organizations");
    this.districtOfficers = Objects.requireNonNull(districtOfficers, "districtOfficers");
    this.auditRecords = Objects.requireNonNull(auditRecords, "auditRecords");
    this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
    this.textValidator = Objects.requireNonNull(textValidator, "textValidator");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Offers the levels above an active warning's current level (Escalate step 4, A3; sd message
   * 3.1). A warning at Level 3 – Red gets no level (A3).
   *
   * @param warningId the identifier of the warning
   * @return the warning and the levels above its current one, possibly none
   * @throws WarningNotEscalatableException if the warning has expired (E2 4c–4d)
   * @throws java.util.NoSuchElementException if no warning has that identifier
   */
  @Transactional(readOnly = true)
  public EscalationOptions getEscalationOptions(String warningId) {
    Warning warning = warnings.findById(warningId).orElseThrow();
    if (warning.statusAt(LocalDateTime.now(clock)) != WarningStatus.ACTIVE) {
      throw new WarningNotEscalatableException(warning);
    }
    List<WarningLevel> allowed =
        Arrays.stream(WarningLevel.values())
            .filter(level -> isAbove(level, warning.getWarningLevel()))
            .toList();
    return new EscalationOptions(warning, allowed);
  }

  /**
   * Validates the escalation details (Escalate step 8, E3; sd messages 7.1 and 10.1): the new level
   * above the warning's current level, and the text rules of {@link WarningTextValidator} on the
   * warning's channels.
   *
   * @param warningId the identifier of the warning
   * @param details the new level, message, instructions and expiry time
   * @return the result; its errors are shown beside the form when it is not valid
   * @throws java.util.NoSuchElementException if no warning has that identifier
   */
  @Transactional(readOnly = true)
  public ValidationResult validateEscalation(String warningId, EscalationDetails details) {
    Warning warning = warnings.findById(warningId).orElseThrow();
    List<String> errors = new ArrayList<>();
    if (details.newLevel() == null || !isAbove(details.newLevel(), warning.getWarningLevel())) {
      errors.add("The new level must be above the warning's current level");
    }
    errors.addAll(
        textValidator.validate(
            details.message(),
            details.instructions(),
            warning.getChannels(),
            details.expiryTime()));
    return new ValidationResult(errors.isEmpty(), errors);
  }

  /**
   * Previews the re-broadcast (Escalate step 8; sd message 7.2): the warning's covered districts
   * and channels with the new level and texts, and the citizens per district, organisations and
   * district officers who will be notified (PR10). Called after {@link #validateEscalation}
   * accepted the details.
   *
   * @param warningId the identifier of the warning
   * @param details valid escalation details
   * @return the preview
   * @throws java.util.NoSuchElementException if no warning has that identifier
   */
  @Transactional(readOnly = true)
  public WarningPreview previewEscalation(String warningId, EscalationDetails details) {
    Warning warning = warnings.findById(warningId).orElseThrow();
    Set<District> covered = warning.coveredDistricts();
    return new WarningPreview(
        warning.getHazard().getType(),
        details.newLevel(),
        covered,
        details.message(),
        details.instructions(),
        warning.getChannels(),
        details.expiryTime(),
        WarningController.citizensPerDistrict(covered, citizens.findByDistrictIn(covered)),
        organizations.findByDistrictIn(covered).size(),
        districtOfficers.findByDistrictIn(covered).size());
  }

  /**
   * Escalates the warning and re-broadcasts it (Escalate steps 10–12; sd message 10.2): the warning
   * takes the new level and texts, the audit record WARNING_ESCALATED records the officer, the time
   * and the level change, and the broadcast is a new round. It does not validate the details again:
   * the confirm handler re-calls {@link #validateEscalation} first (P62 FX-05).
   *
   * @param warningId the identifier of the warning
   * @param details the confirmed details, already validated
   * @param officerId the identifier of the escalating officer
   * @return the escalated warning and the summary of the new round
   * @throws WarningNotEscalatableException if the warning has expired or is no longer below the
   *     chosen level (E2 10a); nothing is changed
   * @throws IllegalStateException if the details lack what {@link #validateEscalation} requires (a
   *     programming error)
   * @throws java.util.NoSuchElementException if the warning or the officer does not exist
   */
  @Transactional
  public IssueOutcome escalateWarning(
      String warningId, EscalationDetails details, String officerId) {
    requireValidated(details);
    Warning warning = warnings.findById(warningId).orElseThrow();
    LocalDateTime now = LocalDateTime.now(clock);
    WarningLevel oldLevel = warning.getWarningLevel();
    if (warning.statusAt(now) != WarningStatus.ACTIVE || !isAbove(details.newLevel(), oldLevel)) {
      throw new WarningNotEscalatableException(warning);
    }
    DMCOfficer officer = officers.findById(officerId).orElseThrow();
    warning.escalate(details);
    auditRecords.save(
        new AuditRecord(
            AuditAction.WARNING_ESCALATED,
            officer,
            warningId,
            oldLevel.name() + LEVEL_CHANGE_ARROW + details.newLevel().name(),
            now));
    BroadcastSummary summary = broadcaster.broadcastEscalation(warning);
    WarningLevel newLevel = details.newLevel();
    LOG.info("Warning {} escalated from {} to {} by {}", warningId, oldLevel, newLevel, officerId);
    return new IssueOutcome(warning, summary);
  }

  private static boolean isAbove(WarningLevel level, WarningLevel current) {
    return level.compareTo(current) > 0;
  }

  private static void requireValidated(EscalationDetails details) {
    if (details.newLevel() == null
        || details.message() == null
        || details.instructions() == null
        || details.expiryTime() == null) {
      throw new IllegalStateException("escalateWarning was called with details not validated");
    }
  }
}
