# Frames WF2 — UC2 Submit and Verify Hazard Report (phone and verification console)
Status: v2.1 + P87 content review (2026-10-09) — after independent review (2026-10-09) and aligned with wireframe-spec §2.1 (canonical shell, grid, tokens, icons, text formats); draft by Claude (frames writer, WF2), 2026-10-09; review fixes E-1…E-3, M-1…M-7 and the Gaps decisions applied. Sources: `wireframes/wireframe-spec.md` (all, §1–§10); `scenarios/SC2-submit-and-verify-hazard-report.md` (all: main flow, A1–A9, E1–E10, Appendix 1, Appendix 2, Dictionary gaps); `wireframes/frames-WF1.md` Part 0 "Texts in three languages (final drafts)" (IDs W-L2-MSG, W-L2-INS — copied verbatim); `scenarios/SC1-issue-hazard-warning.md` lines 1–70; `model-dictionary.md` v0.9 §0, §1, §2, §3, §4 (rows `CitizenAppUI`, `VerificationConsoleUI`, `EscalationUI`), §5 (UC2 controls); `change-list.md` v2.9 rows CH-21, CH-25, CH-27, 8-2, 8-3, N10, N11, N13; `storyboard-corrections.md` SB2-1…SB2-8; Received Design Record `uc2-submit-and-verify-hazard-report.md` §5 (high-fidelity pp. 31–35); `critique-findings.md` D3-10…D3-17; `implementation-standards.md` §1, §4 (C1 banner), §10 (phone map, language, emulator location), §11 (seed).

Frame counts (wireframe-spec §5, WF2): **phone 6 screens + 15 states; console 2 screens + 8 states = 8 screens + 23 states.** The common error banner (C1) is owned by WF1 (`COMMON-B1`) and is not drawn here; SC2 E5 is realised by it.

## Shell

### Phone shell (CitizenAppUI) — used by WF2P-S01…S06 and their states
- Frame: phone 390×844, wf theme 1 px black outline. Fixed heights: status bar **44 px**, app header **56 px**, bottom tab bar **64 px** → content area **680 px** (16 px side gutters, content width 358 px). Every frame's region heights below sum to ≤ 680 px; nothing scrolls.
- Status bar: time at the left (per frame, given in each frame's "Status bar" line), signal, wifi and battery icons at the right.
- App header: square **DMC** badge and "DMC Sri Lanka" (bold) only (wireframe-spec §3 phone header). No avatar (`HomeView` carries no citizen name) and no bell (Gaps 1).
- One full-width primary button per screen (height 52 px, HCI rule 2).
- Bottom tab bar, three tabs: **Home** (home icon) → `openHome()`; **Report** (plus (circled) icon) → `startReport()`; **Profile** (user icon; Register) signposted (muted, tag "Not in this version"; a click shows "<item> — not available in this version" — wireframe-spec §8 rule 12, P91). Active tab: bold label and 2 px underline (wording of the style ours).
- App labels are English on every screen; only the warning text and the decision-notice text follow the chosen language (CH-25, CH-27, SC2 Conventions).
- No map on the phone: every location is text plus coordinates (wireframe-spec §2, standards §10).

### Console shell (VerificationConsoleUI) — used by WF2C-S01…S02 and their states
- Frame: desktop 1440×1024, no browser chrome, per wireframe-spec §2.1: sidebar **240 px**; header **80 px** across the remaining 1200 px; content area from x = 240, y = 80 with **24 px** padding → usable **1152 × 896 px**; **16 px** grid gaps. Every frame's card widths below add up to exactly 1152 px and its heights fit 896 px.
- Left sidebar: **DMC** badge, "DMC Sri Lanka"; navigation items (wireframe-spec §3, DMC Duty Officer): **Verification** (report icon; selected on every WF2C frame) → `VerificationConsoleUI.openQueue()`; **Warnings** (warning-triangle icon) → `WarningDashboardUI.openDashboard()` (WF1); **Analytics** (chart icon; inherited, CH-04) → `AnalyticsUI.openAnalytics()` (WF4); **Hazards**, **Operational picture** signposted (muted, tag "Not in this version"; a click shows "<item> — not available in this version" — wireframe-spec §8 rule 12, P91).
- Header (80 px): page title (24/32 bold) and a one-line subtitle (14 px, `#757575`) at the left — given per frame; at the right an initials avatar **KP**, "K. Perera" (bold), "DMC Duty Officer" (grey) (SC2 title, steps 26, 25e).

### Signals without colour (both shells; wf theme is monochrome)
- Tokens are wireframe-spec §2.1's wf tokens, named: **Card**, **Primary button**, **Secondary**, **Danger** (white fill, 2 px black border, cross icon, bold text — Reject, Confirm rejection, Discard draft), **Disabled** (`#E0E0E0` fill, `#757575` text), **Selected chip or option** (2 px border, check icon, bold), **Status / level tag** (1 px border, full text), **Error**, **Notice** (`#F2F2F2` fill, 1 px border, matching icon), **Modal** (centred white card, 2 px border, 40 % black scrim), **Map**. In the ui theme danger and errors are also red. Icons are only those of §2.1's icon set.
- **Error** (field and form errors, refusals): field border 2 px black; the **error** icon and the message in bold below the field; a form-level error is a Card with a 2 px border, the error icon and a list.
- **Success** confirmations: check icon + bold headline. **Pending/offline**: clock or offline icon + Status tag. Type scale per §2.1 (phone body 16/24; console body 14/20, labels 12/16 semibold, section titles 18/24 bold). Date-times "21 May 2026, 07:40"; times alone "07:40"; quoted scenario messages keep their own wording.

## Screens

### WF2P-S01 Citizen home — warnings in the chosen language
- Frame: phone 390×844. Status bar 07:40.
- Shows: scenario steps 1–4 (after step 4: Sinhala chosen) and step 2's current location ; boundary CitizenAppUI ; displays `showHome(view : HomeView)` (`warnings`, `myReports`, `pendingDrafts`, `language = SINHALA`, `fromCache = false`), `showLocation(location : GeoPoint)` (current location, via `captureLocation()` — MD `HomeView` note); the warning card is also the component `showAlert(warningId, warningLevel, text)` draws when an alert arrives (UC1 step 15; wireframe-spec §5) ; reached by `openHome()` (step 1), then `selectLanguage(SINHALA)` (step 3).
- Corrects received screen: p. 31 `p31_1.png` (phone home, header "DMC Sri Lanka"; low-fi UC02-01); answers CH-27, CH-25 (language), CH-21 (level wording); findings D3-16, D3-17, D3-15.
- Layout (top to bottom; heights in the 680 px content area, 10 px gaps):
  1. Status bar — "07:40".
  2. App header — DMC badge, "DMC Sri Lanka".
  3. Connectivity row (28 px) — wifi icon + Status tag "Online" (from `HomeView.fromCache = false`).
  4. Language selector (48 px) — label "Warning language" (wording ours); three segments "Sinhala" | "Tamil" | "English"; **Sinhala selected**.
  5. Card "Active warnings" (wording ours; warning-triangle icon) (270 px) — one warning (SC2 step 2):
     - bordered level tag: "Level 2 – Amber (Be Alert)";
     - title: "Flood — Kalu Ganga at Ratnapura";
     - "Ratnapura, Kalutara";
     - "Valid until 21 May 2026, 10:45" (label "Valid until" wording ours; date-time from step 2, §2.1 format);
     - message (Sinhala, W-L2-MSG): "DMC ගංවතුර අවවාදය - මට්ටම 2 ඇම්බර්: රත්නපුර සහ කළුතර දිස්ත්‍රික්කවල කළු ගඟ ඉහළ යයි. පහත් බිම් හැර යාමට සූදානම් වන්න." (draft — native check required)
     - instructions (Sinhala, W-L2-INS): "ලේඛන, වටිනා දෑ උසට ගෙනයන්න. ඉවත් වන්න කීවොත් ග්‍රාම නිලධාරී කී ආරක්ෂිත ස්ථානයට යන්න." (draft — native check required)
  6. Primary button (52 px, full width) — "Report Hazard" (plus (circled) icon).
  7. Card "Current location" (pin icon) (60 px) — "6.5888 N, 79.9640 E"; "GPS · accuracy ±8 m" (values of SC2 step 13 — same phone, same place; Gaps 2).
  8. Section "My reports" (70 px) — empty-list text "No reports yet" (wording ours) (SC2 gives no earlier report by Asela).
  9. Section "My reports — waiting on this phone" (70 px) — empty-list text "Nothing waiting" (wording ours) (step 18: 0 drafts pending).
  10. Tab bar — Home (active), Report.
  (Content total 28 + 48 + 270 + 52 + 60 + 70 + 70 + 6×10 = 658 px.)
- Controls:
  - Language segments "Sinhala" / "Tamil" / "English" — enabled; Sinhala selected → `selectLanguage(language : Language)`.
  - "Report Hazard" (primary) → `startReport()`.
  - Tab "Home" → `openHome()`; tab "Report" → `startReport()`.
  - (Rows in "My reports" open `openReportStatus(reportId)`; "waiting" rows with status Rejected offer "Open" → `openDraft(draftId)` — none present on this frame; see S01b/S01c.)
- Map / chart / image: none (no map on the phone).
- Not shown (removed on purpose): "FLOOD WARNING: Kalutara District / Kelani River water levels rising rapidly" (wrong basin — D3-15; replaced by the Kalu Ganga warning, CH-27, ED3); "CURRENT CONDITIONS 31°C • Heavy Monsoon …" weather card (no flow — D3-16, CH-27: replaced by current location and connectivity); "Emergency Dispatch Tools" heading and "Submit photos & GPS coordinates directly to DMC" (stray "/" — D3-15); the Contacts and Shelters quick links (CH-27); "Recent Alerts near you / See All" with AMBER/RED/GREEN tags (no GREEN level — D3-15, CH-21; no flow); the bell icon and the tabs Alerts, Map, Profile (no flow — Gaps 1).

### WF2P-S02 Choose hazard type
- Frame: phone 390×844. Status bar 07:40.
- Shows: scenario steps 5–6 ; boundary CitizenAppUI ; displays the hazard-type list (screen component, MD §0.11 C2 — no dictionary display); the draft (`ReportDraft`, `draftId`, `capturedAt` 21 May 2026, 07:40) is held by the screen (C3) ; reached by `startReport()` from S01.
- Corrects received screen: p. 34 `p34_1.jpeg` "Identify & Position Incident" (its hazard chips; low-fi UC02-02); answers CH-27, CH-21; findings D3-13, D3-15 (icons not matching labels), SB2-1.
- Layout:
  1. Status bar — "07:40".
  2. App header.
  3. Title "Report a hazard" (wording ours); subtitle "What did you see?" (wording ours) (56 px).
  4. Line "Capture time 21 May 2026, 07:40 — recorded automatically" (wording ours; step 6, SB2-2) (24 px).
  5. List of seven tiles, each 64 px high with 8 px gaps (≈ 500 px), with §2.1's hazard-type icon: "Flood" (flood), "Landslide" (landslide), "Cyclone" (cyclone), "Tsunami" (tsunami), "Drought" (drought), "Blocked road" (road blocked), "Other" (other). None selected.
  6. Tab bar — Report active.
- Controls: each tile → `selectHazardType(type : HazardType)` (FLOOD, LANDSLIDE, CYCLONE, TSUNAMI, DROUGHT, BLOCKED_ROAD, OTHER); tab Home → `openHome()`.
- Map / chart / image: none.
- Not shown: "Step 2 of 3" stepper and progress bar (implies undrawn steps — D3-10); chips "Fallen Tree" and "Road Block" with mismatched icons (D3-13, D3-15, CH-21: one list).

### WF2P-S03 Report form — description, photo, location
- Frame: phone 390×844. Status bar 07:40.
- Shows: scenario steps 7–15 (state after step 14) ; boundary CitizenAppUI ; displays the report form (screen component, C2), `showLocation(location)` (step 14) ; reached by `selectHazardType(FLOOD)` from S02 (step 7), or `editReport()` from S04 (A3 17b), or `openDraft(draftId)` (E8, state S03f).
- Corrects received screen: p. 34 `p34_1.jpeg` "Identify & Position Incident"; answers CH-27, CH-25 (one photo, automatic time); findings D3-13, D3-16 (accuracy and its confirmation), D3-10 (button wording), SB2-3, SB2-4.
- Layout (10 px gaps):
  1. Status bar — "07:40".
  2. App header.
  3. Title "Report a hazard" (wording ours) (32 px).
  4. Row "Hazard type" — value "Flood" (flood icon); link "Change" at the right (44 px).
  5. Line "Capture time 21 May 2026, 07:40 — recorded automatically" (wording ours) (24 px).
  6. Field "Description *" (140 px) — multi-line text: "Kalu Ganga water has breached the sandbag barriers near the Kalutara bridge. Flooding is now moving into the service road. Heavy continuous rainfall still active." Counter below right: "162 / 500".
  7. Block "Photo * (1 required)" (wording ours) (200 px) — one photo placeholder tile 120 px high (wf: grey box with cross, label "photo"; the flooded road, step 10) with caption "1 of 1"; under it two secondary buttons side by side "Take photo" (camera icon) and "Choose from gallery" (44 px); small note "A new photo replaces this one" (wording ours; step 11).
  8. Block "Location *" (pin icon) (64 px) — "6.5888 N, 79.9640 E"; "GPS · accuracy ±8 m" (step 14, exact).
  9. Primary button (52 px, full width) — "Review Report".
  10. Tab bar — Report active.
  (Content total 32 + 44 + 24 + 140 + 200 + 64 + 52 + 6×10 = 616 px.)
- Controls:
  - "Change" (hazard type) → back to S02, then `selectHazardType(type)` (A3 17c allows a changed type).
  - Description field → `enterDescription(text : String)`.
  - "Take photo" / "Choose from gallery" → `attachPhoto(photoUri : String)`.
  - "Review Report" (primary, enabled) → `reviewReport()` (confirms the location — step 15).
  - Tab Home → `openHome()`.
- Map / chart / image: photo placeholder (one). No map: step 14's "on a map" is shown as text plus coordinates (standards §10).
- Not shown: "Add Photos (Up to 3)" with two thumbnails (one photo — CH-25, D3-13); the satellite map with crosshair and "Adjust Map" (phone map as text; manual entry is A1, S03a); "Observation Notes" label (now "Description", as in the scenario); "Transmit Verified Report to DMC" (misleading "Verified" — D3-10; now "Review Report" then S04); "SUBMIT HAZARD REPORT / Step 2 of 3" stepper; Kelani River text and coordinates 6.582, 80.038 (D3-15).

### WF2P-S04 Review report
- Frame: phone 390×844. Status bar 07:40.
- Shows: scenario step 16 (and A3 17a) ; boundary CitizenAppUI ; displays `showReview(draft : ReportDraft)` ; reached by `reviewReport()` from S03 when `validateDraft` is valid.
- Corrects received screen: new — no received high-fidelity screen (storyboard panel 6 "Review Report"); answers CH-27; finding D3-10.
- Layout (10 px gaps):
  1. Status bar — "07:40".
  2. App header.
  3. Title "Review your report" (wording ours); subtitle "Check the details before you send them." (wording ours) (56 px).
  4. Summary card (480 px), label/value rows:
     - "Hazard type" — "Flood" (36 px);
     - "Description" — the full description as S03 (110 px);
     - "Photo" — photo placeholder thumbnail 160×120, "1 of 1" (140 px);
     - "Location" — "6.5888 N, 79.9640 E", "GPS · accuracy ±8 m" (56 px);
     - "Capture time" — "21 May 2026, 07:40" (36 px).
  5. Two buttons side by side (52 px): secondary "Edit" (outlined, left, 1/3 width); primary "Submit Report" (filled, right, 2/3 width).
  6. Tab bar — Report active.
  (Content total 56 + 480 + 52 + 2×10 = 608 px.)
- Controls: "Edit" → `editReport()` (A3 17a → S03 with every value kept); "Submit Report" (primary) → `submitReport()` (step 17).
- Map / chart / image: photo placeholder.
- Not shown: nothing received (new screen).

### WF2P-S05 Confirmation — report submitted (online)
- Frame: phone 390×844. Status bar 07:42.
- Shows: scenario steps 18–20 ; boundary CitizenAppUI ; displays `showSubmissionResult(result : SubmissionResult)` with `state = SUBMITTED`, `report` ; reached by `submitReport()` from S04 (phone online).
- Corrects received screen: p. 33 `p33_1.png` "Report Successfully Logged" (low-fi UC02-03); answers CH-27; findings D3-11, D3-15, D3-16, SB2-5.
- Layout:
  1. Status bar — "07:42".
  2. App header.
  3. Circle (96 px) with check icon, centred, 48 px top margin.
  4. Headline "Report submitted" (bold, 40 px).
  5. Card (120 px), label/value rows: "Report ID" — "RPT-2026-KLT-0847"; "Status" — bordered tag "Pending verification".
  6. Primary button (52 px, full width) — "Track report status".
  7. Tab bar — Report active.
- Controls: "Track report status" (primary) → `openReportStatus(reportId = "RPT-2026-KLT-0847")`; tab Home → `openHome()`.
- Map / chart / image: none.
- Not shown: "Your field report has been routed to the emergency verification queue."; "District Zone Kalutara (Galle Basin)" (wrong basin — D3-15; not in step 20); info box "Report stored locally and fully synced with main DMC servers." (false when offline — D3-11; the offline state is S05a); "Submit Another Observation" (no flow — CH-27, D3-16); "Report Reference ID / Live Review Status" labels and the RPT-2024 ID format (D3-15).

### WF2P-S06 Report status — Verified
- Frame: phone 390×844. Status bar 08:15.
- Shows: scenario steps 31–32 ; boundary CitizenAppUI ; displays `showReportStatus(report : HazardReport)` ; reached by `openReportStatus(reportId)` from the decision notice (S01e), from a "My reports" row, or from S05's "Track report status".
- Corrects received screen: new — no received screen ("Track Report Status" and "My Reports" led nowhere — D3-16); answers CH-27, CH-25; findings D3-16, SB2-8.
- Layout:
  1. Status bar — "08:15".
  2. App header.
  3. Title "Report status" (wording ours) (32 px).
  4. Card (240 px), label/value rows: "Report ID" — "RPT-2026-KLT-0847"; "Hazard type" — "Flood"; "Capture time" — "21 May 2026, 07:40"; "Status" — bordered tag with check icon "Verified"; "Decision time" — "21 May 2026, 08:15".
  5. Tab bar — Home active.
- Controls: tab Home → `openHome()`; tab Report → `startReport()`. No primary action (read-only screen).
- Map / chart / image: none.
- Not shown: nothing received (new screen). The linked hazard and the officer's name are not shown (step 32 lists only ID, type, capture time, status and decision time).

### WF2C-S01 Verification queue
- Frame: desktop 1440×1024.
- Shows: scenario steps 21–22 (also A5 25j–25k, E10 26f) ; boundary VerificationConsoleUI ; displays `showPendingReports(reports : List<HazardReport>)` ; reached by `openQueue()` (sidebar "Verification").
- Corrects received screen: p. 35 `p35_1.jpeg` "Incoming Hazard Verification Queue" (low-fi UC02-04); answers CH-27, CH-21, CH-04 (reporter type); findings D3-12, D3-14, D3-15.
- Layout:
  1. Sidebar — Verification selected.
  2. Header (80 px) — title "Verification queue" (wording ours); subtitle "Reports with the status Pending verification, oldest first" (wording ours); right "K. Perera / DMC Duty Officer".
  3. Summary card row (120 px; kept from Group_060's "Queue Metrics" position, one card 373 px wide — first column of the three-column grid): "Pending verification" — "3 reports" (the count of the rows below).
  4. Card "Pending reports" (wording ours), full width 1152 px (≈ 300 px: title 56, header row 48, three rows × 56; column total 120 + 16 + 300 = 436 px ≤ 896) — table, header row: "Report ID" | "Hazard type" | "District" | "Capture time" | "Reporter type" | "Status" | "" (action column). Rows (oldest first):
     | Report ID | Hazard type | District | Capture time | Reporter type | Status | Action |
     |---|---|---|---|---|---|---|
     | RPT-2026-KLT-0843 | Flood | Kalutara | 21 May 2026, 07:05 | Citizen | Pending verification | View |
     | RPT-2026-KLT-0845 | Flood | Kalutara | 21 May 2026, 07:20 | Citizen | Pending verification | View |
     | RPT-2026-KLT-0847 | Flood | Kalutara | 21 May 2026, 07:40 | Citizen | Pending verification | View |
     ("Citizen" is printed as "Citizen" on the frame; see Gaps 3. Status is a bordered tag.)
- Controls: "View" on each row (secondary button) → `selectReport(reportId : String)`; sidebar items as Shell. No verify or reject on any row (step 22).
- Map / chart / image: none (wireframe-spec §2: UC2 maps only on the report details).
- Not shown: "Incoming Reports Spatial Map" (no step; maps belong on the details — §2); "CRITICAL LEVEL 04 Active" and the Severity column with HIGH/MEDIUM/LOW badges (reports have no severity — D3-14, CH-25); "ACTIVE FILTERS" District/Hazard dropdowns (no input operation; "Flood / Landslide" above "Fallen Tree" — D3-15); "18 PENDING SATISFACTION" badge and "18 Reports" (D3-15); "Under Review" status (CH-21); row-level "Verify Report" / "Reject" buttons (D3-12, CH-27); RPT-2024 IDs and the Galle/Colombo/Ratnapura rows (not in SC2).

### WF2C-S02 Report details — evidence, nearby reports, candidate hazards, decision
- Frame: desktop 1440×1024.
- Shows: scenario steps 23–25 (state at step 25: candidate selected, before "Verify and Link") ; boundary VerificationConsoleUI ; displays `showReportDetails(report : HazardReport, nearbyReports : List<NearbyReport>, candidateHazards : List<Hazard>)` ; reached by `selectReport("RPT-2026-KLT-0847")` from WF2C-S01.
- Corrects received screen: p. 32 `p32_1.jpeg` "Verify Field Report: RPT-2024-KLT-0847" (low-fi UC02-05); answers CH-27, CH-25, CH-06; findings D3-02, D3-12, D3-14, D3-15, D3-16, SB2-6, SB2-7.
- Layout (§2.1 two-column split **744 + 16 + 392 = 1152**: left 744 px evidence, right 392 px action panel — kept from Group_060's right-hand "DMC Command Actions"):
  1. Sidebar — Verification selected.
  2. Header (80 px) — title "Report RPT-2026-KLT-0847" (wording ours); a text link "← Back to queue" (plain text, no icon) at the start of the subtitle line; subtitle "Pending verification — review the evidence and decide" (wording ours); right "K. Perera / DMC Duty Officer".
  3. Left, card "Report" (wording ours) (330 px) — inside 24 px card padding (696 px inner): label/value grid at the left (412 px), 24 px gap, photo at the right (260 px):
     - "Report ID" — "RPT-2026-KLT-0847"; "Status" — bordered tag "Pending verification";
     - "Reporter" — "Asela Gunawardene"; "Reporter type" — "Citizen";
     - "Hazard type" — "Flood";
     - "Description" — "Kalu Ganga water has breached the sandbag barriers near the Kalutara bridge. Flooding is now moving into the service road. Heavy continuous rainfall still active.";
     - "Location" — "6.5888 N, 79.9640 E · GPS · accuracy ±8 m"; "District" — "Kalutara";
     - "Capture time" — "21 May 2026, 07:40"; "Received time" — "21 May 2026, 07:42";
     - "Photo" — one photo placeholder 260×180 labelled "photo" (right side of the card).
  4. Left, card "Location" (wording ours) (300 px) — district map (see Map), caption "Kalutara District — report location 6.5888 N, 79.9640 E".
  5. Left, card "Nearby reports (same type, within 5 km, last 24 hours)" (wording ours; assumed values of step 24) (170 px) — table header: "Report ID" | "Hazard type" | "District" | "Capture time" | "Distance" | "Status". Rows (from `NearbyReport.report` and `NearbyReport.distanceKm`):
     | RPT-2026-KLT-0843 | Flood | Kalutara | 07:05 | 1.2 km | Pending verification |
     | RPT-2026-KLT-0845 | Flood | Kalutara | 07:20 | 2.8 km | Pending verification |
     (Left column total 330 + 300 + 170 + 2×16 = 832 px ≤ 896.)
  6. Right panel, card "Candidate hazards" (wording ours) (220 px) — one selectable row (radio, **selected** — Selected option: 2 px border, check icon, bold): "Flood — Kalu Ganga at Ratnapura"; "HZ-2026-0142"; "Severity Medium"; "Ratnapura, Kalutara"; "River basin Kalu"; "Source Agency bulletin".
  7. Right panel, card "Decision" (wording ours) (260 px):
     - primary button "Verify and Link" (enabled, because a candidate is selected);
     - secondary button "Verify as New Hazard";
     - Danger button "Reject" (2 px border, cross icon, bold);
     (Right column total 220 + 16 + 260 = 496 px ≤ 896.)
     - note "Verification never issues or changes a warning." (wording ours; SC2 Description).
- Controls:
  - Candidate radio → selects the `hazardId` passed with the decision (screen-side selection, no message).
  - "Verify and Link" (primary) → `verifyReport(reportId = "RPT-2026-KLT-0847", hazardId = "HZ-2026-0142")`.
  - "Verify as New Hazard" → opens the new-hazard details form (C2; state S02c), then `verifyAsNewHazard(reportId, details)`.
  - "Reject" → opens the mandatory rejection-reason field (C2; state S02a), then `rejectReport(reportId, reason)`.
  - "← Back to queue" → `openQueue()` (A5 25j).
- Map / chart / image: district map from `districts.svg` (wireframe-spec §2): **Kalutara** filled grey with hatch (the report's district); all other districts white with grey borders; one dot at 6.5888 N, 79.9640 E (equirectangular projection as §2). Display only. Photo placeholder (one).
- Not shown: "UNVERIFIED HIGH SEVERITY" badge and "CONFIRM SEVERITY LEVEL / RED - EMERGENCY" (no report severity — D3-14, CH-25); "OFFICER ASSESSMENT NOTES" text area pre-filled with an approval note (D3-12; the rejection reason is a separate mandatory field, S02a); "POTENTIAL ZONE IMPACT — Estimated Households 450 - 600, Alternative Shelters Galle Basin High School" (CH-27, D3-16); "Verification History Trail" with "Assigned to Capt. K. Perera" (CH-27, D3-16); "Approve & Create National Warning" (D3-02, CH-06 — verification never issues a warning; escalation is a separate prompt, S02g); "Decline / Flag as Spurious" (replaced by "Reject" with a mandatory reason and confirmation — D3-12); reporter phone "+94 77 123 4567 • Registered Citizen" (not in step 24); "Kalutara South, Kelani River Basin", coordinates 6.5823 N, 80.0384 E and the second photo (D3-15); SC2 step 24's place names of the nearby reports (Kalutara North, Nagoda) and the agency name "(Irrigation Department)" (no dictionary attribute — Gaps 9).

## States

### WF2P-S01a Home offline — saved copy, language selector disabled
- Base screen: WF2P-S01; flow: A8 2a–2d, shown after A2 18c (the report saved offline appears under "waiting on this phone"); display: `showHome(view)` with `fromCache = true`, `pendingDrafts` = the queued draft; `showLocation(location)`.
- Change from base:
  - Status bar shows no Wi-Fi bars (offline).
  - Connectivity row: offline icon + Status tag "Offline" (A8 2b).
  - Language selector: **Disabled** token (`#E0E0E0` fill, `#757575` text), "English" selected (the saved copy predates step 3; SC2 step 2 language English); helper text "Language can be changed when you are online" (wording ours; A8 2c) — selector block grows to 64 px.
  - Active warnings card shows the English text — W-L2-MSG (English): "DMC FLOOD WARNING - Level 2 Amber: Kalu Ganga rising in Ratnapura and Kalutara. Prepare to evacuate low-lying areas."; W-L2-INS (English): "Move documents and valuables to higher ground. If told to evacuate, go to the safety centre named by your Grama Niladhari." — with the same tag, title, districts and validity; no "last updated" time (FX-19). Card stays 270 px.
  - "Report Hazard" stays enabled (A8 2b).
  - "My reports — waiting on this phone": one row — "Flood" · "Captured 21 May 2026, 07:40" · bordered tag with clock icon "Pending synchronisation" (A2 18c).
  - "My reports": "No reports yet" (wording ours); section shrinks to 54 px so the total stays ≤ 680 px.
- Sample data: status bar "07:41" (Gaps 2); location as base.

### WF2P-S01b Synchronisation result — "Report sent"
- Base screen: WF2P-S01; flow: A2 18d–18f (connection restored while the app is open, 08:20); display: `showSyncResults(results : List<SyncResult>)` (`outcome = ACCEPTED`, `message` with the report ID), then `showHome(view)`.
- Change from base:
  - Connectivity tag "Online"; language selector enabled, "English" selected (as S01a's session); warnings card in English (as S01a).
  - Result banner (56 px) overlaid at the top of the content (check icon, bold): "Report sent — RPT-2026-KLT-0847, Pending verification" (verbatim 18f), with a close "×" (screen component).
  - "My reports — waiting on this phone": "Nothing waiting" (wording ours) — the draft was removed (18f).
  - "My reports": one row — "RPT-2026-KLT-0847" · "Flood" · "Captured 21 May 2026, 07:40" · bordered tag "Pending verification" · chevron-right icon (→ `openReportStatus(reportId)`).
- Sample data: status bar "08:20".

### WF2P-S01c Synchronisation result — "Report not accepted", rejected draft waiting
- Base screen: WF2P-S01; flow: E8 18j–18k; display: `showSyncResults(results)` (`outcome = REJECTED`, `reason = VALIDATION_FAILED`, `message`), then `showHome(view)` (`pendingDrafts` with the REJECTED draft and its reason).
- Change from base:
  - Connectivity "Online"; language "English" selected, enabled; warnings card in English.
  - Result banner (64 px) overlaid at the top of the content (error icon, 2 px border, bold): "Report not accepted: Validation failed — photo: not a readable JPEG or PNG image" (pattern verbatim from 18k "Report not accepted: <reason>"; reason text reuses E2's photo wording — Gaps 4), close "×".
  - "My reports — waiting on this phone" (90 px): one row — "Flood" · "Captured 21 May 2026, 07:40" · bordered tag with cross icon "Rejected" · reason line "Validation failed — photo: not a readable JPEG or PNG image" · button "Open".
  - "My reports": "No reports yet" (wording ours), 50 px.
- Controls added: "Open" → `openDraft(draftId)` (18l → S03f).
- Sample data: status bar "08:20".

### WF2P-S01d Rejected draft discarded
- Base screen: WF2P-S01; flow: E8 18o; display: `showHome(view)` after `discardDraft(draftId)`.
- Change from base:
  - Connectivity "Online"; language "English", enabled; warnings card in English.
  - Confirmation banner (48 px, check icon, bold) overlaid at the top: "Draft discarded — nothing was sent" (wording ours; HCI rule 4).
  - "My reports — waiting on this phone": "Nothing waiting" (wording ours).
  - "My reports": "No reports yet" (wording ours).
- Sample data: status bar "08:21" (Gaps 2).

### WF2P-S01e Decision notice — verified
- Base screen: WF2P-S01 (Sinhala selected, as after step 4), **unchanged underneath** (the notice is shown by `showReportDecision` only; "My reports" is not changed on this frame); flow: main flow step 30; display: `showReportDecision(notice : ReportDecisionNotice)` (`reportId`, `status = VERIFIED`, `decidedAt`) after the found message `receiveReportDecision(notice)`.
- Change from base:
  - Notice card (160 px) overlaid at the top of the content, below the header, with a shadow: check icon, title "Report decision" (wording ours, app label English), text in Sinhala (Text N-1): "ඔබේ වාර්තාව RPT-2026-KLT-0847 ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් තහවුරු කරන ලදී (2026 මැයි 21, 08:15)" (draft — native check required); English source (step 30, verbatim): "Your report RPT-2026-KLT-0847 was verified by the DMC (21 May 2026, 08:15)". Button "Open report"; close "×".
- Controls added: "Open report" → `openReportStatus("RPT-2026-KLT-0847")` (step 31 → S06).
- Sample data: status bar "08:15".

### WF2P-S01f Decision notice — rejected, with the reason
- Base screen: WF2P-S01 (Sinhala selected), **unchanged underneath** ("My reports" not changed); flow: A4 25f, 25h; display: `showReportDecision(notice)` (`status = REJECTED`, `reason`, `decidedAt`).
- Change from base:
  - Notice card (220 px) overlaid at the top (cross icon, 2 px border), title "Report decision" (wording ours), text in Sinhala (Text N-2): "ඔබේ වාර්තාව RPT-2026-KLT-0847 ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් ප්‍රතික්ෂේප කරන ලදී (2026 මැයි 21, 08:15). හේතුව: RPT-2026-KLT-0843 හි අනුපිටපතකි — ගඟේ එම කොටසම, මිනිත්තු 35කට පෙර; කරුණාකර නව වෙනස්කම් පමණක් වාර්තා කරන්න." (draft — native check required); English source: "Your report RPT-2026-KLT-0847 was rejected by the DMC (21 May 2026, 08:15). Reason: Duplicate of RPT-2026-KLT-0843 — same stretch of the river, 35 minutes earlier; please report only new changes." (sentence frame wording ours, by analogy with step 30; the reason verbatim from 25c). Button "Open report"; close "×".
- Controls added: "Open report" → `openReportStatus("RPT-2026-KLT-0847")` (→ S06a).
- Sample data: status bar "08:15".

### WF2P-S03a GPS location unavailable — manual entry
- Base screen: WF2P-S03; flow: A1 12a–12c; display: `showLocationUnavailable()`.
- Change from base:
  - The "Location *" block (grows to 150 px; the description field shrinks to 110 px and the photo block's note is dropped so the total stays ≤ 680 px) shows a bordered notice (Notice token, pin icon, bold): "GPS location is unavailable" (verbatim 12b), and a secondary button "Enter location manually" (verbatim).
  - Under it (after the button is chosen) two numeric fields side by side: "Latitude (°N)" and "Longitude (°E)", empty, with hint "Type the coordinates of the place you saw" (wording ours); no accuracy line.
  - "Review Report" stays enabled (validation runs on review, step 16).
- Controls added: "Enter location manually" reveals the fields; the fields → `enterLocationManually(location : GeoPoint)` (12c). After a valid entry the block reads "<lat> N, <lon> E" and "Entered manually", no accuracy (12d, via `showLocation`).
- Sample data: other fields as base (description, photo "1 of 1").

### WF2P-S03b Location error
- Base screen: WF2P-S03 with S03a's manual fields (same heights as S03a); flow: E3 12e–12f; display: `showValidationErrors(errors : List<String>)`.
- Change from base:
  - Latitude field shows the entered value "65.888" (sample value ours — Gaps 5), longitude "79.9640"; both fields with a 2 px border.
  - Error line under the fields (error icon, bold): "This location is outside Sri Lanka — check the latitude and longitude" (wording ours).
  - Other values kept (C2).

### WF2P-S03c Photo error
- Base screen: WF2P-S03; flow: E2 11a–11c; display: `showPhotoError(message : String)` (from the "photo: not a readable JPEG or PNG image" error of `validateDraft`).
- Change from base:
  - Photo block: no photo attached — empty placeholder tile (2 px border — Error) with camera icon, caption "0 of 1" (wording ours); error line under it (error icon, bold): "This image cannot be used — choose a JPEG or PNG photo" (verbatim 11b).
  - "Take photo" / "Choose from gallery" remain enabled.
  - Description and location kept.

### WF2P-S03d Camera permission refused
- Base screen: WF2P-S03; flow: E2 11d; display: app-screen handling, not a dictionary display (MD `CitizenAppUI` note, N10).
- Change from base:
  - Photo block: empty placeholder tile, caption "0 of 1" (wording ours); Notice under it (camera-off icon, bold): "Allow camera access to take the required photo" (verbatim 11d), with buttons "Allow camera access" (wording ours; opens the system permission) and "Choose from gallery".
- Controls added: "Allow camera access" — phone permission (no MD operation); "Choose from gallery" → `attachPhoto(photoUri)`.

### WF2P-S03e Form validation errors
- Base screen: WF2P-S03; flow: E1 16a–16c (example: empty description); display: `showValidationErrors(errors)`.
- Change from base:
  - Description field empty, 2 px border; counter "0 / 500"; error line under it (error icon, bold): "Enter a description of what you saw (at most 500 characters)" (wording ours).
  - Summary line above "Review Report" (error icon, bold): "Please correct the field marked above" (wording ours).
  - Hazard type, photo ("1 of 1") and location kept.

### WF2P-S03f Rejected draft reopened with its reason
- Base screen: WF2P-S03; flow: E8 18l–18n (and 18o for Discard); display: the report form loaded from `getDraft(draftId)` (`status = REJECTED`, `rejectionReason`).
- Change from base:
  - Notice at the top of the form (64 px; cross icon, 2 px border, bold): "Not accepted by the DMC: Validation failed — photo: not a readable JPEG or PNG image" (prefix wording ours; reason as S01c — Gaps 4).
  - Photo block: the rejected photo placeholder with a "Replace photo" button (the citizen replaces it — 18n).
  - Values kept: Flood, the description (field 100 px), "6.5888 N, 79.9640 E · GPS · accuracy ±8 m", capture time 21 May 2026, 07:40.
  - Buttons: primary "Review Report" (52 px); Danger button "Discard draft" (wording ours; cross icon, bold, 40 px) under it. The capture-time line and the photo note are dropped to keep the total ≤ 680 px.
- Controls added: "Replace photo" → `attachPhoto(photoUri)`; "Review Report" → `reviewReport()`; "Discard draft" → `discardDraft(draftId)` (18o → S01d).
- Sample data: status bar "08:21" (Gaps 2).

### WF2P-S05a Confirmation — saved on this phone, Pending synchronisation
- Base screen: WF2P-S05; flow: A2 18a–18c (also E6 19d); display: `showSubmissionResult(result)` with `state = QUEUED`.
- Change from base:
  - Status bar without Wi-Fi bars.
  - Icon: clock (with the offline icon in the status line) instead of the check.
  - Headline: "Saved on this phone — Pending synchronisation" (verbatim 18c).
  - Card: "Capture time" — "21 May 2026, 07:40" (the screen takes it from the draft it holds — `ReportDraft.capturedAt`, C3; `SubmissionResult` has no capture time); "Status" — bordered tag with clock icon "Pending synchronisation". **No report ID.**
  - Info text: "It will be sent automatically when the app is open and the connection returns" (verbatim 18c).
  - "Track report status" removed (no ID yet); primary button "Back to home" (wording ours) → `openHome()`.
- Sample data: status bar "07:40".

### WF2P-S05b Confirmation — not accepted
- Base screen: WF2P-S05; flow: E6 19a–19c; display: `showSubmissionResult(result)` with `state = INVALID`, `errors`.
- Change from base:
  - Icon: cross icon in a 96 px circle (2 px border).
  - Headline (bold): "Report not accepted" (wording from E6 "not accepted" / 18k).
  - Error list (error icon, bold): "photo: not a readable JPEG or PNG image" (E2's photo wording — Gaps 4).
  - No report ID, no status tag, **no button**: the screen returns to the review screen (S04) by itself (19c "System … returns to the review screen"); a line "Returning to your report…" (wording ours) states it.
- Sample data: status bar "07:42".

### WF2P-S06a Report status — Rejected, with the reason
- Base screen: WF2P-S06; flow: A4 25h; display: `showReportStatus(report)` (`status = REJECTED`, `rejectionReason`, `verificationTime`).
- Change from base:
  - "Status" — bordered tag with cross icon "Rejected".
  - "Decision time" — "21 May 2026, 08:15".
  - New row "Reason" (card grows to 360 px) — the officer's text as typed, in Sinhala (Text R-1): "RPT-2026-KLT-0843 හි අනුපිටපතකි — ගඟේ එම කොටසම, මිනිත්තු 35කට පෙර; කරුණාකර නව වෙනස්කම් පමණක් වාර්තා කරන්න." (draft — native check required); English translation (25c, verbatim): "Duplicate of RPT-2026-KLT-0843 — same stretch of the river, 35 minutes earlier; please report only new changes." Labels stay English.

### WF2C-S02a Rejection reason and confirmation
- Base screen: WF2C-S02; flow: A4 25a–25d; display: the rejection-reason field and its confirmation dialog (screen components, C2).
- Change from base:
  - Right "Decision" card replaced by card "Reject report" (wording ours) (300 px): field "Rejection reason *" (multi-line, 140 px; helper "Write the reason in the reporter's language — it is sent to the reporter as typed" — wording ours, from SC2 Conventions), containing the Sinhala text R-1 (English translation in this spec only, not on the frame); buttons "Reject" (Danger — 2 px border, cross icon, bold) and "Cancel" (secondary).
  - Modal (520×260 px; centred white card, 2 px border, 40 % black scrim): text "Reject RPT-2026-KLT-0847 with this reason?" (verbatim 25d); the reason repeated in a `#F2F2F2` box; buttons "Confirm rejection" (Danger — white fill, 2 px border, cross icon, bold; wording ours) and "Cancel" (Secondary).
- Controls: "Confirm rejection" → `rejectReport(reportId = "RPT-2026-KLT-0847", reason)`; "Cancel" closes the dialog (C2), values kept.

### WF2C-S02b Reason required
- Base screen: WF2C-S02 with S02a's "Reject report" card (dialog closed); flow: E7 25r–25s; display: `showReasonRequired()`.
- Change from base:
  - "Rejection reason *" field empty, 2 px border; error line under it (error icon, bold): "A rejection reason is required" (verbatim 25s).
  - Buttons "Reject" and "Cancel" as S02a. Status tag still "Pending verification" (nothing recorded).

### WF2C-S02c New-hazard details
- Base screen: WF2C-S02, for report **RPT-2026-KLT-0852**; flow: A6 25m–25n; display: `showReportDetails(report, nearbyReports, candidateHazards)` with no candidate, then the new-hazard form (C2).
- Change from base:
  - Header title "Report RPT-2026-KLT-0852".
  - Report card: "Hazard type" — "Blocked road"; "District" — "Kalutara"; "Status" — "Pending verification". Reporter, reporter type, description, location, capture and received times: each value shown as **"—"**; the photo area shows the muted line "No photo" (P87 UX: a dash reads as "not given" where an empty box reads as a loading failure; SC2 does not give them — Gaps 7).
  - Map: Kalutara hatched; no dot (location not given — Gaps 7).
  - Nearby reports card: the muted line "—" (not given — Gaps 7; P87).
  - Candidate hazards card: "No active blocked-road hazard fits this report" (wording ours; 25m).
  - Right card "Verify as new hazard" (wording ours) (≈ 560 px; the "Candidate hazards" card shrinks to 100 px):
    - "Severity *" select — "Medium" (options Low / Medium / High, `HazardSeverity`) (64 px);
    - "Districts *" — all **25 district chips** in alphabetical order, as in WF1-S03 ("Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo", "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara", "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar", "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya", "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"), wrapped in the 344 px inner width of the 392 px card (24 px padding) (≈ 7 rows, 200 px); **only "Kalutara" selected** (2 px border, check mark);
    - "Place name *" — "Horana–Ratnapura road at Ingiriya" (`NewHazardDetails.locationName`) (64 px);
    - "Description" — empty field (optional; not given — Gaps 7) (80 px);
    - read-only lines "Hazard type Blocked road (from the report)", "Source Verified report" (25o) (48 px);
    - buttons "Verify as New Hazard" (primary) and "Cancel" (52 px).
- Controls: district chips → `NewHazardDetails.districts`; "Verify as New Hazard" → `verifyAsNewHazard(reportId = "RPT-2026-KLT-0852", details : NewHazardDetails)`; "Cancel" → back to the decision card (C2).

### WF2C-S02d New-hazard validation errors
- Base screen: WF2C-S02c; flow: E9 25t–25v; display: `VerificationConsoleUI.showValidationErrors(errors)`.
- Change from base:
  - "Districts *": all 25 chips unselected; the chip group has a 2 px border; error line (error icon, bold): "Districts are required" (verbatim 25u).
  - Severity "Medium", place name "Horana–Ratnapura road at Ingiriya" and the (blank) description kept (25u).
  - Status tag still "Pending verification".

### WF2C-S02e "Report verified"
- Base screen: WF2C-S02; flow: main flow steps 26–28; display: `showDecisionRecorded(report)`.
- Change from base:
  - Report card status tag: "Verified" (check icon).
  - Right panel (both cards) replaced by a confirmation card (300 px; check icon, bold headline): "Report verified" (verbatim 28); rows "Report ID" — "RPT-2026-KLT-0847"; "Linked hazard" — "Flood — Kalu Ganga at Ratnapura (HZ-2026-0142)"; "Verified by" — "K. Perera"; "Verification time" — "21 May 2026, 08:15". Button "Back to queue" → `openQueue()`.

### WF2C-S02f "Report rejected"
- Base screen: WF2C-S02; flow: A4 25e–25g; display: `showDecisionRecorded(report)`.
- Change from base:
  - Report card status tag: "Rejected" (cross icon).
  - Right panel replaced by a confirmation card (340 px; cross icon, bold headline): "Report rejected" (verbatim 25g); rows "Report ID" — "RPT-2026-KLT-0847"; "Reason" — the Sinhala text R-1 as typed; "Rejected by" — "K. Perera"; "Decision time" — "21 May 2026, 08:15". Button "Back to queue" → `openQueue()`.

### WF2C-S02g Escalation prompt
- Base screen: WF2C-S02e ("Report verified" behind the dimmed page); flow: A7 29a–29d (extension point at step 29); display: `showEscalationPrompt(hazard : Hazard, activeWarning : Warning)`.
- Change from base:
  - Modal (560×300 px; centred white card, 2 px border, 40 % black scrim; warning-triangle icon), text verbatim (29a): "Flood — Kalu Ganga at Ratnapura has an active warning — WRN-2026-0087, Level 2 – Amber (Be Alert), Ratnapura and Kalutara, valid until 21 May 2026 10:45. Escalate this warning?" The level is also shown as a bordered tag "Level 2 – Amber (Be Alert)". No time is printed.
  - Buttons: "Escalate" (primary) and "Not now" (secondary).
- Controls: "Escalate" → `EscalationUI.startEscalation(warningId = "WRN-2026-0087")` (Escalate Warning main flow step 3 → WF1b escalation options); "Not now" → `dismissEscalation()` (29d; closes the prompt, nothing changed).

### WF2C-S02h Report already decided
- Base screen: WF2C-S02; flow: E10 26d–26f; display: `showAlreadyDecided(report)`.
- Change from base:
  - Right panel replaced by an alert card (260 px; info icon, bold headline): "This report has already been decided" (verbatim 26e); rows "Recorded status" — bordered tag "Verified"; "Decision time" — "21 May 2026, 08:15" (the E5 → E10 case, 26d: an earlier attempt that seemed to fail had been recorded). Button "Back to queue" → `openQueue()` (26f; the queue no longer lists the report).
  - Decision buttons removed; nothing recorded by this attempt.

## Texts in three languages (warning WRN-2026-0087; message and instructions)

Copied **verbatim** from `frames-WF1.md` Part 0 "Texts in three languages (final drafts)", IDs W-L2-MSG and W-L2-INS (Level 2), with their marks. WF2 uses the Sinhala row on S01 and the English row on S01a–S01d; the Tamil row is shown when "Tamil" is selected (no WF2 frame draws it).

| ID | Language | Text |
|---|---|---|
| W-L2-MSG | English | DMC FLOOD WARNING - Level 2 Amber: Kalu Ganga rising in Ratnapura and Kalutara. Prepare to evacuate low-lying areas. |
| W-L2-INS | English | Move documents and valuables to higher ground. If told to evacuate, go to the safety centre named by your Grama Niladhari. |
| W-L2-MSG | Sinhala | DMC ගංවතුර අවවාදය - මට්ටම 2 ඇම්බර්: රත්නපුර සහ කළුතර දිස්ත්‍රික්කවල කළු ගඟ ඉහළ යයි. පහත් බිම් හැර යාමට සූදානම් වන්න. **(draft — native check required)** |
| W-L2-INS | Sinhala | ලේඛන, වටිනා දෑ උසට ගෙනයන්න. ඉවත් වන්න කීවොත් ග්‍රාම නිලධාරී කී ආරක්ෂිත ස්ථානයට යන්න. **(draft — native check required)** |
| W-L2-MSG | Tamil | DMC வெள்ள எச்சரிக்கை - நிலை 2 அம்பர்: இரத்தினபுரி மற்றும் களுத்துறையில் களு கங்கை உயர்கிறது. தாழ்நிலத்திலிருந்து வெளியேறத் தயாராகவும். **(condensed draft — native check required)** |
| W-L2-INS | Tamil | கூறினால் கிராம உத்தியோகத்தர் கூறும் பாதுகாப்பு நிலையம் செல்லவும். **(condensed draft — native check required)** |

Decision notice and reason (Sinhala only — the reporter's preferred language; SC2 gives English translations; WF2's own drafts):

| ID | English (source) | Sinhala (draft — native check required) |
|---|---|---|
| N-1 verified notice | Your report RPT-2026-KLT-0847 was verified by the DMC (21 May 2026, 08:15) — step 30 | ඔබේ වාර්තාව RPT-2026-KLT-0847 ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් තහවුරු කරන ලදී (2026 මැයි 21, 08:15) |
| N-2 rejected notice | Your report RPT-2026-KLT-0847 was rejected by the DMC (21 May 2026, 08:15). Reason: … (frame wording ours) | ඔබේ වාර්තාව RPT-2026-KLT-0847 ආපදා කළමනාකරණ මධ්‍යස්ථානය විසින් ප්‍රතික්ෂේප කරන ලදී (2026 මැයි 21, 08:15). හේතුව: … |
| R-1 reason | Duplicate of RPT-2026-KLT-0843 — same stretch of the river, 35 minutes earlier; please report only new changes. — 25c | RPT-2026-KLT-0843 හි අනුපිටපතකි — ගඟේ එම කොටසම, මිනිත්තු 35කට පෙර; කරුණාකර නව වෙනස්කම් පමණක් වාර්තා කරන්න. |

## Flow map

Short IDs below: "S01" = WF2P-S01 (phone), "C-S01" = WF2C-S01 (console); state letters as above.

Phone (CitizenAppUI):
- S01 Home —`selectLanguage(SINHALA)`→ S01 (`showHome`, warnings in Sinhala) [steps 3–4]
- S01 —`startReport()` ("Report Hazard" / tab Report)→ S02 (hazard-type list) [5–6]
- S02 —`selectHazardType(FLOOD)`→ S03 (report form) [7–8]
- S03 —`enterDescription`, `attachPhoto` → S03 ("1 of 1"); location → `showLocation` [9–14]
- S03 —`attachPhoto` (unreadable)→ S03c (`showPhotoError`) [E2] —`attachPhoto`→ S03
- S03 —camera refused→ S03d [E2 11d] —"Choose from gallery" `attachPhoto`→ S03
- S03 —no GPS fix→ S03a (`showLocationUnavailable`) [A1] —`enterLocationManually` (outside Sri Lanka)→ S03b (`showValidationErrors`) [E3] —`enterLocationManually`→ S03 ("Entered manually")
- S03 —`reviewReport()` (invalid)→ S03e (`showValidationErrors`) [E1] —`reviewReport()`→ S04
- S03 —`reviewReport()` (valid)→ S04 (`showReview`) [15–16]
- S04 —`editReport()`→ S03 (values kept) [A3]
- S04 —`submitReport()` online→ S05 (`showSubmissionResult` SUBMITTED) [17–20]
- S04 —`submitReport()` offline (or 5xx)→ S05a (`showSubmissionResult` QUEUED) [A2 18a–18c; E6 19d]
- S04 —`submitReport()` server refuses→ S05b (`showSubmissionResult` INVALID) [E6] —automatic return→ S04 [19c]
- S05a —`openHome()` (offline)→ S01a (`showHome`, fromCache, draft "Pending synchronisation") [A8, A2]
- S01a —found `onConnectivityRestored()` → ACCEPTED→ S01b (`showSyncResults` "Report sent — …") [A2 18d–18f]
- S01a —found `onConnectivityRestored()` → REJECTED→ S01c (`showSyncResults` "Report not accepted: …") [E8 18j–18k]
- S01c —`openDraft(draftId)`→ S03f (form with reason) [E8 18l–18m] —`attachPhoto`, `reviewReport()`→ S04 [18n] | —`discardDraft(draftId)`→ S01d (`showHome`) [18o]
- S05 —`openReportStatus(reportId)` ("Track report status")→ S06 (`showReportStatus`)
- console decision recorded → found `receiveReportDecision(notice)` → S01e (`showReportDecision` VERIFIED) [27, 30] or S01f (REJECTED, reason) [25f, 25h]
- S01e —`openReportStatus("RPT-2026-KLT-0847")`→ S06 (Verified) [31–32]
- S01f —`openReportStatus(…)`→ S06a (Rejected with reason) [25h]
- S01b / any home —"My reports" row `openReportStatus(reportId)`→ S06

Console (VerificationConsoleUI):
- sidebar Verification —`openQueue()`→ C-S01 (`showPendingReports`) [21–22]
- C-S01 —`selectReport("RPT-2026-KLT-0847")` ("View")→ C-S02 (`showReportDetails`) [23–24]
- C-S02 —"← Back to queue" `openQueue()`→ C-S01 (report stays Pending verification) [A5]
- C-S02 —`verifyReport(reportId, "HZ-2026-0142")` ("Verify and Link")→ C-S02e (`showDecisionRecorded`) [25–28] —activeWarning present→ C-S02g (`showEscalationPrompt`) [29, A7]
- C-S02g —"Escalate" `EscalationUI.startEscalation("WRN-2026-0087")`→ WF1b escalation options (`showEscalationOptions`) [A7 29b–29c] | —"Not now" `dismissEscalation()`→ C-S02e [29d]
- C-S02 —"Reject"→ C-S02a (reason field, confirmation) [A4 25a–25d] —`rejectReport(reportId, reason)`→ C-S02f (`showDecisionRecorded`) [25e–25g] | empty reason→ C-S02b (`showReasonRequired`) [E7] —`rejectReport` with reason→ C-S02f
- C-S01 —`selectReport("RPT-2026-KLT-0852")`→ C-S02c (no candidate; new-hazard form) [A6 25m–25n] —`verifyAsNewHazard(reportId, details)` valid→ "Report verified" with the new hazard, no prompt (no active warning) [25o–25q] | missing districts→ C-S02d (`showValidationErrors` "Districts are required") [E9] —`verifyAsNewHazard`→ verified
- C-S02 —any decision on a report no longer pending→ C-S02h (`showAlreadyDecided`) [E10] —`openQueue()`→ C-S01 (report gone)
- C-S02e / C-S02f —"Back to queue" `openQueue()`→ C-S01
- C-S02 —server/network failure on a decision→ COMMON-B1 banner (WF1; nothing changed) [E5]

## Gaps

1. **Phone navigation — assumption (Afham may object).** Two tabs only, Home (`openHome`) and Report (`startReport`); no bell and no avatar in the header (wireframe-spec §3 phone header = DMC badge; `HomeView` has no citizen name). Group_060's Alerts and Map tabs are dropped (no use case; warnings are on Home). **Amended P91:** Profile returns as a signposted tab (Register use case).
2. **Phone status-bar times** (low risk). SC2 gives 07:40 (capture), 07:42 (received), 08:15 (decision), 08:20 (replay); 07:41 (S01a) and 08:21 (S01d, S03f) are ours. The home's current location reuses step 13's 6.5888 N, 79.9640 E ±8 m (same phone and place; also the emulator location, standards §10).
3. **Queue rows RPT-2026-KLT-0843 and 0845.** Reporter type not given — "Citizen". District "Kalutara" inferred from SC2's place names (Kalutara North, Nagoda); their times 07:05 and 07:20 are shown as capture times.
4. **Server rejection text (E6, E8).** SC2 gives the reason "Validation failed" and the example "the photo arrived unreadable" but no message; E2's photo wording "photo: not a readable JPEG or PNG image" is reused (S01c, S03f, S05b).
5. **Invalid manual location (E3)** — the sample value latitude "65.888" (with longitude "79.9640") is ours. A1's manual-entry fields are drawn empty (no coordinates given).
6. **Return from "not accepted" (E6 19c)** — drawn as automatic (19c "System … returns to the review screen"); S05b has no button.
7. **A6 report RPT-2026-KLT-0852 — fields left blank — RESOLVED (P85-5): drawn as "—" (P87; the officer's optional description field stays empty):** reporter, reporter type, the report's description, location (coordinates, accuracy), capture time, received time, photo, nearby reports (if any), and the officer's new-hazard description. Given and shown: ID, Blocked road, Kalutara, severity Medium, districts Kalutara, place name "Horana–Ratnapura road at Ingiriya". The report is not in the main-flow queue (C-S01) because its time is unknown.
8. **E10 sample** — "Verified, 21 May 2026, 08:15" (26d with E5: an earlier attempt that seemed to fail had been recorded). Settled.
9. **Scenario–dictionary mismatch — RESOLVED (P85-2, P85-3):** SC2 step 24 now shows the nearby reports' district (Kalutara District) and the source as "Agency bulletin"; the frames already do.

Resolved and removed: former Gap 9 (texts now copied from WF1, E-1), former Gap 10 (the home card stands for `showAlert`), former Gap 11 (08:16 chosen in SC2, 08:25 confirmed — not a gap).
