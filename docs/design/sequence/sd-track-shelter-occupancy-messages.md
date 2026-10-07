### Message script — sd Track Shelter Occupancy

Lifelines, left to right (draw the stereotype icon and underline the head):

- **DO** — actor *District Officer*
- **SMUI** — «boundary» `: ShelterManagementUI`
- **OSC** — «control» `: OfflineSyncController`
- **SC** — «control» `: ShelterController`
- **SH** — «entity» `shelter : Shelter`
- **OR** — «entity» `: OccupancyRecord`
- **CSS** — «entity» `: ClientSyncStatus`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| 1 | DO | SMUI | `enterNewOccupancy(shelterId, newTotal)` | synchronous (filled arrowhead) |
| | | | *note:* the total number of people now in the shelter; an entry that is not a whole number is refused on the page and not sent | note |
| 1.1 | SMUI | OSC | `updateOccupancy(update)` | synchronous (filled arrowhead) |
| | | | **alt [the page is online]** | fragment |
| 1.1.1 | OSC | SC | `updateOccupancy(update)` | synchronous (filled arrowhead) |
| | | | **alt [the total is a whole number of 0 or more]** | fragment |
| | SC … SH | | **ref** «include» Check Shelter Capacity | fragment |
| | | | **alt [the total is below the capacity]** | fragment |
| 1.1.1.1 | SC | OR | `OccupancyRecord(occupantCount, capturedAt, recordedAt, officer)` | «create» |
| 1.1.1.2 | SC | SH | `updateOccupancy(record)` | synchronous (filled arrowhead) |
| | | | *note:* adds the record to the history and sets the current occupancy; the status stays Open | note |
| 1.1.1.3 | SC | CSS | `recordSync(at, pendingCount)` | synchronous (filled arrowhead) |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | **else [the total equals the capacity]** | fragment |
| | | | *note:* the record was created and the shelter marked Full in Mark Shelter Full (through Check Shelter Capacity), which returned the outcome | note |
| | | | **else [the total exceeds the capacity]** | fragment |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| | | | **else [the total is negative]** | fragment |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| — | OSC | SMUI | outcome | return (dashed, open arrowhead) |
| | | | **alt [accepted and the shelter is below its capacity]** | fragment |
| 1.2 | SMUI | SMUI | `showOccupancyUpdated(view)` | synchronous (filled arrowhead) |
| | | | *note:* the view is the outcome's shelter with its remaining capacity | note |
| | | | **else [accepted and the shelter is now full]** | fragment |
| 1.3 | SMUI | SMUI | `showShelterFull(view)` | synchronous (filled arrowhead) |
| | | | *note:* the shelter is shown as Full with Find shelter with space; the officer looks for a shelter with space, or this use case ends | note |
| | | | **else [refused: over capacity]** | fragment |
| 1.4 | SMUI | SMUI | `showCapacityExceeded(capacity, remainingCapacity)` | synchronous (filled arrowhead) |
| | | | *note:* the capacity and remaining places come from the outcome; nothing is saved; the officer enters a total within capacity (message 1) or looks for a shelter with space | note |
| | | | **else [refused: not a valid total]** | fragment |
| 1.5 | SMUI | SMUI | `showInvalidOccupancy(message)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is saved; the officer corrects the value (message 1) | note |
| | | | **end** | fragment |
| | | | **else [the page is offline — extension point: network unavailable]** | fragment |
| | SMUI … OSC | | **ref** «extend» Save Data Offline | fragment |
| | | | **end** | fragment |
