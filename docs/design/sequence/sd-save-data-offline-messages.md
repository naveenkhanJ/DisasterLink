### Message script — sd Save Data Offline

Lifelines, left to right (draw the stereotype icon and underline the head):

- **APP** — «boundary» `: CitizenAppUI`
- **RCC** — «control» `: ReportCaptureController`
- **ORS** — «entity» `: OfflineReportStore`
- **SMUI** — «boundary» `: ShelterManagementUI`
- **OSC** — «control» `: OfflineSyncController`
- **OUT** — «entity» `: OfflineOutbox`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *RCC is already executing (activation from the top of the frame)* | — |
| | | | *OSC is already executing (activation from the top of the frame)* | — |
| | | | **alt [citizen app: a report is submitted without a connection, or the server does not reply]** | fragment |
| | | | *note:* continues the citizen app's submit(draft) call, which found no connection or got no reply | note |
| 1 | RCC | ORS | `save(draft)` | synchronous (filled arrowhead) |
| | | | *note:* the draft is kept as Pending synchronisation with its photo, location, district and original capture time | note |
| — | RCC | APP | result | return (dashed, open arrowhead) |
| 2 | APP | APP | `showSubmissionResult(result)` | synchronous (filled arrowhead) |
| | | | **else [shelter page: a shelter change is entered without a connection]** | fragment |
| | | | *note:* continues the shelter page's write call (for example updateOccupancy(update)), which found no connection | note |
| 3 | OSC | OUT | `save(item)` | synchronous (filled arrowhead) |
| | | | *note:* the page already shows the offline banner, raised when the connection was lost (Manage Emergency Shelters) | note |
| | | | *note:* occupancy updates, registrations and detail changes are queued the same way, with their capture time | note |
| — | OSC | SMUI | none | return (dashed, open arrowhead) |
| 4 | SMUI | OSC | `pendingCount()` | synchronous (filled arrowhead) |
| — | OSC | SMUI | count | return (dashed, open arrowhead) |
| 5 | SMUI | SMUI | `showSavedOffline(pendingCount)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | APP … OUT | | **ref** «include» Synchronise Offline Data | fragment |
| | | | *note:* runs when the connection is restored | note |
