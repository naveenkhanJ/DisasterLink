# Frames WF1 — UC1 Issue Hazard Warning and WF1b — Escalate Warning
Status: v2 + P87 content review (2026-10-09) — after independent review (2026-10-09); aligned to wireframe-spec §2.1 (canonical shell, grid and tokens). Draft by Claude (frames writer WF1/WF1b). Sources: `wireframes/wireframe-spec.md` (all, §1–§10, incl. §2.1); `scenarios/SC1-issue-hazard-warning.md` (all, incl. Appendix 2); `scenarios/SC-escalate-warning.md` (all, incl. Appendix 2); `model-dictionary.md` v0.9 §0 (conventions 10–11), §1 (WarningLevel, HazardType, HazardSeverity, HazardSource, WarningStatus, NotificationChannel, District, RiverBasin), §2 (WarningDetails, WarningPreview, BroadcastSummary, HazardUpdate, HazardDetailsView, EscalationOptions, EscalationDetails, LocalisedText), §3 (Hazard, Warning), §4 (WarningDashboardUI, EscalationUI rows), §5 (WarningController, EscalationController, WarningBroadcaster rows), association A1; `change-list.md` rows CH-06, CH-21, CH-24 and decision 8-2; `storyboard-corrections.md` SB1; Received Design Record `uc1-issue-and-broadcast-disaster-warning.md` §5 (hi-fi p15_1–p19_1); `critique-findings.md` D2-11, D2-13 … D2-19; `implementation-standards.md` §1 (map/charts, wireframes, not built), §4 (C1 banner), §11 (seed data).

This file holds two wireframe sets: **Part 1 — WF1** (5 screens + 8 states) with the common banner **COMMON-B1**, and **Part 2 — WF1b** (4 screens + 4 states). The three-language texts are in one table (Part 0), and the gaps for both sets are at the end.

**Text conventions used below.**
- Text in "quotes" is the exact on-screen text. "(wording ours)" marks text the scenario does not give.
- Levels: "Level 1 – Yellow (Remain Vigilant)", "Level 2 – Amber (Be Alert)", "Level 3 – Red (Evacuate)" (en dash), always written out in a **tag** (wf token; Level 3 with the 2 px border) or, in the ui theme, a tag with the level colour plus the text.
- Dates and times per §2.1: dates "20 May 2026", date-times "20 May 2026, 10:45", times alone "10:45"; numbers with thousands separators.
- Hazard label "Flood — Kalu Ganga at Ratnapura" (type — `locationName`, MD §0.11 / FX-14).

---

# Part 0 — Texts in three languages (final drafts)

English is the scenarios' text, verbatim. Sinhala and Tamil are Claude's drafts, **every one (draft — native check required)**; WF2 copies these verbatim by ID. Counts are Unicode code points (UCS-2 units), as `WarningTextValidator` counts them; an SMS carries message + one line break + instructions, so a language's **counter = message + 1 + instructions**.

**Counter rule (decision, review v2):** each frame's per-language counter shows the *actual* count of the text drawn on that frame. The scenarios' Sinhala and Tamil counts (188, 197, 192, 199, 236, 228) are estimates, because the scenarios do not give those texts; the frames show the drafted texts' counts instead.

| ID | Language | Text | Chars |
|---|---|---|---|
| W-L2-MSG | English | DMC FLOOD WARNING - Level 2 Amber: Kalu Ganga rising in Ratnapura and Kalutara. Prepare to evacuate low-lying areas. | 116 |
| W-L2-INS | English | Move documents and valuables to higher ground. If told to evacuate, go to the safety centre named by your Grama Niladhari. | 122 |
| W-L2-MSG | Sinhala | DMC ගංවතුර අවවාදය - මට්ටම 2 ඇම්බර්: රත්නපුර සහ කළුතර දිස්ත්‍රික්කවල කළු ගඟ ඉහළ යයි. පහත් බිම් හැර යාමට සූදානම් වන්න. **(draft — native check required)** | 116 |
| W-L2-INS | Sinhala | ලේඛන, වටිනා දෑ උසට ගෙනයන්න. ඉවත් වන්න කීවොත් ග්‍රාම නිලධාරී කී ආරක්ෂිත ස්ථානයට යන්න. **(draft — native check required)** | 84 |
| W-L2-MSG | Tamil | DMC வெள்ள எச்சரிக்கை - நிலை 2 அம்பர்: இரத்தினபுரி மற்றும் களுத்துறையில் களு கங்கை உயர்கிறது. தாழ்நிலத்திலிருந்து வெளியேறத் தயாராகவும். **(condensed draft — native check required)** | 134 |
| W-L2-INS | Tamil | கூறினால் கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் செல்லவும். **(condensed draft — native check required)** | 65 |
| W-L2-INS-TA-E3 | Tamil (E3 error variant, SC1 E3 11d — longer instructions the officer later shortens; over the limit on purpose) | ஆவணங்களை மேலே எடுக்கவும். வெளியேறக் கூறினால் கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் செல்லவும். **(draft — native check required)** | 101 |
| W-L3-MSG | English | DMC FLOOD WARNING - Level 3 Red: Kalu Ganga above major flood level in Ratnapura and Kalutara. Evacuate low-lying areas now. | 124 |
| W-L3-INS | English | Leave now for the safety centre named by your Grama Niladhari. Take documents, medicines and drinking water. Do not cross flood water. | 134 |
| W-L3-MSG | Sinhala | DMC ගංවතුර අවවාදය - මට්ටම 3 රතු: රත්නපුර සහ කළුතර දිස්ත්‍රික්කවල කළු ගඟ මහා ගංවතුර මට්ටම ඉක්මවයි. පහත් බිම් හැරයන්න. **(draft — native check required)** | 116 |
| W-L3-INS | Sinhala | ග්‍රාම නිලධාරී කී ආරක්ෂිත ස්ථානයට දැන් යන්න. ලේඛන, ඖෂධ, පැන් ගන්න. ගංවතුරේ නොබසින්න. **(draft — native check required)** | 84 |
| W-L3-MSG | Tamil | DMC வெள்ள எச்சரிக்கை - நிலை 3 சிவப்பு: இரத்தினபுரி மற்றும் களுத்துறையில் களு கங்கை பெருவெள்ளம். தாழ்நிலத்திலிருந்து உடனே வெளியேறவும். **(condensed draft — native check required)** | 133 |
| W-L3-INS | Tamil | கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் இப்போதே செல்லவும். **(condensed draft — native check required)** | 64 |
| W-L3-INS-TA-E3 | Tamil (E3 error variant, Escalate E3 8a; over the limit on purpose) | கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் இப்போதே செல்லவும். வெள்ளநீரைக் கடக்கவே வேண்டாம். **(draft — native check required)** | 94 |
| (E3 variants) | Sinhala | Sinhala instructions **empty** in both error states (SC1 E3 11d; Escalate E3 8a) | 0 |

**Counters (message + 1 + instructions), limit 201 for Sinhala and Tamil, 459 for English:**

| Text set | Sinhala | Tamil | English |
|---|---|---|---|
| Level 2 (W-L2) | 201 / 201 | 200 / 201 | 239 / 459 |
| Level 2, E3 variant (W-L2-MSG + empty SI instructions; W-L2-MSG + W-L2-INS-TA-E3) | 116 / 201 (message alone) | 236 / 201 (error, on purpose) | 239 / 459 |
| Level 3 (W-L3) | 201 / 201 | 198 / 201 | 259 / 459 |
| Level 3, E3 variant (W-L3-MSG + empty SI instructions; W-L3-MSG + W-L3-INS-TA-E3) | 116 / 201 (message alone) | 228 / 201 (error, on purpose) | 259 / 459 |

All main-flow texts fit their limits. The Tamil Level 2 and Level 3 texts are **condensed** to fit 201 (decision on Gap 6; the dropped details are listed in Gap 6); the two E3 variants stay over the limit on purpose, to illustrate the error, and their totals equal the scenarios' figures (236, 228). Notes on the drafts: Sinhala "පැන්" (water, for drinking) and the joined "ගෙනයන්න" / "හැරයන්න" were chosen to stay within 201; Tamil uses the polite "-வும்" imperative throughout and "கிராம உத்தியோகத்தர்" for Grama Niladhari; "DMC" stays in Latin script; the "now" of the Sinhala Level 3 message is carried by "දැන් යන්න" in its instructions (one SMS).

---

# Part 1 — WF1 UC1 Issue Hazard Warning (scenario SC1)

## Shell

One shell for every WF1 frame (desktop **1440×1024**, no browser chrome).

- **Sidebar** (left, 240 px, full height): top: "DMC" badge + "DMC Sri Lanka". Navigation items (wireframe-spec §3, DMC Officer):
  1. "Warnings" (warning-triangle icon) — **active** on every WF1 frame; triggers `WarningDashboardUI.openDashboard()`.
  2. "Analytics" (chart icon) — triggers `AnalyticsUI.openAnalytics()` (WF4).
  3. "Hazards" (hazard icon — cloud with lightning, P98; Record and Monitor Hazard, CH-08) and 4. "Operational picture" (pin icon; View Combined Operational Picture, CH-07) — signposted (muted, tag "Not in this version"; a click shows "<item> — not available in this version" — wireframe-spec §8 rule 12, P91).
  No badge counts, no other items.
- **Header** (§2.1: **80 px** high across the remaining 1200 px): left: page title (24/32 bold) and a one-line subtitle (14 px; given per frame); right: initials avatar "NJ", name "N. Jayasinghe", role "DMC Officer" (SC1 main flow). No photo, no log-out, no settings.
- **Content area** (§2.1): starts at x = 240, y = 80, 24 px padding, usable **1152 × 896 px**; 16 px gaps. Standard splits: two-column **744 + 16 + 392**, three-column **373 + 16 + 374 + 16 + 373**, halves **568 + 16 + 568**; any other split given below adds up to 1152. Every frame fits 896 px — no frame shows a scrolled page. The common error banner (COMMON-B1, 48 px strip) sits at the top of the content area when shown and reduces the usable height by 48 + 16 px.
- **Type** (§2.1): section titles 18/24 bold, body 14/20, labels 12/16 semibold, big numbers 28/36 bold.
- **wf tokens used by name below** (§2.1): **card** (white, 1 px #BDBDBD, radius 12); **primary** / **secondary** / **disabled** button; **selected** (chip, option, tile or list row: 2 px black border, check icon, bold text); **tag** (status or level, 1 px black border, full text; Level 3 – Red with 2 px); **field error** (2 px black field border, error icon, bold message below the field); **error box** (form-level: white card, 2 px border, error icon, list or text); **notice** (#F2F2F2 fill, 1 px border, the matching icon — info, warning-triangle or check); **modal** (centred white card, 2 px border, over a 40 % black scrim); **map** (districts white with #BDBDBD 0.8 px borders; selected districts #BDBDBD fill with a 45° hatch).
- **Icons** only from §2.1: warning-triangle (Warnings nav, alerts, notices), chart, report, error (every field and form error), info, check, cross, plus, chevron-right (pick-lists), clock (date-time fields).

## Screens

### WF1-S01 Warning dashboard — assessable hazards
- Frame: desktop 1440×1024
- Shows: scenario SC1 steps 1–3 ; boundary `WarningDashboardUI` ; displays `showHazards(hazards : List<Hazard>)` ; reached by `openDashboard()` (nav "Warnings")
- Corrects received screen: p18_1 (p. 18) "Emergency Command Dashboard" (low-fi UC01-01), and replaces p15_1 (p. 15) "Active Warnings Registry"; answers CH-24, CH-22 (SB1-2), D2-14, D2-13, D2-17, D2-18
- Layout (top to bottom, left to right):
  1. Header — title "Warning Dashboard" (wording ours); subtitle "Hazards that can be assessed for a warning" (wording ours).
  2. Left **card** (744 px wide, 520 px high) — heading "Assessable hazards" (wording ours); sub-line "Active hazards from agency bulletins or verified citizen reports" (wording ours). Table, column headers: "Hazard" | "Severity" | "Districts" | "Source" | "Start time". One row (the only hazard SC1 gives — Gap 1, resolved):
     - "Flood — Kalu Ganga at Ratnapura" (with "HZ-2026-0142" under it as a label, 12/16, #757575) | **tag** "Medium" | "Ratnapura, Kalutara" | "Agency bulletin" | "20 May 2026, 09:30"
     The whole row is the selectable target.
  3. Right **card** (392 px wide, 520 px high) — heading "Hazard districts" (wording ours); district map (see Map); under it one legend line: a selected-district swatch (**map** token) + "Flood — Kalu Ganga at Ratnapura".
  (Widths 744 + 16 + 392 = 1152; height 520 of 896.)
- Controls:
  - Hazard row — enabled; triggers `selectHazard("HZ-2026-0142")`.
  - Nav "Warnings" (selected), "Analytics" — as in the Shell.
- Map / chart / image: map of the 25 districts; **Ratnapura and Kalutara** as selected districts (**map** token; ui: map fill colour); no points or labels on the map. Display only.
- Not shown (removed on purpose): KPI cards "Active Warnings 03", "Pending Reports 12", "Shelters Open 45", "Teams Deployed 18" (no display behind them — CH-24, D2-18); "Issue New Hazard Warning →" (a warning is issued for a selected hazard — D2-14, CH-24); "Live Severity Monitoring" cards with RED/AMBER badges (undefined scale, D2-13); "Real-Time Operational Activity Log" (no flow); floating map labels "KEGALLE - LANDSLIDE", "RATNAPURA - FLOOD", "GALLE - TSUNAMI ADVISORY" (D2-18); the whole p15_1 registry with its filters, "View Log", "Manage", "Expired"/"Cancelled" (no flow, D2-17); nav items "Dashboard", "Reports", "Coordination", "Shelters", "Settings" and the badge "3" (§3); photo avatar "Capt. K. Perera" (people as initials, §2).

### WF1-S02 Hazard details with the update panel
- Frame: desktop 1440×1024
- Caption: hazard details as shown before the form opens (A1/E4 entry).
- Shows: scenario SC1 steps 4–5 (the instant the details are shown, before the system opens the form; the update panel is the A1 5b input) ; boundary `WarningDashboardUI` ; displays `showHazardDetails(view : HazardDetailsView)` (`view.missingInformation` empty, `view.activeWarning` absent) ; reached by `selectHazard("HZ-2026-0142")` from WF1-S01
- Corrects received screen: new — no received screen (storyboard panel 3 "Hazard Details" had none in the wireframes); answers CH-24 ("hazard details (with update)"), CH-22, D2-14, SB1-2, SB1-3
- Layout (top to bottom, left to right; widths 344 + 16 + 464 + 16 + 312 = 1152; heights within 896 px):
  1. Header — title "Warning Dashboard"; subtitle "Hazard details" (wording ours).
  2. Left column (**344 px** wide, 896 px high) — **card** with the assessable-hazards list, compact: one row "Flood — Kalu Ganga at Ratnapura" / "Medium · Ratnapura, Kalutara" — **selected** token on the row.
  3. Centre column (**464 px** wide; 336 + 16 + 280 = 632 px):
     - **Card** "Hazard details" (wording ours), **336 px** — title line "Flood — Kalu Ganga at Ratnapura", ID "HZ-2026-0142"; definition list: "Type" — "Flood"; "Description" — "Kalu Ganga rising at the Ratnapura gauge; minor flood level exceeded"; "Severity" — **tag** "Medium"; "Location name" — "Kalu Ganga at Ratnapura"; "Districts" — "Ratnapura, Kalutara"; "River basin" — "Kalu"; "Source" — "Agency bulletin"; "Start time" — "20 May 2026, 09:30"; "Status" — **tag** "Active"; "Active warning" — "None" (wording ours).
     - **Card** "Update hazard" (wording ours), **280 px**, pre-filled with the current values (the `HazardUpdate` fields only): "Description" — text area "Kalu Ganga rising at the Ratnapura gauge; minor flood level exceeded"; "Severity" — pick-list (chevron-right icon) "Low" / "Medium" (selected) / "High"; "Districts" — chips "Ratnapura", "Kalutara", both **selected**, and an "Add district" pick-list (plus icon) of the 25 districts (wording ours); **secondary** button "Save hazard update" (wording ours).
  4. Right column (**312 px** wide) — **card** "Hazard districts", **376 px**, with the map (see Map).
- Controls:
  - Selected list row — `selectHazard("HZ-2026-0142")` (already selected).
  - "Description", "Severity", "Districts" — enabled; their values are the `HazardUpdate` of `editHazard`.
  - "Save hazard update" — enabled, secondary style; triggers `editHazard("HZ-2026-0142", update)`.
- Map / chart / image: **Ratnapura and Kalutara** as selected districts (**map** token); display only; no points.
- Not shown (removed on purpose): no "Issue warning" or "Create" button — the system opens the form itself when `missingInformation` is empty and there is no active warning (SC1 step 5; App. 2 row 5), so the next frame is WF1-S03 without an officer input; no hazard-type picker (D2-14).

### WF1-S03 Create Hazard Warning form
- Frame: desktop 1440×1024 (fits without scrolling: regions below sum to 856 px of the 896 px)
- Shows: scenario SC1 steps 5–10 (form opened at step 5; shown filled as at step 10, before "Proceed to preview"); also the E4 12f hazard update on the form ; boundary `WarningDashboardUI` ; displays `showWarningForm(hazard : Hazard)` (the form is a screen component — MD C2) ; reached by the system after `showHazardDetails` (WF1-S02), after a valid `editHazard` (WF1-S02a/b, or on this screen in E4), or by `editWarning()` from WF1-S04 (A4, E4)
- Corrects received screen: p17_1 (p. 17) "Create Hazard Warning" (low-fi UC01-03); answers CH-24, CH-21, CH-22, D2-13, D2-14, D2-15, D2-16, D2-17, D2-19, SB1-3, SB1-4, SB1-6, SB1-7
- Layout (top to bottom, left to right):
  1. Header — title "Create Hazard Warning" (received title kept); subtitle "Prepare the warning for the selected hazard" (wording ours).
  2. **Hazard card** (**card**, 1152 × **64 px**, from `showWarningForm(hazard)`): "Flood — Kalu Ganga at Ratnapura" · "HZ-2026-0142" · "Severity" **tag** "Medium" · "Ratnapura, Kalutara" · "Kalu Ganga rising at the Ratnapura gauge; minor flood level exceeded"; on the right a **secondary** button "Update hazard" (wording ours), which opens the **"Update hazard" panel** — a 392 px drawer (**card**) over the right column (closed in this frame) with the `HazardUpdate` fields as on WF1-S02 ("Description", "Severity", "Districts") and "Save hazard update".
  3. Row (**344 px**; 744 + 16 + 392):
     - Left **card** (744 px; sections 80 + 16 + 168 + 16 + 56 = 336 plus padding): **1 "Warning level"** (80 px) — three option tiles: "Level 1 – Yellow (Remain Vigilant)"; "Level 2 – Amber (Be Alert)" — **selected**; "Level 3 – Red (Evacuate)". **2 "Affected area"** (168 px) — "Districts": 25 chips in alphabetical order in three rows — "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo", "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara", "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar", "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya", "Polonnaruwa", "Puttalam", "Ratnapura" (**selected**), "Trincomalee", "Vavuniya"; "River basins": multi-select pick-list drawn **closed**, showing only the **selected** chip "Kalu" with a cross icon (remove) and the chevron-right icon (its options are the `RiverBasin` literals; not drawn). **3 "Validity"** (56 px; wording ours) — field "Expires" (date-time, clock icon): "21 May 2026, 10:45".
     - Right column (392 px; 232 + 16 + 96 = 344): **card** "Selected area" (wording ours, 232 px) — district map (see Map) and the line "Districts: Ratnapura · River basins: Kalu" (wording ours), with under it the muted note "Basin districts are added on the preview" (wording ours; P87: the map hatches only the selected districts — district shapes only, no basin shapes — and the covered districts, including the basin's Kalutara, are listed on WF1-S04); **card** **5 "Notification channels"** (96 px) — three checkboxes, all checked (check icon): "Push", "SMS", "Audible alert".
  4. Section **4 "Message and emergency instructions"** (**344 px**): section title (24 px) + 8 px, then three **cards** on the three-column split **373 + 16 + 374 + 16 + 373**, each 312 px: "Sinhala" | "Tamil" | "English"; each card: label and counter on one line (32 px), text area "Message" (112 px), text area "Emergency instructions" (112 px):
     - "Sinhala" — counter "201 / 201"; W-L2-MSG (Sinhala), W-L2-INS (Sinhala) **(draft — native check required)**.
     - "Tamil" — counter "200 / 201"; W-L2-MSG (Tamil), W-L2-INS (Tamil) **(condensed draft — native check required)**.
     - "English" — counter "239 / 459"; W-L2-MSG and W-L2-INS (English).
  5. Footer bar (**56 px**) — **primary** button "Proceed to preview" (right).
  (64 + 16 + 344 + 16 + 344 + 16 + 56 = 856 px of 896.)
- Controls:
  - "Update hazard" — enabled, **secondary**; opens the panel; its "Save hazard update" triggers `editHazard("HZ-2026-0142", update)` (E4 12f), after which `showWarningForm(hazard)` redraws the hazard card with the updated hazard.
  - Level tiles — `enterWarningDetails(details)` field `warningLevel` (LEVEL_2_AMBER selected).
  - District chips — `targetDistricts` (Ratnapura); river-basin pick-list — `targetRiverBasins` (Kalu).
  - "Expires" — `expiryTime`.
  - Six text areas — `message`, `instructions` (`LocalisedText`).
  - Channel checkboxes — `channels` (PUSH, SMS, AUDIBLE_ALERT).
  - "Proceed to preview" — enabled, primary; sends `enterWarningDetails(details)` then `requestPreview()`.
- Map / chart / image: display only (no drawing mode); **Ratnapura** as the selected district (**map** token; the selected district chip). The Kalu basin is a pick-list and is not drawn (spec §2); Kalutara appears as covered only on the preview.
- E4 variant (not a separate frame): after 12f the hazard card reads severity **tag** "High" and "Kalu Ganga above major flood level at the Ratnapura gauge", districts "Ratnapura, Kalutara"; the officer then picks "Level 3 – Red (Evacuate)" and the Level 3 texts (12g).
- Not shown (removed on purpose): "Hazard Classification" tiles Flood/Landslide/Cyclone/Tsunami/Drought/"Lightning /" (the hazard is selected, not typed — D2-14); levels "Watch / Advisory / Warning / Emergency" under "Severity & Alert Protocol" (D2-13, 8-4); single "English Broadcast Copy" area and "192 / 300 Characters" (D2-16, D2-18, D2-19); "Geographical Boundary" mode (D2-14, SB1-4); "Cancel & Reset" (no flow); stray " /" and "|" glyphs (D2-18); Colombo/Gampaha/Kalutara + Kelani sample data (D2-18).

### WF1-S04 Warning preview
- Frame: desktop 1440×1024
- Shows: scenario SC1 steps 11–12 ; boundary `WarningDashboardUI` ; displays `showPreview(preview : WarningPreview)` ; reached by `enterWarningDetails(details)` + `requestPreview()` from WF1-S03 (valid details)
- Corrects received screen: p19_1 (p. 19) "Broadcast Preview & Verification" (low-fi UC01-04); answers CH-24, D2-11 (kept: deliberate confirmation), D2-15, D2-16, D2-17 (expiry shown), D2-18, D2-19, SB1-5, SB1-8
- Layout (top to bottom, left to right):
  1. Header — title "Warning Preview" (wording ours); subtitle "Check what will be sent before you confirm the broadcast" (wording ours).
  2. Summary row (**96 px**, four **cards**, 276 + 16 + 276 + 16 + 276 + 16 + 276 = 1152): "Hazard type" — "Flood"; "Warning level" — **tag** "Level 2 – Amber (Be Alert)"; "Covered districts" — "Ratnapura, Kalutara"; "Expires" — "21 May 2026, 10:45".
  3. Row (**672 px**; 744 + 16 + 392):
     - Left **card** (744 × 672 px) "Message and emergency instructions" — table "Language" | "Message" | "Emergency instructions": "Sinhala" | W-L2-MSG (Sinhala) | W-L2-INS (Sinhala) **(draft — native check required)**; "Tamil" | W-L2-MSG (Tamil) | W-L2-INS (Tamil) **(condensed draft — native check required)**; "English" | W-L2-MSG (English) | W-L2-INS (English).
     - Right column (392 px; 280 + 16 + 112 + 16 + 248 = 672): **card** "Covered area" (wording ours, 280 px) — map; **card** "Channels" (112 px) — read-only rows with the check icon: "Push", "SMS", "Audible alert"; **card** "Recipients" (wording ours, 248 px) — table "District" | "Citizens": "Ratnapura" | "18,420"; "Kalutara" | "23,915"; total "All citizens" | "42,335"; "Response organisations" — "14"; "District officers" — "0" with the note "District officers are notified only when a warning is escalated" (SC1 step 11).
  4. Footer bar (**56 px**) — **secondary** "Back & edit" (left); **primary** "Confirm & broadcast" (right).
  (96 + 16 + 672 + 16 + 56 = 856 px of 896.)
- Controls:
  - "Back & edit" — enabled; triggers `editWarning()` (A4, E4 → WF1-S03 with values kept).
  - "Confirm & broadcast" — enabled, primary; triggers `confirmBroadcast()`.
- Map / chart / image: display only; **Ratnapura and Kalutara** as selected districts (**map** token; covered districts, the Kalu basin expanded).
- Not shown (removed on purpose): "Citizen Mobile Display Simulation" with the reworded message and "LOCATE NEAREST SHELTER" (no use case — CH-24; the exact three-language texts replace the simulation — D2-16); "National TV/Radio Interruption", "Coastal Siren Megaphones", "8.2M", "1.4M" (D2-15, CH-24); "ESTIMATED IMMEDIATE REACH 9.6 Million / 99.4%" (D2-18; replaced by the recipient summary); "⚡ AUTHORIZE & BROADCAST NOW" (renamed to the scenario's "Confirm & broadcast").

### WF1-S05 Dispatch status — active warning
- Frame: desktop 1440×1024
- Shows: scenario SC1 steps 13–17 (result of step 17) ; boundary `WarningDashboardUI` ; displays `showActiveWarning(warning : Warning, summary : BroadcastSummary)` ; reached by `confirmBroadcast()` from WF1-S04 (all delivered)
- Corrects received screen: p16_1 (p. 16) "Early Warning Dispatch Status" (low-fi UC01-05); answers CH-24, CH-23, D2-15, D2-17, D2-18, SB1-10
- Layout (top to bottom, left to right):
  1. Header — title "Dispatch Status" (wording ours); subtitle "Flood — Kalu Ganga at Ratnapura" (the warning's hazard, reached through association A1 Warning → Hazard).
  2. Warning banner **card** (1152 × 120 px) — label "Warning", "WRN-2026-0087"; **tag** "Level 2 – Amber (Be Alert)"; **tag** "Active"; "Covered districts: Ratnapura, Kalutara"; "Issued 20 May 2026, 10:45 · Expires 21 May 2026, 10:45".
  3. **Card** "Delivery" (wording ours), 568 × 200 px — big number "102,549", label "Notifications delivered"; line "No failures" (SC1 step 17).
  4. **Card** "Failures by channel" (wording ours), 568 × 200 px (568 + 16 + 568 = 1152; 120 + 16 + 200 = 336 px of 896) — "Push" — "None"; "SMS" — "None"; "Audible alert" — "None"; "Unreachable recipients" — "None".
- Controls: none besides the Shell navigation ("Warnings" → `openDashboard()`). No resend control (only with failures — WF1-S05a).
- Map / chart / image: none (spec §2).
- Not shown (removed on purpose): "BROADCAST IN PROGRESS" and the bars "SMS Gateway Delivery 87%", "Disaster Portal App Push 94%", "National Broadcaster Override ACTIVE" (D2-15, D2-18; the summary has no per-channel delivered figure); "TOTAL TARGET CITIZENS 9,600,000", "CONFIRMED DELIVERIES 8,421,080", "FAILED / NO ACKNOWLEDGEMENT 148,290" (D2-18); "CEASE & RETRACT WARNING /" (no retract flow — D2-17, CH-22); "ESCALATE TO MILITARY MOBILIZATION /" (replaced by Escalate on the hazard details, WF1-S02c, and WF1b — CH-06, D2-17).

## States

### WF1-S02a Hazard information incomplete
- Base screen: WF1-S02; flow: SC1 A1 5a; display: `showHazardDetails(view)` + `showHazardIncomplete(missing : List<String>)` (`missing` = ["affected districts"])
- Change from base:
  - **Notice** (warning-triangle icon, 464 × 64 px) at the top of the centre column (64 + 16 + 336 + 16 + 280 = 712 px): "A warning cannot be issued until the missing information is provided: affected districts" (SC1 A1 5a, verbatim).
  - Left list row reads "Medium · —".
  - "Districts" in the details list reads "—".
  - Update panel: "Districts" has no chips (the "Add district" pick-list is focused); "Description" and "Severity" ("Medium") keep their values; "Save hazard update" becomes the **primary** button.
  - The map shows no selected district. The form does not open.
- Sample data: HZ-2026-0142 as on WF1-S02, recorded without districts.

### WF1-S02b Hazard-update errors
- Base screen: WF1-S02 (in the WF1-S02a situation); flow: SC1 E5 5f (the E4 path, 12i, shows the same error in WF1-S03's "Update hazard" panel — not drawn separately); display: `showValidationErrors(errors : List<String>)`
- Change from base:
  - **Field error** on "Districts" in the update panel: "At least one district is required" (SC1 E5, verbatim) (update card grows to 320 px; centre column 64 + 16 + 336 + 16 + 320 = 752 px).
  - The A1 notice and the "Medium · —" list row of WF1-S02a stay; the hazard details are unchanged.
  - The update panel keeps the description and severity.
- Sample data: as WF1-S02a; the update was submitted with no district.

### WF1-S02c Hazard with an active warning — Escalate offered
- Base screen: WF1-S02; flow: SC1 steps 18–19 (the same screen state as SC1 A5 5d and Escalate A1 1d); display: `showHazardDetails(view)` (`view.activeWarning` present) + `showActiveWarning(warning, summary)` (summary from `WarningController.getBroadcastSummary`)
- Change from base (centre column heights: details 336 + 16 + active warning 200 + 16 + update panel 280 = 848 px ≤ 896):
  - "Active warning" in the details list reads "WRN-2026-0087".
  - New **card** "Active warning" (wording ours, 464 × 200 px) between the details and the update panel: "WRN-2026-0087"; **tag** "Level 2 – Amber (Be Alert)"; **tag** "Active"; "Covered districts: Ratnapura, Kalutara"; "Issued 20 May 2026, 10:45 · Expires 21 May 2026, 10:45"; "102,549 notifications delivered"; "No failures".
  - **Primary** button on that card: "Escalate" (SC1 step 19 / A5, SC2) — triggers `EscalationUI.startEscalation("WRN-2026-0087")` → WF1b-S01.
  - The form does not open (a hazard has at most one active warning).
- Sample data: SC1 steps 18–19, 20 May 2026 at 16:30; hazard as WF1-S02. (A5's WRN-2026-0081 is the same state; its dispatch figures are not given — Gap 3.)

### WF1-S03a Affected area required
- Base screen: WF1-S03; flow: SC1 A2 11a–11b; display: `showValidationErrors(errors)`
- Change from base:
  - **Error box** (error icon, 744 × 40 px) at the top of section 2: "An affected area is required: select at least one district or river basin" (SC1 A2 11b, verbatim). The row grows to 384 px (total 64 + 16 + 384 + 16 + 344 + 16 + 56 = 896 px).
  - No district chip selected; the river-basin pick-list is empty (closed, no chip); the map shows no selected district and its line reads "Districts: none · River basins: none" (wording ours).
  - All other fields keep their values; counters as WF1-S03 (Sinhala "201 / 201", Tamil "200 / 201", English "239 / 459"). No preview.
- Sample data: as WF1-S03 without Ratnapura and Kalu.

### WF1-S03b Validation errors with the counters
- Base screen: WF1-S03; flow: SC1 E3 11d–11e (also 13g, when the confirm-time re-validation fails); display: `showValidationErrors(errors)`
- Change from base:
  - **Error box** (error icon, 1152 × 40 px) under the section 4 title: "Correct 2 fields before the preview" (wording ours); section 4 grows to 384 px (total 896 px).
  - Tamil column: instructions = **W-L2-INS-TA-E3** **(draft — native check required)**; counter "236 / 201" in bold with the error icon; **field error** on the instructions: "Tamil message and instructions: 236 / 201 characters" (SC1 E3 11e, verbatim; the drafted text counts exactly 236).
  - Sinhala column: "Emergency instructions" empty; **field error** "Sinhala instructions are required" (SC1 E3 11e, verbatim); counter "116 / 201" (the message alone).
  - English column unchanged ("239 / 459"). All values kept; no preview.
- Sample data: SC1 E3 11d (Tamil over the limit; Sinhala instructions empty).

### WF1-S04a Active warning already exists
- Base screen: WF1-S04; flow: SC1 E6 13d–13f; display: `showActiveWarningExists(warning : Warning)`
- Change from base:
  - **Modal** (560 × 280 px) over the preview: heading "This hazard already has an active warning" (wording ours); body "WRN-2026-0087 · Level 2 – Amber (Be Alert) · issued 20 May 2026, 10:45" (number, level, issue time — E6 13e); text "No second warning was recorded or broadcast. You can escalate the existing warning instead." (wording ours, from E6 13e).
  - Buttons: **primary** "Escalate" — `EscalationUI.startEscalation("WRN-2026-0087")` → WF1b-S01; **secondary** "Back to dashboard" (wording ours) — `openDashboard()` (E6 13f).
  - "Confirm & broadcast" behind the modal is **disabled**.
- Sample data: the E2 lost-reply path of E6 13d (the existing warning is the one recorded by the first attempt, WRN-2026-0087, Level 2 – Amber (Be Alert), issued 20 May 2026, 10:45) — Gap 4.

### WF1-S05a Delivery-failure alert
- Base screen: WF1-S05; flow: SC1 E1 16a–16c; display: `showActiveWarning(warning, summary)` + `showDeliveryFailure(channel : NotificationChannel, failedRecipientIds : List<String>)` (channel = SMS)
- Change from base:
  - **Notice** with the warning-triangle icon and bold text (1152 × 48 px) above the warning banner: "SMS delivery failed for 1,240 recipients" (SC1 E1 16c, verbatim).
  - "Delivery": "101,309" "Notifications delivered"; "1,240 failed" (wording ours) in place of "No failures".
  - "Failures by channel": "Push" — "None"; "SMS" — **tag** "1,240 recipients" (bold, error icon); "Audible alert" — "None"; "Unreachable recipients" — "None". No claim about other channels.
  - New **card** "Resend to failed recipients" (wording ours, 1152 × 160 px; total 48 + 16 + 120 + 16 + 200 + 16 + 160 = 576 px): pick-list (chevron-right icon) "Channel" with "Push", "SMS", "Audible alert", **"SMS" selected**; hint "Resend on SMS once the SMS Gateway is available again, or on Push to reach only citizens with the app." (wording ours, from E1 16d); **primary** "Resend" (wording ours) — `resendOnChannel("WRN-2026-0087", SMS)`.
  - The warning stays "Active", Level 2 – Amber.
- Sample data: 1,240 failed SMS (Kalutara operator outage — not shown on screen), 101,309 delivered.

### WF1-S05b Dispatch status after the SMS resend
- Base screen: WF1-S05; flow: SC1 E1 16d–16e; display: `showActiveWarning(warning, summary)` (no `showDeliveryFailure`)
- Change from base:
  - **Notice** with the check icon (1152 × 48 px) above the warning banner: "Resend on SMS complete" (wording ours); the updated cards below are the confirmation.
  - "Delivery": "102,549" "Notifications delivered"; "No failures".
  - "Failures by channel": all "None"; "Unreachable recipients" — "None".
  - The resend card of WF1-S05a is gone.
- Sample data: 101,309 + 1,240 = 102,549 delivered; nobody unreachable.

## Common frame

### COMMON-B1 Service unavailable banner
- Base screen: WF1-S01 (drawn once here; in the system the same component appears at the top of the content area of every officer page and app screen — implementation-standards §4, MD §0.10 convention C1); flow: the technical-failure convention of every scenario (SC1 E2 13a–13c, Escalate E4 10d–10f, …); display: none in the dictionary (C1)
- Change from base: a 1152 × **48 px** strip at the top of the content area (§2.1 extra strip), styled as the **error box** token (2 px border, error icon), bold text: "The service is unavailable and nothing was changed. Please try again." (the scenarios' "System shows that the service is unavailable and nothing was changed"; "Please try again." wording ours, from "the actor may retry"). The hazard list beneath stays as on WF1-S01; the usable height becomes 896 − 48 − 16 = 832 px (the 520 px cards still fit). No buttons.
- Sample data: none beyond WF1-S01.

## Flow map

| From | User action (input operation) | Result display | To |
|---|---|---|---|
| (any frame) nav "Warnings" | `openDashboard()` | `showHazards` | WF1-S01 |
| WF1-S01 | `selectHazard("HZ-2026-0142")` | `showHazardDetails` (complete, no active warning) | WF1-S02 |
| WF1-S02 | — (system, step 5) | `showWarningForm` | WF1-S03 |
| WF1-S01 | `selectHazard` (hazard missing districts) | `showHazardDetails` + `showHazardIncomplete` | WF1-S02a |
| WF1-S02a | `editHazard(hazardId, update)` (valid) | `showWarningForm` | WF1-S03 |
| WF1-S02a | `editHazard(hazardId, update)` (no district) | `showValidationErrors` | WF1-S02b |
| WF1-S02b | `editHazard(hazardId, update)` (corrected) | `showWarningForm` | WF1-S03 |
| WF1-S03 | `enterWarningDetails` + `requestPreview()` (valid) | `showPreview` | WF1-S04 |
| WF1-S03 | `enterWarningDetails` + `requestPreview()` (no area) | `showValidationErrors` | WF1-S03a |
| WF1-S03 | `enterWarningDetails` + `requestPreview()` (invalid texts) | `showValidationErrors` | WF1-S03b |
| WF1-S03a / WF1-S03b | `enterWarningDetails` + `requestPreview()` (corrected) | `showPreview` | WF1-S04 |
| WF1-S04 | `editWarning()` (A4: correct a text) | `showWarningForm` (values kept) | WF1-S03 |
| WF1-S04 | **E4:** `editWarning()` → on WF1-S03 "Update hazard" → `editHazard(hazardId, update)` (severity High, new description) | `showWarningForm(hazard)` with the updated hazard | WF1-S03 (E4 variant); then level and texts changed → `requestPreview()` → WF1-S04 |
| WF1-S03 (E4 path) | `editHazard` (invalid, E5 12i) | `showValidationErrors` in the update panel | WF1-S03 (same error as WF1-S02b) |
| WF1-S04 | `confirmBroadcast()` (all delivered) | `showActiveWarning` | WF1-S05 |
| WF1-S04 | `confirmBroadcast()` (SMS failures) | `showActiveWarning` + `showDeliveryFailure` | WF1-S05a |
| WF1-S04 | `confirmBroadcast()` (hazard has an active warning) | `showActiveWarningExists` | WF1-S04a |
| WF1-S04 | `confirmBroadcast()` (re-validation invalid, E3 13g) | `showValidationErrors` | WF1-S03b |
| WF1-S04 | `confirmBroadcast()` (server/network failure, E2) | common banner | COMMON-B1 (on the current screen) |
| WF1-S05a | `resendOnChannel("WRN-2026-0087", SMS)` | `showActiveWarning` | WF1-S05b |
| WF1-S05 / WF1-S05b | `openDashboard()`, then `selectHazard("HZ-2026-0142")` (16:30) | `showHazardDetails` + `showActiveWarning` | WF1-S02c |
| WF1-S02c | `EscalationUI.startEscalation("WRN-2026-0087")` | `showEscalationOptions` | WF1b-S01 |
| WF1-S04a | `EscalationUI.startEscalation(warningId)` | `showEscalationOptions` | WF1b-S01 |
| WF1-S04a | `openDashboard()` | `showHazards` | WF1-S01 |

---

# Part 2 — WF1b Escalate Warning (scenario SC-escalate-warning)

## Shell

Same shell, grid, tokens and icons as WF1 (desktop **1440×1024**, header 80 px, usable 1152 × 896 px, §2.1), with the main flow's user:
- **Sidebar:** "DMC" badge + "DMC Sri Lanka"; navigation for the DMC Duty Officer (wireframe-spec §3): "Verification" (report icon) — `VerificationConsoleUI.openQueue()`; "Warnings" (warning-triangle icon) — **active** on every WF1b frame (Gap 12, resolved) — `WarningDashboardUI.openDashboard()`; "Analytics" (chart icon; inherited from the DMC Officer, CH-04) — `AnalyticsUI.openAnalytics()`; "Hazards" and "Operational picture" signposted (muted, tag "Not in this version"; a click shows "<item> — not available in this version" — wireframe-spec §8 rule 12, P91).
- **Header:** title and subtitle per frame; right: initials avatar "KP", "K. Perera", "DMC Duty Officer" (Escalate main flow, 21 May 2026). When the escalation starts from the dashboard (A1), the user is the DMC Officer and the WF1 shell applies.
- The dashboard and hazard details are WF1-S01 and WF1-S02c; the escalation prompt that starts the main flow is counted in WF2.
- **Hazard label:** every WF1b screen names the hazard "Flood — Kalu Ganga at Ratnapura" (as step 2's prompt does), read from the warning's hazard through association A1 (Warning → Hazard, navigable).

## Screens

### WF1b-S01 Escalation options
- Frame: desktop 1440×1024
- Shows: scenario Escalate steps 3–4 ; boundary `EscalationUI` ; displays `showEscalationOptions(options : EscalationOptions)` ; reached by `startEscalation("WRN-2026-0087")` from the WF2 escalation prompt (main flow, 08:16), from WF1-S02c (A1 1e) or from WF1-S04a (SC1 E6 13f)
- Corrects received screen: new — replaces "ESCALATE TO MILITARY MOBILIZATION /" on p16_1 (p. 16), low-fi UC01-05 "DESTRUCTIVE — escalate to mobilization" (p. 14) and UC2 p. 32's one-click "Approve & Create National Warning"; answers CH-06, CH-24, CH-21, G-UC-12, D2-17, D3-02
- Layout (top to bottom, left to right):
  1. Header — title "Escalate Warning" (use case name); subtitle "Flood — Kalu Ganga at Ratnapura" (A1).
  2. Warning **card** (1152 × 176 px) — "WRN-2026-0087"; "Current level" **tag** "Level 2 – Amber (Be Alert)"; **tag** "Active"; "Target: Ratnapura district, Kalu river basin" (joining wording ours); "Covered districts: Ratnapura, Kalutara"; "Channels: Push, SMS, Audible alert"; "Issued 20 May 2026, 10:45 · Expires 21 May 2026, 10:45".
  3. **Card** "New level" (wording ours, 1152 × 160 px) — one option tile per allowed level: "Level 3 – Red (Evacuate)". Grey text "Only levels above the current level are offered." (wording ours, step 4).
  4. Footer bar (56 px) — **secondary** "Cancel escalation" (wording ours), left.
  (176 + 16 + 160 + 16 + 56 = 424 px of 896.)
- Controls:
  - Tile "Level 3 – Red (Evacuate)" — enabled, primary target; `chooseEscalationLevel(LEVEL_3_RED)`.
  - "Cancel escalation" — enabled; `cancelEscalation()` (A5 5a).
- Map / chart / image: none (Gap 9, resolved).
- Not shown (removed on purpose): military mobilisation, "CEASE & RETRACT" (D2-17, CH-22); Level 1 – Yellow (below the current level).

### WF1b-S02 Escalation details form (pre-filled, with counters)
- Frame: desktop 1440×1024 (fits without scrolling: 176 + 16 + 344 + 16 + 56 = 608 px of 896)
- Shows: scenario Escalate steps 5–6 (the form as pre-filled at step 6; the officer edits it in step 7) ; boundary `EscalationUI` ; displays the details form, a screen component opened by `chooseEscalationLevel` and pre-filled from `options.warning` (MD C2) ; reached by `chooseEscalationLevel(LEVEL_3_RED)` from WF1b-S01
- Corrects received screen: new — no received screen; answers CH-06 (ED1: new text, three languages, pre-filled), CH-21, CH-24, D2-19
- Layout (top to bottom, left to right):
  1. Header — title "Escalate Warning"; subtitle "Flood — Kalu Ganga at Ratnapura · WRN-2026-0087 · Level 2 – Amber (Be Alert) → Level 3 – Red (Evacuate)" (joining wording ours).
  2. Row (**176 px**; 744 + 16 + 392): left **card** (744 px) — "New level" **tag** "Level 3 – Red (Evacuate)" (read-only) and field "Expires" (clock icon) "21 May 2026, 10:45" (the current expiry); right **card** (392 px) "Warning" — "WRN-2026-0087"; "Covered districts: Ratnapura, Kalutara"; "Channels: Push, SMS, Audible alert"; grey note "The message, instructions and expiry time are replaced by what you enter here." (wording ours, step 7 / CH-06).
  3. Section "Message and emergency instructions" (**344 px**: title 24 + 8, then three **cards** on the split 373 + 16 + 374 + 16 + 373, each 312 px) — "Sinhala" | "Tamil" | "English" as on WF1-S03, pre-filled with the warning's current (Level 2) texts:
     - "Sinhala" — counter "201 / 201"; W-L2-MSG, W-L2-INS (Sinhala) **(draft — native check required)**.
     - "Tamil" — counter "200 / 201"; W-L2-MSG, W-L2-INS (Tamil) **(condensed draft — native check required)**.
     - "English" — counter "239 / 459"; W-L2-MSG, W-L2-INS (English).
  4. Footer bar (**56 px**) — **secondary** "Cancel escalation" (left); **primary** "Proceed to preview" (right).
- Controls:
  - Six text areas — `EscalationDetails.message`, `.instructions`; "Expires" — `.expiryTime`; new level — `.newLevel` (LEVEL_3_RED, fixed here).
  - "Proceed to preview" — enabled, primary; `enterEscalationDetails(details)` (validate, then preview — PR3).
  - "Cancel escalation" — enabled; `cancelEscalation()` (A5 7a).
- Map / chart / image: none.
- Not shown (removed on purpose): no channel or area fields (the escalation keeps the warning's target, basin and channels — step 10).

### WF1b-S03 Re-broadcast preview
- Frame: desktop 1440×1024
- Shows: scenario Escalate steps 7–9 (result of step 8) ; boundary `EscalationUI` ; displays `showEscalationPreview(preview : WarningPreview)` ; reached by `enterEscalationDetails(details)` from WF1b-S02 (valid)
- Corrects received screen: new — no received screen (layout follows WF1-S04, which corrects p19_1); answers CH-06, CH-24, D2-16, D2-19
- Layout (top to bottom, left to right):
  1. Header — title "Escalate Warning"; subtitle "Flood — Kalu Ganga at Ratnapura · preview of the re-broadcast" (joining wording ours).
  2. Summary row (**96 px**, four **cards**, 276 + 16 + 276 + 16 + 276 + 16 + 276 = 1152): "Hazard type" — "Flood"; "New level" — **tag** "Level 3 – Red (Evacuate)"; "Covered districts" — "Ratnapura, Kalutara"; "Expires" — "22 May 2026, 08:25".
  3. Row (**672 px**; 744 + 16 + 392): left **card** (744 × 672 px) "Message and emergency instructions" — table "Language" | "Message" | "Emergency instructions": "Sinhala" | W-L3-MSG | W-L3-INS (Sinhala) **(draft — native check required)**; "Tamil" | W-L3-MSG | W-L3-INS (Tamil) **(condensed draft — native check required)**; "English" | W-L3-MSG | W-L3-INS (English). Right column (392 px; 112 + 16 + 248 = 376 px): **card** "Channels" (112 px) — "Push", "SMS", "Audible alert" (read-only, check icon); **card** "Recipients" (248 px) — "Ratnapura" | "18,420"; "Kalutara" | "23,915"; "All citizens" | "42,335"; "Response organisations" — "14"; "District officers" — "4".
  4. Footer bar (**56 px**) — **secondary** "Cancel escalation" (left); **primary** "Confirm escalation" (right; wording ours, "confirms the escalation").
  (96 + 16 + 672 + 16 + 56 = 856 px of 896.)
- Controls:
  - "Confirm escalation" — enabled, primary; `confirmEscalation()`.
  - "Cancel escalation" — enabled; `cancelEscalation()` (A5 9c).
  - Nothing else (Gap 8, resolved: no level-change or back control is drawn).
- Map / chart / image: none.
- Not shown (removed on purpose): —

### WF1b-S04 Escalated warning with the broadcast summary
- Frame: desktop 1440×1024
- Shows: scenario Escalate steps 10–12 (result of step 12) ; boundary `EscalationUI` ; displays `showEscalated(warning : Warning, summary : BroadcastSummary)` ; reached by `confirmEscalation()` from WF1b-S03 (all delivered)
- Corrects received screen: new — no received screen (layout follows WF1-S05, which corrects p16_1); answers CH-06, CH-23, CH-24, G-UC-12
- Layout (top to bottom, left to right):
  1. Header — title "Escalate Warning"; subtitle "Flood — Kalu Ganga at Ratnapura" (A1).
  2. **Notice** with the check icon (1152 × 48 px): "Warning WRN-2026-0087 was escalated to Level 3 – Red (Evacuate) and re-broadcast." (wording ours, steps 10–12).
  3. Warning banner **card** (1152 × 120 px) — "WRN-2026-0087"; **tag** "Level 3 – Red (Evacuate)" (2 px border); **tag** "Active"; "Covered districts: Ratnapura, Kalutara"; "Issued 20 May 2026, 10:45 · Expires 22 May 2026, 08:25".
  4. **Card** "Delivery — this broadcast" (wording ours, 568 × 200 px) — big number "102,553" "Notifications delivered"; "No failures" (step 12 "none failed").
  5. **Card** "Failures by channel" (568 × 200 px; 568 + 16 + 568 = 1152; total 48 + 16 + 120 + 16 + 200 = 400 px of 896) — "Push" — "None"; "SMS" — "None"; "Audible alert" — "None"; "Unreachable recipients" — "None".
- Controls: none besides the Shell navigation (no resend on EscalationUI — MD N7).
- Map / chart / image: none.
- Not shown (removed on purpose): the breakdown "30,100 citizens with the app × 3 …" (not in `BroadcastSummary`); the escalation time 08:25 (not a parameter of `showEscalated` — Gap 13, resolved).

## States

### WF1b-S01a No higher level
- Base screen: WF1b-S01; flow: Escalate A3 4a–4b; display: `showNoHigherLevel(warning : Warning)`
- Change from base:
  - Warning card: "Current level" **tag** "Level 3 – Red (Evacuate)" (2 px border); "Expires 22 May 2026, 08:25".
  - The "New level" card is replaced by a **notice** (info icon, 1152 × 64 px): "This warning is already at the highest level, Level 3 – Red (Evacuate). It cannot be escalated." (wording ours, from A3 4b).
  - No level tiles. Footer button "Close" (wording ours) — `cancelEscalation()` (Gap 7, resolved).
- Sample data: WRN-2026-0087 after the escalation (postconditions): Level 3 – Red (Evacuate), Ratnapura and Kalutara, issued 20 May 2026, 10:45, expires 22 May 2026, 08:25.

### WF1b-S01b Not escalatable
- Base screen: WF1b-S01; flow: Escalate E2 4c–4e (also 10a–10c after confirmation); display: `showNotEscalatable(warning : Warning)`
- Change from base:
  - Warning card: **tag** "Expired" instead of "Active"; level **tag** "Level 2 – Amber (Be Alert)"; "Expires 21 May 2026, 10:45".
  - The "New level" card is replaced by an **error box** (error icon, 1152 × 96 px): "This warning cannot be escalated: it expired on 21 May 2026, 10:45. Nothing was changed." and "You can issue a new warning for the hazard from the warning dashboard." (wording ours, from E2 4d–4e).
  - Buttons: **secondary** "Close" — `cancelEscalation()` (Gap 7); **secondary** "Open warning dashboard" (chevron-right icon) — `WarningDashboardUI.openDashboard()`.
- Sample data: the officer reaches step 4 after 21 May 2026, 10:45; WRN-2026-0087 at Level 2 – Amber, expired. (The 10a variant — raised by another officer — shows its current level "Level 3 – Red (Evacuate)"; not drawn.)

### WF1b-S02a Validation errors
- Base screen: WF1b-S02 (with the step-7 entries); flow: Escalate E3 8a–8b (also 10g); display: `EscalationUI.showValidationErrors(errors)`
- Change from base:
  - The form holds the step-7 entries: "Expires" "22 May 2026, 08:25"; English W-L3-MSG and W-L3-INS (counter "259 / 459"); Sinhala W-L3-MSG; Tamil W-L3-MSG.
  - **Error box** (error icon, 1152 × 40 px) under the section title: "Correct 2 fields before the preview" (wording ours); the section grows to 384 px (total 176 + 16 + 384 + 16 + 56 = 648 px).
  - Tamil column: instructions = **W-L3-INS-TA-E3** **(draft — native check required)**; counter "228 / 201" in bold with the error icon; **field error** on the instructions: "Tamil message and instructions: 228 / 201 characters" (E3 8b, verbatim; the drafted text counts exactly 228).
  - Sinhala column: instructions empty; **field error** "Sinhala instructions are required" (E3 8b, verbatim); counter "116 / 201" (message alone).
  - No preview; all values kept.
- Sample data: Escalate E3 8a.

### WF1b-S04a SMS-failure alert
- Base screen: WF1b-S04; flow: Escalate E1 11a–11c; display: `showEscalated(warning, summary)` + `showDeliveryFailure(SMS, failedRecipientIds)`
- Change from base:
  - **Notice** with the warning-triangle icon and bold text (1152 × 48 px) under the success notice: "The warning was not delivered on SMS to 615 recipients" (from E1 11c).
  - "Delivery — this broadcast": "101,938" "Notifications delivered"; "615 failed" (wording ours).
  - "Failures by channel": "Push" — "None"; "SMS" — **tag** "615 recipients" (bold, error icon); "Audible alert" — "None"; "Unreachable recipients" — "None".
  - **Notice** (info icon, 1152 × 64 px; total 48 + 16 + 48 + 16 + 120 + 16 + 200 + 16 + 64 = 544 px) (wording ours, from E1 11d): "The warning stays at Level 3 – Red (Evacuate). To resend, open the hazard on the warning dashboard when the SMS Gateway is available again." with a **secondary** button "Open warning dashboard" (chevron-right icon) — `WarningDashboardUI.openDashboard()`.
  - No resend control on this screen.
- Sample data: 615 SMS failures (Ratnapura operator outage — not shown on screen); 101,938 delivered.

## Flow map

| From | User action (input operation) | Result display | To |
|---|---|---|---|
| WF2 escalation prompt (UC2) | `EscalationUI.startEscalation("WRN-2026-0087")` (Escalate) | `showEscalationOptions` | WF1b-S01 |
| WF1-S02c (A1) / WF1-S04a (SC1 E6) | `startEscalation(warningId)` | `showEscalationOptions` | WF1b-S01 |
| WF2 / WF1-S02c / WF1-S04a | `startEscalation(warningId)` (warning at Level 3) | `showNoHigherLevel` | WF1b-S01a |
| WF2 / WF1-S02c / WF1-S04a | `startEscalation(warningId)` (warning expired) | `showNotEscalatable` | WF1b-S01b |
| WF1b-S01 | `chooseEscalationLevel(LEVEL_3_RED)` | details form (pre-filled) | WF1b-S02 |
| WF1b-S02 | `enterEscalationDetails(details)` (valid) | `showEscalationPreview` | WF1b-S03 |
| WF1b-S02 | `enterEscalationDetails(details)` (invalid) | `showValidationErrors` | WF1b-S02a |
| WF1b-S02a | `enterEscalationDetails(details)` (corrected) | `showEscalationPreview` | WF1b-S03 |
| WF1b-S03 | `confirmEscalation()` (all delivered) | `showEscalated` | WF1b-S04 |
| WF1b-S03 | `confirmEscalation()` (SMS failures) | `showEscalated` + `showDeliveryFailure` | WF1b-S04a |
| WF1b-S03 | `confirmEscalation()` (re-validation invalid, E3 10g) | `showValidationErrors` | WF1b-S02a |
| WF1b-S03 | `confirmEscalation()` (expired or already raised, E2 10a) | `showNotEscalatable` | WF1b-S01b |
| WF1b-S03 | `confirmEscalation()` (server/network failure, E4) | common banner | COMMON-B1 (on the current screen) |
| WF1b-S01 / S02 / S03 (and "Close" on S01a/S01b) | `cancelEscalation()` (A5) | escalation screen closed | the screen it was opened from (WF2 console or WF1-S02c) |
| WF1b-S04a | `WarningDashboardUI.openDashboard()`, `selectHazard("HZ-2026-0142")`, `resendOnChannel("WRN-2026-0087", SMS)` (E1 11d–11g) | `showHazardDetails` + `showActiveWarning` + `showDeliveryFailure`, then `showActiveWarning` | WF1-S02c, WF1-S05a, WF1-S05b with the escalation round's figures (no new frames — Gap 10) |

---

## Gaps

1. **Dashboard list rows — Resolved (coordinator, review v2):** one row (HZ-2026-0142), as SC1 gives; the demo seed hazards of standards §11 are not added.
2. **Agency name of the source — RESOLVED (P85-3):** the source is shown as "Agency bulletin"; SC1 step 3 now reads "agency bulletin" (no agency-name attribute is added).
3. **A5 variant of WF1-S02c — Resolved (coordinator, review v2):** kept as the step 19 data (WRN-2026-0087 at 16:30); A5's WRN-2026-0081 has no dispatch figures.
4. **E6 sample warning — Resolved (coordinator, review v2):** WF1-S04a uses E6's E2 lost-reply case (WRN-2026-0087), with the wording "No second warning was recorded or broadcast." (M8).
5. **Hazard label — Resolved (coordinator, review v2; MD association A1, Warning → Hazard navigable):** "Flood — Kalu Ganga at Ratnapura" is shown on WF1-S05 and on every WF1b screen (header subtitle).
6. **Sinhala and Tamil counters — Resolved (coordinator decisions, review v2 and Gap 6 decision, 2026-10-09):** counters show the drafted texts' actual counts (Part 0); the 201 limit and the scenarios are unchanged. Sinhala: 201 / 201 for both levels. Tamil is **condensed** to fit: Level 2 = 134 + 1 + 65 = **200 / 201**, Level 3 = 133 + 1 + 64 = **198 / 201**. Kept, in the decision's priority order: hazard, level, districts and river; the action (prepare to leave / leave low-lying areas now, go to the safety centre) and "low-lying"; the Grama Niladhari. **Dropped from the Tamil:** Level 2 — "Move documents and valuables to higher ground" (the whole first instruction), and "to evacuate" is shortened to "if told" ("கூறினால்"); Level 3 — "Take documents, medicines and drinking water" and "Do not cross flood water"; "above major flood level" is condensed to "major flood" ("பெருவெள்ளம்"). No DMC 117 contact is added, because the English source texts do not contain one. The E3 variants (W-L2-INS-TA-E3, W-L3-INS-TA-E3) stay over the limit on purpose and count exactly the scenarios' 236 and 228. The condensed Tamil needs a Tamil reader's approval of the omissions as well as the native check.
7. **Leaving the escalation screen in A3 / E2 — Resolved (coordinator, review v2):** "Close" → `cancelEscalation()`.
8. **A4 level change / back on the escalation preview — Resolved (coordinator, review v2):** nothing drawn.
9. **Map on the escalation preview — Resolved (coordinator, review v2):** no map on WF1b.
10. **Escalate E1 11e–11g resend — Resolved (coordinator, review v2):** no new frames; shown on WF1-S02c / S05a / S05b with the escalation round's figures.
11. **Failed recipient IDs — Resolved (coordinator, review v2):** counts only.
12. **WF1b navigation highlight — Resolved (coordinator, review v2):** "Warnings" active.
13. **Escalation time — Resolved (coordinator, review v2):** not shown.
