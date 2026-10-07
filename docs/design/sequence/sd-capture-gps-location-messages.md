### Message script — sd Capture GPS Location

Lifelines, left to right (draw the stereotype icon and underline the head):

- **CIT** — actor *Citizen*
- **APP** — «boundary» `: CitizenAppUI`
- **RCC** — «control» `: ReportCaptureController`
- **LOC** — «boundary» `: LocationService`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *APP is already executing (activation from the top of the frame)* | — |
| 1 | APP | RCC | `captureLocation()` | synchronous (filled arrowhead) |
| 1.1 | RCC | LOC | `getCurrentLocation()` | synchronous (filled arrowhead) |
| — | LOC | RCC | location | return (dashed, open arrowhead) |
| — | RCC | APP | location | return (dashed, open arrowhead) |
| | | | **alt [a location was returned]** | fragment |
| 2 | APP | APP | `showLocation(location)` | synchronous (filled arrowhead) |
| | | | **else [no location: location services off or no fix in time]** | fragment |
| 3 | APP | APP | `showLocationUnavailable()` | synchronous (filled arrowhead) |
| 4 | CIT | APP | `enterLocationManually(location)` | synchronous (filled arrowhead) |
| 4.1 | APP | RCC | `validateDraft(draft)` | synchronous (filled arrowhead) |
| — | RCC | APP | result | return (dashed, open arrowhead) |
| | | | **alt [the location lies within Sri Lanka]** | fragment |
| 4.2 | APP | APP | `showLocation(location)` | synchronous (filled arrowhead) |
| | | | **else [the location lies outside Sri Lanka or cannot be read]** | fragment |
| 4.3 | APP | APP | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the citizen enters the location again (message 4) | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
