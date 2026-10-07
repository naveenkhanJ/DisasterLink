package lk.sliit.se3070.group058.disasterlink.hazardreport.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;

/**
 * A hazard report a citizen submitted, waiting for or already decided by a duty officer.
 *
 * <p>Implements CH-18 and CH-26 (A4, A5, A6). The rules of the report (verify, reject, isPending)
 * are written by its owner.
 */
@Entity
public class HazardReport {

  @Id private String reportId;

  @Enumerated(EnumType.STRING)
  private HazardType hazardType;

  @Column(length = 2000)
  private String description;

  private String photo;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "latitude", column = @Column(name = "location_latitude")),
    @AttributeOverride(name = "longitude", column = @Column(name = "location_longitude")),
    @AttributeOverride(name = "accuracyMetres", column = @Column(name = "location_accuracy_metres"))
  })
  private GeoPoint location;

  private boolean locationFromGps;

  @Enumerated(EnumType.STRING)
  private District district;

  private LocalDateTime capturedAt;

  private LocalDateTime reportTime;

  @Enumerated(EnumType.STRING)
  private ReportStatus status;

  private LocalDateTime verificationTime;

  @Column(length = 2000)
  private String rejectionReason;

  @Version private long version;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id")
  private Citizen reporter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewer_id")
  private DMCDutyOfficer reviewer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hazard_id")
  private Hazard hazard;

  /**
   * Creates a report from a citizen's draft: the drawn message of sequence diagram SD2 (9.1.2.2).
   * The status is {@code PENDING_VERIFICATION}; the decision data, reviewer and hazard are empty.
   *
   * @param reportId the identifier from {@code IdGenerator}
   * @param reporter the citizen who submitted the report
   * @param draft the draft the app sent
   * @param photo the stored file name of the photo
   * @param receivedAt when the server received the report
   * @throws NullPointerException if an argument is {@code null}
   */
  public HazardReport(
      String reportId,
      Citizen reporter,
      ReportDraft draft,
      String photo,
      LocalDateTime receivedAt) {
    this.reportId = Objects.requireNonNull(reportId, "reportId");
    this.reporter = Objects.requireNonNull(reporter, "reporter");
    Objects.requireNonNull(draft, "draft");
    this.photo = Objects.requireNonNull(photo, "photo");
    this.reportTime = Objects.requireNonNull(receivedAt, "receivedAt");
    this.hazardType = draft.hazardType();
    this.description = draft.description();
    this.location = draft.location();
    this.locationFromGps = draft.locationFromGps();
    this.district = draft.district();
    this.capturedAt = draft.capturedAt();
    this.status = ReportStatus.PENDING_VERIFICATION;
  }

  /**
   * Creates a report from its full stored state. This is the seed's constructor, for reports in any
   * status; it changes no domain rule.
   *
   * @param verificationTime the time of the decision, or {@code null} if undecided
   * @param rejectionReason the reason for a rejection, or {@code null}
   * @param reviewer the officer who decided, or {@code null}
   * @param hazard the linked hazard, or {@code null}
   * @throws NullPointerException if an argument other than those four is {@code null}
   */
  public HazardReport(
      String reportId,
      Citizen reporter,
      HazardType hazardType,
      String description,
      String photo,
      GeoPoint location,
      boolean locationFromGps,
      District district,
      LocalDateTime capturedAt,
      LocalDateTime reportTime,
      ReportStatus status,
      LocalDateTime verificationTime,
      String rejectionReason,
      DMCDutyOfficer reviewer,
      Hazard hazard) {
    this.reportId = Objects.requireNonNull(reportId, "reportId");
    this.reporter = Objects.requireNonNull(reporter, "reporter");
    this.hazardType = Objects.requireNonNull(hazardType, "hazardType");
    this.description = Objects.requireNonNull(description, "description");
    this.photo = Objects.requireNonNull(photo, "photo");
    this.location = Objects.requireNonNull(location, "location");
    this.locationFromGps = locationFromGps;
    this.district = Objects.requireNonNull(district, "district");
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    this.reportTime = Objects.requireNonNull(reportTime, "reportTime");
    this.status = Objects.requireNonNull(status, "status");
    this.verificationTime = verificationTime;
    this.rejectionReason = rejectionReason;
    this.reviewer = reviewer;
    this.hazard = hazard;
  }

  /** Constructor for JPA only. */
  protected HazardReport() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the report.
   *
   * @return the identifier of the report
   */
  public String getReportId() {
    return reportId;
  }

  /**
   * Returns the type of the hazard reported.
   *
   * @return the type of the hazard reported
   */
  public HazardType getHazardType() {
    return hazardType;
  }

  /**
   * Returns the citizen's description of the hazard.
   *
   * @return the citizen's description of the hazard
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the stored file name of the report's photo.
   *
   * @return the stored file name of the report's photo
   */
  public String getPhoto() {
    return photo;
  }

  /**
   * Returns the place of the hazard.
   *
   * @return the place of the hazard
   */
  public GeoPoint getLocation() {
    return location;
  }

  /**
   * Returns the whether the location came from the phone's GPS.
   *
   * @return the whether the location came from the phone's GPS
   */
  public boolean isLocationFromGps() {
    return locationFromGps;
  }

  /**
   * Returns the district of the hazard.
   *
   * @return the district of the hazard
   */
  public District getDistrict() {
    return district;
  }

  /**
   * Returns the time the report was captured on the phone.
   *
   * @return the time the report was captured on the phone
   */
  public LocalDateTime getCapturedAt() {
    return capturedAt;
  }

  /**
   * Returns the time the server received the report.
   *
   * @return the time the server received the report
   */
  public LocalDateTime getReportTime() {
    return reportTime;
  }

  /**
   * Returns the status of the report.
   *
   * @return the status of the report
   */
  public ReportStatus getStatus() {
    return status;
  }

  /**
   * Returns the time of the officer's decision, verified or rejected.
   *
   * @return the time of the officer's decision, verified or rejected, or empty if there is none
   */
  public Optional<LocalDateTime> getVerificationTime() {
    return Optional.ofNullable(verificationTime);
  }

  /**
   * Returns the reason the officer gave for rejecting the report.
   *
   * @return the reason the officer gave for rejecting the report, or empty if there is none
   */
  public Optional<String> getRejectionReason() {
    return Optional.ofNullable(rejectionReason);
  }

  /**
   * Returns the citizen who submitted the report (A4).
   *
   * @return the citizen who submitted the report (A4)
   */
  public Citizen getReporter() {
    return reporter;
  }

  /**
   * Returns the duty officer who decided the report (A5).
   *
   * @return the duty officer who decided the report (A5), or empty if there is none
   */
  public Optional<DMCDutyOfficer> getReviewer() {
    return Optional.ofNullable(reviewer);
  }

  /**
   * Returns the hazard the report was linked to (A6).
   *
   * @return the hazard the report was linked to (A6), or empty if there is none
   */
  public Optional<Hazard> getHazard() {
    return Optional.ofNullable(hazard);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof HazardReport that && Objects.equals(getReportId(), that.getReportId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getReportId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getReportId() + "]";
  }
}
