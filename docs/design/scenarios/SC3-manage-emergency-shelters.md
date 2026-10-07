# Use Case: UC3 Manage Emergency Shelters
**Use Case Scenario:** During a Level 2 – Amber (Be Alert) flood warning for Colombo District, the District Officer records that Rathmalana Maha Vidyalaya now shelters 350 people against a capacity of 500

*Primary actor: District Officer. Secondary actors: none. Extension points: `new shelter to register` (step 2), `network unavailable` (step 6; also steps 2e and 4g). Included: Track Shelter Occupancy (steps 5–8), which includes Check Shelter Capacity (step 7; extension point `shelter capacity full`, extended by Mark Shelter Full).*

**Description:**
This use case scenario describes how a District Officer of the District Disaster Management Coordinating Unit keeps the emergency shelters (safety centres) of the officer's own district up to date during a disaster response: the officer opens the district's shelter list, selects a shelter, sees its details and remaining capacity, and records the new total number of people in it, which the system checks against the shelter's capacity before keeping it as an occupancy record and setting the shelter's status to Open or Full. Registering a new shelter, editing a shelter's details, finding a shelter with space and recording updates while the network is unavailable are handled as alternate flows.

*Conventions:* Forms and pick-lists (the "New total occupancy" field, the registration and edit forms) are parts of the screen; a form shown again keeps the values already entered.

**Preconditions:**
- A disaster warning or emergency situation has been identified and requires district-level response (here, a Level 2 – Amber flood warning for Colombo District).
- The District Officer is identified to the system as an officer of Colombo District (sign-in is outside the scope of this design).
- Zero or more shelters of Colombo District are registered, each with name, address, district, capacity, contact number, current occupancy and status; in this instance Rathmalana Maha Vidyalaya, Dehiwala Municipal Council Hall and Piliyandala Central College are among them (with none registered, the officer starts with A5).
- The Shelter Management page is opened while the network is available; the connection may be lost afterwards (A6). The browser keeps no offline copy of the page (no service worker).

**Main Flow:**
1. District Officer opens **Shelter Management** in the officer web application.
2. System lists the shelters of the officer's district, Colombo, each with its capacity, current occupancy and status (Open or Full), and shows any entries still waiting for synchronisation on this computer as pending items with the pending count; pending items left from an earlier session are sent at once, because the page is opened online (as A6 6d–6f). *(Extension point `new shelter to register`.)*
3. District Officer selects **Rathmalana Maha Vidyalaya**.
4. System retrieves the selected shelter and shows its name, address (Rathmalana, Colombo), district, contact number (071 234 5678), capacity (500), current occupancy (320), remaining capacity (180, calculated as capacity minus current occupancy) and status (Open).
5. District Officer enters the new total number of people now in the shelter, **350**, in the field labelled "New total occupancy" (the total, not the number of new arrivals), and submits it.
6. System sends the update with the officer's identity, a new submission ID and the entry time (14:05) to the server; the network is available. *(Extension point `network unavailable`.)*
7. System validates the new total (a whole number, not negative) and checks it against the shelter's capacity (*Check Shelter Capacity*): 350 is within 500. Nothing has been saved yet.
8. System creates an occupancy record (350 people, captured at 14:05, received at 14:05, recorded by the officer) and adds it to the shelter's history; the shelter's current occupancy becomes 350, its remaining capacity 150 and its status Open (Open whenever the total is below capacity, so a Full shelter whose total falls is opened again). System also records the submission as processed and updates this computer's synchronisation status.
9. System confirms "Occupancy updated" and shows the shelter with current occupancy 350, remaining capacity 150 and status Open; the use case ends.

**Alternate Flows:**

**A1 — No suitable shelter available** (at step 4; also reachable from A2 and E1)
- 4a. District Officer needs places for 200 evacuees, more than the 180 remaining, and enters 200 under **Find shelter with space**.
- 4b. System takes the district of the selected shelter (Colombo), searches the shelters of Colombo District for at least 200 remaining places and shows that the selected shelter cannot take them, with the alternatives found: Dehiwala Municipal Council Hall (capacity 400, occupancy 120, 280 remaining). Piliyandala Central College (600, 410, 190 remaining) is not listed.
- 4c. District Officer selects Dehiwala Municipal Council Hall from the alternatives; the flow continues at step 4 for that shelter.
- 4d. If no shelter in the district has enough places (for example, the District Officer enters 300 and the most any Colombo shelter has left is 280), System shows that no shelter in Colombo District can take 300 people; District Officer registers a new shelter (A5, from step 2a) or the use case ends.

**A2 — Shelter occupancy reaches capacity** (at steps 7–8; *Mark Shelter Full* extends *Check Shelter Capacity* at extension point `shelter capacity full`)
- 8a. At step 7 the new total entered at step 5 is 500, equal to the capacity of 500.
- 8b. System creates the occupancy record as in step 8 and sets the shelter's current occupancy to 500, its remaining capacity to 0 and its status to **Full**.
- 8c. System shows "Rathmalana Maha Vidyalaya is full (500 of 500)" with the shelter marked Full and the **Find shelter with space** action; District Officer continues with A1 (step 4a) or the use case ends.

**A4 — Shelter information updated** (at step 4)
- 4e. District Officer chooses **Edit details**, raises the capacity from 500 to 550 (two more classrooms opened) and changes the contact number to 071 234 5679, and submits.
- 4f. System checks the changed details with the server against the shelter-detail rules listed in E2 and, for this shelter, the capacity rule of E4 (capacity not below the current occupancy); if they fail, E2 or E4 applies. (While the network is unavailable this check is skipped and the server re-checks the details on replay — A6.)
- 4g. System sends the change with a new submission ID as in step 6 (if the network is unavailable, A6 applies). The server re-validates it, also checking again that the new capacity is not below the current occupancy (E4, slip-through case), updates the shelter's details and recomputes its status: Full when the current occupancy equals the capacity, otherwise Open.
- 4h. System confirms "Shelter details updated" and shows capacity 550, current occupancy 320, remaining capacity 230 and status Open; the flow continues at step 5, or the use case ends.

**A5 — Register Emergency Shelter** (at step 2; extension point `new shelter to register`)
- 2a. District Officer selects **Register Emergency Shelter**.
- 2b. System shows the registration form with the district fixed to Colombo.
- 2c. District Officer enters the name (Wellampitiya Community Hall), address (Avissawella Road, Wellampitiya), capacity (250), contact number (011 253 4417) and, optionally, the operating organisation (none here), and submits.
- 2d. System checks the details as in step 4f; if they fail, E2 applies.
- 2e. System sends the registration with a new submission ID as in step 6 (if the network is unavailable, A6 applies); the server re-validates it (a refusal by this re-check is shown online as in E4's slip-through case, in the synchronisation results only, and on replay as in A6 6f) and creates the shelter with current occupancy 0 and status Open, recorded as registered by the officer.
- 2f. System confirms "Shelter registered" and shows Wellampitiya Community Hall with capacity 250, occupancy 0 and remaining capacity 250; the flow continues at step 2 with the new shelter in the list.

**A6 — Save Data Offline** (at step 6, and at steps 2e and 4g; extension point `network unavailable`; *Save Data Offline* includes *Synchronise Offline Data*, which runs when the connection is restored)
*Precondition of this flow: the page was opened while online.*
- 6a. The browser reports that the connection has been lost, and System shows the offline banner ("Offline – entries are saved on this computer"). (If the update at step 6 fails with a network error before the browser reports the loss, it is treated as from 6a: the offline banner is shown and the entry is saved as in 6b.)
- 6b. System saves the entry on this computer as a pending item (occupancy update for Rathmalana Maha Vidyalaya, new total 350, entered at 14:05, with its submission ID; status "Pending synchronisation") and shows "Saved offline – pending sync" and the pending count (1). The shelter's figures on screen are not changed.
- 6c. District Officer may enter further updates for the shelters listed on the page. Offline, selecting a listed shelter shows its details from the list data already on the page, without a server read (only in this offline case does the page itself work out the remaining capacity, from the capacity and current occupancy in that list data); each update is saved the same way and the pending count rises (2 after the officer selects Piliyandala Central College, capacity 600, occupancy 410, and enters a new total of 610). Only the server reads are unavailable offline — Find shelter with space and the details check of steps 2d and 4f; for these System shows that the service is unavailable and nothing was changed. The page does not check an offline total against the capacity it shows; the server checks it on replay (6e) — a stated limitation of this design (a warning on the page is future work). Registrations and detail edits are saved the same way, without the details check of steps 2d and 4f, which needs the server; the server checks them on replay (6e). A shelter registered offline appears in the list only after synchronisation, and its occupancy cannot be updated before then.
- 6d. When the connection is restored (or when the page is opened online while pending items remain from an earlier session — step 2), System removes the offline banner and sends each pending item to the server with its original submission ID and entry time. (While online, the officer can also press **Sync now** to send the pending items at once, for example items left pending by a server error.)
- 6e. The server re-validates each item as in steps 7–8 (or 2e, 4g): the Rathmalana update is accepted and its occupancy record keeps captured at 14:05 and received at 14:40 (had another officer recorded a newer total for Rathmalana online meanwhile, captured after 14:05, the replayed record would be kept in the shelter's history without changing its current occupancy); if an item had already been processed by the server (its reply was lost), it would be reported as a duplicate and not recorded twice; the Piliyandala update is rejected with the reason "over capacity" (610 exceeds 600). An item that meets a server error or a new network failure stays pending for the next attempt. *Limitation (stated):* entry times come from the clock of the computer on which each entry was made, so a clock running behind or ahead on one officer's computer can make a fresh total look older than another officer's (it is then kept as history only) or a stale one look newer; clock differences between computers are not corrected in this design.
- 6f. System removes the accepted and duplicate items from the pending list, marks the rejected item "Rejected" with its reason, and shows the synchronisation results ("1 accepted, 1 rejected — Piliyandala Central College: 610 exceeds the capacity of 600"); the rejected item stays visible in the pending list with its reason. System then shows the shelter list again with the accepted totals (Rathmalana Maha Vidyalaya: current occupancy 350, status Open). A registration or detail edit that fails the server's re-check is reported in the same way.
- 6g. District Officer reads the rejection and dismisses the rejected Piliyandala item.
- 6h. System removes the rejected item from this computer and shows the pending list without it (pending count 0).
- 6i. District Officer re-enters a correct total for Piliyandala Central College (the flow continues at step 3), or the use case ends.

**A7 — Another shelter** (at step 9)
- 9a. District Officer selects another shelter from the list; the flow continues at step 3.

**Postconditions:**
- Each accepted update is kept as an occupancy record of its shelter (number of people, captured-at and received-at times, recording officer), so occupancy over time can be reported.
- The shelter's current occupancy equals the accepted total with the latest capture time (a replayed total captured before the latest recorded one is kept in the history only), and its status is Open below capacity and Full at capacity (recomputed after a capacity change); a rejected entry leaves the shelter unchanged.
- A shelter registered in this use case is stored with name, address, district, capacity, contact number, occupancy 0 and status Open, linked to the registering officer.
- Every accepted submission is recorded once (a replayed duplicate creates nothing), and this computer's synchronisation status (last contact, pending count) is updated.
- After an offline session every pending item has been accepted, rejected with its reason (and shown to the officer until the officer dismisses it), or is still pending and shown as such.
- The District Officer has seen the outcome of each entry.

**Exception Flows:**

*Technical failure (convention): if a server or network failure occurs while the screen reads from the server (the shelter list, a shelter's details while online, Find shelter with space, or the details check of steps 2d and 4f), System shows that the service is unavailable and nothing was changed, and the officer may retry. Writes follow A6 instead.*

**E1 — Occupancy exceeds capacity** (at step 7)
- 7a. District Officer entered 520 at step 5; Check Shelter Capacity finds that 520 exceeds the capacity of 500. Nothing is saved.
- 7b. System rejects the update (reason: over capacity) and shows "Capacity exceeded: 520 is more than the capacity of 500; 180 places remain", with the current occupancy still 320.
- 7c. District Officer enters a total within capacity (the flow continues at step 5) or looks for a shelter with space for the others (A1, step 4a).

**E2 — Shelter details fail validation** (at step 2d of A5 or step 4f of A4)
- 2g / 4i. System checks the details against the shelter-detail rules — name, address and contact number present; the contact number a 10-digit Sri Lankan number starting with 0; the district the officer's own district; the capacity a whole number above zero; the operating organisation, if given, a registered organisation — and finds errors (for example, capacity "0" and contact number "71 234 5678", nine digits without the leading 0). It saves nothing and shows each error beside its field; the form keeps the values entered.
- 2h / 4j. District Officer corrects the details and submits again; the flow continues at step 2d (registration) or step 4f (edit), or the officer cancels and the flow continues at step 2 (registration) or step 4 (edit).

**E3 — Invalid Shelter Occupancy** (at step 7; at step 5 for a non-numeric entry)
- 7d. District Officer entered a negative total (−20) at step 5; System finds it is not a valid total, rejects the update (reason: invalid value) and saves nothing. A non-numeric entry (for example "3S0") cannot be read as a whole number and is refused on the page without being sent.
- 7e. System shows "Enter the total number of people now in the shelter as a whole number of 0 or more."
- 7f. District Officer corrects the value; the flow continues at step 5.

**E4 — Capacity below current occupancy** (at step 4f of A4)
- 4k. At step 4e the District Officer entered a capacity of 300 (part of the building closed), below the current occupancy of 320.
- 4l. The check of step 4f (the details validated for this shelter) fails: "Capacity 300 is below the current occupancy of 320"; System shows the error beside the capacity field, keeps the values entered and saves nothing; the shelter keeps capacity 500 and status Open. (The server repeats the check when the change arrives; a change that slips through — e.g. occupancy rose meanwhile — is refused with reason validation failed; online nothing is held on this computer, so System shows the refusal in the synchronisation results only, with no pending item.)
- 4m. District Officer enters a capacity of at least 320 (the flow continues at step 4e), moves people to another shelter first (A1, step 4a), or cancels (the flow continues at step 4). A detail edit saved offline and refused on replay for this reason is reported as in A6 6f.

---

## Appendix 1 — Changes applied (working appendix — not for the report body)

| CH | What changed relative to Group_060's scenario (their step / flow IDs) | Findings answered |
|---|---|---|
| CH-28 | Restructured on the lecture's ATM pattern: one transaction in the main flow (list district shelters → select → details and remaining capacity → enter the **new total** → validated and checked against capacity **before** saving → occupancy record, status Open/Full → confirmation, use case ends — their steps 1–4, 10–21); their step 22 ("continues monitoring", no end) replaced by an explicit end at step 9 and A7 "Another shelter" (the ATM example's "Multiple Transactions"). Registration (their steps 5–9) moved to A5 with its own validation failure (E2). Their A1 kept with a defined end (continues at step 4, or A5 / end); their A2 kept with a defined end (A1 or end) as Mark Shelter Full; their A4 kept (title "Shelter information updated"), with validation failure (E2), the capacity-below-occupancy refusal (new E4), the status recomputed after a capacity change, and an end; their A3 (exceeds capacity) became E1, told apart from E3 by the rejection reason (over capacity vs negative or non-numeric value); their E1 (system unavailable, "operation remains pending") replaced by A6 (offline entry, pending state, replay, server re-validation, rejection reported and dismissed) and, for reads, the stated technical-failure convention; their E2 (save fails) recast as E2 "Shelter details fail validation", covering registration and edits, with the validation rules listed. Steps 2–3 order fixed (their step 2 showed the interface before step 3 selected it). Status set Open below capacity and Full at capacity (their A2.5 only ever set Full). Their titles are kept where their flows survive: A4 "Shelter information updated" and E3 "Invalid Shelter Occupancy" (restored in P62, R4F-09). | D4-02, D4-03, G-UC-20 (scenario side), G-UC-14, G-CD-07 |
| CH-29 | Step order fixed to match SD3: list (step 2, look-up absent in their SD), shelter retrieval (4), validation and capacity check before any save (7), record creation and status written (8), failure branches continue to a stated step (E1–E4, A6) instead of stopping at the error display. No login; every system step maps to boundary → control → entity (Appendix 2); every write goes through the offline-aware path (step 6). | D4-05, D4-06, D4-07, D4-08 (scenario side) |
| CH-30 | Screen states named so WF3 can draw each: shelter list of one district with Open/Full statuses (2), detail with remaining capacity and Edit details (4, 4e), "New total occupancy" field (5), "Occupancy updated" (9), full (8c), capacity exceeded (7b), invalid value (7e), find shelter with space and alternatives (4a–4d), registration form and confirmation (2a–2f), field errors (E2), capacity-below-occupancy refusal (E4), offline banner shown and removed (6a, 6d), "Saved offline – pending sync", pending count, Sync now, synchronisation results, rejected items with reasons and their dismissal (6b–6h). A registered shelter starts at occupancy 0 (their storyboard panel 3 showed 320); no login panel; the user is the District Officer of one district. | D4-10, D4-11, D4-13, D4-14, D4-15 |
| CH-03 | UC3 includes Track Shelter Occupancy (steps 5–8), which includes Check Shelter Capacity (step 7); Register Emergency Shelter extends UC3 at `new shelter to register` (A5, step 2); Mark Shelter Full extends Check Shelter Capacity at `shelter capacity full` (A2). Their "System saves the emergency shelter details" (step 8) no longer appears as a behaviour of its own (*Save Shelter* removed). | G-UC-20, G-UC-04, G-UC-05 (part) |
| CH-05 | Extension points declared and named as on the UCD; offline via Save Data Offline at `network unavailable` (A6) for all UC3 data entry (occupancy, registration, detail edits) with precondition "the page was opened while online", no service worker, offline banner on connection loss, offline entry limited to the shelters already listed (details shown from the list data) and without the online details check, replay on reconnection, on opening the page online or "Sync now", server re-validation, duplicates recognised, a REJECTED reply or a 4xx error → rejected with reason and dismissed by the officer, 5xx / network error → stays pending; an online refusal of a write is shown in the synchronisation results only (no pending item — P62, R4F-08). | G-UC-09, G-UC-14 |
| CH-14 | Each accepted update creates an occupancy record (number of people, captured at, received at, recording officer) added to the shelter's history (step 8, A2, A6). | G-CD-07 |
| CH-21 | Shelter status vocabulary Open / Full only (no Closed, no Active); pending items "Pending synchronisation" / "Rejected". | D4-14 |
| CH-01, CH-09 | Title is the UCD name "UC3 Manage Emergency Shelters"; no login step or precondition ("authorised", "has access" replaced by the officer's district identity); behaviour name Mark Shelter Full. | G-UC-04, G-UC-15 |
| CH-04 | Primary actor District Officer only, no longer inheriting duty-officer use cases. | G-UC-07 |

## Appendix 2 — Step-to-design map (model-dictionary v0.9) (working appendix — not for the report body)

Boundary = `ShelterManagementUI`; controls `ShelterController` (server) and `OfflineSyncController` (browser). Reads go boundary → `ShelterController`; every write goes boundary → `OfflineSyncController` → `ShelterController` (online) or `OfflineOutbox` (offline). Finders and idempotency are not drawn (MD §0.1: duplicate replay = guard `[duplicate submissionId]`). Forms are screen components, not displays; the technical-failure banner of the reads is built by the web layer and not drawn. Every read after the first page load is a page-script request for data (JSON), so the page stays open when a read fails; offline, `selectShelter` makes no request (FX-15, row A6 6c).

| Step / flow | Boundary | Control | Entity / gateway |
|---|---|---|---|
| 1 | `openShelters()` | — | — |
| 2 | `showShelters(shelters)`; `showPending(items)` when items exist; on load while online the page raises `onConnectivityRestored()` → as A6 6d–6f | `ShelterController.getDistrictShelters(officerId)`; `OfflineSyncController.getPending()`; `OfflineSyncController.replayPending()` when QUEUED items exist | `Shelter` (retrieved by district); `OfflineOutbox.findPending()` |
| 3 | `selectShelter(shelterId)` | — | — |
| 4 | `showShelterDetails(view)` | `ShelterController.getShelterDetails(shelterId)` → `ShelterView` | `Shelter.checkAvailability()` |
| 5 | `enterNewOccupancy(shelterId, newTotal)` | — | — |
| 6 | — | `OfflineSyncController.updateOccupancy(update)` (`OccupancyUpdate` with `ClientMeta`) → `ShelterController.updateOccupancy(update)` | — |
| 7 | — | `ShelterController.updateOccupancy` validates `newTotal` | `Shelter.canAccommodate(newTotal)` |
| 8 | — | `ShelterController.updateOccupancy` «create» `OccupancyRecord`; reply `ShelterOutcome` (ACCEPTED, `shelter`, `remainingCapacity`) | `OccupancyRecord` «create» (A3, A12 `DistrictOfficer`); `Shelter.updateOccupancy(record)` (status OPEN); `Shelter.checkAvailability()`; `ClientSyncStatus.recordSync(at, pendingCount)` |
| 9 | `showOccupancyUpdated(view)` (view from the outcome's shelter and remaining capacity — MD v0.10) | — | — |
| A1 4a–4b | `findShelterWithSpace(needed)`; `showNoShelterAvailable(alternatives)` | `ShelterController.findAlternativeShelters(district, needed)` (`district` = the selected shelter's district, passed by the screen) | `Shelter` (retrieved with space) |
| A1 4c | `selectShelter(shelterId)` → step 4 | as step 4 | as step 4 |
| A1 4d | `showNoShelterAvailable(alternatives)` (empty list) | as 4b | as 4b |
| A2 8a–8b | — | `ShelterController.updateOccupancy` | `Shelter.canAccommodate(newTotal)`; `OccupancyRecord` «create»; `Shelter.updateOccupancy(record)` (status FULL) |
| A2 8c | `showShelterFull(view)` (ACCEPTED with `shelter.status = FULL`) | — | — |
| A4 4e | `editShelterDetails(shelterId, details)` | — | — |
| A4 4f | — (E2 / E4 on errors — `valid = false`; skipped offline) | `ShelterController.validateShelterDetails(details, shelterId)` → `ValidationResult` | — |
| A4 4g | — | `OfflineSyncController.updateShelterDetails(shelterId, submission)` → `ShelterController.updateShelterDetails(shelterId, submission)` → `ShelterOutcome` (capacity not below `currentOccupancy`, else REJECTED VALIDATION_FAILED — E4 slip-through case) | `Shelter.updateDetails(details)` (status recomputed: FULL when occupancy = capacity, else OPEN); `ClientSyncStatus.recordSync(at, pendingCount)` |
| A4 4h | `showShelterUpdated(view)` | — | — |
| A5 2a–2b | `startRegistration()` (the form is the boundary's own; no control call) | — | — |
| A5 2c | `enterShelterDetails(details)` | — | — |
| A5 2d | — (E2 on errors; skipped offline) | `ShelterController.validateShelterDetails(details, shelterId)` (no `shelterId` for a new shelter) → `ValidationResult` | — |
| A5 2e | — (a REJECTED VALIDATION_FAILED reply online → `showSyncResults([result])` only, no pending item — as E4 slip-through) | `OfflineSyncController.registerShelter(submission)` → `ShelterController.registerShelter(submission)` → `ShelterOutcome` | `Shelter` «create» (status OPEN, occupancy 0; A10 `DistrictOfficer` of `meta.userId`; A11 `Organization` when `operatorId` given); `ClientSyncStatus.recordSync(at, pendingCount)` |
| A5 2f | `showShelterRegistered(view)`; then `showShelters(shelters)` | `ShelterController.getDistrictShelters(officerId)` | as step 2 |
| A6 6a | found `onConnectivityLost()`; `showConnectivity(false)` | — | — |
| A6 6b | `showConnectivity(false)` first when the write failed with a network error before `onConnectivityLost()` (6a case); `showSavedOffline(pendingCount)`; `showPending(items)` | `OfflineSyncController.updateOccupancy` / `registerShelter` / `updateShelterDetails` → empty reply; `OfflineSyncController.pendingCount()`; `OfflineSyncController.getPending()` | `PendingSubmission` «create» (`SubmissionAction`, `payload`, `meta`, status QUEUED); `OfflineOutbox.save(item)`; `OfflineOutbox.findPending()` |
| A6 6c | `enterNewOccupancy(shelterId, newTotal)` / `enterShelterDetails(details)` / `editShelterDetails(shelterId, details)`, then as 6b (no `validateShelterDetails` offline); offline, `selectShelter(shelterId)` builds `showShelterDetails(view)` from the `Shelter` already received by `showShelters`, with no fetch (no `getShelterDetails` call; the page computes remaining capacity = `capacity` − `currentOccupancy` from the list data in this offline case only — online it comes from `Shelter.checkAvailability()`, step 4; FX-15); only `findShelterWithSpace(needed)` and `validateShelterDetails` fall back to the technical-failure banner offline | as 6b | as 6b |
| A6 6d | found `onConnectivityRestored()` (also raised by the page on load while online — step 2); `showConnectivity(true)`; or `syncNow()` | `OfflineSyncController.replayPending()` | `OfflineOutbox.findPending()` |
| A6 6e | — | per QUEUED item, by `SubmissionAction`: `ShelterController.updateOccupancy` / `registerShelter` / `updateShelterDetails` (meta rebuilt from `PendingSubmission.meta`, `pendingCount` recomputed) → `ShelterOutcome` with `SyncResult` (ACCEPTED / DUPLICATE / REJECTED + `RejectionReason`) | as steps 7–8, 2e, 4g; `Shelter.updateOccupancy(record)` keeps every record but changes `currentOccupancy` only when `record.capturedAt` is not earlier than the latest recorded `capturedAt` (a stale replay is kept as history); guard `[duplicate submissionId]` |
| A6 6f | `showSyncResults(results)`; `showPending(items)`; then `showShelters(shelters)` | `OfflineSyncController.replayPending()` → `List<SyncResult>`; `OfflineSyncController.getPending()`; `ShelterController.getDistrictShelters(officerId)` | `OfflineOutbox.remove(submissionId)` (ACCEPTED, DUPLICATE); `OfflineOutbox.markRejected(submissionId, reason)` (REJECTED); `PendingSubmission.withStatus(status)`; `Shelter` (retrieved by district) |
| A6 6g–6h | `dismissPending(submissionId)`; `showPending(items)` | `OfflineSyncController.dismiss(submissionId)`; `OfflineSyncController.getPending()` | `OfflineOutbox.remove(submissionId)`; `OfflineOutbox.findPending()` |
| A6 6i | `selectShelter(shelterId)` → step 3 | — | — |
| A7 9a | `selectShelter(shelterId)` → step 3 | — | — |
| E1 7a–7b | `showCapacityExceeded(capacity, remainingCapacity)` | `ShelterController.updateOccupancy` → `ShelterOutcome` (REJECTED, reason OVER_CAPACITY, `shelter` present) | `Shelter.canAccommodate(newTotal)` (false) |
| E1 7c | `enterNewOccupancy(shelterId, newTotal)` / `findShelterWithSpace(needed)` | — | — |
| E2 2g / 4i | `showShelterDetailsErrors(errors)` | `ShelterController.validateShelterDetails(details, shelterId)` (`shelterId` empty for a registration) → `ValidationResult` (`valid = false`, `errors`; rules: name, address, contact number present; contact number 10 digits starting 0; district = officer's district; capacity whole number > 0; `operatorId`, if given, an existing `Organization`) | — |
| E2 2h / 4j | `enterShelterDetails(details)` / `editShelterDetails(shelterId, details)` | — | — |
| E3 7d–7e | `showInvalidOccupancy(message)` | `ShelterController.updateOccupancy` → `ShelterOutcome` (REJECTED, reason INVALID_VALUE, no `shelter`); non-numeric: boundary only (cannot bind) | — |
| E3 7f | `enterNewOccupancy(shelterId, newTotal)` | — | — |
| E4 4k–4l | `showShelterDetailsErrors(errors)` | `ShelterController.validateShelterDetails(details, shelterId)` → `ValidationResult` (`valid = false`; capacity below that shelter's `currentOccupancy`) (slip-through case only, e.g. occupancy rose meanwhile: `OfflineSyncController.updateShelterDetails(shelterId, submission)` → `ShelterController.updateShelterDetails(shelterId, submission)` → `ShelterOutcome` REJECTED VALIDATION_FAILED → online `showSyncResults([result])` only, no pending item (R4F-08); on replay `showSyncResults(results)` and `showPending(items)`, as A6 6f) | — (no `Shelter.updateDetails`) |
| E4 4m | `editShelterDetails(shelterId, details)` / `findShelterWithSpace(needed)` / `selectShelter(shelterId)` | — | — |
| Technical failure (reads) | common error banner (web layer, not drawn) | — | — |

## Dictionary gaps

1. **Online display of a capacity-below-occupancy refusal (E4).** Resolved (MD v0.7.1/v0.8): the capacity rule is part of `validateShelterDetails(details, shelterId)`, so E4 branches at step 4f and shows `showShelterDetailsErrors(errors)`; the REJECTED VALIDATION_FAILED reply of `updateShelterDetails` (→ `showSyncResults`) remains only for the slip-through case and replay.
2. **Banner after a network error without a browser event (6a).** Resolved (MD v0.7.1/v0.8): a write that fails with a network error counts as offline — it is queued and the page shows `showConnectivity(false)` and `showSavedOffline`; the scenario treats the case as from 6a.

Gaps resolved in P60: Sync now (N14, `syncNow()`), offline banner (N15, `onConnectivityLost()` / `showConnectivity(online)`), offline reads and offline details check (N16), dismissing a rejected pending item (N17, `dismissPending` / `dismiss`), `validateShelterDetails` rules (N18), capacity below occupancy and status recomputation (N19), district passed to `findAlternativeShelters` (N20); technical failures of reads (C1) and form state (C2) stated. Resolved in P61 (no gap was open): offline details from the list data (R3F-06), list refreshed after replay (R3F-11), stale replay kept as history (PR8, R3F-12), replay on opening the page online (PR11, R3F-13). Resolved in P62 (fix spec `verification/fix-spec-P62.md`; no gap was open): the offline `selectShelter` rule carried into MD and the standards (FX-15, R4F-01) and stated as the only case where the page computes remaining capacity (R4F-11); online VALIDATION_FAILED refusals shown by `showSyncResults([result])` only (R4F-08); A2 placed at steps 7–8 (R4F-10); CH-05 row reworded (R4F-12); clock-skew limitation (R4F-13); Group_060's E3 title restored (R4F-09); Description and Conventions line (FX-24); P61's resolutions renamed PR1–PR16 (R4B-05); Appendix 2 cites model-dictionary v0.9.
