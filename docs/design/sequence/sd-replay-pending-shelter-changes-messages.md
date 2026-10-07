### Message script — sd Replay Pending Shelter Changes

Lifelines, left to right (draw the stereotype icon and underline the head):

- **SMUI** — «boundary» `: ShelterManagementUI`
- **OSC** — «control» `: OfflineSyncController`
- **SC** — «control» `: ShelterController`
- **OUT** — «entity» `: OfflineOutbox`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *SMUI is already executing (activation from the top of the frame)* | — |
| | | | *note:* continues its trigger: the connection is restored or the officer selects Sync now (Synchronise Offline Data), or the page opens online with pending entries (Manage Emergency Shelters) | note |
| 1 | SMUI | OSC | `replayPending()` | synchronous (filled arrowhead) |
| 1.1 | OSC | OUT | `findPending()` | synchronous (filled arrowhead) |
| — | OUT | OSC | items | return (dashed, open arrowhead) |
| | | | **loop [for each pending item]** | fragment |
| 1.2 | OSC | SC | `updateOccupancy(update)` | synchronous (filled arrowhead) |
| | | | *note:* registrations and detail changes are sent to registerShelter and updateShelterDetails in the same way | note |
| | | | **alt [the change was accepted, or had already been recorded]** | fragment |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| 1.3 | OSC | OUT | `remove(submissionId)` | synchronous (filled arrowhead) |
| | | | **else [the change was not accepted]** | fragment |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| 1.4 | OSC | OUT | `markRejected(submissionId, reason)` | synchronous (filled arrowhead) |
| | | | **else [no reply: server error or connection lost]** | fragment |
| | | | *note:* the item stays pending; tried again later | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| — | OSC | SMUI | results | return (dashed, open arrowhead) |
| 2 | SMUI | SMUI | `showSyncResults(results)` | synchronous (filled arrowhead) |
| 3 | SMUI | OSC | `getPending()` | synchronous (filled arrowhead) |
| — | OSC | SMUI | items | return (dashed, open arrowhead) |
| 4 | SMUI | SMUI | `showPending(items)` | synchronous (filled arrowhead) |
| 5 | SMUI | SC | `getDistrictShelters(officerId)` | synchronous (filled arrowhead) |
| — | SC | SMUI | shelters | return (dashed, open arrowhead) |
| 6 | SMUI | SMUI | `showShelters(shelters)` | synchronous (filled arrowhead) |
