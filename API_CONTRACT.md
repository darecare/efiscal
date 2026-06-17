# API CONTRACT

## 1. Scope
This document defines backend API contracts between React frontend and Java backend, plus integration contract boundaries for external providers.

Integration boundary clarification:
- Java backend calls MerchantPro API directly.
- Java backend calls Serbian Tax Authority API directly.
- MerchantPro does not call Serbian Tax Authority API for this application.

Provider reference documentation:
- MerchantPro API (official docs): https://docs.merchantpro.com/api/
- MerchantPro Orders endpoint docs: https://docs.merchantpro.com/api/endpoints/orders
- Serbian Tax Authority eInvoice Create endpoint docs: https://tap.sandbox.suf.purs.gov.rs/Help/view/1522287161/Create-Invoice/en-US
- Serbian Tax Authority fiscal bill example (Normal Sale): https://tap.sandbox.suf.purs.gov.rs/Help/view/535663692/Normal-Sale/en-US
- Serbian Tax Authority tax model/example docs: https://tap.sandbox.suf.purs.gov.rs/Help/view/417621922/Model-and-Example/en-US
- Serbian Tax Authority tax amounts docs: https://tap.sandbox.suf.purs.gov.rs/Help/view/1034863356/Tax-Amounts/en-US

## 2. Standards
- Base URL: `/api/v1`
- Transport: HTTPS only
- Content Type: `application/json`
- Auth: `Authorization: Bearer <jwt>`
- Time format: ISO-8601 UTC
- Idempotency: required for write operations that may be retried

## 3. Authentication Endpoints

### POST /auth/login
- Description: Authenticate user and issue access token.
- Request:
```json
{
  "email": "user@example.com",
  "password": "string"
}
```
- 200 Response:
```json
{
  "accessToken": "jwt",
  "expiresInSeconds": 1800,
  "user": {
    "id": "1000",
    "email": "ops@acme.rs",
    "fullName": "Acme Operations",
    "roleName": "CLIENT_ADMIN",
    "clientId": 1001,
    "clientName": "Acme Retail",
    "subscriptionStatus": "ACTIVE",
    "subscriptionExpiresAt": "2026-11-23",
    "actions": ["ROLES_MANAGE", "USERS_MANAGE", "FISCAL_CREATE_BILL"],
    "allowedOrgIds": [1002, 1003],
    "preferredLanguage": "sr"
  }
}
```
- Errors: `400`, `401`, `403`, `429`, `500`

Subscription behavior:
- Normal users must have active, non-expired subscription to receive valid access.
- Expired subscription returns `403` with code `SUBSCRIPTION_EXPIRED`.
- Bootstrap SuperAdmin is exempt from subscription expiration validation.
- **Note:** The `subscriptionExpiresAt` field in the authentication/session responses is formatted as a date-only string (`YYYY-MM-DD`) due to the service implementation (`DemoDataService`), whereas standard user management endpoints return a full ISO-8601 UTC timestamp (`YYYY-MM-DDTHH:mm:ssZ`).

### POST /auth/refresh
- Description: Refresh short-lived access token (if implemented).
- Request/Response: [define]
- Errors: `401`, `500`

## 4. Fiscalization Endpoints

### POST /fiscalbill
- Description: Submit a fiscalization request for an order (backend calls Serbian Tax Authority API directly).
- Headers:
  - `Idempotency-Key: <uuid>`
- Request:
```json
{
  "orderId": "string",
  "customer": {
    "name": "string"
  },
  "items": [
    {
      "sku": "string",
      "name": "string",
      "quantity": 1,
      "unitPrice": 100.0,
      "taxRate": 20.0
    }
  ],
  "currency": "RSD",
  "paymentMethod": "CARD"
}
```
- 201 Response:
```json
{
  "fiscalDocumentId": "uuid",
  "status": "PENDING",
  "createdAt": "2026-03-24T10:00:00Z"
}
```
- Errors: `400`, `401`, `409`, `422`, `429`, `500`, `502`, `504`

### GET /fiscalbill/{id}
- Description: Get status and provider references.
- 200 Response:
```json
{
  "fiscalDocumentId": "uuid",
  "status": "SUCCESS",
  "providerReference": "string",
  "lastError": null,
  "updatedAt": "2026-03-24T10:05:00Z"
}
```

### GET /fiscalbill/{id}/details
- Description: Get detailed bill data including tax and payment rows.
- 200 Response: [bill object with items, taxes, and payments]

### GET /fiscalbill/{id}/pdf
- Description: Download fiscal bill as PDF generated from selected HTML template.
- Query:
  - `format` (optional): `a4` (default) or `roll80`
- 200 Response:
  - Content-Type: `application/pdf`
  - Content-Disposition: `attachment; filename=fiscal-bill-{id}-{format}.pdf`
- Notes:
  - Template includes fiscal header, line items (`fiscalbillline`), tax area (`fiscalbilltax`) and payments area (`fiscalbillpay`).
  - Available templates in current implementation:
    - `a4` -> `pdf-templates/default-a4.html`
    - `roll80` -> `pdf-templates/default-roll80.html` (57mm–80mm paper roll style)
  - Textual layout follows the fiscal receipt textual-display requirements (start/end fiscal section lines and grouped receipt metadata).

### POST /fiscalbill/from-order
- Description: Create fiscal bill from an existing shop order.
- Request: same as POST /fiscalbill but specifically for order-linked flows.
- Additional request fields used by the UI:
  - `sendEmail` - default `true` on the order issuance screen
  - `customerEmail` - taken from the sales order payload and passed through when emailing is enabled

### POST /fiscalbill/manual
- Description: Create fiscal bill from manual input.
- Request: [manual entry payload]
- Additional request fields used by the UI:
  - `sendEmail` - optional boolean flag
  - `customerEmail` - optional recipient email for future manual-email flows

### GET /fiscalbill/status
- Description: Get overall fiscal status summary for an organization.
- Errors: `401`, `404`, `500`

### POST /fiscalbill/{id}/retry
- Description: Retry failed/transient submission safely.
- Headers:
  - `Idempotency-Key: <uuid>`
- 202 Response:
```json
{
  "fiscalDocumentId": "uuid",
  "status": "RETRYING"
}
```
- Errors: `400`, `401`, `404`, `409`, `500`, `502`, `504`

## 5. MerchantPro Sync Endpoints

### GET /merchantpro/orders
- Description: Pull/import orders from MerchantPro API.
- Parameters:
  - orgId: (required)
  - createdAfter: (ISO date)
  - shippingStatus: (string)
  - start: (offset, default 0)
  - limit: (default 100)
- Notes:
  - `createdAfter` and `shippingStatus` are the primary MVP filter fields.
  - Backend resolves and validates allowed filter keys, then maps to provider URL query parameters.
  - Order line items expose the provider `product_ean` value as `ean` when MerchantPro returns it; fiscalization uses that value as the `gtin` source.
- 202 Response:
```json
{
  "syncJobId": "uuid",
  "status": "STARTED"
}
```
- Errors: `401`, `429`, `500`, `502`, `504`

## 5B. Products Endpoints

Base path: `/products`

Required action codes:
- `FISCAL_MANAGE_PRODUCTS` — list, create, update, delete, sync
- `FISCAL_CREATE_BILL` — catalog search (inline autocomplete), live lookup (price verification)

All endpoints require `orgId` scope validation via user's `allowedOrgIds` (except superadmin).

### GET /products
- Description: List products for an organization (paginated). Optional text search across name, SKU, EAN, product ID, MerchantPro product ID, and last known price.
- Query:
  - `orgId` (required)
  - `page` (int, default `0`)
  - `size` (int, default `100`, max `500`)
  - `q` (optional): case-insensitive substring match across catalog fields (OR logic)
- 200 Response:
```json
{
  "items": [ /* ProductDto[] */ ],
  "totalCount": 1250,
  "page": 0,
  "size": 100
}
```
- Errors: `401`, `403`, `404`

### POST /products
- Description: Create a product manually.
- Query: `orgId` (required)
- Request:
```json
{
  "name": "Widget A",
  "sku": "W-001",
  "ean": "1234567890123",
  "lastKnownPrice": 1200.00,
  "isActive": true
}
```
- 201 Response: `ProductDto`
- Errors: `400` (missing/blank name, or both sku/ean missing), `401`, `403`

### PUT /products/{id}
- Description: Update a product.
- Request: same shape as POST body
- 200 Response: `ProductDto`
- Errors: `400`, `401`, `403`, `404`

### GET /products/ids
- Description: Return product IDs for an organization (for cross-page bulk selection). Uses the same optional `q` filter as `GET /products`. Capped at 5000 IDs.
- Query:
  - `orgId` (required)
  - `q` (optional): same semantics as `GET /products`
- 200 Response:
```json
{
  "productIds": [1, 2, 3]
}
```
- Errors: `401`, `403`, `404`

### DELETE /products/bulk
- Description: Soft-delete multiple products in one request. Only non-deleted products in the given org are affected. Either pass explicit `productIds` or set `selectAll: true` to target all products (optionally filtered by `q`, same semantics as `GET /products`).
- Query: `orgId` (required)
- Request (by IDs):
```json
{
  "productIds": [1, 2, 3]
}
```
- Request (select all matching):
```json
{
  "selectAll": true,
  "q": "widget"
}
```
- Limits: 1–500 distinct IDs per request when using `productIds`; no ID limit when using `selectAll`
- 200 Response:
```json
{
  "deleted": 3
}
```
- Errors: `400` (empty list or too many IDs), `401`, `403`, `404`

### PATCH /products/bulk/status
- Description: Activate or deactivate multiple products in one request. Either pass explicit `productIds` or set `selectAll: true` with optional `q` filter (same semantics as `GET /products`).
- Query: `orgId` (required)
- Request (by IDs):
```json
{
  "productIds": [1, 2, 3],
  "isActive": false
}
```
- Request (select all matching):
```json
{
  "selectAll": true,
  "isActive": false,
  "q": "widget"
}
```
- Limits: 1–500 distinct IDs per request when using `productIds`; no ID limit when using `selectAll`
- 200 Response:
```json
{
  "updated": 3
}
```
- Errors: `400` (empty list or too many IDs), `401`, `403`, `404`

### DELETE /products/{id}
- Description: Remove a product from the visible catalog. `MANUAL` products are soft-deleted (`deleted_at`). `MERCHANTPRO` products are hidden locally (`hidden_at`) and can be restored via `GET /products/sync?mode=RESET_FULL`.
- 204 Response: empty
- Errors: `401`, `403`, `404`

### GET /products/search
- Description: Search local product catalog (used by Create Fiscal Bill inline autocomplete on line item Name). Returns active products only.
- Query:
  - `orgId` (required)
  - `q` (recommended): single term matched with OR logic — `name` contains term (case-insensitive), or exact `sku`/`ean`
  - Legacy filters: `name`, `sku`, `ean` (combined with AND when `q` is omitted)
- Minimum client query length for autocomplete: 2 characters (enforced in UI, not API)
- 200 Response: array of `ProductDto`
- Errors: `401`, `403`

### GET /products/sync/status
- Description: Pollable sync job status for an organization (DB-backed). Use after page refresh or alongside SSE.
- Query: `orgId` (required)
- 200 Response:
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
- When no job exists, `running` is `false` and other fields are null/zero.
- When idle, the most recent job for the org may be returned with `running: false` (for “last sync” UI).
- **Stale jobs:** `RUNNING` jobs older than 2 hours are auto-failed when this endpoint is called (and on sync start).
- Errors: `401`, `403`, `404`

### POST /products/sync/cancel
- Description: Mark the org's active product sync job as `FAILED` with message `Cancelled by user`. Idempotent when no job is running.
- Query: `orgId` (required)
- 204 Response: no body
- Errors: `401`, `403`, `404`

### GET /products/sync
- Description: Pull products from MerchantPro via `GET_PRODUCTS` template and upsert into local `product` table. Streams progress as Server-Sent Events (SSE). Creates a `product_sync_job` row and updates it on each page.
- Query:
  - `orgId` (required)
  - `mode` (optional, default `AUTO`): `AUTO` | `INCREMENTAL` | `FULL` | `RESET_FULL`
- Response: `text/event-stream` with JSON event payloads:
```json
{ "synced": 0, "total": 120, "done": false, "syncType": "FULL" }
```
```json
{ "synced": 120, "total": 120, "done": true, "syncType": "FULL" }
```
```json
{ "synced": 0, "total": 0, "done": true, "syncType": "INCREMENTAL", "error": "INCREMENTAL_FILTER_UNSUPPORTED: ..." }
```
- **Sync mode / type selection (server-side):**
  - `AUTO` (default) — `INCREMENTAL` when a prior completed `FULL` or `RESET_FULL` job exists and catalog has visible products; otherwise `FULL`. Forces `FULL` when the org has zero visible products (empty catalog safety net).
  - `FULL` — full MerchantPro catalog fetch; updates hidden synced rows in place but does not unhide them; marks unseen synced rows `sync_status = MISSING_IN_SOURCE`.
  - `RESET_FULL` — same as `FULL` but clears `hidden_at` on matched `MERCHANTPRO` rows (rebuild from shop).
  - `INCREMENTAL` — requires a prior completed `FULL` or `RESET_FULL` job; MerchantPro list calls use `modified[gte]=YYYY-MM-DD` with `filter_from` = last full job `finished_at` minus 1 day.
  - Job `syncType` values: `FULL`, `INCREMENTAL`, `RESET_FULL`.
- **Unsupported incremental filters:** if MerchantPro returns `undefined_filter` / "No such filter", the job fails with `errorMessage` prefixed by `INCREMENTAL_FILTER_UNSUPPORTED`. The server does **not** auto-fallback to full sync.
- **Concurrency:** at most one `RUNNING` job per org (partial unique index). Second start while running → `409` with body = current `sync/status` payload. Concurrent duplicate starts that race past the check also return `409` (unique index violation).
- **Stale jobs:** `RUNNING` jobs older than 2 hours are auto-failed on sync start and on `GET /products/sync/status`.
- Notes:
  - Client should use `fetch` with `Accept: text/event-stream, application/json` and `Authorization: Bearer <token>` (native `EventSource` cannot send the Bearer header).
  - Server-side emitter has no timeout; sync may run for large catalogs (rate-limited to ~80 MP requests/min).
  - Pagination follows MerchantPro `meta.links.next` (stops when `next` is null).
  - Client must handle stream end without `done: true` as an error.
  - Poll `GET /products/sync/status` every 2–3s while syncing (refresh recovery).
- Errors: `401`, `403`, `404`, `409` (sync already running), `429`, `502`

### GET /products/lookup
- Description: Live price lookup from MerchantPro via `GET /api/v2/inventory/{type}/{identifier}` (`type` = `sku` or `ean`). Tries SKU first, then EAN.
- Query: `orgId` (required), `sku` and/or `ean`
- 200 Response:
```json
{
  "name": "Widget A",
  "sku": "W-001",
  "ean": "1234567890123",
  "priceGross": 1250.00,
  "mpProductId": 1001
}
```
- Errors: `400`, `401`, `403`, `404`, `429`, `502`

`ProductDto` fields: `productId`, `clientId`, `orgId`, `mpProductId` (number), `name`, `sku`, `ean`, `lastKnownPrice`, `isActive`, `sourceType` (`MANUAL` | `MERCHANTPRO`), `syncStatus` (`ACTIVE` | `MISSING_IN_SOURCE`), `hiddenAt` (ISO-8601 timestamp or null)

## 5A. Access Control Endpoints (Role and Action Management)

### GET /roles
- Description: List roles for active client scope.
- Query:
  - `includeInactive` (boolean, default `false`): when `true`, inactive roles are included in the response.
- 200 Response:
```json
[
  {
    "roleId": 1000,
    "roleCode": "RESTRICTED_OPERATOR",
    "name": "Restricted Operator",
    "description": "Standard operational access",
    "clientId": 1001,
    "actionIds": [1002, 1003],
    "isActive": true
  }
]
```
- Errors: `401`, `403`, `500`

### POST /roles
- Description: Create a new custom or global role with assigned actions.
- Request:
```json
{
  "roleCode": "CASHIER_ROLE",
  "name": "Cashier",
  "description": "Handles daily sales operations",
  "clientId": 1001,
  "actionIds": [1000, 1001]
}
```
- 201 Response:
```json
{
  "roleId": 1004,
  "roleCode": "CASHIER_ROLE",
  "name": "Cashier",
  "description": "Handles daily sales operations",
  "clientId": 1001,
  "actionIds": [1000, 1001]
}
```
- Errors: `400`, `401`, `403`, `409`, `500`

### PUT /roles/{roleId}
- Description: Update role metadata (name, description, active flag).
- Request:
```json
{
  "name": "Updated Role Name",
  "description": "Updated description",
  "isActive": true
}
```
- 200 Response: updated role object (same shape as GET /roles items).
- Errors: `400`, `401`, `403`, `404`, `500`

### PUT /roles/{roleId}/actions
- Description: Replace all action assignments for a role.
- Request:
```json
{
  "actionIds": [1000, 1001, 1002]
}
```
- 200 Response: updated role object including `actionIds`.
- Errors: `400`, `401`, `403`, `404`, `500`

### DELETE /roles/{roleId}
- Description: Delete a custom/client-scoped role.
- Parameters:
  - `reassignToRoleId`: Query parameter (optional). Role ID to reassign active users to if the role is currently in use.
- Notes:
  - Global roles (where `clientId` is null) can only be deleted by SuperAdmin.
  - Immutable built-in system roles (`SUPERADMIN`, `CLIENT_ADMIN`, `OPERATOR`) cannot be deleted (returns `400`).
  - If the role is in use by active users and `reassignToRoleId` is not provided, returns `409 Conflict`.
  - When `reassignToRoleId` is provided:
    - Target role must be active and must not equal the role being deleted (`400`).
    - Client-scoped roles may only be reassigned to another role in the same client scope or to a global role (`403` if cross-client).
    - Global roles may only be reassigned to another global role (`400` if target is client-scoped).
    - Reassigning users to `SUPERADMIN` requires SuperAdmin caller (`403`).
- 204 Response: No Content
- Errors: `400`, `401`, `403`, `404`, `409`, `500`

### GET /actions
- Description: List available module actions (permission catalog).
- Query: optional `module` filter (e.g. `?module=FISCAL`).
- 200 Response:
```json
[
  {
    "actionId": 1000,
    "moduleCode": "FISCAL",
    "actionCode": "FISCAL_CREATE_BILL",
    "name": "Create Fiscal Bill",
    "description": "Allows submitting new fiscal bills"
  }
]
```
- Errors: `401`, `403`, `500`

### GET /users
- Description: List users for the active client scope. For SuperAdmin, lists all users across all clients.
- 200 Response:
```json
[
  {
    "userId": 1000,
    "email": "ops@acme.rs",
    "fullName": "Acme Operations",
    "roleCode": "CLIENT_ADMIN",
    "roleName": "Client Administrator",
    "roleId": 1002,
    "clientId": 1001,
    "clientName": "Acme Retail",
    "subscriptionStatus": "ACTIVE",
    "subscriptionStartAt": "2026-01-01T00:00:00Z",
    "subscriptionExpiresAt": "2026-12-31T23:59:59Z",
    "isActive": true,
    "orgIds": [1002, 1003],
    "preferredLanguage": "sr"
  }
]
```
- Errors: `401`, `403`, `500`

### PATCH /users/me/language
- Description: Update the authenticated user's preferred UI language (self-service; no `USERS_MANAGE` required).
- Request:
```json
{
  "preferredLanguage": "sr"
}
```
- Supported values: `en`, `sr`
- 200 Response: No content
- Errors: `400` (unsupported language), `401`, `404`, `500`

### GET /users/{userId}
- Description: Get detailed user profile by user ID.
- 200 Response: Single user object matching the shape above.
- Errors: `401`, `403`, `404`, `500`

### POST /users
- Description: Create a new user under the client scope.
- Request:
```json
{
  "email": "newuser@example.com",
  "password": "Password123!",
  "fullName": "New User",
  "clientId": 1001,
  "roleId": 1002,
  "subscriptionStatus": "ACTIVE",
  "subscriptionStartAt": "2026-01-01T00:00:00Z",
  "subscriptionExpiresAt": "2026-12-31T23:59:59Z",
  "orgIds": [1002, 1003],
  "preferredLanguage": "sr"
}
```
- `preferredLanguage` is optional; when omitted or null, the user has no stored preference (browser/local default applies until they choose a language in the UI).
- 201 Response: Created user object matching the shape above.
- Errors: `400`, `401`, `403`, `409` (email in use), `500`

### PUT /users/{userId}
- Description: Update user details (including status, password, client, role, and subscription details).
- Request:
```json
{
  "fullName": "Updated Name",
  "roleId": 1003,
  "clientId": 1001,
  "subscriptionStatus": "ACTIVE",
  "subscriptionStartAt": "2026-01-01T00:00:00Z",
  "subscriptionExpiresAt": "2026-12-31T23:59:59Z",
  "isActive": true,
  "newPassword": "NewPassword123!",
  "orgIds": [1002, 1003],
  "preferredLanguage": "en"
}
```
- 200 Response: Updated user object.
- Errors: `400`, `401`, `403`, `404`, `500`

### DELETE /users/{userId}
- Description: Soft-delete a user (sets `deletedAt` to current timestamp).
- Notes:
  - Non-superadmins can only delete users belonging to their own client scope.
  - Users cannot delete their own account.
- 204 Response: No Content
- Errors: `400` (self-deletion), `401`, `403` (client scope mismatch), `404`, `500`

## 5B. Application Info Endpoint

### GET /app-info
- Description: Return easily accessible software identity information for the About screen.
- 200 Response:
```json
{
  "manufacturer": "eFiscal",
  "serialNumber": "EFISCAL-VPS-001",
  "softwareVersion": "0.0.1-SNAPSHOT"
}
```
- Notes:
  - Values are sourced from backend application configuration.
  - The endpoint is intended for authenticated UI use in the header About/help menu.
- Errors: `401`, `500`

## 5C. Organization Management Endpoints

### GET /orgs
- Description: List organizations within caller scope.
- Query:
  - `clientId` (optional; honored for SuperAdmin calls)
- 200 Response:
```json
[
  {
    "orgId": 1002,
    "clientId": 1001,
    "clientName": "Acme Retail",
    "name": "Acme Belgrade",
    "taxId": "101234567",
    "status": "ACTIVE",
    "currency": "RSD",
    "isActive": true,
    "smtpServer": "smtp.example.com",
    "smtpPort": 587,
    "emailFrom": "no-reply@example.com",
    "smtpUsername": "smtp-user",
    "smtpConnectionSecurity": "STARTTLS",
    "logoImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
    "createdAt": "2026-03-24T10:00:00Z"
  }
]
```
- Notes:
  - `smtpPassword` is write-only and is never returned in API responses.
  - `logoImage` is optional and, when present, should be a Data URL image string.
- Errors: `401`, `403`, `500`

### GET /orgs/{orgId}
- Description: Get organization details by ID (requires scope and action checks).
- 200 Response: Same object shape as `GET /orgs` item.
- Errors: `401`, `403`, `404`, `500`

### POST /orgs
- Description: Create organization.
- Request:
```json
{
  "clientId": 1001,
  "name": "Acme Novi Sad",
  "taxId": "101234568",
  "status": "ACTIVE",
  "currency": "RSD",
  "isActive": true,
  "smtpServer": "smtp.example.com",
  "smtpPort": 587,
  "emailFrom": "no-reply@example.com",
  "smtpUsername": "smtp-user",
  "smtpPassword": "secret",
  "smtpConnectionSecurity": "STARTTLS",
  "logoImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```
- Validation:
  - `smtpPort` range: `1..65535`
  - `smtpConnectionSecurity` allowed values: `STARTTLS`, `SSL_TLS`
  - `logoImage` max payload length: `2097152` characters
- 201 Response: Created organization object (same as `GET /orgs`, excluding `smtpPassword`).
- Errors: `400`, `401`, `403`, `404`, `500`

### PUT /orgs/{orgId}
- Description: Update organization fields.
- Request: Partial update payload using the same fields as `POST /orgs`.
- 200 Response: Updated organization object (same as `GET /orgs`, excluding `smtpPassword`).
- Errors: `400`, `401`, `403`, `404`, `500`

### GET /orgs/{orgId}/payment-types
- Description: Get allowed payment types for organization.
- 200 Response:
```json
[1, 2, 4]
```
- Errors: `401`, `403`, `404`, `500`

### POST /orgs/{orgId}/payment-types
- Description: Replace allowed payment types for organization.
- Request:
```json
[1, 2, 4]
```
- 200 Response: Empty body
- Errors: `400`, `401`, `403`, `404`, `500`

## 5D. Email Template Management Endpoints

### GET /email-templates
- Description: List email templates for an organization.
- Query:
  - `orgId` (required)
- 200 Response:
```json
[
  {
    "emailTemplateId": 2001,
    "orgId": 1002,
    "orgName": "Acme Belgrade",
    "templateName": "Default Fiscal Bill Email",
    "subject": "Your fiscal bill",
    "bodyHtml": "<p>Dear {{ customername }},</p>",
    "isActive": true,
    "createdAt": "2026-06-04T10:00:00Z"
  }
]
```
- Notes:
  - `bodyHtml` stores raw HTML and is rendered later when sending the email.
- Errors: `401`, `403`, `404`, `500`

### POST /email-templates
- Description: Create a new email template for an organization.
- Request:
```json
{
  "orgId": 1002,
  "templateName": "Default Fiscal Bill Email",
  "subject": "Your fiscal bill",
  "bodyHtml": "<p>Dear {{ customername }},</p>",
  "isActive": true
}
```
- Errors: `400`, `401`, `403`, `404`, `500`

### PUT /email-templates/{templateId}
- Description: Update email template metadata/body.
- Request: Partial update payload using the same fields as `POST /email-templates`.
- 200 Response: Updated email template object.
- Errors: `400`, `401`, `403`, `404`, `500`

### DELETE /email-templates/{templateId}
- Description: Soft-delete an email template.
- 204 Response: No Content
- Errors: `401`, `403`, `404`, `500`

## 6. Error Model
All non-2xx responses should follow:
```json
{
  "timestamp": "2026-03-24T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Human readable message",
  "correlationId": "uuid",
  "details": []
}
```

## 7. Reliability and Timeouts
- Inbound request timeout: [e.g., 30s]
- Outbound provider timeout: [e.g., connect 3s, read 15s]
- Retry policy: [e.g., max 3 attempts, exponential backoff]
- Circuit breaker: [define thresholds]

## 8. Security Requirements
- JWT validation on all protected routes
- Role-based access controls per endpoint
- Action-based authorization checks per endpoint (module action code)
- Client and organization scope checks for all scoped business operations
- Subscription validity checks for normal users on login and protected operations
- Input validation on all payloads
- Mask sensitive fields in logs
- Rate limiting on auth and expensive endpoints

## 9. Versioning and Backward Compatibility
- API version in path (`/api/v1`)
- Breaking changes only via new version (`/api/v2`)
- Deprecation notice period: [define]

## 10. Open Contract Items
- AC-001: Tax Authority authentication mechanism details — Owner: [name] — Due: [date]
- AC-002: Final status enum values — Owner: [name] — Due: [date]
- AC-003: Pagination/filtering requirements for list endpoints — Owner: [name] — Due: [date]
