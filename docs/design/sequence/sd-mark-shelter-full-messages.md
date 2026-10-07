### Message script — sd Mark Shelter Full

Lifelines, left to right (draw the stereotype icon and underline the head):

- **OSC** — «control» `: OfflineSyncController`
- **SC** — «control» `: ShelterController`
- **SH** — «entity» `shelter : Shelter`
- **OR** — «entity» `: OccupancyRecord`
- **CSS** — «entity» `: ClientSyncStatus`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *SC is already executing (activation from the top of the frame)* | — |
| | | | *note:* continues ShelterController.updateOccupancy(update) when the new total equals the shelter's capacity | note |
| 1 | SC | OR | `OccupancyRecord(occupantCount, capturedAt, recordedAt, officer)` | «create» |
| 2 | SC | SH | `updateOccupancy(record)` | synchronous (filled arrowhead) |
| | | | *note:* the current occupancy now equals the capacity: the shelter's status becomes Full | note |
| 3 | SC | CSS | `recordSync(at, pendingCount)` | synchronous (filled arrowhead) |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | *note:* the outcome reports the shelter as Full; the page shows it (Track Shelter Occupancy) | note |
