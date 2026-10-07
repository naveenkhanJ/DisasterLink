package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A warning issued for a hazard. It carries its notifications, one per recipient and channel and
 * round.
 *
 * <p>Implements CH-17, CH-21 and CH-22 (A1, A2, A7).
 */
@Entity
public class Warning {

  @Id private String warningId;

  @Enumerated(EnumType.STRING)
  private WarningLevel warningLevel;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "sinhala", column = @Column(name = "message_sinhala", length = 2000)),
    @AttributeOverride(name = "tamil", column = @Column(name = "message_tamil", length = 2000)),
    @AttributeOverride(name = "english", column = @Column(name = "message_english", length = 2000))
  })
  private LocalisedText message;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "sinhala",
        column = @Column(name = "instructions_sinhala", length = 2000)),
    @AttributeOverride(
        name = "tamil",
        column = @Column(name = "instructions_tamil", length = 2000)),
    @AttributeOverride(
        name = "english",
        column = @Column(name = "instructions_english", length = 2000))
  })
  private LocalisedText instructions;

  private LocalDateTime issuedTime;

  private LocalDateTime expiryTime;

  @ElementCollection
  @CollectionTable(name = "warning_target_district", joinColumns = @JoinColumn(name = "warning_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "district")
  @OrderColumn(name = "district_order")
  private List<District> targetDistricts;

  @ElementCollection
  @CollectionTable(
      name = "warning_target_river_basin",
      joinColumns = @JoinColumn(name = "warning_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "river_basin")
  @OrderColumn(name = "river_basin_order")
  private List<RiverBasin> targetRiverBasins;

  @ElementCollection
  @CollectionTable(name = "warning_channel", joinColumns = @JoinColumn(name = "warning_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "channel")
  private Set<NotificationChannel> channels;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "hazard_id")
  private Hazard hazard;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "officer_id")
  private DMCOfficer officer;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "warning_id")
  @OrderColumn(name = "notification_order")
  private List<Notification> notifications;

  /**
   * Creates a warning for a hazard from the details an officer entered.
   *
   * @param warningId the identifier from {@code IdGenerator}
   * @param hazard the hazard the warning is for
   * @param details the level, texts, targets, channels and expiry time of the warning
   * @param officer the officer who issues the warning
   * @param issuedTime when the warning is issued
   * @throws NullPointerException if an argument is {@code null}
   */
  public Warning(
      String warningId,
      Hazard hazard,
      WarningDetails details,
      DMCOfficer officer,
      LocalDateTime issuedTime) {
    this.warningId = Objects.requireNonNull(warningId, "warningId");
    this.hazard = Objects.requireNonNull(hazard, "hazard");
    Objects.requireNonNull(details, "details");
    this.officer = Objects.requireNonNull(officer, "officer");
    this.issuedTime = Objects.requireNonNull(issuedTime, "issuedTime");
    this.warningLevel = details.warningLevel();
    this.message = details.message();
    this.instructions = details.instructions();
    this.expiryTime = details.expiryTime();
    this.targetDistricts = new ArrayList<>(details.targetDistricts());
    this.targetRiverBasins = new ArrayList<>(details.targetRiverBasins());
    this.channels = EnumSet.noneOf(NotificationChannel.class);
    this.channels.addAll(details.channels());
    this.notifications = new ArrayList<>();
  }

  /**
   * Creates a warning from its stored state. This is the seed's constructor: it restores a warning
   * together with the notifications already sent for it, and changes no domain rule. Each
   * notification is added to the warning and attached to it, as {@code addNotification} will do.
   *
   * @param warningId the identifier
   * @param hazard the hazard the warning is for
   * @param details the stored level, texts, targets, channels and expiry time
   * @param officer the officer who issued the warning
   * @param issuedTime when the warning was issued
   * @param notifications the notifications already sent, in order
   * @throws NullPointerException if an argument is {@code null}
   */
  // javac's this-escape lint: attachTo only stores this warning as the notification's read-only
  // back-reference, and no class in the application extends Warning (Hibernate's proxies do not
  // run this constructor).
  @SuppressWarnings("this-escape")
  public Warning(
      String warningId,
      Hazard hazard,
      WarningDetails details,
      DMCOfficer officer,
      LocalDateTime issuedTime,
      List<Notification> notifications) {
    this(warningId, hazard, details, officer, issuedTime);
    for (Notification notification : notifications) {
      this.notifications.add(notification);
      notification.attachTo(this);
    }
  }

  /** Constructor for JPA only. */
  protected Warning() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the warning.
   *
   * @return the identifier of the warning
   */
  public String getWarningId() {
    return warningId;
  }

  /**
   * Returns the level of the warning.
   *
   * @return the level of the warning
   */
  public WarningLevel getWarningLevel() {
    return warningLevel;
  }

  /**
   * Returns the message of the warning in the three languages.
   *
   * @return the message of the warning in the three languages
   */
  public LocalisedText getMessage() {
    return message;
  }

  /**
   * Returns the instructions of the warning in the three languages.
   *
   * @return the instructions of the warning in the three languages
   */
  public LocalisedText getInstructions() {
    return instructions;
  }

  /**
   * Returns the time the warning was issued.
   *
   * @return the time the warning was issued
   */
  public LocalDateTime getIssuedTime() {
    return issuedTime;
  }

  /**
   * Returns the time the warning expires.
   *
   * @return the time the warning expires
   */
  public LocalDateTime getExpiryTime() {
    return expiryTime;
  }

  /**
   * Returns the districts the warning is sent to.
   *
   * @return an unmodifiable view of the districts the warning is sent to
   */
  public List<District> getTargetDistricts() {
    return Collections.unmodifiableList(targetDistricts);
  }

  /**
   * Returns the river basins the warning covers.
   *
   * @return an unmodifiable view of the river basins the warning covers
   */
  public List<RiverBasin> getTargetRiverBasins() {
    return Collections.unmodifiableList(targetRiverBasins);
  }

  /**
   * Returns the channels the warning is sent through.
   *
   * @return an unmodifiable view of the channels the warning is sent through
   */
  public Set<NotificationChannel> getChannels() {
    return Collections.unmodifiableSet(channels);
  }

  /**
   * Returns the hazard the warning is for (A1).
   *
   * @return the hazard the warning is for (A1)
   */
  public Hazard getHazard() {
    return hazard;
  }

  /**
   * Returns the officer who issued the warning (A7).
   *
   * @return the officer who issued the warning (A7)
   */
  public DMCOfficer getOfficer() {
    return officer;
  }

  /**
   * Returns the notifications generated for the warning (A2).
   *
   * @return an unmodifiable view of the notifications generated for the warning (A2)
   */
  public List<Notification> getNotifications() {
    return Collections.unmodifiableList(notifications);
  }

  /**
   * Returns every district the warning covers: its target districts together with the districts of
   * its target river basins.
   *
   * @return an unmodifiable set of the covered districts
   */
  public Set<District> coveredDistricts() {
    Set<District> covered = EnumSet.noneOf(District.class);
    covered.addAll(targetDistricts);
    for (RiverBasin basin : targetRiverBasins) {
      covered.addAll(basin.districts());
    }
    return Collections.unmodifiableSet(covered);
  }

  /**
   * Derives the status of the warning at a time; the status is never stored.
   *
   * @param time the time to judge at
   * @return {@code ACTIVE} while {@code time} is before the expiry time, otherwise {@code EXPIRED}
   * @throws NullPointerException if {@code time} is {@code null}
   */
  public WarningStatus statusAt(LocalDateTime time) {
    Objects.requireNonNull(time, "time");
    return time.isBefore(expiryTime) ? WarningStatus.ACTIVE : WarningStatus.EXPIRED;
  }

  /**
   * Escalates the warning to a higher level with a new message, instructions and expiry time.
   *
   * <p>Placeholder owned by Lakmal: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities").
   *
   * @param details the new level, message, instructions and expiry time
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public void escalate(EscalationDetails details) {
    throw new UnsupportedOperationException("not implemented yet — Lakmal");
  }

  /**
   * Adds a notification to the warning.
   *
   * <p>Placeholder owned by Lakmal: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities"). The implementation adds the notification and calls
   * {@code notification.attachTo(this)}.
   *
   * @param notification the notification to add
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public void addNotification(Notification notification) {
    throw new UnsupportedOperationException("not implemented yet — Lakmal");
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof Warning that && Objects.equals(getWarningId(), that.getWarningId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getWarningId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getWarningId() + "]";
  }
}
