### Message script — sd Broadcast Warning

Lifelines, left to right (draw the stereotype icon and underline the head):

- **WB** — «control» `: WarningBroadcaster`
- **WARN** — «entity» `warning : Warning`
- **REC** — «entity» `recipient : Recipient`
- **NOTE** — «entity» `: Notification`
- **GW** — «boundary» `: NotificationGateway`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *WB is already executing (activation from the top of the frame)* | — |
| | | | *note:* delivers the warning to the target recipients that the referencing operation has identified | note |
| | | | **loop [for each target recipient and each target channel]** | fragment |
| 1 | WB | REC | `canReceive(channel)` | synchronous (filled arrowhead) |
| — | REC | WB | receivable | return (dashed, open arrowhead) |
| | | | **opt [receivable]** | fragment |
| 2 | WB | REC | `getPreferredLanguage()` | synchronous (filled arrowhead) |
| — | REC | WB | language | return (dashed, open arrowhead) |
| 3 | WB | NOTE | `Notification(recipient, channel, text, round)` | «create» |
| 4 | WB | WARN | `addNotification(notification)` | synchronous (filled arrowhead) |
| 5 | WB | GW | `supports(channel)` | synchronous (filled arrowhead) |
| — | GW | WB | supported | return (dashed, open arrowhead) |
| | | | **opt [supported]** | fragment |
| 6 | WB | GW | `send(notification)` | synchronous (filled arrowhead) |
| — | GW | WB | sent | return (dashed, open arrowhead) |
| | | | **alt [sent]** | fragment |
| 7 | WB | NOTE | `markDelivered(at)` | synchronous (filled arrowhead) |
| | | | **else [not sent]** | fragment |
| 8 | WB | NOTE | `markFailed(at)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | **end** | fragment |
