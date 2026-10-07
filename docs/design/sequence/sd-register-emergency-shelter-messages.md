### Message script — sd Register Emergency Shelter

Lifelines, left to right (draw the stereotype icon and underline the head):

- **DO** — actor *District Officer*
- **SMUI** — «boundary» `: ShelterManagementUI`
- **OSC** — «control» `: OfflineSyncController`
- **SC** — «control» `: ShelterController`
- **NS** — «entity» `newShelter : Shelter`
- **CSS** — «entity» `: ClientSyncStatus`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| 1 | DO | SMUI | `startRegistration()` | synchronous (filled arrowhead) |
| | | | *note:* the registration form opens with the district fixed to the officer's district | note |
| 2 | DO | SMUI | `enterShelterDetails(details)` | synchronous (filled arrowhead) |
| | | | **alt [the page is online]** | fragment |
| 2.1 | SMUI | SC | `validateShelterDetails(details, shelterId)` | synchronous (filled arrowhead) |
| | | | *note:* no shelterId is given for a new shelter | note |
| — | SC | SMUI | result | return (dashed, open arrowhead) |
| | | | **alt [the details are valid]** | fragment |
| 2.2 | SMUI | OSC | `registerShelter(submission)` | synchronous (filled arrowhead) |
| 2.2.1 | OSC | SC | `registerShelter(submission)` | synchronous (filled arrowhead) |
| | | | **alt [the server's check passes]** | fragment |
| 2.2.1.1 | SC | NS | `Shelter(details, officer, operator)` | «create» |
| | | | *note:* current occupancy 0, status Open; registered by the officer; operated by an organisation when one is given | note |
| 2.2.1.2 | SC | CSS | `recordSync(at, pendingCount)` | synchronous (filled arrowhead) |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | **else [the server's check fails]** | fragment |
| — | SC | OSC | outcome | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| — | OSC | SMUI | outcome | return (dashed, open arrowhead) |
| | | | **alt [registered]** | fragment |
| 2.3 | SMUI | SMUI | `showShelterRegistered(view)` | synchronous (filled arrowhead) |
| | | | *note:* the view is the outcome's shelter with its remaining capacity | note |
| 2.4 | SMUI | SC | `getDistrictShelters(officerId)` | synchronous (filled arrowhead) |
| — | SC | SMUI | shelters | return (dashed, open arrowhead) |
| 2.5 | SMUI | SMUI | `showShelters(shelters)` | synchronous (filled arrowhead) |
| | | | **else [refused by the server's check]** | fragment |
| 2.6 | SMUI | SMUI | `showSyncResults(results)` | synchronous (filled arrowhead) |
| | | | *note:* the refusal from the outcome; nothing is held on this computer | note |
| | | | **end** | fragment |
| | | | **else [a detail is missing or invalid]** | fragment |
| 2.7 | SMUI | SMUI | `showShelterDetailsErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is saved; the form keeps the values; the officer corrects them (message 2) or cancels | note |
| | | | **end** | fragment |
| | | | **else [the page is offline — extension point: network unavailable]** | fragment |
| 2.8 | SMUI | OSC | `registerShelter(submission)` | synchronous (filled arrowhead) |
| | | | *note:* Manage Emergency Shelters' extension point network unavailable, reached when the registration is submitted | note |
| | SMUI … OSC | | **ref** «extend» Save Data Offline | fragment |
| | | | **end** | fragment |
