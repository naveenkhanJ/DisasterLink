package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.time.LocalDateTime;

/**
 * The reference times of the seed. Every seed time is relative to {@code now}, taken from the
 * injected clock when the seed runs (implementation standards section 11): the storyline keeps its
 * order and spacing, never a fixed calendar date. The completed storyline of the Kalu Ganga flood
 * starts eight days before today, so a {@code 20 May 2026} in the scenarios is "day 0" here and the
 * closing on {@code 25 May 2026} is "day 5", three days before today.
 *
 * @param now the time the seed runs, to the second
 */
record SeedContext(LocalDateTime now) {

  /** Days between the start of the completed storyline and today. */
  private static final int EVENT_DAYS_AGO = 8;

  /** The midnight that starts day 0 of the completed storyline (the scenarios' 20 May 2026). */
  LocalDateTime eventDay() {
    return now.toLocalDate().minusDays(EVENT_DAYS_AGO).atStartOfDay();
  }

  /**
   * A time on a day of the completed storyline.
   *
   * @param day the day, 0 for the scenarios' 20 May
   * @param hour the hour
   * @param minute the minute
   * @return the time
   */
  LocalDateTime event(int day, int hour, int minute) {
    return eventDay().plusDays(day).plusHours(hour).plusMinutes(minute);
  }

  /**
   * A time before now.
   *
   * @param hours whole hours before now
   * @param minutes further minutes before now
   * @return the time
   */
  LocalDateTime before(int hours, int minutes) {
    return now.minusHours(hours).minusMinutes(minutes);
  }
}
