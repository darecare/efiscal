# Details for each function

## Function - User Login
**Purpose:** Authenticate users and redirect to dashboard

**User Story:** As a user, I want to log in with my credentials so I can access the dashboard.

**Inputs:**
- Username (string, required)
- Password (string, required)

**Process:**
1. User enters username and password on login page
2. Frontend sends POST request to `/api/auth/login`
3. Backend validates credentials against database
4. If valid, generate JWT access token
5. Return token to frontend
6. Frontend stores token in localStorage
7. Frontend calls `GET /api/auth/me` to load current user details
8. Redirect to dashboard

**Outputs:**
- Success: JWT access token (frontend loads user via `/api/auth/me`)
- Failure: 401 error with message "Incorrect username or password"

**Business Rules:**
- Only active users (is_active=true) can log in
- Three roles: **superuser**, **user** (regular), **dobavljac** (supplier). Role and optional `vendor_name` are returned by `GET /auth/me`.
- Only users with `role === "superuser"` see the "Korisnici" (Users) menu option in the sidebar and can access `/users`.
- **Dobavljac** users see only their own vendor's orders on the Orders page and have a forced date range (max 2 weeks).

**UI Elements:**
- Login form with username/password fields
- "Login" submit button
- Error message display area

**API Endpoint:**
- **Method:** POST
- **URL:** `/api/auth/login`
- **Request Body:** form-data (`username`, `password`)
- **Response:** `{"access_token": "string", "token_type": "bearer"}`

---

## Function - Fetch Sales Orders
**Purpose:** Retrieve sales orders from MerchantPro API and display in dashboard

**User Story:** As a user, I want to view sales orders that will be used to change substatus of products in each order

**Inputs:**
Parameter filters:
- Date from (optional): start_date, end_date (ISO 8601 format)
- Shipping Status filter (optional): temporary, awaiting, confirmed, in_process, shipped, delivered, returned, cancelled
- Pagination start (optional, default: 0) - offset into result set (computed as `(page_number - 1) * page_size`)
- Page size / limit (optional, default: 100, max: 100)
- Vendor - this specific filter parameter is used on json response to filter directly products that have field line_items[].meta_fields.dobavljac = vendor
- commercialist - this specific filter parameter is used on json response to filter directly products that have field line_items[].meta_fields.komercijalista = commercialist

**Process:**
1. User navigates to "Sales Orders" page
2. User optionally sets filters (date range, status )
3. Frontend sends GET request to `/api/orders/` with query parameters
4. Backend authenticates request using JWT Bearer token
5. Backend calls MerchantPro API with credentials from `.env`
6. Backend transforms MerchantPro response to internal format
7. Backend returns paginated list of orders
8. Frontend displays orders in table (product rows by default, grouped-by-order optional).
9. Data displayed in table should be denormalized from JSON response. If order has 3 line items, then in table there will be 3 rows shown.
10. backend will call MerchantPro API and keep json data response in memory.
11. Frontend will do additional filter per dobavljac and komercijalista.


**Notes:**
- Status column shows colored square (🟨🟦🟩🟥) + status text
- Order-level data repeats for each line item row
- Default API sort: `date_created.desc`
- Filterable: Date, Shipping Status (`awaiting`, `confirmed` in current UI), Status plaćanja, Način plaćanja, Vendor (Dobavljač), Commercialist (Komercijalista)

**Outputs:**
- Success: Array of sales order objects with pagination metadata
- Failure: 500 error if MerchantPro API is unavailable

**MerchantPro API documentation**
https://docs.merchantpro.com/api/

**MerchantPro API documentation orders endpoint**
https://docs.merchantpro.com/api/endpoints/orders

**Data Model - Sales Order:**
```json
{
    "id": "42209911",
    "sys_id": 105903863,
    "public_code": "e4df098597d267f1a0f0f849ded355e9",
    "channel_id": null,
    "payment_status": "awaiting",
    "payment_status_text": "Plaćanje na čekanju",
    "payment_method_code": "cash_delivery",
    "payment_method_name": "Plaćanje pouzećem",
    "payment_details": [],
    "shipping_status": "awaiting",
    "shipping_status_text": "Porudžbina na čekanju",
    "shipping_method_id": 1,
    "shipping_method_name": "Kurirska služba",
    "shipping_delivery_info": {
        "delivery_time": "Isporuka u 1-2 dana"
    },
    "shipping_amount": 0,
    "shipping_tax_percent": 19,
    "shipping_tax_amount": 0,
    "shipping_cod_amount": 40498,
    "subtotal_including_tax": 40498,
    "subtotal_excluding_tax": 33748.3334,
    "tax_amount": 6749.6666,
    "subtotal_amount": 40498,
    "total_amount": 40498,
    "paid_amount": 0,
    "currency": "RSD",
    "has_returns": false,
    "customer_id": 4,
    "customer_email": "darko_z@yahoo.com",
    "customer_ip_address": null,
    "customer_ip_country": null,
    "customer_device": null,
    "customer_lang": "sr_RS",
    "customer_note": null,
    "date_created": "2025-12-18T10:40:46+02:00",
    "date_modified": "2025-12-18T10:40:46+02:00",
    "payment_date": null,
    "created_by": "system",
    "source_type": "system",
    "meta_fields": {
        "test": "Da, putem Viber aplikacije",
        "viber1": "0641237082"
    },
    "billing_type": "individual",
    "billing_name": "Darko",
    "billing_country_id": 227,
    "billing_country_code": "RS",
    "billing_country_name": "Serbia",
    "billing_state_id": 4351,
    "billing_state": "Grad Beograd",
    "billing_district": null,
    "billing_city": "Beograd (Rakovica)",
    "billing_address": "Mihaila Bulgakova, 68d",
    "billing_postal_code": "11160",
    "billing_full_address": "Mihaila Bulgakova, 68d, 11160, Beograd (Rakovica), Grad Beograd, RS",
    "billing_phone": "0641237082",
    "shipping_as_billing": true,
    "shipping_name": "Darko",
    "shipping_country_id": 227,
    "shipping_country_code": "RS",
    "shipping_country_name": "Serbia",
    "shipping_state_id": 4351,
    "shipping_state": "Grad Beograd",
    "shipping_district": null,
    "shipping_city": "Beograd (Rakovica)",
    "shipping_address": "Mihaila Bulgakova, 68d",
    "shipping_postal_code": "11160",
    "shipping_phone": "0641237082",
    "shipping_full_address": "Mihaila Bulgakova, 68d, 11160, Beograd (Rakovica), Grad Beograd, RS",
    "bank_name": null,
    "bank_account": null,
    "line_items": [
        {
            "item_type": "product",
            "product_id": 125011,
            "product_sku": null,
            "product_ean": null,
            "stock_snapshot": 0,
            "product_stock": "-1.000",
            "product_name": "SPRT SP-POS887 Termalni štampač",
            "product_url": "https://probaproba.shopmania.biz/kupi?id=125011",
            "product_image_url": "https://c.cdnmp.net/203139170/img/site/t_nophoto_1_250x250.gif",
            "product_tax_name": "PDV",
            "product_tax_percent": 20,
            "category_id": 244,
            "category_name": "Štampači",
            "manufacturer_id": 1000334,
            "manufacturer_name": "N/A",
            "product_availability_id": null,
            "quantity": 1,
            "unit_price_net": 13749.1667,
            "unit_tax_amount": 2749.8333,
            "unit_price_gross": 16499,
            "line_subtotal_net": 13749.1667,
            "line_tax_amount": 2749.8333,
            "line_subtotal_gross": 16499,
            "weight": 0,
            "backorder_quantity": -1,
            "meta_fields": {
                "dobavljac": "Master Team",
                "komercijalista": "Darko Veličković"
            },
            "status": {
                "id": 9,
                "name": "10 - Poslat upit",
                "color": "#FFFF00FF"
            },
            "gift_card_delivery": null
        },
        {
            "item_type": "product",
            "product_id": 125020,
            "product_sku": null,
            "product_ean": null,
            "stock_snapshot": 0,
            "product_stock": "-1.000",
            "product_name": "Teleskop SkyOptics BM-900114 EQIII",
            "product_url": "https://probaproba.shopmania.biz/kupi?id=125020",
            "product_image_url": "https://c.cdnmp.net/203139170/img/site/t_nophoto_1_250x250.gif",
            "product_tax_name": "PDV",
            "product_tax_percent": 20,
            "category_id": 800,
            "category_name": "Teleskopi",
            "manufacturer_id": null,
            "manufacturer_name": null,
            "product_availability_id": null,
            "quantity": 1,
            "unit_price_net": 19999.1667,
            "unit_tax_amount": 3999.8333,
            "unit_price_gross": 23999,
            "unit_price_regular": 34999,
            "line_subtotal_net": 19999.1667,
            "line_tax_amount": 3999.8333,
            "line_subtotal_gross": 23999,
            "weight": 0,
            "backorder_quantity": -1,
            "meta_fields": {
                "dobavljac": "Techno Team",
                "komercijalista": "Milic M"
            },
            "gift_card_delivery": null
        }
    ],
    "info_url": "https://probaproba.shopmania.biz/naruci/info/e4df098597d267f1a0f0f849ded355e9.1771106400.ywu3DlRhcb/42209911",
    "proforma_url": "https://probaproba.shopmania.biz/naruci/proforma/e4df098597d267f1a0f0f849ded355e9.1771106400.ywu3DlRhcb/42209911",
    "carrier_tracking": []
}
```

**Business Rules:**
- Only authenticated users can access sales orders
- **Superuser and user:** See all orders; all filters (Dobavljač, Komercijalista, etc.) available; no date limit.
- **Dobavljac (supplier):** See only line items where `vendor` (dobavljac) matches their `vendor_name`. "Datum od" is required and limited to the last 2 weeks. Vendor and Commercialist filters are hidden. "Preuzmite fajlove" action is hidden.
- Maximum 100 orders per page (MerchantPro max `limit=100`; backend enforces `limit <= 100`)

**UI Elements:**
Page `Orders`, same template as `Dashboard. 
- App Menu Sidebar
- Filter panel with Parameter filters
- Pagination controls (prev/next, page number) for client-side pagination of currently loaded rows/groups
- **Preuzmite porudžbine** button – fetches orders using the selected "Filteri za preuzimanje" (date, shipping status). Requires at least one of these filters to be set. Requests use `sort=date_created.desc` by default.
- **Preuzmite za obradu** button – one-click fetch of all orders ready for processing. Visible only to superuser and user (hidden for dobavljac). On click, the app sends three parallel paginated GET requests to `/api/orders/` with `shipping_status=awaiting` and, respectively: (1) `payment_status=awaiting`, `payment_method_code=cash_delivery`; (2) `payment_status=paid`, `payment_method_code=wire`; (3) `payment_status=paid`, `payment_method_code=intesa`. Requests use `sort=date_created.desc`. Results are merged into a single table. Optional "Datum od" is applied when set; the shipping-status dropdown is not used (always awaiting). A dismissible badge "Prikaz: porudžbine za obradu (3 kombinacije)" appears above the table when this view is active; it is cleared when the user clicks "Preuzmite porudžbine" or changes the fetch filters.
- Loading spinner during API calls
- Error message display area

Data table with columns (product view):
- Checkbox column (first): row checkbox, header has "Select all on page" and "Select all (all pages)" checkboxes
- Šifra porudžbine (order_id)
- Ime kupca (shipping_name)
- Način plaćanja (payment_method_name)
- Status plaćanja (payment_status)
- Status isporuke (shipping_status)
- Datum kreiranja (date_created)
- Količina (line item quantity)
- Status proizvoda (line_items[].status.name + color)
- Naziv proizvoda (line_items[].product_name)
- Šifra dobavljača (`sifra_dobavljaca`, with fallback to `supplier_sku`)
- line_items[].status.name and small square with line_items[].status.color - if line_item status is not defined, than fields will not appear in json response, show blank in table if missing
- line_items[].meta_fields.dobavljac - if line_item dobavljac is not defined, than fields will not appear in json response, show blank in table if missing
- line_items[].meta_fields.komercijalista - if line_item komercijalista is not defined, than fields will not appear in json response, show blank in table if missing

Grouped-by-order view columns:
- Checkbox + expand control
- Šifra porudžbine, Ime kupca, Način plaćanja, Status plaćanja, Status isporuke, Datum kreiranja
- Broj stavki
- Tagovi

Data table 

Possible Status values:
1 - Poslat upit			
2 - Dostupno			
3 - Poručeno za WEB			
4 - Poručeno za VP			
5 - Stiglo u VP			
6 - Nije stiglo			
7 - Nema na stanju

**Bulk Selection (Base for Actions):**

Selection UI is used by active product and order actions. Implemented in [frontend/src/pages/Orders.jsx](frontend/src/pages/Orders.jsx).

- **Row checkboxes**: One per line item row; toggle individual selection.
- **"Select all on page"**: Selects all displayed rows (respects current filters).
- **"Select all (all pages)"**: Flag-based; indicates intent to select all items matching current filters across all pages. No upfront fetch; action handlers resolve when invoked.

**Selection state:**
- `selectedIds`: Set of row IDs (format: `order_id-line_item_id` or `order_id-product_id-index`).
- `selectAllMatching`: Boolean; when true, user wants all items matching current filters.

**For action handlers:** Check `selectAllMatching` first; if true, apply to all matching items (fetch IDs or use API filters). Else use `selectedIds` for specific items. Selection clears when filters change. After action-triggered refresh (bulk status update or tag update), the UI restores selection to the intersection of previously selected rows and refreshed rows (or keeps `selectAllMatching` active when it was active before refresh). The bulk status update API expects `selected_ids` as `order_id-line_item_id`; the backend matches by line item id so updates succeed for all orders (fixes cases where status update previously failed for some orders).

**Grouped View + Action Availability:**
- Orders page supports toggle between product rows and grouped-by-order view.
- Selection is preserved while toggling grouping.
- When switching from grouped view back to product view, selected orders are expanded into all product line-item selections for those orders.
- Action dropdown is context-aware:
  - Product view: product actions only (`Izmenite statuse proizvoda u porudžbinama`, `Preuzmite fajlove`).
  - Grouped view: order actions only (`Dodajte nove tagove`, `Označi porudžbine kao preuzete`).

**UI Mockup - Table Structure:**

| ☐ | Šifra porudžbine | Ime kupca | Način plaćanja | Status plaćanja | Status isporuke | Datum kreiranja | Količina | Status | Naziv proizvoda | Dobavljač | Šifra dobavljača | Komercijalista |
|----|-------------------|-----------|----------------|----------------|-----------------|----------------|----------|--------|----------------|-----------|------------------|----------------|
| ☐ | 42209911 | Darko | Plaćanje pouzećem | awaiting | awaiting | 2025-12-18 10:40:46 | 1 | 🟨 10 - Poslat upit | SPRT SP-POS887 Termalni štampač | Master Team | MT-001 | Darko Veličković |
| ☐ | 42209911 | Darko | Plaćanje pouzećem | awaiting | awaiting | 2025-12-18 10:40:46 | 1 | 🟦 20 - Naručen | Teleskop SkyOptics BM-900114 EQIII | Techno Team | TT-002 | Milic M |

**API Endpoint:**
- **Method:** GET
- **URL:** `/api/orders/`
- **Query Parameters:**
  - `created_after` - Order creation date (optional): YYYY-MM-DD
  - `shipping_status` (optional): backend supports MerchantPro statuses; current UI dropdown exposes `awaiting` and `confirmed`
  - `payment_status` (optional): Filter by payment status (e.g. awaiting, paid); forwarded to MerchantPro.
  - `payment_method_code` (optional): Filter by payment method code (e.g. cash_delivery, wire, intesa); forwarded to MerchantPro.
  - `sort` (optional): sort parameter (`date_created.desc` used by default in Orders UI fetch/refresh flows)
  - `start` (optional): pagination offset in **orders** (default: 0)
  - `limit` (optional): max orders per page (default: 100, max: 100)
- **Headers:** `Authorization: Bearer <token>`
- **Response:**
```json
{
  "data": [...denormalized rows, one per line item...],
  "meta": {
    "start": 0,
    "limit": 100,
    "count": 174,
    "total": 140
  }
}
```
- **Pagination:** backend API pagination is order-based (`start`/`limit`), while current Orders UI loads available API pages and paginates filtered rows/groups client-side.

**External API Integration:**
- **Service:** MerchantPro API
- **Authentication:** basic Auth Username (stored in `MERCHANTPRO_API_USERNAME` env variable) and Password (stored in `MERCHANTPRO_API_PASSWORD` env variable)
- **Base URL:** Stored in `MERCHANTPRO_API_URL` env variable
- **Endpoint:** `GET /api/v2/orders` (pagination uses `start` + `limit`, max `limit=100`)
- **Rate Limit:** App-level throttling in service layer: 3 calls/second and 90 calls/minute
- **Retry Logic:** 3 retries with exponential backoff on 5xx errors
- **Timeout:** 30 seconds

**Error Handling:**
List of MerchantPro API errors
Code	Description
200	Request successful
400	Bad Request
401	Unauthorized
403	Forbidden
404	Resource not found
405	Method not allowed
429	Too many requests
500	Internal server error

- 401: User not authenticated → Redirect to login
- 403: User not authorized → Show error message
- 500: MerchantPro API error → Show "Service unavailable, please try again"
- 504: Timeout → Show "Request timed out, please try again"
- Network error → Show "Network error, check connection"

Orders page shows inline fetch errors (`error-message`) and uses alerts for action failures/summaries.

**Performance Requirements:**
- API response time: < 3 seconds (90th percentile)

**Testing:**
- Unit tests for API endpoint handler
- Integration tests for MerchantPro API calls
- Frontend tests for table rendering and filtering
- E2E test for complete user flow

**Dependencies:**
- Backend: `httpx` for HTTP calls
- Frontend: custom React table rendering in `Orders.jsx`
- Database: None (data fetched in real-time from MerchantPro)

---

## Function - User Management (Superuser only)
**Purpose:** Superusers can list, add, edit, and delete users and set their role (superuser, user, dobavljac).

**User Story:** As a superuser, I want to manage all users so that I can add suppliers (dobavljac), promote admins (superuser), or remove users.

**Inputs:**
- List: none (paginated list)
- Add: username, email, password, role (superuser | user | dobavljac), vendor_name (required when role is dobavljac)
- Edit: same fields, password optional
- Delete: user id (cannot delete self)

**Process:**
1. Superuser opens "Korisnici" from the sidebar
2. Table shows all users with columns: ID, Korisničko ime, Email, Uloga, Dobavljač, Aktivan, Datum kreiranja, Akcije
3. "Dodajte korisnika" opens a modal: username, email, password, role dropdown, vendor_name (shown only when role is Dobavljač). Submit calls `POST /api/users/`
4. Edit (✏️) opens modal with same fields; password optional. Submit calls `PUT /api/users/{id}`
5. Delete (🗑️) shows confirmation; submit calls `DELETE /api/users/{id}`. Backend rejects delete of current user

**Outputs:**
- Success: list refreshes or modal closes
- Failure: 400 (e.g. email/username taken, dobavljac without vendor_name), 403 (not superuser), 404 (user not found)

**Business Rules:**
- Only `role === "superuser"` can access the Users page and user CRUD APIs
- Dobavljac users must have `vendor_name` set (exact match to order line_items meta_fields.dobavljac)
- `is_superuser` is kept in sync with `role === "superuser"` in the backend

**API Endpoints:**
- `GET /api/users/` – list users (superuser only)
- `POST /api/users/` – create user (superuser only)
- `GET /api/users/{id}` – get user
- `PUT /api/users/{id}` – update user (role/vendor_name only by superuser)
- `DELETE /api/users/{id}` – delete user (superuser only; cannot delete self)

---

## Function - change user data
**Purpose:** User can change data related to his account

**User Story:** As a user, I want to edit account data, such as username, email and password

**Inputs:**

**Process:**
1. User navigates to "Account" page
2. User edits available fields:
- Username
- Password - double password entry for security and
- Email
3. After user clicks on Save button, system show info message - 'User account details have been updated.'

**UI Elements**
- Page `Account` using same template as for `Dashboard`
- In upper right corner of window, icon User with a link to a page `Account`


## Function - change product status
**Purpose:** User can bulk update orders products data

**User Story:** As a user, I want to edit order products status data, that will be updated in online shop using API.

**Inputs:**
- User selects rows from a data table (using existing checkbox selection)
- User selects new product status from dropdown

**Process:**
1. User selects rows from orders table using checkboxes (existing functionality)
2. User selects option `Edit Order Products status` from "Select Action" dropdown in bulk actions toolbar
3. User clicks on "Apply" button
4. System validates that rows are selected (existing validation - no action if no selection)
5. System calls GET method to refresh order data with same filters used in "Fetch Sales Orders"
   - If GET refresh fails: Stop process and show error message, do not proceed with PATCH
   - If GET succeeds: Continue to step 6
6. Modal dialog opens showing:
   - Summary: "X items will be updated"
   - Dropdown field: "Product Status" with list of status names
   - "Update" button
   - "Cancel" button
   - Modal can be closed by: clicking Cancel, pressing ESC, or clicking outside modal area
7. User selects new product status from dropdown and clicks "Update"
8. System displays progress bar showing update progress
9. System calls API PATCH method for each unique order (one by one, sequentially):
   - Each PATCH includes ALL line_items for that order (to prevent data loss)
   - Only selected line_items have updated status.id
   - If multiple rows belong to same order, only ONE PATCH call is made for that order
   - If PATCH succeeds (HTTP 200): Create log entry and continue to next order
   - If PATCH fails: Log error, continue processing remaining orders
10. After all updates complete, show summary modal:
    - "Successfully updated X of Y orders"
    - If failures exist: Show separate list of failed order IDs with error details
11. For each successful update (HTTP 200), create record in `order_update_logs` table with:
    - user_id (foreign key to users table)
    - order_id (sales order ID)
    - updated_at (timestamp of update)
    - old_data (TEXT: complete JSON of order before update)
    - new_data (TEXT: complete JSON of PATCH request body)
    - response_code (HTTP status code)

**Outputs:**
- Success: Summary showing "Successfully updated X of Y orders"
- Partial Success: Summary showing successful count + list of failed orders with error messages
- Failure (refresh): Error message "Failed to refresh order data. Please try again."
- Failure (validation): No action if no rows selected (existing behavior)

**Data Model - Log Table Schema:**
```sql
CREATE TABLE order_update_logs (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    order_id VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    old_data TEXT,
    new_data TEXT,
    response_code INTEGER,
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at)
);
```

**Business Rules:**
- Only authenticated users can update product status
- "Apply" button only enabled when rows are selected (existing behavior)
- Only checked rows are updated (respects user selection)
- Each order PATCH must include ALL line_items to prevent data loss
- Updates continue even if individual orders fail
- All updates are logged for audit trail

**UI Elements:**
- Bulk actions toolbar with "Select Action" dropdown (existing - add new option "Edit Order Products status")
- "Apply" button (existing)
- Modal dialog with:
  - Title: "Update Product Status"
  - Summary text: "X items will be updated"
  - Dropdown: "Product Status" (populated from status list below)
  - Progress bar (shown during updates)
  - "Update" button (primary action)
  - "Cancel" button (secondary action)
  - Close on: ESC key, click outside, Cancel button
- Summary modal after completion:
  - Success count
  - Failed orders list (if any) with Order IDs and error messages
  - "Close" button

**API Endpoint:**
- **Method:** PATCH
- **URL:** `/api/v2/orders/{order_id}` (example: `/api/v2/orders/42209911`)
- **Headers:** 
  - `Authorization: Basic <merchantpro_credentials>`
  - `Content-Type: application/json`
- **Request Body**: Must contain ALL line_items for the order. Only selected line_items have updated status.
```json
{
    "line_items": [
        {
            "item_type": "product",
            "product_id": 125011,
            "manufacturer_name": "N/A",
            "product_availability_id": null,
            "quantity": 1,
            "unit_price_net": 13749.1667,
            "unit_tax_amount": 2749.8333,
            "unit_price_gross": 16499,
            "line_subtotal_net": 13749.1667,
            "line_tax_amount": 2749.8333,
            "line_subtotal_gross": 16499,
            "weight": 0,
            "backorder_quantity": -1,
            "meta_fields": {
                "dobavljac": "Master Team",
                "komercijalista": "Darko Veličković"
            },
            "status": {
                "id": 9
            },
            "gift_card_delivery": null
        }
    ]
}
```
- **Response:** JSON with complete updated sales order data

**Product Status Values Configuration:**
Environment-aware config loading:
- First tries `backend/app/config/product_statuses_<ENVIRONMENT>.json` (for example: `product_statuses_qa.json`, `product_statuses_prod.json`)
- Falls back to `backend/app/config/product_statuses.json` if the environment-specific file does not exist

Example values:
```json
[
    {"id": 9, "name": "10 - Poslat upit", "color": "#FFFF00FF"},
    {"id": 12, "name": "20 - Dostupno", "color": "#00FFFFFF"},
    {"id": 15, "name": "30 - Poručeno za WEB", "color": "#000000FF"},
    {"id": 18, "name": "40 - Poručeno za VP", "color": "#0000FFFF"},
    {"id": 21, "name": "50 - Stiglo u VP", "color": "#38761DFF"},
    {"id": 24, "name": "60 - Nije stiglo", "color": "#FF9900FF"},
    {"id": 27, "name": "70 - Nema na stanju", "color": "#FF0000FF"}
]
```
API endpoint: `GET /api/config/product-statuses` returns this configuration

**Error Handling:**
- No rows selected: Existing validation prevents action (no change needed)
- GET refresh fails: Show error "Failed to refresh order data. Please try again." - Stop process
- PATCH fails for order: Continue with remaining orders, log failure, show in summary
- Network timeout: Show error "Request timed out" for that order, continue with others
- 400 Bad Request: Show "Invalid data for Order {order_id}", continue
- 401 Unauthorized: Show "Authentication failed", stop all updates
- 403 Forbidden: Show "Not authorized to update orders", stop all updates
- 429 Too Many Requests: Show "Rate limit exceeded, please try again later", stop updates
- 500 Internal Server Error: Log error, continue with remaining orders

**Performance Requirements:**
- Progress bar updates in real-time during sequential PATCH calls
- Each PATCH call timeout: 30 seconds
- Display summary modal within 1 second of completion
- Rate limiting enforced: 3 calls per second, 90 calls per minute (MerchantPro API limits)
- No limit on number of selected orders (rate limiting handles large batches automatically)

**Testing:**
- Unit test: PATCH request body includes all line_items
- Unit test: Log entry creation on successful update
- Unit test: Error handling continues to next order on failure
- Unit test: Rate limit compliance (3 calls/sec, 90 calls/min)
- Unit test: Unique order grouping (multiple rows → single PATCH)
- Integration test: GET refresh before PATCH
- Integration test: Sequential PATCH calls for multiple orders
- Integration test: Error responses from MerchantPro (400, 401, 403, 429, 500)
- Integration test: Database log insertion
- Frontend test: Modal dialog interactions (Cancel, ESC, outside click)
- Frontend test: Status dropdown population from config
- Frontend test: Progress bar updates
- Frontend test: Summary modal displays correct counts
- Frontend test: Error message display
- E2E test: Complete bulk update flow with mixed success/failure
- Edge cases: Order with missing line_item status, Network timeout, Token expired, Rate limit exceeded, All updates fail, All updates succeed

**Dependencies:**
- Backend: SQLAlchemy for log table, `httpx` for MerchantPro API calls
- Frontend: React modal component, progress bar component
- Database: PostgreSQL for order_update_logs table



## Template for New Functions

## Function - [Function Name]
**Purpose:** [One sentence description]

**User Story:** As a [role], I want to [action] so that [benefit].

**Inputs:**
- [Input 1]: [type, required/optional, format]
- [Input 2]: [type, required/optional, format]

**Process:**
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Outputs:**
- Success: [Description of successful output]
- Failure: [Description of error cases]

**Data Model:** [If applicable]

**Business Rules:**
- [Rule 1]
- [Rule 2]

**UI Elements:**
- [Element 1]
- [Element 2]

**API Endpoint:**
- **Method:** [GET/POST/PUT/DELETE]
- **URL:** [/api/endpoint]
- **Request/Response:** [Format]

**Error Handling:**
- [Error code]: [Description] → [Action]

**Performance Requirements:**
- [Requirement]

**Testing:**
- [Test type]: [Description]

**Dependencies:**
- [Library/Service]