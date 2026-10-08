package lk.sliit.se3070.group058.disasterlink.warning.web.mapper;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningStatus;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.DispatchView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.HazardView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.PreviewView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.WarningView;

/**
 * Maps the domain to the warning dashboard's view records (WF1-S01 to S05); the forms are mapped by
 * {@link WarningFormMapper}. The mapping from entities runs inside the dashboard's read-only
 * transaction, because the hazard's and the warning's collections are lazy and open-in-view is off.
 *
 * <p>It also holds the screen wording: the labels of the enums ("Level 2 – Amber (Be Alert)",
 * "Nuwara Eliya", "Agency bulletin"), the date format "20 May 2026, 09:30" and the grouping of
 * numbers ("102,549"). Implementation-only class (standards section 4, "No entities on the wire").
 */
public final class WarningDashboardMapper {

  private static final String NONE = "None";

  private static final String DASH = "—";

  private static final String SEPARATOR = ", ";

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.ENGLISH);

  private static final Map<WarningLevel, String> LEVEL_LABELS = levelLabels();

  private static final Map<NotificationChannel, String> CHANNEL_LABELS = channelLabels();

  private static final Map<Language, String> SCRIPT_CLASSES = scriptClasses();

  private WarningDashboardMapper() {
    throw new AssertionError("WarningDashboardMapper is a utility class");
  }

  /**
   * Maps a hazard; call inside a transaction (its districts are lazy).
   *
   * @param hazard the hazard
   * @return the view of the hazard
   */
  public static HazardView toHazardView(Hazard hazard) {
    List<District> districts = List.copyOf(hazard.getDistricts());
    return new HazardView(
        hazard.getHazardId(),
        hazardLabel(hazard),
        label(hazard.getType()),
        hazard.getDescription(),
        hazard.getSeverity() == null ? DASH : label(hazard.getSeverity()),
        hazard.getSeverity(),
        hazard.getLocationName(),
        districts.isEmpty() ? DASH : districtList(districts),
        districtList(districts),
        districts,
        hazard.getRiverBasin().map(WarningDashboardMapper::nameLabel).orElse(DASH),
        label(hazard.getSource()),
        DATE_TIME.format(hazard.getStartTime()),
        label(hazard.getStatus()));
  }

  /**
   * Maps the hazards of the dashboard list; call inside a transaction.
   *
   * @param hazards the hazards
   * @return their views, in the same order
   */
  public static List<HazardView> toHazardViews(List<Hazard> hazards) {
    return hazards.stream().map(WarningDashboardMapper::toHazardView).toList();
  }

  /**
   * Maps a warning; call inside a transaction (its hazard and districts are lazy).
   *
   * @param warning the warning
   * @param now the time its status is judged at
   * @return the view of the warning
   */
  public static WarningView toWarningView(Warning warning, LocalDateTime now) {
    boolean active = warning.statusAt(now) == WarningStatus.ACTIVE;
    return new WarningView(
        warning.getWarningId(),
        hazardLabel(warning.getHazard()),
        levelLabel(warning.getWarningLevel()),
        levelClass(warning.getWarningLevel()),
        label(active ? WarningStatus.ACTIVE : WarningStatus.EXPIRED),
        active,
        districtList(ordered(warning.coveredDistricts(), warning.getTargetDistricts())),
        DATE_TIME.format(warning.getIssuedTime()),
        DATE_TIME.format(warning.getExpiryTime()));
  }

  /**
   * Maps a delivery summary (SC1 steps 17 and 19, E1 16c and 16e).
   *
   * @param summary the summary of the current round
   * @return the dispatch view
   */
  public static DispatchView toDispatch(BroadcastSummary summary) {
    List<DispatchView.ChannelRow> rows = new ArrayList<>();
    List<DispatchView.ChannelFailure> failures = new ArrayList<>();
    int failedTotal = 0;
    for (NotificationChannel channel : NotificationChannel.values()) {
      int failed = summary.failedRecipientIds().getOrDefault(channel, List.of()).size();
      failedTotal += failed;
      String label = CHANNEL_LABELS.get(channel);
      rows.add(
          new DispatchView.ChannelRow(
              label, failed == 0 ? NONE : number(failed) + " recipients", failed > 0));
      if (failed > 0) {
        failures.add(
            new DispatchView.ChannelFailure(
                channel.name(), label + " delivery failed for " + number(failed) + " recipients"));
      }
    }
    int unreachable = summary.unreachableRecipientIds().size();
    return new DispatchView(
        number(summary.deliveredCount()),
        number(failedTotal),
        failedTotal > 0,
        rows,
        failures,
        unreachable == 0 ? NONE : number(unreachable));
  }

  /**
   * Maps a preview (SC1 step 11).
   *
   * @param preview the preview from {@code previewWarning}
   * @param targetDistricts the districts the officer chose, listed first
   * @return the preview view
   */
  public static PreviewView toPreview(WarningPreview preview, List<District> targetDistricts) {
    List<District> covered = ordered(preview.coveredDistricts(), targetDistricts);
    List<PreviewView.DistrictCount> recipients = new ArrayList<>();
    int total = 0;
    for (District district : covered) {
      int citizens = preview.citizensPerDistrict().getOrDefault(district, 0);
      total += citizens;
      recipients.add(new PreviewView.DistrictCount(nameLabel(district), number(citizens)));
    }
    List<PreviewView.LanguageText> texts = new ArrayList<>();
    for (Language language : Language.values()) {
      texts.add(
          new PreviewView.LanguageText(
              label(language),
              SCRIPT_CLASSES.get(language),
              preview.message().textIn(language),
              preview.instructions().textIn(language)));
    }
    return new PreviewView(
        label(preview.hazardType()),
        levelLabel(preview.warningLevel()),
        levelClass(preview.warningLevel()),
        districtList(covered),
        districtList(covered),
        DATE_TIME.format(preview.expiryTime()),
        texts,
        channelLabels(preview.channels()),
        recipients,
        number(total),
        number(preview.organizationCount()),
        number(preview.districtOfficerCount()));
  }

  /**
   * The label of a channel ("SMS", "Audible alert").
   *
   * @param channel the channel
   * @return its label
   */
  public static String channelLabel(NotificationChannel channel) {
    return CHANNEL_LABELS.get(channel);
  }

  /**
   * The level label on the NBRO scale (MD section 1, PD1).
   *
   * @param level the level
   * @return for example "Level 2 – Amber (Be Alert)"
   */
  public static String levelLabel(WarningLevel level) {
    return LEVEL_LABELS.get(level);
  }

  private static String levelClass(WarningLevel level) {
    return "tag--l" + (level.ordinal() + 1);
  }

  private static String hazardLabel(Hazard hazard) {
    return label(hazard.getType()) + " — " + hazard.getLocationName();
  }

  private static String districtList(List<District> districts) {
    return districts.stream()
        .map(WarningDashboardMapper::nameLabel)
        .collect(Collectors.joining(SEPARATOR));
  }

  /**
   * The covered districts in the order the officer reads them: the chosen districts first, then
   * those a river basin adds ("Ratnapura, Kalutara" for Ratnapura and the Kalu basin).
   */
  private static List<District> ordered(Set<District> covered, List<District> first) {
    Set<District> ordered = new LinkedHashSet<>();
    first.stream().filter(covered::contains).forEach(ordered::add);
    covered.stream().sorted().forEach(ordered::add);
    return List.copyOf(ordered);
  }

  /** "AGENCY_BULLETIN" becomes "Agency bulletin". */
  static String label(Enum<?> constant) {
    String words = constant.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    return Character.toUpperCase(words.charAt(0)) + words.substring(1);
  }

  /** "NUWARA_ELIYA" becomes "Nuwara Eliya", the name the district map also uses. */
  static String nameLabel(Enum<?> constant) {
    return Arrays.stream(constant.name().split("_"))
        .map(word -> word.charAt(0) + word.substring(1).toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(" "));
  }

  private static String number(int value) {
    return NumberFormat.getIntegerInstance(Locale.ENGLISH).format(value);
  }

  private static Map<WarningLevel, String> levelLabels() {
    Map<WarningLevel, String> labels = new EnumMap<>(WarningLevel.class);
    labels.put(WarningLevel.LEVEL_1_YELLOW, "Level 1 – Yellow (Remain Vigilant)");
    labels.put(WarningLevel.LEVEL_2_AMBER, "Level 2 – Amber (Be Alert)");
    labels.put(WarningLevel.LEVEL_3_RED, "Level 3 – Red (Evacuate)");
    return labels;
  }

  private static Map<NotificationChannel, String> channelLabels() {
    Map<NotificationChannel, String> labels = new EnumMap<>(NotificationChannel.class);
    labels.put(NotificationChannel.PUSH, "Push");
    labels.put(NotificationChannel.SMS, "SMS");
    labels.put(NotificationChannel.AUDIBLE_ALERT, "Audible alert");
    return labels;
  }

  private static List<String> channelLabels(Set<NotificationChannel> channels) {
    return Arrays.stream(NotificationChannel.values())
        .filter(channels::contains)
        .map(CHANNEL_LABELS::get)
        .toList();
  }

  private static Map<Language, String> scriptClasses() {
    Map<Language, String> classes = new EnumMap<>(Language.class);
    classes.put(Language.SINHALA, "si");
    classes.put(Language.TAMIL, "ta");
    classes.put(Language.ENGLISH, "");
    return classes;
  }
}
