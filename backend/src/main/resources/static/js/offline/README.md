# Browser offline module (UC3 page script — for Weliwitigoda)

Plain ES modules written and tested by the skeleton (standards §10, §12.4). Your page script only creates the
controller, calls it, and draws the `show…` displays from what it returns. Nothing here draws anything.

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
  userId: DEMO_DISTRICT_OFFICER_ID,                    // the demo district officer, e.g. "DO-COL-01"
});
```

`clock`, `isOnline` and `uuid` are optional (tests inject them; the defaults are the system clock,
`navigator.onLine !== false` and `crypto.randomUUID`).

## Calls and what the replies drive (MD `ShelterManagementUI`, standards §12.4)

Every write needs a `meta` from `await sync.createMeta()` (a new `submissionId`, `capturedAt` = now with `+05:30`,
`pendingCount`). Make a new `meta` for each new entry; never reuse one.

| Page action | Call | Reply |
|---|---|---|
| `enterNewOccupancy(shelterId, newTotal)` | `sync.updateOccupancy({ shelterId, newTotal, meta })` | `ShelterOutcome`, or `undefined` when it was queued |
| `enterShelterDetails(details)` (registration) | `sync.registerShelter({ details, meta })` | same |
| `editShelterDetails(shelterId, details)` | `sync.updateShelterDetails(shelterId, { details, meta })` | same |
| `syncNow()` and `onConnectivityRestored()` | `sync.replayPending()` | the `SyncResult`s the server answered |
| load of the page, after a replay | `sync.getPending()` | the items for `showPending(items)` |
| `dismissPending(submissionId)` | `sync.dismiss(submissionId)`, then `getPending()` | nothing (throws for a QUEUED item) |
| after any write | `sync.pendingCount()` | the number for `showSavedOffline(pendingCount)` |

The reads (`GET /api/shelters`, `/api/shelters/{id}`, `/api/shelters/alternatives?district=&needed=`) and the check
`POST /api/shelters/validation` are plain `fetch` calls of the page; they are not queued. Offline, do not call
`validateShelterDetails`, and build `showShelterDetails(view)` from the list already on the page (standards §10).

### Reply → display

* `undefined` (the change was queued): `showSavedOffline(await sync.pendingCount())` and `showConnectivity(false)`.
* A `ShelterOutcome` whose `result.outcome` is
  * `ACCEPTED` or `DUPLICATE`: `shelter.status === 'FULL'` → `showShelterFull(view)`, otherwise `showOccupancyUpdated(view)`
    for an occupancy update, `showShelterRegistered(view)` for a registration, `showShelterUpdated(view)` for a detail
    change; build `view` as `{ shelter, remainingCapacity }` from the reply;
  * `REJECTED` — by `result.reason`, never by the text:
    * `OVER_CAPACITY` → `showCapacityExceeded(shelter.capacity, remainingCapacity)`; offer the alternatives
      (`GET /api/shelters/alternatives`) → `showNoShelterAvailable(alternatives)` when none fit;
    * `INVALID_VALUE` → `showInvalidOccupancy(result.message)`;
    * `VALIDATION_FAILED` → `showSyncResults([result])` only; no pending item is made online (a 4xx problem reply
      from the server is turned into this result by the module and is not queued).
* After `replayPending()`: `showSyncResults(results)`, then `showPending(await sync.getPending())` (QUEUED items and
  REJECTED items with `rejectionReason`), then refresh the list (`showShelters`).

## Connectivity

The page must raise `onConnectivityRestored()` **on load when it is online** (so items queued while the page was
closed are sent at once — PR11) **and on the `online` event**; it calls `showConnectivity(true)` and then
`sync.replayPending()`. On the `offline` event it calls `onConnectivityLost()` → `showConnectivity(false)`. A `fetch`
network error counts as offline: the module has already queued the change, so show `showSavedOffline` and the banner.

`replayPending()` is single-flight: a second call during a run returns the same promise, so it is safe to call it from
both the `online` event and the Sync now button.

## Things to know

* `clientId` is made once per browser (IndexedDB database `disasterlink-offline`) and sent in every `meta`.
* An item's `payload` is the serialised arguments without `meta` (`shelterId` included); on replay the module rebuilds
  the request with the item's `meta`, `pendingCount` recomputed (QUEUED items other than this one) and the original
  `capturedAt`.
* 5xx replies and network errors never reject an item: it stays QUEUED with `attempts` + 1 and is sent again next time.
* Dismiss is for REJECTED items only; `dismiss` of a QUEUED item throws.
* Tests: `web/test/offline/` (Jest, `fake-indexeddb`); run `npm test -- --coverage` in `web/`.
