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
    "id": "uuid",
    "role": "ADMIN",
    "subscriptionActive": true,
    "subscriptionStartAt": "2026-01-01T00:00:00Z",
    "subscriptionExpiresAt": "2026-12-31T23:59:59Z"
  }
}
```
- Errors: `400`, `401`, `403`, `429`, `500`

Subscription behavior:
- Normal users must have active, non-expired subscription to receive valid access.
- Expired subscription returns `403` with code `SUBSCRIPTION_EXPIRED`.
- Bootstrap SuperAdmin is exempt from subscription expiration validation.

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

### POST /fiscalbill/from-order
- Description: Create fiscal bill from an existing shop order.
- Request: same as POST /fiscalbill but specifically for order-linked flows.

### POST /fiscalbill/manual
- Description: Create fiscal bill from manual input.
- Request: [manual entry payload]

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
- 202 Response:
```json
{
  "syncJobId": "uuid",
  "status": "STARTED"
}
```
- Errors: `401`, `429`, `500`, `502`, `504`

## 5A. Access Control Endpoints (Role and Action Management)

### GET /roles
- Description: List roles for active client scope.
- 200 Response:
```json
[
  {
    "roleId": 1000,
    "roleCode": "RESTRICTED_OPERATOR",
    "name": "Restricted Operator",
    "description": "Standard operational access",
    "clientId": 1001,
    "actionIds": [1002, 1003]
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

### PUT /users/{userId}/role
- Description: Assign role to user.
- Errors: `400`, `401`, `403`, `404`, `500`

### PUT /users/{userId}/organizations
- Description: Assign organization access scope to user.
- Errors: `400`, `401`, `403`, `404`, `500`

### PUT /users/{userId}/subscription
- Description: Set or update normal user subscription validity window and status.
- Request:
```json
{
  "subscriptionStatus": "ACTIVE",
  "subscriptionStartAt": "2026-01-01T00:00:00Z",
  "subscriptionExpiresAt": "2026-12-31T23:59:59Z"
}
```
- Notes:
  - Allowed statuses: `ACTIVE`, `EXPIRED`, `SUSPENDED`.
  - `subscriptionStartAt` and `subscriptionExpiresAt` are required for normal users.
  - This endpoint is restricted to authorized admin/superadmin roles.
- Errors: `400`, `401`, `403`, `404`, `409`, `500`

### GET /users/{userId}/subscription-status
- Description: Get computed subscription status for user access validation.
- 200 Response:
```json
{
  "userId": "uuid",
  "subscriptionStatus": "ACTIVE",
  "subscriptionStartAt": "2026-01-01T00:00:00Z",
  "subscriptionExpiresAt": "2026-12-31T23:59:59Z",
  "isAccessAllowed": true
}
```
- Notes:
  - For bootstrap superadmin, `isAccessAllowed` is true independent of subscription dates.
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
