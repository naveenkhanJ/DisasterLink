package lk.sliit.se3070.group058.disasterlink.analytics.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OrderColumn;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ColumnLengths;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteriaConverter;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResultsConverter;

/**
 * A disaster-response report: its criteria, its computed results and the organisations it was
 * shared with.
 *
 * <p>Implements CH-18, CH-20 and CH-31 (A21, A22, A23). The rules of the report (recordFindings,
 * markProvisional, isProvisional, shareWith) are written by its owner.
 */
@Entity
public class DisasterReport {

  @Id private String reportId;

  private String reportTitle;

  private LocalDateTime generatedDate;

  @Convert(converter = ReportCriteriaConverter.class)
  @Lob
  private ReportCriteria criteria;

  @Convert(converter = ReportResultsConverter.class)
  @Lob
  private ReportResults results;

  @Column(length = ColumnLengths.FINDINGS)
  private String findings;

  @ElementCollection
  @CollectionTable(
      name = "disaster_report_provisional",
      joinColumns = @JoinColumn(name = "report_id"))
  @MapKeyEnumerated(EnumType.STRING)
  @MapKeyColumn(name = "district")
  @Column(name = "since")
  private Map<District, LocalDateTime> provisionalSince;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "officer_id")
  private DMCOfficer officer;

  @ManyToMany
  @JoinTable(
      name = "disaster_report_hazard",
      joinColumns = @JoinColumn(name = "report_id"),
      inverseJoinColumns = @JoinColumn(name = "hazard_id"))
  @OrderColumn(name = "hazard_order")
  private List<Hazard> hazards;

  @ManyToMany
  @JoinTable(
      name = "disaster_report_shared_with",
      joinColumns = @JoinColumn(name = "report_id"),
      inverseJoinColumns = @JoinColumn(name = "organization_id"))
  @OrderColumn(name = "organization_order")
  private List<Organization> sharedWith;

  /**
   * Creates a freshly generated report: the drawn message of sequence diagram SD4 (1). It has no
   * findings, no provisional district and is shared with nobody.
   *
   * @param reportId the identifier from {@code IdGenerator}
   * @param reportTitle the title
   * @param generatedDate when the report was generated
   * @param criteria the criteria
   * @param results the computed results
   * @param officer the officer who generates the report
   * @param hazards the hazards the report covers
   * @throws NullPointerException if an argument is {@code null}
   */
  public DisasterReport(
      String reportId,
      String reportTitle,
      LocalDateTime generatedDate,
      ReportCriteria criteria,
      ReportResults results,
      DMCOfficer officer,
      List<Hazard> hazards) {
    this(
        reportId,
        reportTitle,
        generatedDate,
        criteria,
        results,
        officer,
        hazards,
        null,
        Map.of(),
        List.of());
  }

  /**
   * Creates a report from its full stored state. This is the seed's constructor, for reports with
   * findings, provisional districts and shares; it changes no domain rule.
   *
   * @param findings the findings, or {@code null} if there are none
   * @param provisionalSince the provisional districts with the time since when; empty if final
   * @param sharedWith the organisations the report was shared with
   * @throws NullPointerException if an argument other than {@code findings} is {@code null}
   */
  public DisasterReport(
      String reportId,
      String reportTitle,
      LocalDateTime generatedDate,
      ReportCriteria criteria,
      ReportResults results,
      DMCOfficer officer,
      List<Hazard> hazards,
      String findings,
      Map<District, LocalDateTime> provisionalSince,
      List<Organization> sharedWith) {
    this.reportId = Objects.requireNonNull(reportId, "reportId");
    this.reportTitle = Objects.requireNonNull(reportTitle, "reportTitle");
    this.generatedDate = Objects.requireNonNull(generatedDate, "generatedDate");
    this.criteria = Objects.requireNonNull(criteria, "criteria");
    this.results = Objects.requireNonNull(results, "results");
    this.officer = Objects.requireNonNull(officer, "officer");
    this.hazards = new ArrayList<>(hazards);
    this.findings = findings;
    this.provisionalSince = new EnumMap<>(District.class);
    this.provisionalSince.putAll(provisionalSince);
    this.sharedWith = new ArrayList<>(sharedWith);
  }

  /** Constructor for JPA only. */
  protected DisasterReport() {
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
   * Returns the title of the report.
   *
   * @return the title of the report
   */
  public String getReportTitle() {
    return reportTitle;
  }

  /**
   * Returns the time the report was generated.
   *
   * @return the time the report was generated
   */
  public LocalDateTime getGeneratedDate() {
    return generatedDate;
  }

  /**
   * Returns the criteria the report was generated for.
   *
   * @return the criteria the report was generated for
   */
  public ReportCriteria getCriteria() {
    return criteria;
  }

  /**
   * Returns the computed results of the report.
   *
   * @return the computed results of the report
   */
  public ReportResults getResults() {
    return results;
  }

  /**
   * Returns the findings the officer wrote.
   *
   * @return the findings the officer wrote, or empty if there is none
   */
  public Optional<String> getFindings() {
    return Optional.ofNullable(findings);
  }

  /**
   * Returns the districts whose data is provisional, with the time since when.
   *
   * @return an unmodifiable view of the districts whose data is provisional, with the time since
   *     when
   */
  public Map<District, LocalDateTime> getProvisionalSince() {
    return Collections.unmodifiableMap(provisionalSince);
  }

  /**
   * Returns the officer who generated the report (A21).
   *
   * @return the officer who generated the report (A21)
   */
  public DMCOfficer getOfficer() {
    return officer;
  }

  /**
   * Returns the hazards the report covers (A22).
   *
   * @return an unmodifiable view of the hazards the report covers (A22)
   */
  public List<Hazard> getHazards() {
    return Collections.unmodifiableList(hazards);
  }

  /**
   * Returns the organisations the report was shared with (A23).
   *
   * @return an unmodifiable view of the organisations the report was shared with (A23)
   */
  public List<Organization> getSharedWith() {
    return Collections.unmodifiableList(sharedWith);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof DisasterReport that
            && Objects.equals(getReportId(), that.getReportId());
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
