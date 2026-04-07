# API CONTRACT

## 1. Scope
This document defines backend API contracts between React frontend and Java backend, plus integration contract boundaries for external providers.

Integration boundary clarification:
- Java backend calls MerchantPro API directly.
- Java backend calls Serbian Tax Authority API directly.
- MerchantPro does not call Serbian Tax Authority API for this application.

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
    "role": "ADMIN"
  }
}
```
- Errors: `400`, `401`, `429`, `500`

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
  "OrderId": "string",
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

### POST /merchantpro/orders
- Description: Pull/import orders from MerchantPro API (backend-to-MerchantPro integration).
- 202 Response:
```json
{
  "syncJobId": "uuid",
  "status": "STARTED"
}
```
- Errors: `401`, `429`, `500`, `502`, `504`

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
