# Frames WF3 — UC3 Manage Emergency Shelters
Status: v3 + P87 content review (2026-10-09) — aligned with wireframe-spec §2.1 (canonical shell, grid and tokens) after the package review, on top of v2 (after independent review), 2026-10-09. Written by Claude (frames writer, WF3). Sources: `wireframe-spec.md` (all, §1–§10, including §2.1; §5 WF3 inventory: 4 screens + 15 states); `scenarios/SC3-manage-emergency-shelters.md` (all, including Appendix 1 and Appendix 2); `model-dictionary.md` v0.9 §0 conventions 2, 10, 11, §1 enumerations (ShelterStatus, PendingStatus, SyncOutcome, RejectionReason, SubmissionAction), §2 value types (ShelterDetails, ShelterView, ShelterOutcome, PendingSubmission, SyncResult), §3 Shelter, §4 rows `ShelterManagementUI`, `ShelterController`, `OfflineSyncController`; `change-list.md` CH-21, CH-30 (and §17 N14–N20, §19 FX-15 lines); `storyboard-corrections.md` SB3; Received Design Record `uc3-manage-emergency-shelters.md` §5 (hi-fi p45_1, p45_2, p46_1, p46_2, p47_1); `critique-findings.md` D4-10, D4-11, D4-13, D4-14, D4-15; `implementation-standards.md` §1 (Not built, Wireframes), §4 (technical failures, C1), §10 (UC3 pages, offline shelter details), §11 (seed data, demo order), §12.4.

All frames are desktop 1440×1024 (CH-30; spec §4: the District Officer uses a desktop browser). No phone frames, no map, no chart, no photo in WF3.

**Drawing rule (spec §2 "frames carry no annotations").** Only the texts in quotes or in bold values are drawn. Markers in parentheses after a text — "(wording ours)", "(verbatim, …)", "(behaviour ours)", step and flow references, CH/finding IDs, Gap numbers — are notes for this file and for `captions.md`, never drawn. Review notes for a frame are collected in its **Caption notes** line.

## Shell

One shell, reused by every WF3 frame (spec §2.1 "Desktop shell", with spec §3 "Our adjustments").

- **Frame:** desktop 1440×1024, no browser chrome.
- **Sidebar:** 240 px wide, full height. The DMC badge and "DMC Sri Lanka" at the top; one navigation item, **Shelters** (icon: shelter), shown selected on every WF3 frame (selected = §2.1 selected-option token: 2 px black border, bold text). Clicking it is `openShelters()`. Then "Rescue teams" (user icon; Dispatch Rescue Team, Track Rescue Team Status) and "Relief resources" (file icon; Manage Relief Resources, Record Resource Distribution), signposted (muted, tag "Not in this version"; a click shows "<item> — not available in this version" — wireframe-spec §8 rule 12, P91). No other items (Group_060's Dashboard, Warnings, Live Operations, Settings and the footer "Command Center / National Coordination v3.0" are dropped — CH-30, D4-13; its Rescue Dispatch and Supply Chain return as the signposted items). No log-in, log-out or settings.
- **Header:** 80 px high across the remaining 1200 px.
  - Left: page title (page title 24/32 bold) and a one-line subtitle (body 14/20) — given per screen below. Where a screen has a parent, a breadcrumb sits in the subtitle line: "Shelters" (link) + icon chevron-right + the current page name.
  - Right: initials avatar "DO" (icon set: none; a 40 px circle with the initials) with name line **District Officer** and role line **Colombo District** (the scenario's actor and district; SC3 preconditions). No personal name and no photograph (Gaps 1).
- **Content area:** starts at x = 240, y = 80, with 24 px padding on every side: usable **1152 × 896 px**. Grid gaps 16 px. Every screen uses the standard two-column split **744 + 16 + 392 = 1152**.
- **Synchronisation strip** (top of the content area, 48 px high, full 1152 px width; HCI rule 3 — the queued state is always visible on the UC3 page):
  - Left: icon sync + "Waiting for synchronisation: 0" (body 14/20; wording ours; the pending count, QUEUED items only — MD `OfflineSyncController.pendingCount()`, FD-21).
  - Right: secondary button **Sync now** (`syncNow()`), 36 px high; drawn with the Disabled token when the count is 0 or the page is offline, as a secondary button when the count is above 0 and the page is online.
- **Offline banner** (`showConnectivity(false)`): 48 px, full width, above the strip, Notice token (`#F2F2F2` fill, 1 px border) with icon offline and the text **"Offline – entries are saved on this computer"** (SC3 6a, verbatim). Removed by `showConnectivity(true)` (6d).
- **Vertical budget:** usable height 896 − strip 48 − gap 16 = **832 px** for cards online; **784 px** when the offline banner is shown (− 48). Every screen's regions below give heights within this budget; nothing scrolls.
- **Tokens used (spec §2.1 wf theme, by name only):** Card (white, 1 px `#BDBDBD` border, radius 12); Primary button; Secondary button; Disabled; Status tag; Error (field border 2 px black, icon error, message bold below the field) and the form-level error box; Notice (`#F2F2F2` fill, 1 px border, matching icon); selected option. Occupancy bars: 8 px high, `#E0E0E0` track with `#757575` fill (wf greys, §2), no percentage figure.
- **Icons used (spec §2.1 set only):** shelter, sync, offline, check, warning-triangle (notices), error (field and form errors), chevron-right, info.
- **Statuses** are written in full in a Status tag: shelter status **Open** / **Full** (`ShelterStatus`, CH-21); pending item status **Pending synchronisation** / **Rejected** (`PendingStatus` QUEUED / REJECTED, CH-21).
- **Type:** section titles 18/24 bold; body 14/20; field labels 12/16 semibold; figure-card numbers 28/36 bold.
- **Common error banner (C1)** is not drawn in WF3; it is the single frame COMMON-B1 owned by `frames-WF1.md` and is the display for any failed read on this page (list, details online, Find shelter with space, details check) — SC3 "Technical failure (convention)".

## Screens

### WF3-S01 Shelter registry — Colombo District
- Frame: desktop 1440×1024
- Shows: scenario steps 1–2 (and the pending list of step 2) ; boundary `ShelterManagementUI` ; displays `showShelters(shelters)`, `showPending(items)` (items empty in the main flow — Gaps 6) ; reached by `openShelters()` (sidebar **Shelters**, or opening the page).
- Corrects received screen: p45_2 "Safe Shelter Registry & Monitoring" (Received Design Record UC3 §5.2; low-fi p44_1 UC03-05); also replaces p45_1 "Emergency Resource Command"'s "+ Open New Shelter" entry point (§5.1). Answers CH-30, CH-21; D4-13, D4-14, D4-15.
- Layout (top to bottom, left to right):
  1. Header — title **"Shelter Management"**; subtitle **"Emergency shelters of Colombo District"** (wording ours).
  2. Synchronisation strip (48) — "Waiting for synchronisation: 0"; **Sync now** Disabled.
  3. Message slot (left column, 744 × 56, + 16 gap) — empty in the base frame; used by S01a and S01c (Notice token). P88: when empty the slot is collapsed on the drawn screens (only screens are drawn), so the cards start under the strip.
  4. Left column (744), Card "Shelters — Colombo District" (wording ours; section title), height 360: card header 56 with, at the right, Primary button **Register Emergency Shelter** (icon plus); column header 40; up to 4 rows × 56; padding 40. Table (Group_060's registry table, kept), column widths 296 | 96 | 208 | 88 | 56 = 744:
     | Shelter name | Capacity | Current occupancy | Status | |
     |---|---|---|---|---|
     | Rathmalana Maha Vidyalaya | 500 | 320 of 500 + occupancy bar | Open | Select |
     | Dehiwala Municipal Council Hall | 400 | 120 of 400 + occupancy bar | Open | Select |
     | Piliyandala Central College | 600 | 410 of 600 + occupancy bar | Open | Select |
     Every row exactly as SC3 gives it (preconditions, step 4, A1 4b; the seed holds exactly these — standards §11). The statuses of Dehiwala and Piliyandala follow step 8's rule "Open whenever the total is below capacity".
  5. Right column (392), Card **"Pending on this computer"** (wording ours; section title), top aligned with region 3, height 280: card header 56; body. The pending list (`showPending(items)`) — in this base frame empty, with the static line "No entries waiting for synchronisation." (wording ours; Gaps 6). When items exist (S01b, S01c, S02g) each item is a stacked block 104 px high (136 with a Dismiss button), separated by 1 px `#E0E0E0` rules, lines in order: entry (bold) / new value · entered at / Status tag / reason (Rejected only) / button (Rejected only).
  Height check: 56 + 16 + 360 = 432 ≤ 832 (left); 280 ≤ 832 (right).
- Controls:
  - **Register Emergency Shelter** — Primary, enabled → `startRegistration()` (A5 2a) → S04.
  - **Select** on each row (text button, the whole row is also clickable) — enabled → `selectShelter(shelterId)` (step 3) → S02 for that shelter.
  - **Sync now** — Disabled (count 0) → `syncNow()`.
  - Sidebar **Shelters** — selected → `openShelters()`.
- Map / chart / image: none.
- Not shown (removed on purpose): the three KPI cards "Total Shelter Capacity 12,400 / Current Occupancy 8,230 / Available Spots 4,170" (totals not from any scenario step and inconsistent with the rows — D4-15; CH-30 "consistent totals"); "Filter by District" (one district — D4-13, CH-30); the "District" column; the "Amenities (Available)" column and icons (no attribute or step — D4-14, CH-30 "no amenities"); the "Closed" status and the "Matara Rahula Auditorium" row (Open/Full only — CH-21, D4-14); the rows from Ratnapura, Kalutara, Galle and Matara (Group_060's national data; SC3 keeps Group_060's Colombo shelter data — ED3); the "Active Relief Camps & Safe Havens" heading; the percentage figures; the "Lt. Col. S. Jayasinghe / Emergency Response Coordinator" user and photo (D4-13, D4-15).
- Caption notes: the empty pending card and its line are ours (Gaps 6).

### WF3-S02 Shelter details — Rathmalana Maha Vidyalaya, with the occupancy update
- Frame: desktop 1440×1024
- Shows: scenario steps 3–5 (details shown at step 4; the new total 350 typed at step 5, not yet submitted) ; boundary `ShelterManagementUI` ; displays `showShelterDetails(view)` (`ShelterView`: `shelter`, `remainingCapacity`) ; reached by `selectShelter(shelterId)` from S01 (also from A1 4c, A7 9a, A6 6i).
- Corrects received screen: new — no received hi-fi screen (Group_060's hi-fi has no shelter detail, availability or update screen — D4-13); it draws storyboard SB3 panels 4–5 as corrected (SB3-4: the field is the new total). Answers CH-30; D4-10, D4-13.
- Layout (top to bottom, left to right):
  1. Header — title **"Rathmalana Maha Vidyalaya"**; subtitle line: breadcrumb **"Shelters"** (link → `openShelters()`) + chevron-right + **"Shelter details — Colombo District"** (wording ours).
  2. Synchronisation strip (48) — "Waiting for synchronisation: 0"; **Sync now** Disabled.
  3. Message slot (left column, 744 × 56, + 16 gap) — empty in the base frame; used by S02a, S02b, S02e, S02g (Notice token).
  4. Left column (744), Card "Shelter details" (wording ours; section title), height 192: card header 56 with, at the right, Secondary button **Edit details**; five label/value rows × 24; padding 16:
     - Name: Rathmalana Maha Vidyalaya
     - Address: Rathmalana, Colombo
     - District: Colombo
     - Contact number: 071 234 5678
     - Status: Status tag **Open**
  5. Left column, availability row (16 gap above), three figure Cards 237 + 16 + 238 + 16 + 237 = 744, height 120 (Group_060's summary-card style, reused for this shelter only), each a label (12/16) and a big number (28/36):
     - **Capacity** 500
     - **Current occupancy** 320, with the occupancy bar "320 of 500" along the card's bottom edge
     - **Remaining capacity** 180 (= capacity − current occupancy, step 4)
  6. Left column, Card **"Update occupancy"** (wording ours; section title), 16 gap above, height 200: card header 56; field label **"New total occupancy"** (verbatim, step 5) above a 40 px field, 320 px wide, value **350**; right of the field, body text "Remaining capacity: 180"; under the field the help text "The total number of people now in the shelter, not the number of new arrivals." (wording ours, from step 5's "(the total, not the number of new arrivals)"); a 24 px reserved line for a field error; Primary button **Update occupancy** (40 px).
  7. Right column (392), Card **"Find shelter with space"** (verbatim action name, A1 4a; section title), top aligned with region 3, height 320: card header 56; field label "People needing places" (wording ours) above a 40 px field, empty; Secondary button **Find shelter with space**; result area 140 px, empty in this frame (filled in S02c, S02d).
  8. Right column, Card "Pending on this computer" — the S01 region 5 component, 16 gap above, height 240, empty with its static line.
  Height check: left 56 + 16 + 192 + 16 + 120 + 16 + 200 = 616 ≤ 784 (fits offline); right 320 + 16 + 240 = 576 ≤ 784.
- Controls:
  - **New total occupancy** field — enabled, whole-number entry, value 350.
  - **Update occupancy** — Primary, enabled → `enterNewOccupancy(shelterId, newTotal)` (step 5) → S02a (or S02b, S02h, S02i; offline S02g).
  - **Find shelter with space** (+ its field) — Secondary, enabled → `findShelterWithSpace(needed)` (A1 4a) → S02c / S02d.
  - **Edit details** — Secondary, enabled → opens S03 (the edit form is a screen component, C2; its submission is `editShelterDetails`).
  - Breadcrumb **Shelters** / sidebar **Shelters** → `openShelters()` → S01.
  - **Sync now** — Disabled.
- Map / chart / image: none.
- Not shown (removed on purpose): "New Occupant Count" (ambiguous label — D4-10, SB3-4); "Check Availability" as a separate button (availability is shown on selection, step 4); any "No additional occupants can be assigned." text (D4-10).

### WF3-S03 Edit shelter details — Rathmalana Maha Vidyalaya
- Frame: desktop 1440×1024
- Shows: A4 4e (the officer has raised the capacity to 550 and changed the contact number, before submitting) ; boundary `ShelterManagementUI` ; display: none (the form is a screen component, MD §0.11 C2), pre-filled from the `ShelterView` of S02 ; reached by **Edit details** on S02.
- Corrects received screen: new — no received screen (no received artefact edits a shelter; Group_060's A4 had no screen — D4-13). Answers CH-30 ("details and availability (with edit details)").
- Layout (top to bottom, left to right):
  1. Header — title **"Edit details — Rathmalana Maha Vidyalaya"** (wording ours); subtitle line: breadcrumb **"Shelters"** + chevron-right + **"Rathmalana Maha Vidyalaya"** (link → `selectShelter(shelterId)`) + chevron-right + **"Edit details"**.
  2. Synchronisation strip (48) — "Waiting for synchronisation: 0"; **Sync now** Disabled.
  3. Left column (744), form Card "Shelter details" (wording ours; section title), height 600: card header 56; six fields × 72 (label 16 + 4 + field 40 + 12), fields 480 px wide; buttons row 64; padding 48. Fields top to bottom (the `ShelterDetails` fields):
     - Name: Rathmalana Maha Vidyalaya
     - Address: Rathmalana, Colombo
     - District: **Colombo** — read-only text with icon lock (the officer's own district; E2 rule "the district the officer's own district")
     - Capacity: **550** (changed from 500)
     - Contact number: **071 234 5679** (changed from 071 234 5678)
     - Operating organisation (optional): closed pick-list, value **"None"** (SC3 A5 2c "none here"; Gaps 7)
     Buttons row, right-aligned: Secondary **Cancel**, Primary **Save details**.
  4. Right column (392), Notice card, height 88, icon info: **"Current occupancy: 320"** (wording ours) — the figure the capacity is checked against (A4 4f, E4).
  Height check: 600 (+ 24 for the error line in S03a = 624) ≤ 832.
- Controls:
  - Name, Address, Capacity, Contact number, Operating organisation — enabled, values as above. District — read-only.
  - **Save details** — Primary → `editShelterDetails(shelterId, details)` (A4 4e) → online check `validateShelterDetails` → S02e on success, S03a on E4, field errors on E2 (offline: saved as a pending item, as S02g).
  - **Cancel** — Secondary → `selectShelter(shelterId)` (E2 4j / E4 4m "cancels … continues at step 4") → S02.
- Map / chart / image: none.
- Not shown (removed on purpose): amenities (D4-14); any status selector (status is computed — Open below capacity, Full at capacity, A4 4g; no flow closes a shelter — CH-21).

### WF3-S04 Register Emergency Shelter
- Frame: desktop 1440×1024
- Shows: A5 2a–2c (form shown with the district fixed to Colombo; the officer's values entered, before submitting) ; boundary `ShelterManagementUI` ; display: none (`startRegistration()` opens the boundary's own form — SC3 App. 2 A5 2a–2b; C2) ; reached by **Register Emergency Shelter** on S01 (`startRegistration()`), or from S02d.
- Corrects received screen: no received hi-fi registration form; it replaces p45_2's "Register Shelter" button target and p45_1's "+ Open New Shelter" ("open" vs "register" ambiguity — D4-14), and draws storyboard SB3 panel 2 as corrected. Answers CH-30 ("register form", "no 'open new shelter' ambiguity"); D4-13, D4-14.
- Layout (top to bottom, left to right):
  1. Header — title **"Register Emergency Shelter"** (verbatim, A5 2a); subtitle line: breadcrumb **"Shelters"** (link → `openShelters()`) + chevron-right + **"Colombo District"**.
  2. Synchronisation strip (48) — "Waiting for synchronisation: 0"; **Sync now** Disabled.
  3. Left column (744): form-error slot (empty here; used by S04a: 96 + 16 gap), then form Card "Shelter details" (wording ours; section title), height 600 (as S03). Fields top to bottom (`ShelterDetails`):
     - Name: **Wellampitiya Community Hall**
     - Address: **Avissawella Road, Wellampitiya**
     - District: **Colombo** — read-only text with icon lock (A5 2b "with the district fixed to Colombo")
     - Capacity: **250**
     - Contact number: **011 253 4417**
     - Operating organisation (optional): closed pick-list, value **"None"** (A5 2c "none here")
     Buttons row, right-aligned: Secondary **Cancel**, Primary **Register shelter** (wording ours).
  4. Right column (392), Notice card, height 88, icon info: **"A new shelter starts with 0 occupants and status Open."** (wording ours; A5 2e; SB3-3).
  Height check: 600 ≤ 832; S04a: 96 + 16 + 600 + 2 × 24 error lines = 760 ≤ 832.
- Controls:
  - All fields except District — enabled, values as above.
  - **Register shelter** — Primary → `enterShelterDetails(details)` (A5 2c) → online check `validateShelterDetails` → S01a on success, S04a on E2 (offline: saved as a pending item; the shelter appears in the list only after synchronisation — A6 6c).
  - **Cancel** — Secondary → `openShelters()` (E2 2h "cancels … continues at step 2") → S01.
- Map / chart / image: none.
- Not shown (removed on purpose): an initial-occupancy field (registration produces occupancy 0 — D4-11, SB3-3); amenities (D4-14); a district selector (one district — D4-13).

## States

### WF3-S01a Shelter registered
- Base screen: WF3-S01; flow: A5 2e–2f; display: `showShelterRegistered(view)`, then `showShelters(shelters)`.
- Change from base: message slot (744 × 56) shows a Notice with icon check: **"Shelter registered"** (verbatim, 2f) in bold, then on the same line **"Wellampitiya Community Hall — capacity 250, occupancy 0, remaining capacity 250"** (values from 2f; sentence form ours). The table has a fourth row with `#F2F2F2` fill marking the new entry (behaviour ours).
- Sample data: new row | Wellampitiya Community Hall | 250 | 0 of 250 + empty occupancy bar | Open | Select. Other three rows unchanged (Rathmalana 320, Dehiwala 120, Piliyandala 410). Height: 56 + 16 + 360 = 432.
- Caption notes: remaining capacity 250 from the view (MD v0.10, P85-1); the row highlight is our behaviour.

### WF3-S01b Pending items online, with "Sync now"
- Base screen: WF3-S01; flow: A6 6d — "While online, the officer can also press **Sync now** to send the pending items at once, for example items left pending by a server error" — and 6e — "An item that meets a server error or a new network failure stays pending for the next attempt"; display: `showPending(items)`, `showConnectivity(true)` (no offline banner).
- Change from base: strip reads **"Waiting for synchronisation: 2"**; **Sync now** enabled (Secondary — the page's Primary stays Register Emergency Shelter). The "Pending on this computer" card lists two stacked item blocks:
  1. **Occupancy update — Rathmalana Maha Vidyalaya** / "New total 350 · 14:05" / Status tag **Pending synchronisation**
  2. **Occupancy update — Piliyandala Central College** / "New total 610 · 14:20" / Status tag **Pending synchronisation**
- Sample data: registry unchanged from S01 (Rathmalana 320, Piliyandala 410 — pending entries do not change the figures, 6b). Pending card height 56 + 2 × 104 + 16 = 280.
- Caption notes: the Piliyandala entry time 14:20 is an assumption (P85-5; SC3 bounds it between 14:05 and 14:40).

### WF3-S01c Synchronisation results with the rejected item
- Base screen: WF3-S01; flow: A6 6e–6f; display: `showSyncResults(results)`, `showPending(items)`, then `showShelters(shelters)`.
- Change from base: message slot shows a Notice with icon sync: **"1 accepted, 1 rejected — Piliyandala Central College: 610 exceeds the capacity of 600"** (verbatim, 6f). The "Pending on this computer" card lists one stacked item block (136 px):
  - **Occupancy update — Piliyandala Central College** / "New total 610 · 14:20" / Status tag **Rejected** / reason **"over capacity"** (verbatim, 6e) followed by "610 exceeds 600" (wording ours) / Secondary button **Dismiss** (icon cross).
  Strip reads "Waiting for synchronisation: 0" (rejected items are not counted — FD-21); **Sync now** Disabled.
- Sample data: registry row Rathmalana Maha Vidyalaya | 500 | **350 of 500** | Open (the accepted total, 6f); Dehiwala and Piliyandala unchanged (120, 410). Control: **Dismiss** → `dismissPending(submissionId)` (6g).
- Caption notes: the entry time 14:20 is an assumption (P85-5); "610 exceeds 600" beside the reason is our wording.

### WF3-S01d Rejected item dismissed
- Base screen: WF3-S01; flow: A6 6g–6h; display: `showPending(items)`.
- Change from base: the message slot is empty again; the pending card shows the static empty line "No entries waiting for synchronisation." (wording ours); strip "Waiting for synchronisation: 0"; **Sync now** Disabled.
- Sample data: registry as after sync — Rathmalana 350 of 500 Open; Dehiwala 120 of 400 Open; Piliyandala 410 of 600 Open (the officer may now re-enter Piliyandala — 6i → S02 for Piliyandala).

### WF3-S02a Occupancy updated
- Base screen: WF3-S02; flow: steps 6–9; display: `showOccupancyUpdated(view)`.
- Change from base: message slot shows a Notice with icon check: **"Occupancy updated"** (verbatim, step 9). Figure cards: Capacity **500** | Current occupancy **350** (bar "350 of 500") | Remaining capacity **150**; status tag Open. The "New total occupancy" field is empty (behaviour ours); the text beside it reads "Remaining capacity: 150".
- Sample data: current occupancy 350, remaining capacity 150, status Open (step 9).
- Caption notes: remaining capacity 150 from the view (MD v0.10, P85-1). Clearing the field is our behaviour.

### WF3-S02b Shelter full
- Base screen: WF3-S02; flow: A2 8a–8c; display: `showShelterFull(view)`.
- Change from base: message slot shows a Notice with icon warning-triangle: **"Rathmalana Maha Vidyalaya is full (500 of 500)"** (verbatim, 8c). Status tag **Full**. Figure cards: Capacity **500** | Current occupancy **500** (bar full) | Remaining capacity **0**. The "Find shelter with space" card has a 2 px black border as the suggested next action (8c "with the Find shelter with space action").
- Sample data: new total entered 500; occupancy 500; remaining 0; status Full.
- Caption notes: remaining capacity 0 from the view (MD v0.10, P85-1).

### WF3-S02c Alternatives found
- Base screen: WF3-S02; flow: A1 4a–4b (then 4c); display: `showNoShelterAvailable(alternatives)` (non-empty list).
- Change from base: "People needing places" field value **200**. Result area (140 px) of the Find card:
  - Line 1 (body): **"Rathmalana Maha Vidyalaya cannot take 200 people — 180 places remain."** (wording ours; 4b "shows that the selected shelter cannot take them").
  - Line 2 (label 12/16): **"Shelters in Colombo District with at least 200 places"** (wording ours).
  - One stacked result block, 1 px `#BDBDBD` border: **Dehiwala Municipal Council Hall** / "Capacity 400 · Occupancy 120 · 280 remaining" / Status tag **Open** and, right, text button **Select**.
- Sample data: needed 200; one alternative (Piliyandala Central College, 600/410/190 remaining, is not listed — 4b). Rathmalana figures unchanged (320, 180). Control: **Select** → `selectShelter(shelterId)` (4c) → S02 for Dehiwala Municipal Council Hall (not drawn; Gaps 4).
- Caption notes: "280 remaining" from each alternative's view (MD v0.10, P85-1).

### WF3-S02d No shelter with space
- Base screen: WF3-S02; flow: A1 4d — the conditional branch "If no shelter in the district has enough places"; display: `showNoShelterAvailable(alternatives)` (empty list).
- Change from base: "People needing places" field value **300**. Result area: Notice with icon info: **"No shelter in Colombo District can take 300 people."** (from 4d "no shelter in Colombo District can take 300 people"; sentence form ours), and under it Secondary button **Register Emergency Shelter** (→ `startRegistration()`, 4d "registers a new shelter (A5, from step 2a)") → S04.
- Sample data: needed 300; no alternatives — Dehiwala Municipal Council Hall has 280 places left, Piliyandala Central College 190. Rathmalana unchanged (320, 180).
- Caption notes: 300 is SC3 A1 4d's example figure (P87; it was 200, which Dehiwala's 280 places could take).

### WF3-S02e Details updated
- Base screen: WF3-S02; flow: A4 4f–4h; display: `showShelterUpdated(view)`.
- Change from base: message slot shows a Notice with icon check: **"Shelter details updated"** (verbatim, 4h). Details card: Contact number **071 234 5679**. Figure cards: Capacity **550** | Current occupancy **320** (bar "320 of 550") | Remaining capacity **230**; status tag Open. The text beside the empty "New total occupancy" field reads "Remaining capacity: 230" — the flow may continue at step 5.
- Sample data: capacity 550, contact 071 234 5679, occupancy 320, remaining 230, Open (4h).
- Caption notes: remaining capacity 230 from the view (MD v0.10, P85-1).

### WF3-S02f Offline banner
- Base screen: WF3-S02; flow: A6 6a; display: `showConnectivity(false)` (after the found message `onConnectivityLost()`).
- Change from base: the offline banner (48 px, Notice, icon offline) appears above the strip: **"Offline – entries are saved on this computer"** (verbatim, 6a). **Sync now** Disabled. All other content unchanged (details from the list data already on the page — 6c, FX-15): Capacity 500 | Current occupancy 320 | Remaining capacity 180; "New total occupancy" 350 typed; **Update occupancy** still enabled (the entry will be saved offline). **Find shelter with space** stays enabled, with the muted hint "Needs a connection" (wording ours; P87 UX) under the button, because the search runs on the server (SC3 6c).
- Sample data: as S02. Height: left column 616 ≤ 784.
- Caption notes: pressing Find shelter with space offline shows the common banner COMMON-B1 (drawn once, in WF1) alongside the offline banner (SC3 6c; Gap 11).

### WF3-S02g Saved offline – pending sync
- Base screen: WF3-S02; flow: A6 6b; display: `showSavedOffline(pendingCount)`, `showPending(items)` (banner still shown — `showConnectivity(false)`).
- Change from base: offline banner as S02f. Message slot shows a Notice with icon sync: **"Saved offline – pending sync"** (verbatim, 6b); strip reads **"Waiting for synchronisation: 1"** (the pending count, 6b); **Sync now** Disabled (offline). The pending card lists one stacked item block:
  - **Occupancy update — Rathmalana Maha Vidyalaya** / "New total 350 · 14:05" / Status tag **Pending synchronisation**
  The shelter's figures are **not** changed: Capacity 500 | Current occupancy 320 | Remaining capacity 180; status Open (6b "The shelter's figures on screen are not changed"). The "New total occupancy" field is empty (behaviour ours).
- Sample data: pending count 1; item entered 14:05. Height: left 616, right 320 + 16 + 176 = 512, both ≤ 784.
- Caption notes: clearing the field is our behaviour.

### WF3-S02h Capacity exceeded
- Base screen: WF3-S02; flow: E1 7a–7b; display: `showCapacityExceeded(capacity, remainingCapacity)`.
- Change from base: "New total occupancy" field keeps **520** with the Error token (2 px black border); in the reserved line under it, icon error and, in bold, **"Capacity exceeded: 520 is more than the capacity of 500; 180 places remain"** (verbatim, 7b). Figure cards unchanged: Capacity 500 | Current occupancy 320 | Remaining capacity 180 (7b "with the current occupancy still 320"). **Update occupancy** and **Find shelter with space** both enabled (7c).
- Sample data: entered 520; capacity 500; remaining 180.

### WF3-S02i Invalid occupancy
- Base screen: WF3-S02; flow: E3 7d–7e; display: `showInvalidOccupancy(message)`.
- Change from base: "New total occupancy" field keeps **−20** with the Error token; under it, icon error and, in bold, **"Enter the total number of people now in the shelter as a whole number of 0 or more."** (verbatim, 7e). Figure cards unchanged (500, 320, 180).
- Sample data: entered −20 (the same message is shown for a non-numeric entry such as "3S0", refused on the page — 7d; not drawn separately).

### WF3-S03a Capacity below occupancy
- Base screen: WF3-S03; flow: E4 4k–4l; display: `showShelterDetailsErrors(errors)`.
- Change from base: Capacity field keeps **300** with the Error token; under it, icon error and, in bold, **"Capacity 300 is below the current occupancy of 320"** (verbatim, 4l). Other fields keep the values entered (C2): Name Rathmalana Maha Vidyalaya; Address Rathmalana, Colombo; District Colombo; Contact number **071 234 5679**; Operating organisation None — only the capacity differs from S03. Notice "Current occupancy: 320" unchanged. **Save details** and **Cancel** enabled (4m).
- Sample data: capacity entered 300; contact number 071 234 5679 (as entered in the form); current occupancy 320; the shelter keeps capacity 500 and status Open (nothing saved — 4l). Height 624 ≤ 832.

### WF3-S04a Shelter-details errors
- Base screen: WF3-S04; flow: E2 2g (registration; the same display serves 4i for an edit); display: `showShelterDetailsErrors(errors)`.
- Change from base: the form-error slot shows the form-level error box (white card, 2 px border, icon error, 96 px): **"2 fields need correcting. Nothing was saved."** (wording ours; 2g "It saves nothing") and a two-item list "Capacity", "Contact number". Two fields with the Error token, each with icon error and a bold message under it (the form keeps every value entered — 2g):
  - Capacity: value **0** → **"Capacity must be a whole number above zero."** (wording ours; rule "the capacity a whole number above zero").
  - Contact number: value **71 234 5678** → **"Contact number must be a 10-digit Sri Lankan number starting with 0."** (wording ours; rule "a 10-digit Sri Lankan number starting with 0").
- Sample data: Name Wellampitiya Community Hall; Address Avissawella Road, Wellampitiya; District Colombo; Capacity 0; Contact number 71 234 5678; Operating organisation None (E2 example values in the A5 form). Height 760 ≤ 832.
- Caption notes: the summary line and the two field messages are our wording; SC3 E2 gives the rules only (Gap 8).

## Flow map

Desktop thumbnails, arrows labelled "user action (input operation) → display".

- (page opened) → **S01**: `openShelters()` → `showShelters`, `showPending` (on load online with pending items: `onConnectivityRestored()` → as S01c).
- S01 → **S02**: Select Rathmalana Maha Vidyalaya — `selectShelter(shelterId)` → `showShelterDetails(view)`.
- S01 → **S04**: Register Emergency Shelter — `startRegistration()` → registration form.
- S02 → **S02a**: Update occupancy (350) — `enterNewOccupancy(shelterId, 350)` → `showOccupancyUpdated(view)`.
- S02 → **S02b**: Update occupancy (500) — `enterNewOccupancy(shelterId, 500)` → `showShelterFull(view)`.
- S02 → **S02h**: Update occupancy (520) — `enterNewOccupancy(shelterId, 520)` → `showCapacityExceeded(500, 180)`.
- S02 → **S02i**: Update occupancy (−20) — `enterNewOccupancy(shelterId, −20)` → `showInvalidOccupancy(message)`.
- S02h → S02 (corrected total, step 5) or → S02c (`findShelterWithSpace`).
- S02i → S02: corrected value — `enterNewOccupancy` (E3 7f).
- S02 / S02b / S02h → **S02c**: Find shelter with space (200) — `findShelterWithSpace(200)` → `showNoShelterAvailable([Dehiwala Municipal Council Hall])`.
- S02 → **S02d**: Find shelter with space (300), none found — `findShelterWithSpace(300)` → `showNoShelterAvailable([])`.
- S02c → S02 (Dehiwala Municipal Council Hall): Select — `selectShelter(shelterId)`.
- S02d → **S04**: Register Emergency Shelter — `startRegistration()`.
- S02 → **S03**: Edit details (form opens).
- S03 → **S02e**: Save details (550, 071 234 5679) — `editShelterDetails(shelterId, details)` → `showShelterUpdated(view)`.
- S03 → **S03a**: Save details (capacity 300) — `editShelterDetails(shelterId, details)` → `showShelterDetailsErrors(errors)`.
- S03 / S03a → S02: Cancel — `selectShelter(shelterId)`.
- S02e → S02a: step 5 continues — `enterNewOccupancy`.
- S04 → **S01a**: Register shelter — `enterShelterDetails(details)` → `showShelterRegistered(view)`, `showShelters(shelters)`.
- S04 → **S04a**: Register shelter (capacity 0, contact 71 234 5678) — `enterShelterDetails(details)` → `showShelterDetailsErrors(errors)`; S04a → S01a after correction; S04 / S04a → S01: Cancel — `openShelters()`.
- S02 → **S02f**: connection lost — found `onConnectivityLost()` → `showConnectivity(false)`.
- S02f → **S02g**: Update occupancy (350) offline — `enterNewOccupancy(shelterId, 350)` → `showSavedOffline(1)`, `showPending(items)`.
- S02g → (Piliyandala Central College selected offline, details from list data; new total 610 entered offline — `enterNewOccupancy(shelterId, 610)` → `showSavedOffline(2)`, `showPending(items)`; not drawn) → **S01c**: connection restored — found `onConnectivityRestored()` → `showConnectivity(true)`, `showSyncResults(results)`, `showPending(items)`, `showShelters(shelters)`.
- (items left pending while online, e.g. after a server error — 6d, 6e) → **S01b**: `showPending(items)` with Sync now enabled.
- S01b → **S01c**: Sync now — `syncNow()` → `showSyncResults(results)`, `showPending(items)`, `showShelters(shelters)`.
- S01c → **S01d**: Dismiss — `dismissPending(submissionId)` → `showPending(items)`.
- S01d → S02 (Piliyandala Central College): re-enter a correct total — `selectShelter(shelterId)` (6i).
- S02a / any → S01: Shelters — `openShelters()`; S02a → S02 (another shelter) via S01 — `selectShelter` (A7 9a).

## Gaps

1. **District Officer's name** — resolved: no source names the Colombo District Officer; the header shows "District Officer / Colombo District" with initials "DO".
2. **Remaining capacity in five displays — RESOLVED (P85-1, MD v0.10):** the displays take `ShelterView` (the alternatives `List<ShelterView>`), built from the write reply's `ShelterOutcome.shelter` and `remainingCapacity`.
3. **Entry time of the offline Piliyandala update (610)** — RESOLVED as an assumption (P85-5): 14:20, between 14:05 and 14:40 (SC3 6b–6e).
4. **Dehiwala Municipal Council Hall and Piliyandala Central College details** — not blocking: SC3 gives no address or contact number for them, so the registry has no address column; their details screens are not §5 frames and are not drawn.
5. **A1 4d sample data** — resolved (P87): S02d and SC3 A1 4d use 300 people; no Colombo shelter has 300 places left (Dehiwala 280, Piliyandala 190), so the frame agrees with the registry.
6. **Empty pending list** — resolved: `showPending(items)` is called only when items exist; the base S01/S02 frames keep the "Pending on this computer" card with a static line "No entries waiting for synchronisation." (wording ours), so the queued state has a fixed place (HCI rule 3).
7. **Operating organisation pick-list** — resolved: shows "None" only (SC3 A5 2c "none here"); no organisation options are drawn.
8. **E2 and E4 wording and values** — resolved: the E4 text is SC3's, verbatim; the two E2 field messages and the S04a summary line are ours (Caption notes). In S03a the form keeps the values entered (C2): contact number 071 234 5679 from S03; only the capacity changes, to 300.
9. **Online refusal of a registration or edit by the server's re-check (A5 2e, E4 slip-through)** — shown by `showSyncResults([result])` only; not in the §5 inventory, not drawn.
10. **Colombo flood warning (precondition)** — not shown: `ShelterManagementUI` has no display for warnings.
11. **Find shelter with space while offline** — resolved: pressing it offline shows the common banner COMMON-B1 (SC3 6c, App. 2 6c, MD C1) alongside the offline banner; COMMON-B1 is drawn once in the WF1 file, so there is no WF3 frame for it.
