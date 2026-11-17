# Reports Module

## Purpose

Extensible reporting for eFiscal, starting with a **fiscal-bill reports hub**. Later hubs (orders, logs, …) use the same nav group and patterns but separate routes/controllers.

## Access

- Action: `REPORTS_VIEW` (module `REPORTS`)
- Seeded by Flyway `V53` for built-in `SUPERADMIN` and `CLIENT_ADMIN`
- Org scope: active organization from the header (`OrgContext` / `requireOrgAccess`)

## Backend

| Piece | Location |
|---|---|
| HTTP API | `ReportController` → `/api/v1/reports/fiscal` |
| Registry | `FiscalReportRegistry` |
| Handler SPI | `FiscalReportHandler` |
| First report | `ProductsSoldNetNormalReport` (`products-sold-net-normal`) |
| SQL | `FiscalBillReportRepository` (native queries) |
| Date bounds | `ReportDateRange` (Belgrade day expansion) |

### Adding a fiscal report

1. Implement `FiscalReportHandler` as a Spring `@Component` (auto-picked up by the registry).
2. Document the report code, filters, and columns in `API_CONTRACT.md`.
3. Add `reports.catalog.*` / `reports.columns.*` keys to `en.json` and `sr.json`.
4. No new route is required for fiscal reports — the hub loads the catalog and runs by `report` code.

### Products sold (Net Normal)

- Status `SUCCESS`, invoice type Normal (`0`) only
- Sale (`transactionType=0`): sold columns; Refund (`1`): refunded columns; net = sold − refunded
- Period: `fiscalbill.efiscal_sdcdatetime` (nulls excluded)
- Grouping key: `product_id` → `sku` → `gtin` → `name`
- Index: `idx_fiscalbill_report_org_sdc`

### CSV export

- Same filters as the on-screen run; **all** matching rows (not the current page)
- UTF-8 with BOM; headers localized via `lang`
- Rejected when row count &gt; `app.reports.export-max-rows` (default 50_000)

## Frontend

- Nav: Reports → Fiscal bills → `/reports/fiscal`
- Page: `frontend/src/pages/ReportsFiscal.jsx`
- API: `reportsApi` in `frontend/src/services/api.js`

## Non-fiscal reports (later)

Add e.g. `/reports/orders` with its own hub page and `/api/v1/reports/orders` controller. Do **not** put non-fiscal handlers into `FiscalReportRegistry`.
