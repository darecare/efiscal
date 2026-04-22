# API Documentation

## Overview

The Kliklak Dashboard API provides endpoints for user authentication, user management, and MerchantPro integration.

Base URL: `http://localhost:8000/api`

## Authentication

All endpoints except `/auth/register` and `/auth/login` require authentication using a Bearer token, with one current exception: `GET /config/product-statuses` is publicly accessible.

Include the token in the Authorization header:
```
Authorization: Bearer <your_access_token>
```

## Endpoints

### Authentication

#### Register a New User

**POST** `/auth/register`

Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "securepassword123"
}
```

**Response:** (201 Created)
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "is_active": true,
  "is_superuser": false,
  "role": "user",
  "vendor_name": null,
  "created_at": "2024-01-22T10:30:00Z"
}
```
Public registration always creates users with `role: "user"`. Use `POST /users/` (superuser only) to create superuser or dobavljac accounts.

#### Login

**POST** `/auth/login`

Authenticate and receive an access token.

**Request Body:** (form-data)
```
username: johndoe
password: securepassword123
```

**Response:** (200 OK)
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

#### Get Current User

**GET** `/auth/me`

Get information about the currently authenticated user.

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** (200 OK)
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "is_active": true,
  "is_superuser": false,
  "role": "user",
  "vendor_name": null,
  "created_at": "2024-01-22T10:30:00Z"
}
```

### Users

User management endpoints are **superuser-only** unless noted. Roles: `superuser`, `user`, `dobavljac`. Dobavljac users must have `vendor_name` set (used to filter orders).

#### List All Users

**GET** `/users/`

Get a list of all users (admin only).

**Query Parameters:**
- `skip` (int): Number of records to skip (default: 0)
- `limit` (int): Maximum number of records to return (default: 100)

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** (200 OK)
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "username": "johndoe",
    "is_active": true,
    "is_superuser": false,
    "role": "user",
    "vendor_name": null,
    "created_at": "2024-01-22T10:30:00Z"
  }
]
```

#### Create User (Superuser only)

**POST** `/users/`

Create a new user. Requires superuser.

**Request Body:**
```json
{
  "email": "newuser@example.com",
  "username": "newuser",
  "password": "securepassword123",
  "role": "user",
  "vendor_name": null
}
```

- `role`: One of `superuser`, `user`, `dobavljac`. Default `user`.
- `vendor_name`: Required when `role` is `dobavljac`; must match the vendor name used in order line items (`meta_fields.dobavljac`). Ignored for other roles.

**Response:** (201 Created) – Same shape as Get User by ID.

**Errors:** 400 if email/username already exists, or if `role` is `dobavljac` and `vendor_name` is missing.

#### Get User by ID

**GET** `/users/{user_id}`

Get information about a specific user.

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** (200 OK)
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "is_active": true,
  "is_superuser": false,
  "role": "user",
  "vendor_name": null,
  "created_at": "2024-01-22T10:30:00Z"
}
```

#### Update User

**PUT** `/users/{user_id}`

Update user information.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:** All fields optional. Only superusers may send `role`, `vendor_name`, `is_active`.
```json
{
  "email": "newemail@example.com",
  "username": "newusername",
  "password": "newpassword123",
  "role": "user",
  "vendor_name": null,
  "is_active": true
}
```

**Response:** (200 OK)
```json
{
  "id": 1,
  "email": "newemail@example.com",
  "username": "newusername",
  "is_active": true,
  "is_superuser": false,
  "role": "user",
  "vendor_name": null,
  "created_at": "2024-01-22T10:30:00Z"
}
```

#### Delete User (Superuser only)

**DELETE** `/users/{user_id}`

Permanently delete a user. Requires superuser. Cannot delete your own account.

**Response:** 204 No Content.

**Errors:** 400 if attempting to delete the current user; 403 if not superuser; 404 if user not found.

### Orders

#### List Sales Orders (Denormalized by Line Items)

**GET** `/orders/`

Fetch orders from MerchantPro via the backend and return a **denormalized** list (one row per line item).

**Role-based behavior:**
- **Superuser and user:** Full access; all filters apply; no date restriction.
- **Dobavljac (supplier):** Orders are filtered to line items where `vendor` matches the user's `vendor_name`. `created_after` is enforced to at most 2 weeks ago (defaults to 2 weeks ago if not provided). User must have `vendor_name` set or the API returns 400.

**Query Parameters:**
- `created_after` (string, optional): Filter by creation date (YYYY-MM-DD)
- `shipping_status` (string, optional): Filter by shipping status
- `payment_status` (string, optional): Filter by payment status (forwarded to MerchantPro; e.g. `awaiting`, `paid`)
- `payment_method_code` (string, optional): Filter by payment method code (forwarded to MerchantPro; e.g. `cash_delivery`, `wire`, `intesa`)
- `start` (int, optional): Pagination offset in **orders** (default: 0)
- `limit` (int, optional): Max orders per page (default: 100, max: 100)
- `sort` (string, optional): Sort parameter

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** (200 OK)
```json
{
  "data": [...rows...],
  "meta": {
    "start": 0,
    "limit": 100,
    "count": 174,
    "total": 140
  }
}
```

**Important – pagination semantics:**
- `data` – Denormalized rows (one row per line item). A single order can produce multiple rows.
- `meta.total` – **Order count** (from MerchantPro). Use this for pagination, not row count.
- `meta.count` – Number of rows in this response.
- `start` / `limit` – Order-based pagination. `start=0` fetches the first 100 orders; the response may contain more rows because of denormalization.

**UI action – Preuzmite za obradu (combined processing fetch):**  
The Obrada porudžbina screen provides a **Preuzmite za obradu** button (hidden for dobavljac users). When clicked, the frontend performs **three** parallel paginated GET requests to this endpoint, each with `shipping_status=awaiting` and one of: (1) `payment_status=awaiting`, `payment_method_code=cash_delivery`; (2) `payment_status=paid`, `payment_method_code=wire`; (3) `payment_status=paid`, `payment_method_code=intesa`. Optional `created_after` is sent when the user has set "Datum od". The three result sets are merged into one and shown in the orders table; a badge indicates that the combined "ready for processing" view is active.

#### Bulk Update Product Status

**POST** `/orders/bulk-update-status`

Bulk update product status for selected order line items. **Dobavljac** users: only line items whose `meta_fields.dobavljac` matches their `vendor_name` are updated; others are left unchanged.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "selected_ids": ["42209911-125011", "42209911-125020"],
  "status_id": 12,
  "filters": {
    "start": 0,
    "limit": 100,
    "created_after": "2025-01-01",
    "shipping_status": "awaiting"
  }
}
```

- `selected_ids` – Array of row IDs. Prefer `order_id-line_item_id` (e.g. `"51233021-98765"`). Backend matches by line_item id; product_id is supported for backward compatibility.
- `status_id` – Product status ID to set
- `filters` – Same as GET `/orders/` (used for refresh before update). May include `payment_status`, `payment_method_code`, `shipping_status`, `sort`.
- `filters.combined_combos` (array, optional) – Used by the "Preuzmite za obradu" flow to refresh against the exact 3 combined fetch combinations before PATCH:
  - `{"payment_status":"awaiting","payment_method_code":"cash_delivery"}`
  - `{"payment_status":"paid","payment_method_code":"wire"}`
  - `{"payment_status":"paid","payment_method_code":"intesa"}`

**Refresh behavior before update:**
- Backend re-fetches all matching pages (`start`/`limit` pagination loop) before applying updates.
- When `combined_combos` is provided, backend refreshes each combo separately and merges distinct orders.

**Response:** (200 OK)
```json
{
  "total_orders": 2,
  "successful_updates": 2,
  "failed_updates": 0,
  "results": [
    {"order_id": "42209911", "success": true},
    {"order_id": "42209912", "success": true}
  ]
}
```

#### Bulk Update Order Tags

**POST** `/orders/bulk-update-tags`

Add one or more tags to selected orders. The backend extracts distinct `order_id` values from `selected_ids` and applies each requested tag per order through MerchantPro tag API calls.

**Orders UI selection behavior after bulk actions:**  
After either bulk action endpoint completes and the Orders screen refreshes data, the frontend preserves selection state:
- if explicit rows were selected, it restores only rows that still exist in refreshed results (intersection behavior)
- if "Select all matching" was active, it remains active after refresh

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "selected_ids": ["51233021-98765", "51233021-98766", "51233022-99001"],
  "tags": ["Preuzeto", "Spremno"]
}
```

- `selected_ids` – Row IDs; order IDs are derived from the prefix before `-`
- `tags` – List of tag names to add

**Response:** (200 OK)
```json
{
  "total_orders": 2,
  "successful_updates": 2,
  "failed_updates": 0,
  "results": [
    {"order_id": "51233021", "success": true},
    {"order_id": "51233022", "success": true}
  ]
}
```

### Config

#### Get Product Statuses

**GET** `/config/product-statuses`

Return the list of product status values used for bulk status updates.

Configuration source is environment-aware:
- Tries `backend/app/config/product_statuses_<ENVIRONMENT>.json` first (for example: `product_statuses_qa.json`, `product_statuses_prod.json`).
- Falls back to `backend/app/config/product_statuses.json` if the environment-specific file is not present.

**Authentication:** Not required (public endpoint in current implementation).

**Response:** (200 OK)
```json
[
  {"id": 9, "name": "10 - Poslat upit", "color": "#FFFF00FF"},
  {"id": 12, "name": "20 - Dostupno", "color": "#00FFFFFF"}
]
```

### MerchantPro Integration

#### Get Orders

**GET** `/merchantpro/orders`

Fetch orders from MerchantPro (thin proxy; returns MerchantPro JSON largely unchanged).

**Query Parameters:**
- `order_status` (string, optional): Filter by order status (backend maps this to MerchantPro `status`)
- `limit` (int): Maximum number of records to return (default: 50, max: 100)

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** (200 OK)
```json
{
  "data": [...],
  "meta": {
    "count": { "total": 3, "current": 3, "start": 0, "limit": 50 },
    "links": { "prev": null, "current": "/api/v2/orders", "next": null }
  }
}
```

#### Get Order by ID

**GET** `/merchantpro/orders/{order_id}`

Get details of a specific order from MerchantPro (thin proxy; returns MerchantPro JSON).

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** (200 OK)
```json
{
  "...": "MerchantPro order resource"
}
```

#### Get Products

**GET** `/merchantpro/products`

Fetch products from MerchantPro (thin proxy; returns MerchantPro JSON largely unchanged).

**Query Parameters:**
- `limit` (int): Maximum number of records to return (default: 50, max: 100)

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** (200 OK)
```json
{
  "data": [...],
  "meta": {
    "count": { "total": 100, "current": 50, "start": 0, "limit": 50 },
    "links": { "prev": null, "current": "/api/v2/products", "next": "/api/v2/products?start=50&limit=50" }
  }
}
```

#### Update Order Status

**PUT** `/merchantpro/orders/{order_id}/status`

Update the status of an order in MerchantPro.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "status": "processing"
}
```

**Response:** (200 OK)
```json
{
  "id": "ORD-12345",
  "status": "processing",
  "updated_at": "2024-01-22T11:00:00Z"
}
```

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "detail": "Error message describing what went wrong"
}
```

### 401 Unauthorized
```json
{
  "detail": "Could not validate credentials"
}
```

### 403 Forbidden
```json
{
  "detail": "Not enough permissions"
}
```

### 404 Not Found
```json
{
  "detail": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "detail": "Internal server error message"
}
```

## Interactive API Documentation

When the backend is running, you can access interactive API documentation at:

- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

These interfaces allow you to test API endpoints directly from your browser.

## Rate Limiting

MerchantPro integration calls are currently throttled in the service layer:
- Up to ~3 calls/second
- Up to ~90 calls/minute

These limits are enforced to reduce API throttling errors when fetching/updating many orders.

## CORS

The API supports CORS for the origins specified in the `ALLOWED_ORIGINS` environment variable.

## Authentication Flow

1. Register a new user via `/auth/register` (creates `role: "user"`) or have a superuser create an account via `POST /users/` (any role).
2. Login via `/auth/login` to receive an access token.
3. Frontend calls `GET /auth/me` to load current user (including `role`, `vendor_name`).
4. Include the token in the `Authorization` header for all subsequent requests.
5. Tokens expire after 30 minutes (configurable via `ACCESS_TOKEN_EXPIRE_MINUTES`).
6. After expiration, login again to get a new token.
