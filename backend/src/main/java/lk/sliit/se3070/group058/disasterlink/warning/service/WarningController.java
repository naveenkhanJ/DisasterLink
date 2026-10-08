package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardDetailsView;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.IssueOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningStatus;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The control of use case UC1, Issue Hazard Warning (sd Issue Hazard Warning, sd Modify Warning).
 * It finds the assessable hazards, shows a hazard with its active warning, applies hazard updates,
 * validates and previews warning details, issues a warning and has {@link WarningBroadcaster}
 * deliver it, and reports the dispatch status.
 *
 * <p>Other use cases call it too: {@code ReportVerificationController} reads {@link
 * #getActiveWarningForHazard} (FD-16), and the citizen app reads {@link #getActiveWarnings} through
 * {@code WarningApiController}. The text rules are delegated to {@link WarningTextValidator} (PD6).
 * {@link #issueWarning} does not validate again: the confirm handler re-calls {@link
 * #validateWarning} first (P62 FX-05). Every lazy read happens inside one of these transactions,
 * because open-session-in-view is off (FX-27).
 *
 * <p>Implements CH-19, CH-22 and CH-23.
 */
@Service
public class WarningController {

  /** District officers are notified only when a warning is escalated (SC1 step 11). */
  private static final int NO_DISTRICT_OFFICERS = 0;

  private static final Logger LOG = LoggerFactory.getLogger(WarningController.class);

  private final HazardRepository hazards;

  private final WarningRepository warnings;

  private final DMCOfficerRepository officers;

  private final CitizenRepository citizens;

  private final OrganizationRepository organizations;

  private final WarningBroadcaster broadcaster;

  private final WarningTextValidator textValidator;

  private final IdGenerator idGenerator;

  private final Clock clock;

  /**
   * Creates the control.
   *
   * @param hazards finds and stores hazards
   * @param warnings finds and stores warnings with their notifications
   * @param officers finds the issuing officer
   * @param citizens counts the citizens of the covered districts for the preview
   * @param organizations counts the organisations of the covered districts for the preview
   * @param broadcaster delivers an issued warning
   * @param textValidator checks the text rules of the details
   * @param idGenerator makes the warning identifier
   * @param clock supplies the current time
   * @throws NullPointerException if an argument is {@code null}
   */
  public WarningController(
      HazardRepository hazards,
      WarningRepository warnings,
      DMCOfficerRepository officers,
      CitizenRepository citizens,
      OrganizationRepository organizations,
      WarningBroadcaster broadcaster,
      WarningTextValidator textValidator,
      IdGenerator idGenerator,
      Clock clock) {
    this.hazards = Objects.requireNonNull(hazards, "hazards");
    this.warnings = Objects.requireNonNull(warnings, "warnings");
    this.officers = Objects.requireNonNull(officers, "officers");
    this.citizens = Objects.requireNonNull(citizens, "citizens");
    this.organizations = Objects.requireNonNull(organizations, "organizations");
    this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
    this.textValidator = Objects.requireNonNull(textValidator, "textValidator");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Finds the hazards a warning can be issued for (SC1 step 2; sd message 1.1): each recorded
   * active hazard that {@link Hazard#isAssessable} accepts.
   *
   * @return the assessable hazards, oldest start first
   */
  @Transactional(readOnly = true)
  public List<Hazard> getAssessableHazards() {
    return hazards.findAssessable().stream().filter(Hazard::isAssessable).toList();
  }

  /**
   * Shows a hazard with its active warning and what its record lacks (SC1 step 5, A1 5a, A5 5d; sd
   * message 2.1).
   *
   * @param hazardId the identifier of the hazard
   * @return the hazard, its active warning or {@code null}, and its missing information
   * @throws java.util.NoSuchElementException if no hazard has that identifier
   */
  @Transactional(readOnly = true)
  public HazardDetailsView getHazardDetails(String hazardId) {
    Hazard hazard = hazards.findById(hazardId).orElseThrow();
    List<String> missing = hazard.missingInformation();
    Warning activeWarning = getActiveWarningForHazard(hazardId).orElse(null);
    return new HazardDetailsView(hazard, activeWarning, missing);
  }

  /**
   * Finds the active warning of a hazard (sd message 2.1.2); also called by {@code
   * ReportVerificationController} (FD-16). The warning recorded for the hazard counts only while
   * its {@link Warning#statusAt} the current time is {@code ACTIVE}.
   *
   * @param hazardId the identifier of the hazard
   * @return the active warning, or empty if there is none
   */
  @Transactional(readOnly = true)
  public Optional<Warning> getActiveWarningForHazard(String hazardId) {
    LocalDateTime now = LocalDateTime.now(clock);
    return warnings
        .findActiveByHazard(hazardId, now)
        .filter(warning -> warning.statusAt(now) == WarningStatus.ACTIVE);
  }

  /**
   * Applies an officer's update to a hazard (SC1 A1 5b–5c, E4 12f; sd message 3.1). An update
   * without a description, a severity or at least one district is refused and changes nothing (E5).
   *
   * @param hazardId the identifier of the hazard
   * @param update the new description, severity and districts
   * @return the updated hazard
   * @throws InvalidHazardUpdateException if the update is invalid; it carries the errors
   * @throws java.util.NoSuchElementException if no hazard has that identifier
   */
  @Transactional
  public Hazard updateHazard(String hazardId, HazardUpdate update) {
    List<String> errors = new ArrayList<>();
    if (update.description() == null || update.description().isBlank()) {
      errors.add("A description is required");
    }
    if (update.severity() == null) {
      errors.add("A severity is required");
    }
    if (update.districts().isEmpty()) {
      errors.add("At least one district is required");
    }
    if (!errors.isEmpty()) {
      throw new InvalidHazardUpdateException(errors);
    }
    Hazard hazard = hazards.findById(hazardId).orElseThrow();
    hazard.updateHazard(update);
    LOG.info("Hazard {} updated", hazardId);
    return hazards.save(hazard);
  }

  /**
   * Validates the details of a warning (SC1 step 11, A2, E3; sd messages 5.1 and 6.1): the hazard
   * has no missing information, an affected area and a channel are chosen, and the text rules of
   * {@link WarningTextValidator} hold.
   *
   * @param hazardId the identifier of the hazard
   * @param details the warning details
   * @return the result; its errors are shown beside the form when it is not valid
   * @throws java.util.NoSuchElementException if no hazard has that identifier
   */
  @Transactional(readOnly = true)
  public ValidationResult validateWarning(String hazardId, WarningDetails details) {
    Hazard hazard = hazards.findById(hazardId).orElseThrow();
    List<String> errors = new ArrayList<>();
    List<String> missing = hazard.missingInformation();
    if (!missing.isEmpty()) {
      errors.add(
          "A warning cannot be issued until the missing information is provided: "
              + String.join(", ", missing));
    }
    if (details.targetDistricts().isEmpty() && details.targetRiverBasins().isEmpty()) {
      errors.add("An affected area is required: select at least one district or river basin");
    }
    if (details.channels().isEmpty()) {
      errors.add("At least one notification channel is required");
    }
    errors.addAll(
        textValidator.validate(
            details.message(), details.instructions(), details.channels(), details.expiryTime()));
    return new ValidationResult(errors.isEmpty(), errors);
  }

  /**
   * Previews a broadcast (SC1 step 11; sd message 5.2): the covered districts, with the river
   * basins expanded to their districts, and the citizens per district and organisations that will
   * be notified. Called after {@link #validateWarning} accepted the details.
   *
   * @param hazardId the identifier of the hazard
   * @param details valid warning details
   * @return the preview
   * @throws java.util.NoSuchElementException if no hazard has that identifier
   */
  @Transactional(readOnly = true)
  public WarningPreview previewWarning(String hazardId, WarningDetails details) {
    Hazard hazard = hazards.findById(hazardId).orElseThrow();
    Set<District> covered = coveredDistricts(details);
    return new WarningPreview(
        hazard.getType(),
        details.warningLevel(),
        covered,
        details.message(),
        details.instructions(),
        details.channels(),
        details.expiryTime(),
        citizensPerDistrict(covered, citizens.findByDistrictIn(covered)),
        organizations.findByDistrictIn(covered).size(),
        NO_DISTRICT_OFFICERS);
  }

  /**
   * Issues a warning and broadcasts it (SC1 steps 13–16; sd message 6.2). It does not validate the
   * details again: the confirm handler re-calls {@link #validateWarning} first (P62 FX-05).
   *
   * @param hazardId the identifier of the hazard
   * @param details the confirmed details, already validated
   * @param officerId the identifier of the officer who issues it
   * @return the recorded warning and the summary of its broadcast
   * @throws ActiveWarningExistsException if the hazard has an active warning by now (E6); nothing
   *     is recorded or broadcast
   * @throws IllegalStateException if the details lack what {@link #validateWarning} requires (a
   *     programming error)
   * @throws java.util.NoSuchElementException if the hazard or the officer does not exist
   */
  @Transactional
  public IssueOutcome issueWarning(String hazardId, WarningDetails details, String officerId) {
    requireValidated(details);
    Optional<Warning> activeWarning = getActiveWarningForHazard(hazardId);
    if (activeWarning.isPresent()) {
      throw new ActiveWarningExistsException(activeWarning.get());
    }
    Hazard hazard = hazards.findById(hazardId).orElseThrow();
    DMCOfficer officer = officers.findById(officerId).orElseThrow();
    Warning warning =
        new Warning(
            idGenerator.next(IdKind.WARNING, null),
            hazard,
            details,
            officer,
            LocalDateTime.now(clock));
    BroadcastSummary summary = broadcaster.broadcast(warning);
    Warning recorded = warnings.save(warning);
    String warningId = recorded.getWarningId();
    LOG.info("Warning {} issued for hazard {} by {}", warningId, hazardId, officerId);
    return new IssueOutcome(recorded, summary);
  }

  /**
   * Sends the warning again on one channel to the recipients whose latest notification of the
   * current round failed (SC1 E1 16d–16e; sd message 7.1).
   *
   * @param warningId the identifier of the warning
   * @param channel the channel the officer chose
   * @return the summary of the current round, with the unreachable recipients
   * @throws java.util.NoSuchElementException if no warning has that identifier
   */
  @Transactional
  public BroadcastSummary resendOnChannel(String warningId, NotificationChannel channel) {
    // The loaded warning is managed, so the resend notifications are stored with it at commit.
    Warning warning = warnings.findById(warningId).orElseThrow();
    return broadcaster.resend(warning, channel);
  }

  /**
   * Summarises the delivery of a warning's current round from its recorded notifications (SC1 steps
   * 18–19, A5 5d; sd message 2.4).
   *
   * @param warningId the identifier of the warning
   * @return the summary of the current round
   * @throws java.util.NoSuchElementException if no warning has that identifier
   */
  @Transactional(readOnly = true)
  public BroadcastSummary getBroadcastSummary(String warningId) {
    Warning warning = warnings.findById(warningId).orElseThrow();
    return WarningBroadcaster.summarise(warning.getNotifications(), List.of());
  }

  /**
   * Finds the warnings active now that cover a district; the citizen app shows them (standards
   * section 12.2).
   *
   * @param district the district
   * @return the warnings whose status now is {@code ACTIVE} and whose covered districts include
   *     {@code district}
   */
  @Transactional(readOnly = true)
  public List<Warning> getActiveWarnings(District district) {
    LocalDateTime now = LocalDateTime.now(clock);
    return warnings.findActiveByDistrict(district, now).stream()
        .filter(warning -> warning.statusAt(now) == WarningStatus.ACTIVE)
        .toList();
  }

  /**
   * Counts the citizens of each covered district for a preview, 0 for a district without any.
   * Package-private so that {@code EscalationController.previewEscalation} counts the same way (L03
   * Duplicate Code).
   *
   * @param covered the covered districts
   * @param citizens the citizens registered in them
   * @return the number of citizens per covered district
   */
  static Map<District, Integer> citizensPerDistrict(Set<District> covered, List<Citizen> citizens) {
    Map<District, Integer> counts = new EnumMap<>(District.class);
    for (District district : covered) {
      counts.put(district, 0);
    }
    for (Citizen citizen : citizens) {
      counts.merge(citizen.getDistrict(), 1, Integer::sum);
    }
    return counts;
  }

  /** The districts the details cover, as {@code Warning.coveredDistricts()} will compute them. */
  private static Set<District> coveredDistricts(WarningDetails details) {
    Set<District> covered = EnumSet.noneOf(District.class);
    covered.addAll(details.targetDistricts());
    for (RiverBasin basin : details.targetRiverBasins()) {
      covered.addAll(basin.districts());
    }
    return covered;
  }

  private static void requireValidated(WarningDetails details) {
    if (details.message() == null
        || details.instructions() == null
        || details.expiryTime() == null
        || details.channels().isEmpty()
        || (details.targetDistricts().isEmpty() && details.targetRiverBasins().isEmpty())) {
      throw new IllegalStateException("issueWarning was called with details not validated");
    }
  }
}
