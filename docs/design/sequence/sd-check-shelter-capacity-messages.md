### Message script — sd Check Shelter Capacity

Lifelines, left to right (draw the stereotype icon and underline the head):

- **SC** — «control» `: ShelterController`
- **SH** — «entity» `shelter : Shelter`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *SC is already executing (activation from the top of the frame)* | — |
| | | | *note:* continues ShelterController.updateOccupancy(update) for a total that is a whole number of 0 or more; newTotal is the total taken from the update | note |
| 1 | SC | SH | `canAccommodate(newTotal)` | synchronous (filled arrowhead) |
| — | SH | SC | accommodates | return (dashed, open arrowhead) |
| 2 | SC | SH | `checkAvailability()` | synchronous (filled arrowhead) |
| — | SH | SC | remainingCapacity | return (dashed, open arrowhead) |
| | | | *note:* a total above the capacity is refused (over capacity, with the remaining places; the refusal is drawn in Track Shelter Occupancy) | note |
| | | | **opt [the total equals the capacity — extension point: shelter capacity full]** | fragment |
| | SC … SH | | **ref** «extend» Mark Shelter Full | fragment |
| | | | **end** | fragment |
| | | | *note:* Mark Shelter Full returns the outcome to the caller; below the capacity, Track Shelter Occupancy records the total | note |
