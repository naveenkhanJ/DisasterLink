# Browser offline module (UC3 page script — for Weliwitigoda)

Plain ES modules written and tested by the skeleton (standards §10, §12.4). Your page script only creates the
controller, calls it, and draws the `show…` displays from what it returns. Nothing here draws anything.

Sources cited below: **MD** = `docs/design/model-dictionary.md` (§4 `ShelterManagementUI`, §2 value types, §5
controls); **SC3** = `docs/design/scenarios/SC3-manage-emergency-shelters.md`; **S§** = `implementation-standards.md`.

| File | What it is |
|---|---|
| `constants.js` | API paths, IndexedDB names, the `+05:30` offset, and the `SubmissionAction`, `PendingStatus`, `SyncOutcome` and `RejectionReason` literals |
| `offline-outbox.js` | `OfflineOutbox` — the pending items in IndexedDB (`save`, `findPending`, `remove`, `markRejected`, `clientId`) |
| `offline-sync.js` | `OfflineSyncController` — every UC3 write, `replayPending`, `getPending`, `dismiss`, `pendingCount`, `createMeta` |

## Creating the controller (once per page load)

```html
<script type="module" th:src="@{/js/shelter-page.js}"></script>
```

```js
import { OfflineOutbox } from '/js/offline/offline-outbox.js';
import { OfflineSyncController } from '/js/offline/offline-sync.js';

const outbox = await OfflineOutbox.open();            // opens the database, reads or makes the clientId
const sync = new OfflineSyncController({
  outbox,
  fetchFn: (...args) => window.fetch(...args),         // never pass window.fetch unbound
  userId: OFFICER_ID,                                  // see "Where userId comes from"
});
```

`clock`, `isOnline` and `uuid` are optional (tests inject them; the defaults are the system clock,
`navigator.onLine !== false` and `crypto.randomUUID`, which falls back to a UUID built from
`crypto.getRandomValues` on a page that is not a secure context).

**Where `userId` comes from (MD `ClientMeta.userId`, S§1 "no login", MD §6 `CurrentUserService`).** The acting officer is
the fixed demo district officer. Your page controller renders the identifier into the page when it builds the model, from
`CurrentUserService.userId(DemoRole.DISTRICT_OFFICER)` (for example in a `data-officer-id` attribute or a script
constant), and the page script passes that value as `userId`. Do not hard-code `DO-COL-01` in the script. The server
`GET /api/shelters` uses the same service to choose the district.

## Calls and what the replies drive

Every write needs a `meta` from `await sync.createMeta()` (a new `submissionId`, `capturedAt` = now with `+05:30`,
`pendingCount`). Make a new `meta` for each new entry; never reuse one. The write operations are asynchronous; `await`
them.

| Page action (MD `ShelterManagementUI`) | Call | Reply |
|---|---|---|
| `enterNewOccupancy(shelterId, newTotal)` | `sync.updateOccupancy({ shelterId, newTotal, meta })` | `ShelterOutcome`, or `undefined` when it was queued |
| `enterShelterDetails(details)` (registration) | `sync.registerShelter({ details, meta })` | same |
| `editShelterDetails(shelterId, details)` | `sync.updateShelterDetails(shelterId, { details, meta })` | same |
| `syncNow()` and `onConnectivityRestored()` | `sync.replayPending()` | the `SyncResult`s the server answered |
| load of the page, after a replay or a dismissal | `sync.getPending()` | the items for `showPending(items)` |
| `dismissPending(submissionId)` | `sync.dismiss(submissionId)`, then `sync.getPending()` | nothing (throws for a QUEUED item) |
| any queued write | `sync.pendingCount()` | the number for `showSavedOffline(pendingCount)` |

The reads (`GET /api/shelters`, `/api/shelters/{id}`, `/api/shelters/alternatives?district=&needed=`) and the check
`POST /api/shelters/validation` are plain `fetch` calls of the page; they are not queued. Offline, do not call
`validateShelterDetails`, and build `showShelterDetails(view)` from the list already on the page (S§10; MD `ShelterManagementUI`
P62 FX-15).

### Reply → display, per write (S§12.4 "Reason → display"; MD `ShelterManagementUI`)

A `ShelterOutcome` has `result` (`submissionId`, `outcome`, `reason`, `message`), `shelter` (absent on DUPLICATE and on
INVALID_VALUE — MD §2 `ShelterOutcome`), `remainingCapacity` and `alternatives`. Build `view` as `{ shelter,
remainingCapacity }` from the reply (MD §2 `ShelterView`). Read `shelter` only when `result.outcome` is `ACCEPTED`, or
`REJECTED` with `OVER_CAPACITY`.

| Write | Reply | Display |
|---|---|---|
| **Occupancy update** | `ACCEPTED`, `shelter.status === 'FULL'` | `showShelterFull(view)` (SC3 A2 8c) |
| | `ACCEPTED`, otherwise | `showOccupancyUpdated(view)` (SC3 step 9) |
| | `REJECTED`, `OVER_CAPACITY` | `showCapacityExceeded(shelter.capacity, remainingCapacity)` (SC3 E1 7b); the officer may then use find shelter with space (below) |
| | `REJECTED`, `INVALID_VALUE` | `showInvalidOccupancy(result.message)` (SC3 E3 7d–7e) |
| | `DUPLICATE` | `showSyncResults([result])` ("already recorded", SC3 A6 6e) and a list refresh (`GET /api/shelters`, `showShelters`); no `shelter` in the reply (R4) |
| **Registration** | `ACCEPTED` | `showShelterRegistered(view)`, then the list refresh `showShelters(shelters)` with the new shelter in it (SC3 A5 2f) |
| | `REJECTED`, `VALIDATION_FAILED` | `showSyncResults([result])` only — online, no pending item is made (S§12.4; SC3 A5 2e) |
| | `DUPLICATE` | `showSyncResults([result])` and a list refresh |
| **Detail change** | `ACCEPTED` | `showShelterUpdated(view)` (SC3 A4 4h; sequence `sd3` 5.3) |
| | `REJECTED`, `VALIDATION_FAILED` | `showSyncResults([result])` only (SC3 E4, slip-through case) |
| | `DUPLICATE` | `showSyncResults([result])` and a list refresh |
| **Any write**, queued | `undefined` | `showSavedOffline(await sync.pendingCount())` **and** `showPending(await sync.getPending())` (SC3 A6 6b: "Saved offline – pending sync" and the pending list); also `showConnectivity(false)` when the cause was a network error |

A 4xx problem reply from the server is turned into the `VALIDATION_FAILED` result of the table by the module and is
never queued. `showShelterDetailsErrors(errors)` is fed **only** by the page's own `POST /api/shelters/validation`
check before a registration or detail edit (MD `ShelterManagementUI`, P59), never by a write reply.

### Before the module: what the page itself catches

* **A non-numeric occupancy** (for example `3S0`) cannot be read as a whole number: the page refuses it without calling
  the module and shows `showInvalidOccupancy(message)` (MD `ShelterManagementUI`, "also when the web layer cannot bind a
  non-numeric entry"; SC3 E3 7d). A non-numeric capacity is blocked the same way (the request record requires a number
  for `capacity`; a missing number would be a 400 problem, not a `show…` display). A negative occupancy is **not**
  caught here: send it, and the server's `INVALID_VALUE` reply drives `showInvalidOccupancy`.
* The details check of a registration or edit (`POST /api/shelters/validation`) runs online only; offline it is skipped
  and the server re-checks on replay (MD `ShelterManagementUI` P60 N16; SC3 A6 6c).

### Find shelter with space (`findShelterWithSpace(needed)`; SC3 A1)

Take the district of the selected shelter and call `GET /api/shelters/alternatives?district=<district>&needed=<needed>`
(MD `ShelterManagementUI` P60 N20). SC3 A1 steps 4b–4d say what to show:

* **4b — alternatives found:** show that the selected shelter cannot take that many, with the alternatives: call
  `showNoShelterAvailable(alternatives)` with the returned `ShelterView`s (SC3 example: Dehiwala Municipal Council Hall,
  capacity 400, occupancy 120, 280 remaining — Piliyandala Central College with 190 remaining is not listed).
* **4c — the officer selects one:** the flow continues at step 4 for that shelter (`selectShelter` → `showShelterDetails`).
* **4d — nothing fits** (empty list): `showNoShelterAvailable([])` — the display states that no shelter in the district can
  take that many people; the officer may register a new shelter (A5) or leave.
* Offline, this read is unavailable: show the offline banner or the common error banner (S§10; MD P62 FX-15).

## Connectivity

The page must raise `onConnectivityRestored()` **on load when it is online** (so items queued while the page was
closed are sent at once — MD `ShelterManagementUI` PR11; SC3 A6 6d) **and on the `online` event**; it calls
`showConnectivity(true)` and then `sync.replayPending()`. On the `offline` event it calls `onConnectivityLost()` →
`showConnectivity(false)`. A `fetch` network error counts as offline: the module has already queued the change, so show
`showSavedOffline` and the banner (S§10).

After `replayPending()` (sequence `sd-replay-pending-shelter-changes`): `showSyncResults(results)`, then
`showPending(await sync.getPending())` (QUEUED items and REJECTED items with `rejectionReason`), then refresh the list
(`GET /api/shelters` → `showShelters`). `replayPending()` is single-flight: a second call during a run returns the same
promise, so it is safe to call it from both the `online` event and the Sync now button.

## Things to know

* `clientId` is made once per browser (IndexedDB database `disasterlink-offline`) and sent in every `meta`.
* An item's `payload` is the serialised arguments without `meta` (`shelterId` included); on replay the module rebuilds
  the request with the item's `meta`, `pendingCount` recomputed (QUEUED items other than this one) and the original
  `capturedAt` (MD `ClientMeta`, FE-04).
* 5xx replies and network errors never reject an item: it stays QUEUED with `attempts` + 1 and is sent again next time.
* Dismiss is for REJECTED items only; `dismiss` of a QUEUED item throws.
* `ShelterResponse` carries `operatorId` beside `operatorName`: pre-select the operator pick-list of the edit form from it
  (the list of organisations comes from your page controller's model), so an edit does not clear the operator.
* Tests: `web/test/offline/` (Jest, `fake-indexeddb`); run `npm test -- --coverage` in `web/`.
