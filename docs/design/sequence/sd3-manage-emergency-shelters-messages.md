### Message script — sd Manage Emergency Shelters

Lifelines, left to right (draw the stereotype icon and underline the head):

- **DO** — actor *District Officer*
- **SMUI** — «boundary» `: ShelterManagementUI`
- **OSC** — «control» `: OfflineSyncController`
- **SC** — «control» `: ShelterController`
- **CSS** — «entity» `: ClientSyncStatus`
- **SH** — «entity» `shelter : Shelter`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *note:* objects are loaded by their identifiers (persistence is not drawn); a failed read while online is shown on the page's common error banner (not drawn) | note |
| 1 | DO | SMUI | `openShelters()` | synchronous (filled arrowhead) |
| 1.1 | SMUI | SC | `getDistrictShelters(officerId)` | synchronous (filled arrowhead) |
| — | SC | SMUI | shelters | return (dashed, open arrowhead) |
| 1.2 | SMUI | SMUI | `showShelters(shelters)` | synchronous (filled arrowhead) |
| 1.3 | SMUI | OSC | `getPending()` | synchronous (filled arrowhead) |
| — | OSC | SMUI | items | return (dashed, open arrowhead) |
| | | | **opt [entries from an earlier session are still pending on this computer]** | fragment |
| 1.4 | SMUI | SMUI | `showPending(items)` | synchronous (filled arrowhead) |
| | SMUI … SC | | **ref** Replay Pending Shelter Changes | fragment |
| | | | **end** | fragment |
| | | | **opt [the browser reports that the connection is lost]** | fragment |
| 2 | ● | SMUI | `onConnectivityLost()` | found message (arrow from a filled dot; sender unknown) |
| 2.1 | SMUI | SMUI | `showConnectivity(online)` | synchronous (filled arrowhead) |
| | | | *note:* online is false: the offline banner is shown and later entries are saved on this computer | note |
| | | | **end** | fragment |
| | | | **opt [a new shelter is to be registered — extension point: new shelter to register]** | fragment |
| | DO … CSS | | **ref** «extend» Register Emergency Shelter | fragment |
| | | | **end** | fragment |
| 3 | DO | SMUI | `selectShelter(shelterId)` | synchronous (filled arrowhead) |
| | | | **alt [the page is online]** | fragment |
| 3.1 | SMUI | SC | `getShelterDetails(shelterId)` | synchronous (filled arrowhead) |
| 3.1.1 | SC | SH | `checkAvailability()` | synchronous (filled arrowhead) |
| — | SH | SC | remainingCapacity | return (dashed, open arrowhead) |
| — | SC | SMUI | view | return (dashed, open arrowhead) |
| 3.2 | SMUI | SMUI | `showShelterDetails(view)` | synchronous (filled arrowhead) |
| | | | **else [the page is offline]** | fragment |
| 3.3 | SMUI | SMUI | `showShelterDetails(view)` | synchronous (filled arrowhead) |
| | | | *note:* built from the shelter already received in the list (message 1.2), without a server read | note |
| | | | **end** | fragment |
| | | | **loop [while the officer works on the selected shelter]** | fragment |
| | | | **alt [the officer records the new total number of people]** | fragment |
| | DO … SH | | **ref** «include» Track Shelter Occupancy | fragment |
| | | | **else [the officer looks for a shelter with space]** | fragment |
| 4 | DO | SMUI | `findShelterWithSpace(needed)` | synchronous (filled arrowhead) |
| 4.1 | SMUI | SC | `findAlternativeShelters(district, needed)` | synchronous (filled arrowhead) |
| — | SC | SMUI | alternatives | return (dashed, open arrowhead) |
| 4.2 | SMUI | SMUI | `showNoShelterAvailable(alternatives)` | synchronous (filled arrowhead) |
| | | | *note:* the officer selects an alternative (message 3) or registers a new shelter; offline this search is unavailable | note |
| | | | **else [the officer edits the shelter's details]** | fragment |
| 5 | DO | SMUI | `editShelterDetails(shelterId, details)` | synchronous (filled arrowhead) |
| | | | **alt [the page is online]** | fragment |
| 5.1 | SMUI | SC | `validateShelterDetails(details, shelterId)` | synchronous (filled arrowhead) |
| — | SC | SMUI | result | return (dashed, open arrowhead) |
| | | | **alt [the details are valid]** | fragment |
| 5.2 | SMUI | OSC | `updateShelterDetails(shelterId, submission)` | synchronous (filled arrowhead) |
| 5.2.1 | OSC | SC | `updateShelterDetails(shelterId, submission)` | synchronous (filled arrowhead) |
| | | | **alt [the capacity is not below the current occupancy]** | fragment |
| 5.2.1.1 | SC | SH | `updateDetails(details)` | synchronous (filled arrowhead) |
| 5.2.1.2 | SC | CSS | `recordSync(at, pendingCount)` | synchronous (filled arrowhead) |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | **else [the occupancy rose above the new capacity meanwhile]** | fragment |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| — | OSC | SMUI | outcome | return (dashed, open arrowhead) |
| | | | **alt [accepted]** | fragment |
| 5.3 | SMUI | SMUI | `showShelterUpdated(view)` | synchronous (filled arrowhead) |
| | | | *note:* the view is the outcome's shelter with its remaining capacity | note |
| | | | **else [refused by the server's check]** | fragment |
| 5.4 | SMUI | SMUI | `showSyncResults(results)` | synchronous (filled arrowhead) |
| | | | *note:* the refusal from the outcome; nothing is held on this computer | note |
| | | | **end** | fragment |
| | | | **else [a detail is missing or invalid, or the capacity is below the current occupancy]** | fragment |
| 5.5 | SMUI | SMUI | `showShelterDetailsErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is saved; the form keeps the values; the officer corrects them (message 5) or cancels | note |
| | | | **end** | fragment |
| | | | **else [the page is offline — extension point: network unavailable]** | fragment |
| 5.6 | SMUI | OSC | `updateShelterDetails(shelterId, submission)` | synchronous (filled arrowhead) |
| | SMUI … OSC | | **ref** «extend» Save Data Offline | fragment |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | *note:* after each choice the officer may record a new total, look for a shelter with space or edit the details again; the officer may then select another shelter (message 3); otherwise this use case ends | note |
