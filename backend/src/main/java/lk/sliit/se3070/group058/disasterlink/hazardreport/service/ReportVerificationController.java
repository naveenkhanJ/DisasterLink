package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.NearbyReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.VerificationOutcome;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoDistance;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NewHazardDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Recipient;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.NotificationGateway;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCDutyOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The officer's half of use case UC2, Submit and Verify Hazard Report: the pending queue, the
 * details and nearby reports of one report, and the three decisions a duty officer can take on it:
 * verify it against an existing hazard, verify it as a new hazard, or reject it with a reason.
 *
 * <p>Implements CH-19, CH-25, CH-26 and CH-27, and CH-06 for the {@link VerificationOutcome}. The
 * operations follow sequence diagram SD2 (messages 10 to 14.4), {@code sd Reject Report}, {@code sd
 * Record Rejection Reason} and {@code sd Send Decision Notice}, and scenario SC2 (steps 21 to 30,
 * A4 to A7, A9, E5, E7, E9, E10).
 *
 * <p><b>Every decision has the same shape</b> (standards section 4, "Concurrent decisions on one
 * report"):
 *
 * <ol>
 *   <li>In one transaction the report is loaded and {@code isPending()} is checked <em>first</em>;
 *       a report that is no longer pending is refused with {@link ReportAlreadyDecidedException}
 *       before anything else is looked at. Then the decision's own checks and writes run, and the
 *       report is saved. While the session is still open the notice is prepared: the reporter, a
 *       lazy association, is unproxied, because the notice is sent after the commit.
 *   <li>Two duty officers can both pass {@code isPending()}; the second commit then fails on the
 *       report's {@code @Version} with {@link ObjectOptimisticLockingFailureException}. That is
 *       caught <em>outside</em> the transaction (a failed transaction is never continued), the
 *       report is re-read in a read-only transaction and {@link ReportAlreadyDecidedException} is
 *       thrown; no notice is sent.
 *   <li>Only after the commit is the citizen told (SC2 step 27, "once the decision is recorded"):
 *       by push when the citizen has a device token, by SMS when there is none or the push failed
 *       (A9). If neither could be sent the decision stands and a warning is logged; SC2 has no flow
 *       for it, which is the Group_058 reading.
 * </ol>
 *
 * <p>A decision on a report that is not pending is {@link ReportAlreadyDecidedException} for all
 * three decisions, checked before the reason of a rejection or the details of a new hazard.
 *
 * <p>{@code WarningController.getActiveWarningForHazard} is UC1's (merged into {@code develop} in
 * {@code c8cdf16}); a failure there makes {@code verifyReport} and {@code verifyAsNewHazard} fail
 * and roll back (SC2 E5: nothing changed); {@code rejectReport} does not call it.
 *
 * <p>The reads return entities whose associations are lazy (reporter, reviewer, hazard, a hazard's
 * districts); the caller maps them inside its own transaction. The one exception is the active
 * warning of a {@link VerificationOutcome}: it is returned after the commit, so its target
 * districts, which the escalation prompt shows, are initialised inside the decision's transaction
 * (UC2-4).
 */
@Service
public class ReportVerificationController {

  private static final Logger LOG = LoggerFactory.getLogger(ReportVerificationController.class);

  /** The radius of "nearby", in kilometres: a Group_058 assumption (SC2 step 24). */
  static final double NEARBY_RADIUS_KM = 5.0;

  /** The age of "nearby" reports, in hours: a Group_058 assumption (SC2 step 24). */
  static final long NEARBY_WINDOW_HOURS = 24;

  /** The error for a new hazard without a severity (SC2 E9). */
  static final String SEVERITY_REQUIRED = "Severity is required";

  /** The error for a new hazard without districts (SC2 E9). */
  static final String DISTRICTS_REQUIRED = "Districts are required";

  /** The error for a new hazard without a place name (SC2 E9). */
  static final String PLACE_NAME_REQUIRED = "Place name is required";

  private final HazardReportRepository hazardReportRepository;

  private final HazardRepository hazardRepository;

  private final DMCDutyOfficerRepository dutyOfficerRepository;

  private final WarningController warningController;

  private final IdGenerator idGenerator;

  private final NotificationGateway pushGateway;

  private final NotificationGateway smsGateway;

  private final TransactionTemplate transaction;

  private final TransactionTemplate readOnlyTransaction;

  private final Clock clock;

  /**
   * Creates the control. The push and the SMS gateway are picked from the given gateways by what
   * they {@link NotificationGateway#supports support} (Strategy, FD-03): no {@code instanceof} and
   * no lookup by class.
   *
   * @param hazardReportRepository stores and finds the reports
   * @param hazardRepository stores and finds the hazards
   * @param dutyOfficerRepository finds the deciding officer
   * @param warningController UC1's control, asked for the active warning of a hazard
   * @param idGenerator makes the identifier of a new hazard
   * @param gateways every notification gateway; one must support PUSH and one SMS
   * @param transactionManager runs the decisions and the reads of the decision paths
   * @param clock supplies the time of a decision and the start of the nearby window
   * @throws IllegalStateException if no gateway supports PUSH or none supports SMS
   */
  public ReportVerificationController(
      HazardReportRepository hazardReportRepository,
      HazardRepository hazardRepository,
      DMCDutyOfficerRepository dutyOfficerRepository,
      WarningController warningController,
      IdGenerator idGenerator,
      List<NotificationGateway> gateways,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.hazardReportRepository =
        Objects.requireNonNull(hazardReportRepository, "hazardReportRepository");
    this.hazardRepository = Objects.requireNonNull(hazardRepository, "hazardRepository");
    this.dutyOfficerRepository =
        Objects.requireNonNull(dutyOfficerRepository, "dutyOfficerRepository");
    this.warningController = Objects.requireNonNull(warningController, "warningController");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    this.pushGateway = gatewayFor(gateways, NotificationChannel.PUSH);
    this.smsGateway = gatewayFor(gateways, NotificationChannel.SMS);
    this.transaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction.setReadOnly(true);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  private static NotificationGateway gatewayFor(
      List<NotificationGateway> gateways, NotificationChannel channel) {
    return gateways.stream()
        .filter(gateway -> gateway.supports(channel))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("no notification gateway supports " + channel));
  }

  /**
   * Lists the reports that wait for a decision, oldest received first (SC2 step 22).
   *
   * @return the pending reports
   */
  @Transactional(readOnly = true)
  public List<HazardReport> getPendingReports() {
    return hazardReportRepository.findPending();
  }

  /**
   * Returns one report for the console's details screen (SC2 step 23).
   *
   * @param reportId the identifier of the report
   * @return the report
   * @throws NoSuchElementException if there is no such report (answered as a 404)
   */
  @Transactional(readOnly = true)
  public HazardReport getReportDetails(String reportId) {
    return findReport(reportId);
  }

  /**
   * Finds the other pending or verified reports of the same hazard type within 5 km of the selected
   * report and captured in the last 24 hours, nearest first, each with its distance from the
   * selected report (SC2 step 24; FX-02, R4G-35).
   *
   * <p>The repository's time filter is on the capture time ({@code capturedAt}, its signature),
   * while SC2 step 24 says "received in the last 24 hours". A report captured earlier but received
   * late can therefore be left out: a known difference between the design text and the finder.
   *
   * @param reportId the identifier of the selected report
   * @return the nearby reports, without the selected one, nearest first
   * @throws NoSuchElementException if there is no such report (answered as a 404)
   */
  @Transactional(readOnly = true)
  public List<NearbyReport> findNearbyReports(String reportId) {
    HazardReport selected = findReport(reportId);
    LocalDateTime since = LocalDateTime.now(clock).minusHours(NEARBY_WINDOW_HOURS);
    return hazardReportRepository
        .findNearby(selected.getLocation(), NEARBY_RADIUS_KM, since)
        .stream()
        .filter(other -> isComparable(selected, other))
        .map(
            other ->
                new NearbyReport(
                    other, GeoDistance.haversineKm(selected.getLocation(), other.getLocation())))
        .sorted(Comparator.comparingDouble(NearbyReport::distanceKm))
        .toList();
  }

  private static boolean isComparable(HazardReport selected, HazardReport other) {
    return !other.getReportId().equals(selected.getReportId())
        && other.getHazardType() == selected.getHazardType()
        && (other.getStatus() == ReportStatus.PENDING_VERIFICATION
            || other.getStatus() == ReportStatus.VERIFIED);
  }

  /**
   * Finds the active hazards the report could be verified against: those of its type that cover its
   * district or lie within 5 km of it (SC2 step 24; PR5).
   *
   * @param reportId the identifier of the report
   * @return the candidate hazards
   * @throws NoSuchElementException if there is no such report (answered as a 404)
   */
  @Transactional(readOnly = true)
  public List<Hazard> findCandidateHazards(String reportId) {
    HazardReport report = findReport(reportId);
    return hazardRepository.findActiveNear(
        report.getLocation(), report.getHazardType(), report.getDistrict());
  }

  /**
   * Verifies a report against an existing hazard (SC2 steps 25 to 28; SD2 12.1.2 and 12.1.3): the
   * report is linked to the hazard with the officer and the time, and the hazard's active warning
   * is asked for, so that the console can offer to escalate it (A7).
   *
   * <p>The warning is asked for inside the decision's transaction, so a failure there rolls the
   * decision back and nothing changes (SC2 E5).
   *
   * @param reportId the identifier of the report
   * @param hazardId the identifier of the hazard to link
   * @param officerId the identifier of the deciding duty officer
   * @return the outcome: the verified report and the hazard's active warning, or {@code null} if it
   *     has none (A6, 25p)
   * @throws ReportAlreadyDecidedException if the report is no longer pending
   * @throws NoSuchElementException if the report, the officer or the hazard is unknown
   */
  public VerificationOutcome verifyReport(String reportId, String hazardId, String officerId) {
    return decide(
        reportId,
        officerId,
        (report, officer, at) -> {
          Hazard hazard =
              hazardRepository
                  .findById(hazardId)
                  .orElseThrow(
                      () -> new NoSuchElementException("hazard " + hazardId + " not found"));
          report.verify(officer, hazard, at);
          return outcomeOf(report, hazardId);
        });
  }

  /**
   * Verifies a report as a new hazard (SC2 A6; SD2 14.1.1 to 14.1.4): the officer's details are
   * checked, a hazard is created from the report and the details, and the report is linked to it.
   *
   * <p>When the officer entered no description the new hazard gets the report's description (MD
   * v0.13; a Group_058 assumption).
   *
   * @param reportId the identifier of the report
   * @param details the severity, districts, place name and optional description of the new hazard
   * @param officerId the identifier of the deciding duty officer
   * @return the outcome: the verified report and the new hazard's active warning (none yet, so
   *     {@code null}, unless UC1 already has one)
   * @throws ReportAlreadyDecidedException if the report is no longer pending
   * @throws InvalidHazardDetailsException if the severity, the districts or the place name is
   *     missing; every missing one is listed and nothing is created (E9)
   * @throws NoSuchElementException if the report or the officer is unknown
   * @throws NullPointerException if {@code details} is {@code null}
   */
  public VerificationOutcome verifyAsNewHazard(
      String reportId, NewHazardDetails details, String officerId) {
    Objects.requireNonNull(details, "details");
    return decide(
        reportId,
        officerId,
        (report, officer, at) -> {
          NewHazardDetails complete = completed(details, report);
          String hazardId = idGenerator.next(IdKind.HAZARD, null);
          Hazard hazard =
              hazardRepository.save(
                  new Hazard(
                      hazardId,
                      report.getHazardType(),
                      report.getLocation(),
                      report.getCapturedAt(),
                      complete));
          report.verify(officer, hazard, at);
          return outcomeOf(report, hazardId);
        });
  }

  /**
   * Rejects a report with a reason (SC2 A4; {@code sd Reject Report} and {@code sd Record Rejection
   * Reason}); the citizen is told the reason.
   *
   * @param reportId the identifier of the report
   * @param reason why the report is rejected
   * @param officerId the identifier of the deciding duty officer
   * @return the rejected report
   * @throws ReportAlreadyDecidedException if the report is no longer pending (checked before the
   *     reason)
   * @throws ReasonRequiredException if the reason is missing or blank (E7)
   * @throws NoSuchElementException if the report or the officer is unknown
   */
  public HazardReport rejectReport(String reportId, String reason, String officerId) {
    return decide(
        reportId,
        officerId,
        (report, officer, at) -> {
          if (reason == null || reason.isBlank()) {
            throw new ReasonRequiredException();
          }
          report.reject(officer, reason, at);
          return report;
        });
  }

  /** What one decision does to a pending report inside the decision's transaction. */
  @FunctionalInterface
  private interface Decision<R> {
    R apply(HazardReport report, DMCDutyOfficer officer, LocalDateTime at);
  }

  /** The result of a committed decision and what the notice after the commit needs. */
  private record Decided<R>(R result, Recipient recipient, ReportDecisionNotice notice) {}

  private <R> R decide(String reportId, String officerId, Decision<R> decision) {
    Decided<R> decided;
    try {
      decided =
          Objects.requireNonNull(
              transaction.execute(status -> record(reportId, officerId, decision)));
    } catch (ObjectOptimisticLockingFailureException e) {
      throw lostToConcurrentDecision(reportId, e);
    }
    return announce(decided, officerId);
  }

  private <R> Decided<R> record(String reportId, String officerId, Decision<R> decision) {
    HazardReport report = findReport(reportId);
    if (!report.isPending()) {
      throw new ReportAlreadyDecidedException(report);
    }
    DMCDutyOfficer officer =
        dutyOfficerRepository
            .findById(officerId)
            .orElseThrow(() -> new NoSuchElementException("officer " + officerId + " not found"));
    LocalDateTime at = LocalDateTime.now(clock);
    R result = decision.apply(report, officer, at);
    hazardReportRepository.save(report);
    Recipient reporter = (Citizen) Hibernate.unproxy(report.getReporter());
    ReportDecisionNotice notice =
        new ReportDecisionNotice(
            report.getReportId(), report.getStatus(), report.getRejectionReason().orElse(null), at);
    return new Decided<>(result, reporter, notice);
  }

  private ReportAlreadyDecidedException lostToConcurrentDecision(
      String reportId, ObjectOptimisticLockingFailureException cause) {
    HazardReport reread =
        Objects.requireNonNull(readOnlyTransaction.execute(s -> findReport(reportId)));
    LOG.info("Decision on {} lost to a concurrent decision", reportId);
    return new ReportAlreadyDecidedException(reread, cause);
  }

  private <R> R announce(Decided<R> decided, String officerId) {
    ReportDecisionNotice notice = decided.notice();
    String reportId = notice.reportId();
    ReportStatus decision = notice.status();
    NotificationChannel channel = sendNotice(decided.recipient(), notice);
    if (channel == null) {
      LOG.warn("No decision notice could be sent for report {}", reportId);
    }
    LOG.info(
        "Report {} {} by officer {}, notice sent by {}", reportId, decision, officerId, channel);
    return decided.result();
  }

  /** Sends the notice by push when there is a device token, else, or when that fails, by SMS. */
  private NotificationChannel sendNotice(Recipient recipient, ReportDecisionNotice notice) {
    if (recipient.getDeviceToken().isPresent() && pushGateway.sendNotice(recipient, notice)) {
      return NotificationChannel.PUSH;
    }
    return smsGateway.sendNotice(recipient, notice) ? NotificationChannel.SMS : null;
  }

  private NewHazardDetails completed(NewHazardDetails details, HazardReport report) {
    List<String> errors = new ArrayList<>();
    if (details.severity() == null) {
      errors.add(SEVERITY_REQUIRED);
    }
    if (details.districts().isEmpty()) {
      errors.add(DISTRICTS_REQUIRED);
    }
    if (details.locationName() == null || details.locationName().isBlank()) {
      errors.add(PLACE_NAME_REQUIRED);
    }
    if (!errors.isEmpty()) {
      throw new InvalidHazardDetailsException(errors);
    }
    if (details.description() != null && !details.description().isBlank()) {
      return details;
    }
    return new NewHazardDetails(
        details.severity(), details.districts(), details.locationName(), report.getDescription());
  }

  private VerificationOutcome outcomeOf(HazardReport report, String hazardId) {
    Warning warning = warningController.getActiveWarningForHazard(hazardId).orElse(null);
    if (warning != null) {
      // The outcome is returned after the commit, and the escalation prompt reads the warning's
      // target districts, a lazy collection: load them while the session is open (SC2 29a). The
      // getter returns an unmodifiable wrapper, so Hibernate.initialize on it would do nothing;
      // reading the size loads the collection behind the wrapper.
      int districts = warning.getTargetDistricts().size();
      String warningId = warning.getWarningId();
      LOG.debug("Active warning {} targets {} district(s)", warningId, districts);
    }
    return new VerificationOutcome(report, warning);
  }

  private HazardReport findReport(String reportId) {
    return hazardReportRepository
        .findById(reportId)
        .orElseThrow(() -> new NoSuchElementException("report " + reportId + " not found"));
  }
}
