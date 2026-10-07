package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * One entry of the alert timeline: a warning issued or escalated.
 *
 * <p>Implements CH-20.
 *
 * @param warningId the identifier of the warning
 * @param issuedTime when the warning was issued or escalated
 * @param warningLevel the level at that time
 * @param districts the districts the warning covers
 * @param event whether the entry is an issue or an escalation
 */
public record AlertTimelineEntry(
    String warningId,
    LocalDateTime issuedTime,
    WarningLevel warningLevel,
    Set<District> districts,
    TimelineEvent event) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public AlertTimelineEntry(
      String warningId,
      LocalDateTime issuedTime,
      WarningLevel warningLevel,
      Set<District> districts,
      TimelineEvent event) {
    this.warningId = Objects.requireNonNull(warningId, "warningId");
    this.issuedTime = Objects.requireNonNull(issuedTime, "issuedTime");
    this.warningLevel = Objects.requireNonNull(warningLevel, "warningLevel");
    this.districts = Set.copyOf(districts);
    this.event = Objects.requireNonNull(event, "event");
  }
}
