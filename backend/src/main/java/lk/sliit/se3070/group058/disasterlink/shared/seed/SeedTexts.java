package lk.sliit.se3070.group058.disasterlink.shared.seed;

import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;

/**
 * The warning texts of the seed in the three languages. The English texts are those of the
 * scenarios (SC1 step 9 for Level 2, Escalate Warning step 7 for Level 3); the Sinhala and Tamil
 * texts are the drafts of frames-WF1 (W-L2-MSG, W-L2-INS, W-L3-MSG, W-L3-INS, Part 0). Group_058
 * draft: native check pending (Afham's open item). The Tamil texts are the condensed drafts that
 * fit the SMS limit of 201 characters.
 */
final class SeedTexts {

  /**
   * SC1 step 9: Level 2 message; Sinhala and Tamil from frames-WF1 W-L2-MSG (Group_058 draft,
   * native check pending).
   */
  static final LocalisedText LEVEL_2_MESSAGE =
      new LocalisedText(
          "DMC ගංවතුර අවවාදය - මට්ටම 2 ඇම්බර්: රත්නපුර සහ කළුතර දිස්ත්‍රික්කවල කළු ගඟ ඉහළ යයි. "
              + "පහත් බිම් හැර යාමට සූදානම් වන්න.",
          "DMC வெள்ள எச்சரிக்கை - நிலை 2 அம்பர்: இரத்தினபுரி மற்றும் களுத்துறையில் களு கங்கை "
              + "உயர்கிறது. தாழ்நிலத்திலிருந்து வெளியேறத் தயாராகவும்.",
          "DMC FLOOD WARNING - Level 2 Amber: Kalu Ganga rising in Ratnapura and Kalutara. "
              + "Prepare to evacuate low-lying areas.");

  /**
   * SC1 step 9: Level 2 instructions; Sinhala and Tamil from frames-WF1 W-L2-INS (Group_058 draft,
   * native check pending).
   */
  static final LocalisedText LEVEL_2_INSTRUCTIONS =
      new LocalisedText(
          "ලේඛන, වටිනා දෑ උසට ගෙනයන්න. ඉවත් වන්න කීවොත් ග්‍රාම නිලධාරී කී ආරක්ෂිත ස්ථානයට යන්න.",
          "கூறினால் கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் செல்லவும்.",
          "Move documents and valuables to higher ground. If told to evacuate, go to the safety "
              + "centre named by your Grama Niladhari.");

  /**
   * Escalate Warning step 7: Level 3 message; Sinhala and Tamil from frames-WF1 W-L3-MSG (Group_058
   * draft, native check pending).
   */
  static final LocalisedText LEVEL_3_MESSAGE =
      new LocalisedText(
          "DMC ගංවතුර අවවාදය - මට්ටම 3 රතු: රත්නපුර සහ කළුතර දිස්ත්‍රික්කවල කළු ගඟ මහා ගංවතුර "
              + "මට්ටම ඉක්මවයි. පහත් බිම් හැරයන්න.",
          "DMC வெள்ள எச்சரிக்கை - நிலை 3 சிவப்பு: இரத்தினபுரி மற்றும் களுத்துறையில் களு கங்கை "
              + "பெருவெள்ளம். தாழ்நிலத்திலிருந்து உடனே வெளியேறவும்.",
          "DMC FLOOD WARNING - Level 3 Red: Kalu Ganga above major flood level in Ratnapura and "
              + "Kalutara. Evacuate low-lying areas now.");

  /**
   * Escalate Warning step 7: Level 3 instructions; Sinhala and Tamil from frames-WF1 W-L3-INS
   * (Group_058 draft, native check pending).
   */
  static final LocalisedText LEVEL_3_INSTRUCTIONS =
      new LocalisedText(
          "ග්‍රාම නිලධාරී කී ආරක්ෂිත ස්ථානයට දැන් යන්න. ලේඛන, ඖෂධ, පැන් ගන්න. ගංවතුරේ නොබසින්න.",
          "கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் இப்போதே செல்லவும்.",
          "Leave now for the safety centre named by your Grama Niladhari. Take documents, "
              + "medicines and drinking water. Do not cross flood water.");

  private SeedTexts() {
    throw new AssertionError("SeedTexts is a utility class");
  }
}
