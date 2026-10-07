package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;

/**
 * The text of a report decision notice in each language, shared by the two mock gateways. The
 * wording is Group_058's; the Sinhala and Tamil sentences await a native-speaker check.
 *
 * <p>In a pattern {@code {0}} is the report identifier and {@code {1}} the rejection reason.
 *
 * <p>Implements CH-26 (model dictionary section 2, {@code ReportDecisionNotice}; P62 FX-13).
 */
final class DecisionNoticeText {

  private static final Map<ReportStatus, Map<Language, String>> PATTERNS = patterns();

  private DecisionNoticeText() {
    throw new AssertionError("DecisionNoticeText is a utility class");
  }

  /**
   * Renders a notice.
   *
   * @param language the recipient's preferred language
   * @param status the decision: {@code VERIFIED} or {@code REJECTED}
   * @param reportId the identifier of the report
   * @param reason the rejection reason, or {@code null} for a verified report
   * @return the text of the notice
   * @throws IllegalArgumentException if the status is not a decision
   */
  static String render(Language language, ReportStatus status, String reportId, String reason) {
    Objects.requireNonNull(language, "language");
    Objects.requireNonNull(status, "status");
    Map<Language, String> byLanguage = PATTERNS.get(status);
    if (byLanguage == null) {
      throw new IllegalArgumentException("a decision notice needs a decision, not " + status);
    }
    return MessageFormat.format(
        byLanguage.get(language), reportId, Objects.requireNonNullElse(reason, ""));
  }

  private static Map<ReportStatus, Map<Language, String>> patterns() {
    Map<Language, String> verified = new EnumMap<>(Language.class);
    verified.put(Language.ENGLISH, "Your hazard report {0} was verified. Thank you.");
    verified.put(Language.SINHALA, "ඔබගේ උපද්‍රව වාර්තාව {0} තහවුරු කරන ලදී. ස්තූතියි.");
    verified.put(Language.TAMIL, "உங்கள் அபாய அறிக்கை {0} உறுதிப்படுத்தப்பட்டது. நன்றி.");
    Map<Language, String> rejected = new EnumMap<>(Language.class);
    rejected.put(Language.ENGLISH, "Your hazard report {0} was not accepted: {1}");
    rejected.put(Language.SINHALA, "ඔබගේ උපද්‍රව වාර්තාව {0} පිළිගනු නොලැබීය: {1}");
    rejected.put(Language.TAMIL, "உங்கள் அபாய அறிக்கை {0} ஏற்றுக்கொள்ளப்படவில்லை: {1}");
    Map<ReportStatus, Map<Language, String>> patterns = new EnumMap<>(ReportStatus.class);
    patterns.put(ReportStatus.VERIFIED, verified);
    patterns.put(ReportStatus.REJECTED, rejected);
    return patterns;
  }
}
