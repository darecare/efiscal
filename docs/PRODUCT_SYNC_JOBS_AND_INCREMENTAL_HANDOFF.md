# Product sync jobs + incremental pull — implementation handoff

This document gives another agent everything needed to implement **persistent sync job tracking (Option B)** and **incremental MerchantPro product downloads**, without re-reading the whole project. It assumes the current `feat/products-page-implementation` branch (or equivalent) with the product sync fixes already merged.

---

## 1. Business goal

Users pull products from MerchantPro into the local `product` table via **Pull from Shop**.

**Desired behavior:**

1. **First sync (or after no successful full sync):** download the **entire** shop catalog (paginated, up to ~500k products for largest clients).
2. **Later syncs:** only fetch products **created or modified** since the last successful sync window, and upsert them into the local catalog (incremental build-up).
3. **Rule for switching to incremental:** only after a **completed full sync** (user requirement: “100% of products from the store on one sync”). Practically: last job for the org with `status = DONE`, `sync_type = FULL`, and evidence the run completed the catalog (see §5).
4. **Survive navigation and page refresh:** user can browse other pages and return to Products; progress should be recoverable. **Page refresh currently drops UI and stops the server sync** — Option B + polling fixes this.

---

## 2. What already exists (do not re-implement from scratch)

### Backend

| Piece | Location | Behavior |
|-------|----------|----------|
| Product table | `V31__create_product_table.sql`, `ProductEntity` | Org-scoped catalog; soft delete; `mp_product_id` (BIGINT after V35); indexes on org+mp, org+sku, org+ean (V34) |
| MP list fetch | `MerchantProProductService.fetchProducts(orgId, start, limit)` | `GET` via `GET_PRODUCTS` template; `fields=id,name,sku,ean,price_gross`; pagination uses `meta.links.next` (not page size heuristic) |
| MP lookup | `fetchInventoryByIdentifier` | `GET /api/v2/inventory/{sku\|ean}/{id}` for live price |
| Sync loop | `ProductService.syncFromShopStream` → `runSyncStream` | Async on common pool; `orgId` + `clientId` primitives (no lazy-init on `OrgEntity` in worker); 750ms throttle between pages; SSE timeout `0` (no server cap) |
| Upsert | `upsertPage` / `upsertFromMerchantPro` | Match by `mp_product_id`, then SKU, then EAN |
| Rate limits | `MerchantProProductService.executeGet` | 429 retry with 60s / 120s / 240s backoff |
| HTTP timeouts | `AppConfig` RestTemplate | 10s connect, 30s read |
| API | `GET /api/v1/products/sync?orgId=` | Returns `SseEmitter`; events `{ synced, total, done }` |
| List API | `GET /api/v1/products?orgId=&page=&size=` | Returns `ProductPage { items, totalCount, page, size }` |

**Not implemented yet:**

- No `product_sync_job` table or job repository
- No `GET /products/sync/status`
- No per-org “sync already running” guard (issue #16)
- No date filter on MP product list calls
- Sync progress is **not** persisted server-side

### Frontend

| Piece | Location | Behavior |
|-------|----------|----------|
| Global sync state | `frontend/src/contexts/SyncContext.jsx` | Holds SSE stream + progress; survives route change **within same session** |
| Products page | `frontend/src/pages/Products.jsx` | Uses context; `consumeResult` on mount if sync finished while away |
| Topbar indicator | `AppShell.jsx` | Spinner + progress text while `syncing` |
| SSE client | `productsApi.syncStream` in `api.js` | Bearer fetch; `failedMessage` i18n; end-of-stream without `done` → error |

**Gap:** Full **browser refresh** wipes `SyncContext` and drops the SSE connection; backend completes emitter on disconnect and the worker exits on next `sendProgress` failure. **Partial DB upserts remain**; UI has no job to reconnect to.

### Docs to update when done

- `API_CONTRACT.md` — §5B sync + new status endpoint, incremental behavior
- `DATA_MODEL.md` — `product_sync_job` table
- `FISCAL_BILL_MODULE_SPEC.md` / `PRODUCT_REQUIREMENTS.md` if product sync behavior is specified there

---

## 3. MerchantPro API — date filters (critical for incremental)

### Official patterns

- **Pagination:** `start`, `limit` (max 100/page). Stop when `meta.links.next` is **null**.
- **Date filters (framework):** MerchantPro v2 uses bracket notation on collection endpoints, e.g. `field[gte]=YYYY-MM-DD` or `field[gt]=...`. Documented generically at https://docs.merchantpro.com/api/ (Date filter section). Format in examples is **date-only**, not full ISO datetime.
- **Orders (confirmed in this codebase):** `MerchantProOrderService` appends `&created%5Bgt%5D=` + value when `createdAfter` is set (URL-encoded `created[gt]`).

### Products endpoint — what is confirmed vs assumed

| Param | Status | Notes |
|-------|--------|--------|
| `created[gt]` / `created[gte]` | **Assumed** for products | Same pattern as orders; **must be validated** against a real shop |
| `date_modified[gte]` / `date_modified[gt]` | **Assumed** for incremental | Best fit for “modified since last sync”; **not confirmed** in public product docs (product-specific KB pages 404) |
| `date_created[gte]` | **Assumed** | Catches new products; combine with modified filter if API allows only one |
| `is_new=true` | **Legacy only** | Used in `legacy/.../MPGetProducts.java` — boolean, create-oriented, not a timestamp |
| `status=active` | **Legacy** | Status filter, not date |

**Recommendation for implementer:**

1. **Spike on dev/staging MP shop:** try `date_modified[gte]=YYYY-MM-DD` and `created[gt]=YYYY-MM-DD` on `GET /api/v2/products` (or path from `GET_PRODUCTS` template). Confirm which param(s) work and what field MP filters on.
2. If only one date param works, prefer **modification** date for incremental; optionally run a second pass for `created[gt]` if needed.
3. **Safety margin:** subtract 1 day from `filter_from` when building the query (date-only precision → avoid missing same-day edits).
4. Add `date_modified` (and `date_created` if useful) to the `fields=` list in `MerchantProProductService.FIELDS` when you need to display or debug; upsert logic can ignore them if unchanged.

### Rate limits (from MP docs)

- ~4 req/s, 80/min, 3600/hour, 60000/day.
- Current throttle: 750ms between pages ≈ 80 pages/min — appropriate for full sync; incremental runs will be much shorter.

---

## 4. Option B — database-backed sync jobs (recommended long-term)

Option A (in-memory `ConcurrentHashMap`) was discussed for a quick fix; **this task should implement Option B** so refresh, audit, and multi-instance readiness are covered.

### Proposed table: `product_sync_job`

```sql
CREATE TABLE product_sync_job (
    sync_job_id   BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    org_id        BIGINT NOT NULL REFERENCES org(org_id),
    status        VARCHAR(16) NOT NULL,  -- RUNNING | DONE | FAILED | CANCELLED (optional)
    sync_type     VARCHAR(16) NOT NULL,  -- FULL | INCREMENTAL
    synced        INT NOT NULL DEFAULT 0,
    total         INT NOT NULL DEFAULT 0,
    error_message TEXT,
    filter_from   TIMESTAMPTZ,           -- MP filter lower bound used; NULL for FULL
    started_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at   TIMESTAMPTZ
);

CREATE INDEX idx_product_sync_job_org_started
    ON product_sync_job(org_id, started_at DESC);

-- Optional: only one RUNNING job per org (partial unique index)
CREATE UNIQUE INDEX idx_product_sync_job_org_running
    ON product_sync_job(org_id) WHERE status = 'RUNNING';
```

Flyway: next version after **V35** → `V36__create_product_sync_job.sql`.

### JPA / service responsibilities

- **`ProductSyncJobEntity` + `ProductSyncJobRepository`**
- **`ProductSyncJobService`** (or methods on `ProductService`):
  - `startJob(orgId, syncType, filterFrom)` → insert `RUNNING`, return job id
  - `updateProgress(jobId, synced, total)`
  - `completeJob(jobId, status, error?)`
  - `findLatestDoneJob(orgId)`
  - `findRunningJob(orgId)` — for status API and duplicate-start prevention
  - `getStatus(orgId)` → DTO for polling

**Worker thread must update the DB row on every page** (or every N pages to reduce writes), not only SSE. SSE becomes a **view** of job progress; polling reads the same source of truth.

---

## 5. FULL vs INCREMENTAL decision logic

Pseudocode when user starts sync for `orgId`:

```
running = findRunningJob(orgId)
if running != null:
  reject 409 or attach to existing job (product choice; prefer 409 + status endpoint)

lastFull = findLatestDoneJob(orgId) where sync_type = FULL

useIncremental = false
if lastFull != null:
  // User rule: only incremental after a complete full download
  if lastFull.synced >= lastFull.total && lastFull.total > 0:
    useIncremental = true
  // Edge case: total from MP meta can be approximate; consider also:
  //   lastFull.status == DONE && lastFull.sync_type == FULL && no FAILED since
  // Document chosen rule in API_CONTRACT.

filterFrom = null
if useIncremental:
  filterFrom = lastFull.finished_at  // or started_at; prefer finished_at
  // Apply -1 day safety for date-only MP param (see §3)
  syncType = INCREMENTAL
else:
  syncType = FULL

job = startJob(orgId, syncType, filterFrom)
runSyncStream(orgId, clientId, jobId, filterFrom, emitter)
```

**Incremental API call:** extend `fetchProducts(orgId, start, limit, Optional<LocalDate> modifiedSince)` to append the validated MP query param(s).

**After successful completion:** `completeJob(DONE, synced, total)`. **On failure:** `FAILED` + message. **On client disconnect:** decide policy — mark `FAILED` with “client disconnected” or leave `RUNNING` until timeout cleanup (prefer fail fast + allow retry).

**Partial sync:** If user refreshes mid-run, job may stay `RUNNING` or move to `FAILED`. Next start: if `RUNNING` stale (e.g. started_at > 2h ago), auto-fail or allow “resume” (resume is harder; v1 can fail stale RUNNING and require new pull).

---

## 6. API contract additions

### `GET /api/v1/products/sync/status?orgId={id}`

**Auth:** `FISCAL_MANAGE_PRODUCTS` + org access (same as sync).

**200 example:**

```json
{
  "running": true,
  "syncJobId": 42,
  "syncType": "INCREMENTAL",
  "status": "RUNNING",
  "synced": 2900,
  "total": 120584,
  "filterFrom": "2026-06-03T14:22:00Z",
  "startedAt": "2026-06-04T12:00:00Z",
  "finishedAt": null,
  "errorMessage": null
}
```

When idle after a recent completion, optionally include last job summary so UI can show “Last sync: 120584 products at …” without extra call.

### `GET /api/v1/products/sync?orgId={id}` (existing)

Keep SSE for live updates. **Also** write progress to `product_sync_job` each page.

Optional query later: `?forceFull=true` to override incremental (admin/debug).

---

## 7. Frontend changes (minimal for refresh recovery)

### `productsApi.syncStatus(orgId)`

Poll every 2–3s while `running` or on Products mount / app init if user has `FISCAL_MANAGE_PRODUCTS`.

### `SyncContext.jsx` evolution

1. On `startSync`: call sync endpoint as today; store `syncJobId` if returned (optional header or first SSE event).
2. On mount (provider level or Products): `GET sync/status` — if `RUNNING`, set `syncing=true`, `syncProgress`, start polling (and optionally open SSE only if you add “attach stream” — polling alone may suffice for UI).
3. When poll sees `DONE` / `FAILED`, mirror current `onDone` / `onError` behavior (`consumeResult`, reload list).
4. Show **sync type** in UI: i18n keys e.g. `products.syncTypeFull`, `products.syncTypeIncremental` (add `en` + `sr`).

**SSE + polling together:** SSE for smooth progress while connected; polling heals refresh and tab sleep.

---

## 8. Backend files to touch (checklist)

| Action | File / area |
|--------|-------------|
| New migration | `db/migration/V36__create_product_sync_job.sql` |
| New entity/repo | `ProductSyncJobEntity`, `ProductSyncJobRepository` |
| New or extended service | Job lifecycle + `getStatus` |
| Edit | `ProductService.runSyncStream` — create job, pass filter, update job, complete/fail |
| Edit | `MerchantProProductService.fetchProducts` — optional date filter param |
| Edit | `ProductController` — `GET /sync/status`, maybe 409 on duplicate start |
| Edit | `API_CONTRACT.md`, `DATA_MODEL.md` |

---

## 9. Known issues already fixed (do not regress)

These were addressed in a prior pass; incremental work should build on top:

- Pagination stop uses `meta.links.next == null`
- No `sku_equals` / `ean_equals` on list endpoint; lookup uses inventory API
- 429 backoff, RestTemplate timeouts
- Lazy-init fix: pass `orgId`, `clientId` into async worker
- Paginated `GET /products` (not full 500k load)
- Search capped at 50 rows
- SSE client: stream end without `done` → error; i18n `failedMessage`
- `SyncContext` for cross-page navigation within session

**Still open (this task can close):**

- Concurrent sync per org (#16) — unique index on `RUNNING`
- Refresh-safe progress — Option B + polling
- Incremental MP fetch — date filter + job `filter_from`

---

## 10. Testing plan for implementer

1. **FULL sync** small org — job row `DONE`, `sync_type=FULL`, `synced`/`total` sensible.
2. **Second sync** — `INCREMENTAL`, `filter_from` set, fewer MP pages than full.
3. **Refresh mid-sync** — poll shows `RUNNING`; after finish, Products shows success and updated list.
4. **Navigate away and back** — progress visible (context + poll).
5. **Duplicate start** — second click rejected or no-op with clear message.
6. **MP date param spike** — document actual param name in code comment + `API_CONTRACT.md`.
7. **Disconnect mid-sync** — job not left `RUNNING` forever (stale policy).

---

## 11. Out of scope (unless explicitly requested)

- Multi-node sync coordination beyond DB unique constraint
- Resuming a partially completed page mid-job without restart
- Webhook-driven sync from MerchantPro
- Deleting local products removed from shop (sync is upsert-only today)

---

## 12. Reference snippets

### Orders date filter pattern (mirror for products)

```java
// MerchantProOrderService.java ~67
if (createdAfter != null && !createdAfter.isBlank()) {
    rawUrl += "&created%5Bgt%5D=" + createdAfter;
}
```

### Current product list URL (no date filter yet)

```java
// MerchantProProductService.java ~53-57
apiBase + template.getEndpointPath()
    + "?fields=" + FIELDS
    + "&start=" + start
    + "&limit=" + limit;
```

### SSE progress payload

```java
// ProductService.SyncProgress
record SyncProgress(int synced, int total, boolean done) {}
```

---

## 13. User-facing copy (i18n)

Add matching keys in `frontend/src/locales/en.json` and `sr.json`:

- Sync type labels (Full / Incremental)
- “Sync already in progress”
- “Last sync completed at …” (optional)
- Stale job / failed job messages

Follow project rule: never hardcode user-visible strings in components.

---

*End of handoff. Implement Option B + incremental filters; validate MP date params on a real connection before locking param names in production.*
