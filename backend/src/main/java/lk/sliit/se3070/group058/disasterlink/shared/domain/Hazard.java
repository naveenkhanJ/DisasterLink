package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A recorded hazard, such as a flood. A hazard comes from an agency bulletin or from a verified
 * citizen report.
 *
 * <p>Implements CH-08, CH-17 and CH-21 (R-03).
 */
@Entity
public class Hazard {

  @Id private String hazardId;

  @Enumerated(EnumType.STRING)
  private HazardType type;

  @Column(length = ColumnLengths.LONG_TEXT)
  private String description;

  @Enumerated(EnumType.STRING)
  private HazardSeverity severity;

  private LocalDateTime startTime;

  private LocalDateTime endTime;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "latitude", column = @Column(name = "location_latitude")),
    @AttributeOverride(name = "longitude", column = @Column(name = "location_longitude")),
    @AttributeOverride(name = "accuracyMetres", column = @Column(name = "location_accuracy_metres"))
  })
  private GeoPoint location;

  private String locationName;

  @ElementCollection
  @CollectionTable(name = "hazard_district", joinColumns = @JoinColumn(name = "hazard_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "district")
  @OrderColumn(name = "district_order")
  private List<District> districts;

  @Enumerated(EnumType.STRING)
  private RiverBasin riverBasin;

  @Enumerated(EnumType.STRING)
  private HazardSource source;

  @Enumerated(EnumType.STRING)
  private HazardStatus status;

  /**
   * Creates a hazard from its full stored state. This is the seed's constructor: it restores
   * hazards in any status, for example a closed one, and changes no domain rule.
   *
   * @param endTime the time the hazard ended, or {@code null} if it has not ended
   * @param riverBasin the river basin of the hazard, or {@code null} if it has none
   * @throws NullPointerException if an argument other than {@code endTime} and {@code riverBasin}
   *     is {@code null}
   */
  public Hazard(
      String hazardId,
      HazardType type,
      String description,
      HazardSeverity severity,
      LocalDateTime startTime,
      LocalDateTime endTime,
      GeoPoint location,
      String locationName,
      List<District> districts,
      RiverBasin riverBasin,
      HazardSource source,
      HazardStatus status) {
    this.hazardId = Objects.requireNonNull(hazardId, "hazardId");
    this.type = Objects.requireNonNull(type, "type");
    this.description = Objects.requireNonNull(description, "description");
    this.severity = Objects.requireNonNull(severity, "severity");
    this.startTime = Objects.requireNonNull(startTime, "startTime");
    this.endTime = endTime;
    this.location = Objects.requireNonNull(location, "location");
    this.locationName = Objects.requireNonNull(locationName, "locationName");
    this.districts = new ArrayList<>(districts);
    this.riverBasin = riverBasin;
    this.source = Objects.requireNonNull(source, "source");
    this.status = Objects.requireNonNull(status, "status");
  }

  /**
   * Creates the hazard that an officer records while verifying a report: the drawn message {@code
   * Hazard(report, details)} of sequence diagram SD2 (14.1.2).
   *
   * <p>The report itself is not passed, because {@code shared.domain} never imports a feature
   * package and the report lives in {@code hazardreport.domain}. The caller passes the three values
   * the hazard reads from the report instead: its type, its location and its capture time. The new
   * hazard has the source {@code VERIFIED_REPORT}, the status {@code ACTIVE}, starts when the
   * report was captured, and has no end time and no river basin (SC2 A6).
   *
   * @param hazardId the identifier from {@code IdGenerator}
   * @param type the hazard type of the report
   * @param location the location of the report
   * @param capturedAt when the report was captured
   * @param details the severity, districts, location name and description the officer entered; the
   *     hazard is created only after {@code verifyAsNewHazard} has checked them, so the components
   *     that the record accepts as {@code null} are required here (P155 X1)
   * @throws NullPointerException if an argument, or a required component of {@code details}, is
   *     {@code null}
   */
  public Hazard(
      String hazardId,
      HazardType type,
      GeoPoint location,
      LocalDateTime capturedAt,
      NewHazardDetails details) {
    this(
        hazardId,
        type,
        Objects.requireNonNull(details, "details").description(),
        details.severity(),
        capturedAt,
        null,
        location,
        details.locationName(),
        details.districts(),
        null,
        HazardSource.VERIFIED_REPORT,
        HazardStatus.ACTIVE);
  }

  /** Constructor for JPA only. */
  protected Hazard() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the hazard.
   *
   * @return the identifier of the hazard
   */
  public String getHazardId() {
    return hazardId;
  }

  /**
   * Returns the type of the hazard.
   *
   * @return the type of the hazard
   */
  public HazardType getType() {
    return type;
  }

  /**
   * Returns the description of the hazard.
   *
   * @return the description of the hazard
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the assessed severity of the hazard.
   *
   * @return the assessed severity of the hazard
   */
  public HazardSeverity getSeverity() {
    return severity;
  }

  /**
   * Returns the time the hazard started.
   *
   * @return the time the hazard started
   */
  public LocalDateTime getStartTime() {
    return startTime;
  }

  /**
   * Returns the time the hazard ended.
   *
   * @return the time the hazard ended, or empty if there is none
   */
  public Optional<LocalDateTime> getEndTime() {
    return Optional.ofNullable(endTime);
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
   * Returns the name of the place of the hazard.
   *
   * @return the name of the place of the hazard
   */
  public String getLocationName() {
    return locationName;
  }

  /**
   * Returns the districts the hazard affects.
   *
   * @return an unmodifiable view of the districts the hazard affects
   */
  public List<District> getDistricts() {
    return Collections.unmodifiableList(districts);
  }

  /**
   * Returns the river basin of the hazard.
   *
   * @return the river basin of the hazard, or empty if there is none
   */
  public Optional<RiverBasin> getRiverBasin() {
    return Optional.ofNullable(riverBasin);
  }

  /**
   * Returns the source of the hazard record.
   *
   * @return the source of the hazard record
   */
  public HazardSource getSource() {
    return source;
  }

  /**
   * Returns the status of the hazard.
   *
   * @return the status of the hazard
   */
  public HazardStatus getStatus() {
    return status;
  }

  /**
   * Applies an officer's update of the description, severity and districts.
   *
   * <p>Placeholder owned by Lakmal: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities").
   *
   * @param update the new description, severity and districts
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public void updateHazard(HazardUpdate update) {
    throw new UnsupportedOperationException("not implemented yet — Lakmal");
  }

  /**
   * Lists the information a hazard update could still supply.
   *
   * <p>Placeholder owned by Lakmal: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities").
   *
   * @return the missing items
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public List<String> missingInformation() {
    throw new UnsupportedOperationException("not implemented yet — Lakmal");
  }

  /**
   * Tells whether a warning can be issued for the hazard, which is the case while it is active.
   *
   * <p>Placeholder owned by Lakmal: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities").
   *
   * @return {@code true} if the hazard is assessable
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public boolean isAssessable() {
    throw new UnsupportedOperationException("not implemented yet — Lakmal");
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof Hazard that && Objects.equals(getHazardId(), that.getHazardId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getHazardId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getHazardId() + "]";
  }
}
