package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.Arrays;
import java.util.Set;

/**
 * The principal river basins that warnings can name, each with the districts it lies in.
 *
 * <p>Implements CH-17. The basin-to-district table is a Group_058 assumption, compiled from the
 * sources named above each literal; a district is listed only where a source shows a substantial
 * part of the basin in it. The Irrigation Department issues flood warnings per river, which is why
 * a warning can target a basin.
 */
public enum RiverBasin {
  // Natural Resource Profile of the Kelani River Basin, section 2.1 and Table 1 (UNICEF Sri Lanka):
  // https://www.unicef.org/srilanka/media/301/file/Natural%20Resource%20Profile%20of%20the%20kelani%20river%20Basin.pdf
  // Kandy and Kalutara (two divisional secretariats each) are left out as marginal.
  KELANI(
      District.COLOMBO,
      District.GAMPAHA,
      District.KEGALLE,
      District.NUWARA_ELIYA,
      District.RATNAPURA),

  // Fixed by scenario SC1: "the Kalu river basin (covered districts Ratnapura and Kalutara)".
  KALU(District.RATNAPURA, District.KALUTARA),

  // Catchment Protection of Gin Ganga (River) as part of the Water Safety Plan, University of
  // Moratuwa repository: https://dl.lib.uom.lk/items/035ee3c9-6dc1-40e9-aa50-dafdcd45f6b0
  // The source also names Ratnapura and Kalutara but gives no share; they are left out.
  GIN(District.GALLE, District.MATARA),

  // Ratnayake, Sachindra and Nandalal, "Rainfall Forecasting for Flood Prediction in the Nilwala
  // Basin", ICSBE 2010 ("nearly 90 per cent of the area ... belongs to the Matara District"):
  // http://www.civil.mrt.ac.lk/web/conference/ICSBE_2010/vol_02/53.pdf
  NILWALA(District.MATARA),

  // Irrigation Department, "Delta Related Interventions in Sri Lanka", Mahaweli basin slide (nine
  // administrative districts):
  // https://gwpo-gwp.org/assets/legacy/globalassets/global/gwp-sas_images/gwp-sas-in-action/ldai/delta-related-interventions-in-sri-lanka-final.pdf
  MAHAWELI(
      District.BADULLA,
      District.MONARAGALA,
      District.MATALE,
      District.AMPARA,
      District.POLONNARUWA,
      District.TRINCOMALEE,
      District.ANURADHAPURA,
      District.NUWARA_ELIYA,
      District.KANDY),

  // "Availability of water resources within the Attanagalla oya basin - a review", University of
  // Peradeniya repository ("Main administrative district within the basin is Gampaha"):
  // https://ir.lib.pdn.ac.lk/handle/20.500.14444/454
  ATTANAGALU(District.GAMPAHA),

  // Molle, Jayakody and de Silva, "Anicut Systems in Sri Lanka: The Case of the Upper Walawe River
  // Basin", IWMI Working Paper 61 (the basin spreads over four districts):
  // https://horizon.documentation.ird.fr/exl-doc/pleins_textes/divers15-08/010048692.pdf
  WALAWE(District.RATNAPURA, District.BADULLA, District.MONARAGALA, District.HAMBANTOTA),

  // IWMI Research Group, "Water Resources Management in a River Basin: Institutional Gaps and
  // Implications - Case Study of Deduru Oya River Basin", Economic Review 2001 (88 % Kurunegala,
  // 9 % Puttalam): https://dl.nsf.gov.lk/bitstreams/98582bac-3a6c-4ab3-89eb-3e4db7d7d0ee/download
  // Kandy and Matale (3 % together) are left out as marginal.
  DEDURU(District.KURUNEGALA, District.PUTTALAM),

  // Central Environmental Authority, Ma Oya water quality page (Kegalle, Kurunegala, Gampaha and a
  // small portion of Puttalam): https://www.cea.lk/web/en/water?id=158
  // Puttalam (2 % of the district) is left out as a small portion.
  MAHA_OYA(District.KEGALLE, District.KURUNEGALA, District.GAMPAHA);

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
