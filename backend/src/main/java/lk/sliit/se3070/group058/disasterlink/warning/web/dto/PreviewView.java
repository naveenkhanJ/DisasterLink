package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import java.util.List;

/**
 * The warning preview (WF1-S04), from a {@code WarningPreview}. Implementation-only view record.
 *
 * @param hazardType the hazard type label
 * @param levelLabel the level label
 * @param levelClass the CSS modifier of the level tag
 * @param coveredDistricts the covered districts as text
 * @param mapDistricts the covered districts as the map's {@code data-selected} text
 * @param expires the expiry time
 * @param texts the message and instructions per language
 * @param channels the channel labels
 * @param recipients the citizens per covered district
 * @param totalCitizens all citizens, for example "42,335"
 * @param organizations the response organisations
 * @param districtOfficers the district officers (0 on an issue broadcast)
 */
public record PreviewView(
    String hazardType,
    String levelLabel,
    String levelClass,
    String coveredDistricts,
    String mapDistricts,
    String expires,
    List<LanguageText> texts,
    List<String> channels,
    List<DistrictCount> recipients,
    String totalCitizens,
    String organizations,
    String districtOfficers) {

  /**
   * The message and instructions in one language.
   *
   * @param language the language label, for example "Sinhala"
   * @param scriptClass the CSS class of the script font, {@code si}, {@code ta} or empty
   * @param message the message
   * @param instructions the instructions
   */
  public record LanguageText(
      String language, String scriptClass, String message, String instructions) {}

  /**
   * The citizens of one covered district.
   *
   * @param district the district label
   * @param citizens the count, for example "18,420"
   */
  public record DistrictCount(String district, String citizens) {}
}
