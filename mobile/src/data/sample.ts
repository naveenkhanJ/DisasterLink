/**
 * sample.ts — the hard-coded sample data of the UI prototype (P89), taken from scenario SC2 and the
 * frames-WF2.md storyline. Replace with the real state and API replies when the screens are wired.
 *
 * The Sinhala and Tamil texts are drafts that still need a native speaker's check (wireframe-spec §10).
 */
export type Language = 'Sinhala' | 'Tamil' | 'English';

export const hazardLabel = 'Flood — Kalu Ganga at Ratnapura';

export const activeWarning = {
  level: 'Level 2 – Amber (Be Alert)',
  districts: 'Ratnapura, Kalutara',
  validUntil: '10 Oct 2026, 09:45',
  /** W-L2-MSG and W-L2-INS per language (frames-WF1.md Part 0). */
  text: {
    Sinhala: {
      message: 'DMC ගංවතුර අවවාදය - මට්ටම 2 ඇම්බර්: රත්නපුර සහ කළුතර දිස්ත්‍රික්කවල කළු ගඟ ඉහළ යයි. පහත් බිම් හැර යාමට සූදානම් වන්න.',
      instructions: 'ලේඛන, වටිනා දෑ උසට ගෙනයන්න. ඉවත් වන්න කීවොත් ග්‍රාම නිලධාරී කී ආරක්ෂිත ස්ථානයට යන්න.',
    },
    Tamil: {
      message: 'DMC வெள்ள எச்சரிக்கை - நிலை 2 அம்பர்: இரத்தினபுரி மற்றும் களுத்துறையில் களு கங்கை உயர்கிறது. தாழ்நிலத்திலிருந்து வெளியேறத் தயாராகவும்.',
      instructions: 'கூறினால் கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் செல்லவும்.',
    },
    English: {
      message: 'DMC FLOOD WARNING - Level 2 Amber: Kalu Ganga rising in Ratnapura and Kalutara. Prepare to evacuate low-lying areas.',
      instructions: 'Move documents and valuables to higher ground. If told to evacuate, go to the safety centre named by your Grama Niladhari.',
    },
  } satisfies Record<Language, { message: string; instructions: string }>,
};

export const location = { text: '6.5888 N, 79.9640 E', source: 'GPS · accuracy ±8 m' };

export const draft = {
  hazardType: 'Flood',
  capturedAt: '9 Oct 2026, 20:35',
  description:
    'Kalu Ganga water has breached the sandbag barriers near the Kalutara bridge. Flooding is now moving into the service road. Heavy continuous rainfall still active.',
  descriptionLimit: 500,
};

export const report = {
  id: 'RPT-2026-KLT-0847',
  decidedAt: '9 Oct 2026, 20:41',
  /** Text N-1 (draft — native check required); English source: SC2 step 30. */
  decisionNoticeSinhala: 'ඔබේ වාර්තාව RPT-2026-KLT-0847 ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් තහවුරු කරන ලදී (2026 ඔක්තෝබර් 9, 20:41)',
};

export const hazardTypes = [
  { label: 'Flood', icon: 'flood' },
  { label: 'Landslide', icon: 'landslide' },
  { label: 'Cyclone', icon: 'cyclone' },
  { label: 'Tsunami', icon: 'tsunami' },
  { label: 'Drought', icon: 'drought' },
  { label: 'Blocked road', icon: 'road-blocked' },
  { label: 'Other', icon: 'other' },
] as const;
