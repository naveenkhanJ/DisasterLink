package lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;

/**
 * The wording of the verification console: dates as "21 May 2026, 07:40", constants as "Blocked
 * road" and "Nuwara Eliya", places as "6.5888 N, 79.9640 E", the warning levels with their labels.
 * One place for the texts of the wireframes, so {@link VerificationConsoleMapper} only maps.
 *
 * <p>Implementation-only utility class (model dictionary section 0.7; WF2C frames). Implements
 * CH-27.
 */
final class ConsoleTexts {

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.ENGLISH);

  private static final DateTimeFormatter PROMPT_DATE_TIME =
      DateTimeFormatter.ofPattern("d MMM uuuu HH:mm", Locale.ENGLISH);

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

  /** The separator of a list of districts. */
  static final String SEPARATOR = ", ";

  /** The dash of "Flood — Kalu Ganga at Ratnapura". */
  static final String DASH = " — ";

  private ConsoleTexts() {
    throw new AssertionError("ConsoleTexts is a utility class");
  }

  /**
   * Words a date and time.
   *
   * @param time the time
   * @return "21 May 2026, 07:40"
   */
  static String dateTime(LocalDateTime time) {
    return DATE_TIME.format(time);
  }

  /**
   * Words a date and time as the escalation prompt does, without a comma.
   *
   * @param time the time
   * @return "21 May 2026 10:45"
   */
  static String promptDateTime(LocalDateTime time) {
    return PROMPT_DATE_TIME.format(time);
  }

  /**
   * Words the time of day.
   *
   * @param time the time
   * @return "07:40"
   */
  static String time(LocalDateTime time) {
    return TIME.format(time);
  }

  /**
   * Words an enum constant.
   *
   * @param constant the constant
   * @return "Blocked road" for {@code BLOCKED_ROAD}
   */
  static String label(Enum<?> constant) {
    String text = constant.name().replace('_', ' ').toLowerCase(Locale.ENGLISH);
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  /**
   * Words a district.
   *
   * @param district the district
   * @return "Nuwara Eliya" for {@code NUWARA_ELIYA}
   */
  static String districtLabel(District district) {
    return Arrays.stream(district.name().split("_"))
        .map(
            word ->
                Character.toUpperCase(word.charAt(0))
                    + word.substring(1).toLowerCase(Locale.ENGLISH))
        .collect(Collectors.joining(" "));
  }

  /**
   * Words the latitude and longitude of a place.
   *
   * @param point the place
   * @return "6.5888 N, 79.9640 E"
   */
  static String place(GeoPoint point) {
    return String.format(
        Locale.ROOT,
        "%.4f %s, %.4f %s",
        Math.abs(point.latitude()),
        point.latitude() >= 0 ? "N" : "S",
        Math.abs(point.longitude()),
        point.longitude() >= 0 ? "E" : "W");
  }

  /**
   * Words the accuracy of a fix.
   *
   * @param point the place
   * @return " · accuracy ±8 m", or an empty text if the accuracy is not known
   */
  static String accuracy(GeoPoint point) {
    return point.accuracyMetres() == null
        ? ""
        : String.format(Locale.ROOT, " · accuracy ±%.0f m", point.accuracyMetres());
  }

  /**
   * Words a warning level.
   *
   * @param level the level
   * @return "Level 2 – Amber (Be Alert)" for the amber level
   */
  static String levelLabel(WarningLevel level) {
    return switch (level) {
      case LEVEL_1_YELLOW -> "Level 1 – Yellow (Remain Vigilant)";
      case LEVEL_2_AMBER -> "Level 2 – Amber (Be Alert)";
      case LEVEL_3_RED -> "Level 3 – Red (Evacuate)";
    };
  }

  /**
   * Gives the style class of a level's tag.
   *
   * @param level the level
   * @return {@code tag--l1}, {@code tag--l2} or {@code tag--l3}
   */
  static String levelClass(WarningLevel level) {
    return switch (level) {
      case LEVEL_1_YELLOW -> "tag--l1";
      case LEVEL_2_AMBER -> "tag--l2";
      case LEVEL_3_RED -> "tag--l3";
    };
  }

  /**
   * Joins names as a sentence does.
   *
   * @param items the names
   * @return "Ratnapura and Kalutara"; "A, B and C" for three
   */
  static String joinWithAnd(List<String> items) {
    if (items.size() < 2) {
      return String.join(SEPARATOR, items);
    }
    return String.join(SEPARATOR, items.subList(0, items.size() - 1))
        + " and "
        + items.get(items.size() - 1);
  }

  /**
   * Turns blank text into {@code null}.
   *
   * @param text the text, or {@code null}
   * @return the text, or {@code null} if it is {@code null} or blank
   */
  static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}
