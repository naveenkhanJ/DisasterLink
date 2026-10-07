### Message script — sd Send Decision Notice

Lifelines, left to right (draw the stereotype icon and underline the head):

- **RVC** — «control» `: ReportVerificationController`
- **CITE** — «entity» `reporter : Citizen`
- **PGW** — «boundary» `: PushNotificationGateway`
- **SGW** — «boundary» `: SmsGateway`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *RVC is already executing (activation from the top of the frame)* | — |
| | | | *note:* the recipient is the report's reporter; the notice is built from the decided report: report ID, decision, decision time and, for a rejection, the reason as the officer typed it; the notice text is in the reporter's preferred language | note |
| 1 | RVC | CITE | `getDeviceToken()` | synchronous (filled arrowhead) |
| — | CITE | RVC | deviceToken | return (dashed, open arrowhead) |
| 2 | RVC | CITE | `getPreferredLanguage()` | synchronous (filled arrowhead) |
| — | CITE | RVC | language | return (dashed, open arrowhead) |
| | | | **opt [the reporter has a device token]** | fragment |
| 3 | RVC | PGW | `sendNotice(recipient, notice)` | synchronous (filled arrowhead) |
| — | PGW | RVC | sent | return (dashed, open arrowhead) |
| | | | *note:* hands the notice to the Push Notification Service, which delivers it to the citizen app | note |
| | | | **end** | fragment |
| | | | **opt [no device token, or the push was not sent]** | fragment |
| 4 | RVC | SGW | `sendNotice(recipient, notice)` | synchronous (filled arrowhead) |
| — | SGW | RVC | sent | return (dashed, open arrowhead) |
| | | | *note:* hands the notice to the SMS gateway of the mobile operator | note |
| | | | **end** | fragment |
