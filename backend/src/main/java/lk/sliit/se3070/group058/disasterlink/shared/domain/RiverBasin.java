package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Arrays;
import java.util.Set;

/**
 * The principal river basins that warnings can name, each with the districts it lies in.
 *
 * <p>Implements CH-17. The basin-to-district table is a Group_058 assumption. A basin's districts
 * are the districts that contain the Divisional Secretariat divisions the Irrigation Department
 * names in its flood warnings for that basin, not the whole catchment. The sources named above each
 * literal are news reports that quote the Irrigation Department's warnings. The Irrigation
 * Department issues flood warnings per river, which is why a warning can target a basin.
 */
public enum RiverBasin {
  // https://www.adaderana.lk/news.php?nid=115247
  KELANI(
      District.COLOMBO,
      District.GAMPAHA,
      District.KEGALLE,
      District.NUWARA_ELIYA,
      District.RATNAPURA),

  // Fixed by scenario SC1: "the Kalu river basin (covered districts Ratnapura and Kalutara)".
  // https://english.newsfirst.lk/2026/05/23/kalu-ganga-reaches-minor-flood-level-flood-warning-extended-in-kalutara-district
  // https://www.adaderana.lk/news/122819/minor-flood-warnings-issued-for-several-areas-of-kalu-ganga-basin
  KALU(District.RATNAPURA, District.KALUTARA),

  // https://www.adaderana.lk/news/93720/amber-flood-warning-issued-to-low-lying-areas-of-3-rivers-extended
  GIN(District.GALLE),

  // https://www.adaderana.lk/news/93720/amber-flood-warning-issued-to-low-lying-areas-of-3-rivers-extended
  NILWALA(District.MATARA),

  // https://www.adaderana.lk/news/115226/heavy-rains-trigger-major-flood-alert-in-mahaweli-river-basin
  // https://hirunews.lk/english/481135/major-flood-risk-to-mahaweli-river-basin-emergency-red-notice-to-10-divisional-secretariat-divisions
  MAHAWELI(
      District.KANDY,
      District.NUWARA_ELIYA,
      District.MATALE,
      District.BADULLA,
      District.AMPARA,
      District.POLONNARUWA,
      District.TRINCOMALEE),

  // https://www.adaderana.lk/news/122520/minor-flood-risk-issued-for-low-lying-areas-along-attanagalu-oya-and-uruwal-oya
  ATTANAGALU(District.GAMPAHA),

  // Molle, Jayakody and de Silva, "Anicut Systems in Sri Lanka: The Case of the Upper Walawe River
  // Basin", IWMI Working Paper 61 (the basin spreads over four districts):
  // https://horizon.documentation.ird.fr/exl-doc/pleins_textes/divers15-08/010048692.pdf
  // Group_058 assumption: no warning listing the basin's divisions was found, so the catchment
  // districts are kept.
  WALAWE(District.RATNAPURA, District.BADULLA, District.MONARAGALA, District.HAMBANTOTA),

  // https://www.adaderana.lk/news/115216/deduru-oya-basin-on-red-alert-flood-warning-issued-following-heavy-rainfall
  DEDURU(District.KURUNEGALA, District.PUTTALAM),

  // https://www.defence.lk/Article/view_article/4124
  MAHA_OYA(District.KURUNEGALA, District.GAMPAHA, District.PUTTALAM);

  private final Set<District> districts;

  RiverBasin(District... districts) {
    this.districts = Set.copyOf(Arrays.asList(districts));
  }

  /**
   * Returns the districts this basin lies in.
   *
   * @return an unmodifiable set of the districts of the basin
   */
  public Set<District> districts() {
    return districts;
  }
}
