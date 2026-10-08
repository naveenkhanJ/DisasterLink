package lk.sliit.se3070.group058.disasterlink.warning.web.mapper;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.HazardUpdateForm;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.HazardView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.OptionView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.WarningForm;

/**
 * Maps the warning dashboard's forms to the domain and back (WF1-S02 update panel, WF1-S03 warning
 * form and its states): the posted form to {@code WarningDetails} and {@code HazardUpdate}, the
 * pick-lists of the form, and the validation errors placed beside their fields. Split from {@link
 * WarningDashboardMapper}, which maps the entities to the pages' view records, so that each has one
 * reason to change (L04 Extract Class); the labels come from that class. Implementation-only class
 * (standards section 4, "No entities on the wire").
 */
public final class WarningFormMapper {

  /** The key of a form-level error that belongs to no single field. */
  public static final String GENERAL_ERROR = "general";

  /** The key of the affected-area error (SC1 A2). */
  public static final String AREA_ERROR = "area";

  private static final String SEPARATOR = ", ";

  private WarningFormMapper() {
    throw new AssertionError("WarningFormMapper is a utility class");
  }

  /**
   * Turns the posted form into the details of {@code enterWarningDetails}; a blank text becomes
   * {@code null} (not entered).
   *
   * @param form the posted form
   * @return the warning details
   */
  public static WarningDetails toDetails(WarningForm form) {
    return new WarningDetails(
        form.warningLevel(),
        new LocalisedText(
            blankToNull(form.messageSinhala()),
            blankToNull(form.messageTamil()),
            blankToNull(form.messageEnglish())),
        new LocalisedText(
            blankToNull(form.instructionsSinhala()),
            blankToNull(form.instructionsTamil()),
            blankToNull(form.instructionsEnglish())),
        form.targetDistricts(),
        form.targetRiverBasins(),
        form.channels(),
        form.expiryTime());
  }

  /**
   * Returns the form with every collection present, so that a template can ask {@code contains}.
   *
   * @param form the form, possibly with {@code null} collections
   * @return the same values with empty collections instead of {@code null}
   */
  public static WarningForm normalised(WarningForm form) {
    return new WarningForm(
        form.warningLevel(),
        form.messageSinhala(),
        form.messageTamil(),
        form.messageEnglish(),
        form.instructionsSinhala(),
        form.instructionsTamil(),
        form.instructionsEnglish(),
        form.targetDistricts() == null ? List.of() : form.targetDistricts(),
        form.targetRiverBasins() == null ? List.of() : form.targetRiverBasins(),
        form.channels() == null ? Set.of() : form.channels(),
        form.expiryTime());
  }

  /**
   * The form as it first opens (SC1 step 5): every channel checked, nothing else entered; the
   * officer chooses the level.
   *
   * @return the empty form
   */
  public static WarningForm emptyForm() {
    return new WarningForm(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        List.of(),
        EnumSet.allOf(NotificationChannel.class),
        null);
  }

  /**
   * Turns the posted update panel into the {@code HazardUpdate} of {@code editHazard}.
   *
   * @param form the posted panel
   * @return the update
   */
  public static HazardUpdate toUpdate(HazardUpdateForm form) {
    return new HazardUpdate(blankToNull(form.description()), form.severity(), form.districts());
  }

  /**
   * The update panel pre-filled with the hazard's current values (WF1-S02).
   *
   * @param hazard the hazard's view
   * @return the pre-filled panel
   */
  public static HazardUpdateForm updateFormOf(HazardView hazard) {
    return new HazardUpdateForm(
        hazard.description(), hazard.severityCode(), hazard.districtCodes());
  }

  /**
   * Reads the posted update panel from the request (SC1 E5), because an exception handler cannot
   * bind a model attribute. The request was bound before the control refused it, so its values are
   * valid constants; a blank severity is {@code null}.
   *
   * @param request the request of the refused update
   * @return the panel with the values the officer entered
   */
  public static HazardUpdateForm updateFormOf(HttpServletRequest request) {
    String[] posted = request.getParameterValues("districts");
    List<District> districts =
        posted == null ? List.of() : Arrays.stream(posted).map(District::valueOf).toList();
    String severity = blankToNull(request.getParameter("severity"));
    return new HazardUpdateForm(
        request.getParameter("description"),
        severity == null ? null : HazardSeverity.valueOf(severity),
        districts);
  }

  /**
   * Places each validation error beside its field (WF1-S03a, S03b): the text errors under the
   * language's message or instructions, the area, channel and expiry errors under their fields, and
   * anything else at the top of the form.
   *
   * @param errors the errors of {@code validateWarning}
   * @return the errors by field key, in their order
   */
  public static Map<String, String> fieldErrors(List<String> errors) {
    Map<String, String> byField = new LinkedHashMap<>();
    for (String error : errors) {
      byField.merge(fieldOf(error), error, (first, next) -> first + " " + next);
    }
    return byField;
  }

  /**
   * The level tiles, in order.
   *
   * @return one option per level
   */
  public static List<OptionView> levelOptions() {
    return Arrays.stream(WarningLevel.values())
        .map(level -> new OptionView(level.name(), WarningDashboardMapper.levelLabel(level)))
        .toList();
  }

  /**
   * The 25 district chips, in alphabetical order.
   *
   * @return one option per district
   */
  public static List<OptionView> districtOptions() {
    return Arrays.stream(District.values())
        .map(
            district -> new OptionView(district.name(), WarningDashboardMapper.nameLabel(district)))
        .toList();
  }

  /**
   * The river basins of the pick-list.
   *
   * @return one option per basin
   */
  public static List<OptionView> basinOptions() {
    return Arrays.stream(RiverBasin.values())
        .map(basin -> new OptionView(basin.name(), WarningDashboardMapper.nameLabel(basin)))
        .toList();
  }

  /**
   * The severities of the update panel.
   *
   * @return one option per severity
   */
  public static List<OptionView> severityOptions() {
    return Arrays.stream(HazardSeverity.values())
        .map(severity -> new OptionView(severity.name(), WarningDashboardMapper.label(severity)))
        .toList();
  }

  /**
   * The notification channels, as checkboxes and in the resend pick-list.
   *
   * @return one option per channel
   */
  public static List<OptionView> channelOptions() {
    return Arrays.stream(NotificationChannel.values())
        .map(
            channel -> new OptionView(channel.name(), WarningDashboardMapper.channelLabel(channel)))
        .toList();
  }

  /**
   * The names of districts or river basins as one line ("Ratnapura, Kalutara"), for the form's
   * "Selected area" card and its map.
   *
   * @param items the selected districts or basins
   * @return their names, or "none" if there are none
   */
  public static String names(List<? extends Enum<?>> items) {
    return items.isEmpty()
        ? "none"
        : items.stream()
            .map(WarningDashboardMapper::nameLabel)
            .collect(Collectors.joining(SEPARATOR));
  }

  private static String fieldOf(String error) {
    for (Language language : Language.values()) {
      String name = WarningDashboardMapper.label(language);
      if (error.startsWith(name + " message is required")) {
        return "message" + name;
      }
      if (error.startsWith(name + " ")) {
        return "instructions" + name;
      }
    }
    if (error.startsWith("An affected area")) {
      return AREA_ERROR;
    }
    if (error.contains("notification channel")) {
      return "channels";
    }
    if (error.contains("expiry time")) {
      return "expiryTime";
    }
    return GENERAL_ERROR;
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}
