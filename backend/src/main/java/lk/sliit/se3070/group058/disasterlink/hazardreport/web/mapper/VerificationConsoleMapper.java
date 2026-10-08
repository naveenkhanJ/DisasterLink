package lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.NearbyReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.NewHazardForm;
import lk.sliit.se3070.group058.disasterlink.shared.domain.CommunityVolunteer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NewHazardDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.User;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

/**
 * Maps the entities the verification console reads to the view records its templates show, with the
 * texts of the wireframes (WF2C-S01 to S02h): dates as "21 May 2026, 07:40", distances as "1.2 km",
 * enum constants as "Blocked road" and "Pending verification", the hazard label "Flood — Kalu Ganga
 * at Ratnapura".
 *
 * <p>The mapper reads lazy associations (the reporter, the reviewer, the linked hazard), so every
 * method that takes an entity must be called inside a transaction (open-in-view is off; standards
 * section 4, "Exceptions"). Templates receive only the records below, never an entity or an {@code
 * Optional}.
 *
 * <p>Implementation-only class (model dictionary section 0.7). Implements CH-26 and CH-27.
 */
@Component
public class VerificationConsoleMapper {

  /** The state of a details page for a report that still waits for a decision. */
  public static final String STATE_DECIDE = "DECIDE";

  /** The state of the new-hazard form (SC2 A6). */
  public static final String STATE_NEW_HAZARD = "NEW_HAZARD";

  /** The state of a report that was decided (SC2 E10, WF2C-S02h). */
  public static final String STATE_DECIDED = "DECIDED";

  /** What the confirmation of "Verify as New Hazard" shows for a value the officer left out. */
  private static final String NOT_ENTERED = "—";

  /** The error the control gives for a missing severity. */
  public static final String SEVERITY_ERROR = "Severity is required";

  /** The error the control gives for missing districts. */
  public static final String DISTRICTS_ERROR = "Districts are required";

  /** The error the control gives for a missing place name. */
  public static final String PLACE_NAME_ERROR = "Place name is required";

  /**
   * A row of the verification queue (WF2C-S01).
   *
   * @param reportId the report identifier
   * @param hazardType the hazard type label
   * @param district the district label
   * @param captureTime the capture time, "21 May 2026, 07:40"
   * @param reporterType "Citizen" or "Community Volunteer"
   * @param status the status label
   */
  public record PendingReportRow(
      String reportId,
      String hazardType,
      String district,
      String captureTime,
      String reporterType,
      String status) {}

  /**
   * A row of the nearby-reports card (WF2C-S02).
   *
   * @param reportId the report identifier
   * @param hazardType the hazard type label
   * @param district the district label
   * @param captureTime the capture time of day, "19:55"
   * @param distance the distance from the selected report, "1.2 km"
   * @param status the status label
   */
  public record NearbyReportRow(
      String reportId,
      String hazardType,
      String district,
      String captureTime,
      String distance,
      String status) {}

  /**
   * A candidate hazard of the candidate card (WF2C-S02).
   *
   * @param hazardId the hazard identifier
   * @param label the hazard label, "Flood — Kalu Ganga at Ratnapura"
   * @param severity "Severity Medium"
   * @param districts the districts joined with ", "
   * @param riverBasin "River basin Kalu", or {@code null} if the hazard has none
   * @param source "Source Agency bulletin"
   */
  public record CandidateHazardView(
      String hazardId,
      String label,
      String severity,
      String districts,
      String riverBasin,
      String source) {}

  /**
   * The report card, the map and the status of the report.
   *
   * @param reportId the report identifier
   * @param status the status label
   * @param statusCode the status constant name, for the tag style
   * @param pending whether the report still waits for a decision
   * @param reporterName the reporter's name
   * @param reporterType "Citizen" or "Community Volunteer"
   * @param hazardType the hazard type label
   * @param hazardTypeLower the hazard type label in lower case, for "No active flood hazard fits"
   * @param description the citizen's description
   * @param location "6.5888 N, 79.9640 E · GPS · accuracy ±8 m"
   * @param district the district label
   * @param districtCode the district constant name
   * @param captureTime the capture time
   * @param receivedTime the received time
   * @param dot the map marker, "lat,lon"
   * @param locationNote "Kalutara District — report location 6.5888 N, 79.9640 E"
   */
  public record ReportView(
      String reportId,
      String status,
      String statusCode,
      boolean pending,
      String reporterName,
      String reporterType,
      String hazardType,
      String hazardTypeLower,
      String description,
      String location,
      String district,
      String districtCode,
      String captureTime,
      String receivedTime,
      String dot,
      String locationNote) {}

  /**
   * What a decided report shows: the confirmation card of WF2C-S02e and S02f and the alert card of
   * S02h.
   *
   * @param reportId the report identifier
   * @param status the recorded status label
   * @param statusCode the recorded status constant name
   * @param rejected whether the report was rejected
   * @param linkedHazard "Flood — Kalu Ganga at Ratnapura (HZ-2026-0142)", or {@code null}
   * @param reason the rejection reason as typed, or {@code null}
   * @param decidedBy the name of the deciding officer
   * @param decisionTime the decision time
   */
  public record DecisionView(
      String reportId,
      String status,
      String statusCode,
      boolean rejected,
      String linkedHazard,
      String reason,
      String decidedBy,
      String decisionTime) {}

  /**
   * Everything the details pages show about one report.
   *
   * @param report the report card
   * @param nearby the nearby reports
   * @param candidates the candidate hazards (empty after a decision)
   * @param selectedHazardId the candidate that is preselected, or {@code null} if none
   * @param decision the decision, or {@code null} while the report is pending
   */
  public record ReportPage(
      ReportView report,
      List<NearbyReportRow> nearby,
      List<CandidateHazardView> candidates,
      String selectedHazardId,
      DecisionView decision) {}

  /**
   * The escalation prompt (WF2C-S02g, SC2 29a).
   *
   * @param text the prompt's sentence
   * @param warningId the active warning's identifier, for the escalate link
   * @param levelLabel "Level 2 – Amber (Be Alert)"
   * @param levelClass the CSS class of the level tag
   */
  public record EscalationPromptView(
      String text, String warningId, String levelLabel, String levelClass) {}

  /**
   * An option of a select or a chip group.
   *
   * @param code the constant name that is posted
   * @param label the text shown
   */
  public record Option(String code, String label) {}

  /**
   * The values of the new-hazard form as the template shows them.
   *
   * @param severity the severity constant name, or an empty text
   * @param districts the ticked district constant names
   * @param locationName the place name typed
   * @param description the description typed
   */
  public record FormView(
      String severity, Set<String> districts, String locationName, String description) {}

  /**
   * What the confirmation of "Verify as New Hazard" repeats (D-UC2-7, P238): the question and the
   * entered type, severity, districts and place name. Values the officer left out show as a dash;
   * the control refuses them when he confirms.
   *
   * @param question "Verify RPT-2026-KLT-0847 as a new hazard?"
   * @param hazardType the hazard type label, from the report
   * @param severity the severity label, or a dash
   * @param districts the district labels joined with ", ", or a dash
   * @param placeName the place name as typed, or a dash
   */
  public record NewHazardSummary(
      String question, String hazardType, String severity, String districts, String placeName) {}

  /**
   * Maps the queue.
   *
   * @param reports the pending reports, oldest first
   * @return the rows, in the same order
   */
  public List<PendingReportRow> toRows(List<HazardReport> reports) {
    return reports.stream()
        .map(
            report ->
                new PendingReportRow(
                    report.getReportId(),
                    ConsoleTexts.label(report.getHazardType()),
                    ConsoleTexts.districtLabel(report.getDistrict()),
                    ConsoleTexts.dateTime(report.getCapturedAt()),
                    reporterType(report),
                    ConsoleTexts.label(report.getStatus())))
        .toList();
  }

  /**
   * Words the number of pending reports.
   *
   * @param count the number of reports
   * @return "3 reports", or "1 report"
   */
  public String countLabel(int count) {
    return count + (count == 1 ? " report" : " reports");
  }

  /**
   * Maps one report with its nearby reports and candidate hazards. A report that is no longer
   * pending carries its decision and no candidates.
   *
   * @param report the report
   * @param nearby the nearby reports, nearest first
   * @param candidates the candidate hazards
   * @return the page
   */
  public ReportPage toPage(
      HazardReport report, List<NearbyReport> nearby, List<Hazard> candidates) {
    boolean pending = report.isPending();
    List<CandidateHazardView> views =
        pending ? candidates.stream().map(this::toCandidate).toList() : List.of();
    return new ReportPage(
        toReportView(report),
        nearby.stream().map(this::toNearby).toList(),
        views,
        views.isEmpty() ? null : views.get(0).hazardId(),
        pending ? null : toDecision(report));
  }

  /**
   * Keeps the hazard the officer chose selected on the page (the confirmation of "Verify and Link"
   * and its Cancel, D-UC2-7, P238). A hazard that is not a candidate leaves the page as it is.
   *
   * @param page the page
   * @param hazardId the identifier of the chosen hazard, or {@code null}
   * @return the page with that candidate selected
   */
  public ReportPage select(ReportPage page, String hazardId) {
    boolean candidate =
        hazardId != null
            && page.candidates().stream().anyMatch(view -> view.hazardId().equals(hazardId));
    if (!candidate) {
      return page;
    }
    return new ReportPage(
        page.report(), page.nearby(), page.candidates(), hazardId, page.decision());
  }

  /**
   * Builds the question of the confirmation of "Verify and Link" (D-UC2-7, P238).
   *
   * @param page the page of a pending report
   * @param hazardId the identifier of the chosen hazard
   * @return "Verify RPT-2026-KLT-0847 and link it to Flood — Kalu Ganga at Kalutara
   *     (HZ-2026-0142)?"
   * @throws NoSuchElementException if the hazard is not one of the page's candidates
   */
  public String linkQuestion(ReportPage page, String hazardId) {
    CandidateHazardView chosen =
        page.candidates().stream()
            .filter(view -> view.hazardId().equals(hazardId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No candidate hazard " + hazardId));
    return "Verify "
        + page.report().reportId()
        + " and link it to "
        + chosen.label()
        + " ("
        + chosen.hazardId()
        + ")?";
  }

  /**
   * Builds what the confirmation of "Verify as New Hazard" repeats (D-UC2-7, P238). No check of the
   * details is made here: incomplete details are refused by the control on Confirm.
   *
   * @param page the page of a pending report
   * @param form the entered form
   * @return the question and the entered values
   */
  public NewHazardSummary summarise(ReportPage page, NewHazardForm form) {
    String districts =
        form.districts().stream()
            .map(ConsoleTexts::districtLabel)
            .sorted()
            .collect(Collectors.joining(", "));
    return new NewHazardSummary(
        "Verify " + page.report().reportId() + " as a new hazard?",
        page.report().hazardType(),
        form.severity() == null ? NOT_ENTERED : ConsoleTexts.label(form.severity()),
        districts.isEmpty() ? NOT_ENTERED : districts,
        form.locationName() == null || form.locationName().isBlank()
            ? NOT_ENTERED
            : form.locationName().strip());
  }

  /**
   * Maps the escalation prompt of a verified report whose hazard has an active warning.
   *
   * @param report the verified report, read in the caller's transaction
   * @param warning the hazard's active warning, whose target districts are initialised
   * @return the prompt
   */
  public EscalationPromptView toPrompt(HazardReport report, Warning warning) {
    WarningLevel level = warning.getWarningLevel();
    String districts =
        ConsoleTexts.joinWithAnd(
            warning.getTargetDistricts().stream().map(ConsoleTexts::districtLabel).toList());
    String text =
        hazardLabel(report.getHazard().orElseThrow())
            + " has an active warning — "
            + warning.getWarningId()
            + ", "
            + ConsoleTexts.levelLabel(level)
            + ", "
            + districts
            + ", valid until "
            + ConsoleTexts.promptDateTime(warning.getExpiryTime())
            + ". Escalate this warning?";
    return new EscalationPromptView(
        text,
        warning.getWarningId(),
        ConsoleTexts.levelLabel(level),
        ConsoleTexts.levelClass(level));
  }

  /**
   * Builds the subtitle of a details page.
   *
   * @param report the report
   * @return the "← Back to queue" link, the status and what to do (HTML; no user input in it)
   */
  public String subtitle(ReportView report) {
    String next = report.pending() ? "review the evidence and decide" : "decision recorded";
    return "<a href=\"/hazardreport\">← Back to queue</a> · "
        + report.status()
        + ConsoleTexts.DASH
        + next;
  }

  /**
   * Gives the new-hazard form its first values: Medium, the report's district, nothing typed.
   *
   * @param districtCode the constant name of the report's district
   * @return the values to show
   */
  public FormView initialForm(String districtCode) {
    return new FormView(HazardSeverity.MEDIUM.name(), Set.of(districtCode), "", "");
  }

  /**
   * Maps what the officer typed in the new-hazard form back to what the template shows.
   *
   * @param form the posted form
   * @return the values to show
   */
  public FormView toFormView(NewHazardForm form) {
    return new FormView(
        form.severity() == null ? "" : form.severity().name(),
        form.districts().stream().map(District::name).collect(Collectors.toSet()),
        form.locationName() == null ? "" : form.locationName(),
        form.description() == null ? "" : form.description());
  }

  /**
   * Maps the form to the details the control checks; text that is blank becomes {@code null}.
   *
   * @param form the posted form
   * @return the details
   */
  public NewHazardDetails toDetails(NewHazardForm form) {
    return new NewHazardDetails(
        form.severity(),
        form.districts(),
        ConsoleTexts.blankToNull(form.locationName()),
        ConsoleTexts.blankToNull(form.description()));
  }

  /**
   * Puts each error of {@code InvalidHazardDetailsException} under its field.
   *
   * @param errors the errors
   * @return the error text by field name: {@code severity}, {@code districts}, {@code locationName}
   */
  public Map<String, String> fieldErrors(List<String> errors) {
    Map<String, String> byField = new HashMap<>();
    for (String error : errors) {
      switch (error) {
        case SEVERITY_ERROR -> byField.put("severity", error);
        case DISTRICTS_ERROR -> byField.put("districts", error);
        case PLACE_NAME_ERROR -> byField.put("locationName", error);
        default -> byField.put("form", error);
      }
    }
    return byField;
  }

  /**
   * Lists the 25 districts for the chip group, alphabetically.
   *
   * @return the options
   */
  public List<Option> districtOptions() {
    return Arrays.stream(District.values())
        .map(district -> new Option(district.name(), ConsoleTexts.districtLabel(district)))
        .sorted(Comparator.comparing(Option::label))
        .toList();
  }

  /**
   * Lists the severities for the select.
   *
   * @return Low, Medium and High
   */
  public List<Option> severityOptions() {
    return Arrays.stream(HazardSeverity.values())
        .map(severity -> new Option(severity.name(), ConsoleTexts.label(severity)))
        .toList();
  }

  private ReportView toReportView(HazardReport report) {
    GeoPoint point = report.getLocation();
    String place = ConsoleTexts.place(point);
    return new ReportView(
        report.getReportId(),
        ConsoleTexts.label(report.getStatus()),
        report.getStatus().name(),
        report.isPending(),
        report.getReporter().getName(),
        reporterType(report),
        ConsoleTexts.label(report.getHazardType()),
        ConsoleTexts.label(report.getHazardType()).toLowerCase(Locale.ENGLISH),
        report.getDescription(),
        place
            + " · "
            + (report.isLocationFromGps() ? "GPS" : "Manual")
            + ConsoleTexts.accuracy(point),
        ConsoleTexts.districtLabel(report.getDistrict()),
        report.getDistrict().name(),
        ConsoleTexts.dateTime(report.getCapturedAt()),
        ConsoleTexts.dateTime(report.getReportTime()),
        point.latitude() + "," + point.longitude(),
        ConsoleTexts.districtLabel(report.getDistrict())
            + " District"
            + ConsoleTexts.DASH
            + "report location "
            + place);
  }

  private NearbyReportRow toNearby(NearbyReport nearby) {
    HazardReport report = nearby.report();
    return new NearbyReportRow(
        report.getReportId(),
        ConsoleTexts.label(report.getHazardType()),
        ConsoleTexts.districtLabel(report.getDistrict()),
        ConsoleTexts.time(report.getCapturedAt()),
        String.format(Locale.ROOT, "%.1f km", nearby.distanceKm()),
        ConsoleTexts.label(report.getStatus()));
  }

  private CandidateHazardView toCandidate(Hazard hazard) {
    return new CandidateHazardView(
        hazard.getHazardId(),
        hazardLabel(hazard),
        "Severity " + ConsoleTexts.label(hazard.getSeverity()),
        hazard.getDistricts().stream()
            .map(ConsoleTexts::districtLabel)
            .collect(Collectors.joining(ConsoleTexts.SEPARATOR)),
        hazard
            .getRiverBasin()
            .map(basin -> "River basin " + ConsoleTexts.label(basin))
            .orElse(null),
        "Source " + ConsoleTexts.label(hazard.getSource()));
  }

  private DecisionView toDecision(HazardReport report) {
    boolean rejected = report.getStatus() == ReportStatus.REJECTED;
    return new DecisionView(
        report.getReportId(),
        ConsoleTexts.label(report.getStatus()),
        report.getStatus().name(),
        rejected,
        report
            .getHazard()
            .map(hazard -> hazardLabel(hazard) + " (" + hazard.getHazardId() + ")")
            .orElse(null),
        report.getRejectionReason().orElse(null),
        report.getReviewer().map(User::getName).orElse(""),
        report.getVerificationTime().map(ConsoleTexts::dateTime).orElse(""));
  }

  private String hazardLabel(Hazard hazard) {
    return ConsoleTexts.label(hazard.getType()) + ConsoleTexts.DASH + hazard.getLocationName();
  }

  private String reporterType(HazardReport report) {
    return Hibernate.unproxy(report.getReporter()) instanceof CommunityVolunteer
        ? "Community Volunteer"
        : "Citizen";
  }
}
