# DATA MODEL

## 1. Overview
- Database: PostgreSQL 15
- Schema strategy: single schema per environment (`public`)
- Migration tool: [e.g., Flyway / Liquibase — choose one]
- Naming convention: `snake_case` for all table and column names
- All tables include: `id` (BIGINT PK, auto-generated identity starting at 1000, up to 10 digits), `created_at`, `updated_at`
- Soft delete pattern: `deleted_at` nullable timestamp (NULL = active)

---

## 2. Entities

### 2.1 users
Represents authenticated users of the eFiscal application.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| user_id             | BIGINT       | PK, NOT NULL, IDENTITY(1000,1) | Auto-generated integer        |
| client_id       | BIGINT       | FK → client.client_id, NOT NULL | User belongs to exactly one client |
| email          | VARCHAR(255) | UNIQUE, NOT NULL         |                               |
| password_hash  | VARCHAR(255) | NOT NULL                 | bcrypt hash, never plaintext  |
| role_id           | BIGINT       | FK → role.role_id, NOT NULL | Assigned role definition      |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| subscription_start_at | TIMESTAMPTZ | NULL                  | Required for normal users     |
| subscription_expires_at | TIMESTAMPTZ | NULL                | Normal user access expires at this timestamp |
| subscription_status | VARCHAR(30) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, EXPIRED, SUSPENDED |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| deleted_at     | TIMESTAMPTZ  | NULL                     | Soft delete                   |
| preferred_language | VARCHAR(10) | NULL                  | UI locale: `en`, `sr`, or NULL (browser default) |
| full_name      | VARCHAR(255) | NOT NULL                 | Display name                  |
| cashier        | VARCHAR(255) | NULL                     | Cashier name/code injected into the Tax Authority CREATE_INVOICE call when non-null |

Rules:
- Bootstrap SuperAdmin may have NULL subscription dates and is not blocked by expiration checks.
- Normal users must have valid subscription range and ACTIVE status to access the system.




---
### 2.2 Client
Table name: client

| Column     | Type    | Constraints                         | Notes                    |
|------------|---------|-------------------------------------|--------------------------|
| client_id  | BIGINT  | PK, NOT NULL, IDENTITY(1000,1)      | Auto-generated integer   |
| name       | VARCHAR(255) | NOT NULL                       |                          |
| status     | VARCHAR(50)  | NOT NULL, DEFAULT 'ACTIVE'     | ACTIVE, SETUP, SUSPENDED, INACTIVE |
| currency   | VARCHAR(10)  | NOT NULL, DEFAULT 'RSD'        |                          |
| is_active  | BOOLEAN | NOT NULL, DEFAULT TRUE              |                          |
| created_at | TIMESTAMPTZ | NOT NULL                       |                          |
| updated_at | TIMESTAMPTZ | NOT NULL                       |                          |
| deleted_at | TIMESTAMPTZ | NULL                           | Soft delete              |
| preferred_language | VARCHAR(10) | NULL                    | Default UI locale for new users under this client (`en`, `sr`, or NULL) |


### 2.3 organizations
Represents a merchant/client organization using eFiscal.
Table name: org
On Organization level is defined connection to mail server. From this mail address system will send mail notifications.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| org_id             | BIGINT       | PK, NOT NULL, IDENTITY(1000,1) | Auto-generated integer        |
| client_id      | BIGINT       | FK → client.client_id, NOT NULL |                               |
| name           | VARCHAR(255) | NOT NULL                 |                               |
| tax_id         | VARCHAR(50)  | NULL                     | PIB (Serbia tax identifier)   |
| status         | VARCHAR(50)  | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, SETUP, SUSPENDED, INACTIVE |
| currency       | VARCHAR(10)  | NOT NULL, DEFAULT 'RSD'  |                               |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| smtp_server    | VARCHAR(255) | NULL                     | SMTP host/server name         |
| smtp_port      | INTEGER      | NULL                     | SMTP port (`1..65535`)        |
| email_from     | VARCHAR(255) | NULL                     | Sender email displayed to recipients |
| smtp_username  | VARCHAR(255) | NULL                     | SMTP authentication username  |
| smtp_password  | VARCHAR(255) | NULL                     | SMTP authentication password (sensitive) |
| smtp_connection_security | VARCHAR(20) | NULL            | Allowed: `STARTTLS`, `SSL_TLS` |
| advertisement_html     | TEXT         | NULL                     | HTML content rendered on fiscal bill PDFs when advertisement is enabled |
| advertisement_enabled  | BOOLEAN      | NOT NULL, DEFAULT FALSE  | Toggle for rendering the advertisement block on PDFs |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| deleted_at     | TIMESTAMPTZ  | NULL                     | Soft delete                   |


### 2.4 role
table name: role

Represents reusable access profile definitions per client.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| role_id        | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      | Auto-generated integer        |
| client_id      | BIGINT       | FK → client.client_id, NULL        | Client scoping for custom role|
| role_code      | VARCHAR(100) | NOT NULL                 | Unique code per client scope  |
| name           | VARCHAR(120) | NOT NULL                 | Display label                 |
| description    | VARCHAR(255) |                          |                               |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |

UNIQUE constraint: `(coalesce(client_id, 0), role_code)`

Rules:
- Custom roles are hard-deleted.
- Deletion of a role is blocked if it is in use by active (non-soft-deleted) users, unless a valid replacement `reassignToRoleId` is provided to migrate users within compatible scope (same client or global, per role type).
- Built-in global roles (`SUPERADMIN`, `CLIENT_ADMIN`, `OPERATOR` with `client_id` NULL) are immutable and cannot be deleted.
- Other global custom roles (if any) are deletable only by SuperAdmin.
- Reassignment target role must be active; cannot equal the role being deleted.


### 2.5 User organization access
table name: user_orgaccess
description: list of organizations where user has access

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| user_id        | BIGINT       | FK → users.user_id, NOT NULL     |                           |
| org_id         | BIGINT       | FK → org.org_id, NOT NULL        |                           |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |

UNIQUE constraint: `(user_id, org_id)`

### 2.5A action_catalog
table name: action_catalog
description: list of assignable module actions (permission catalog).

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| action_id      | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      | Auto-generated integer        |
| module_code    | VARCHAR(80)  | NOT NULL                 | MERCHANTPRO, FISCAL, USERS    |
| action_code    | VARCHAR(120) | NOT NULL                 | MERCHANTPRO_FETCH_ORDERS, FISCAL_CREATE_BILL |
| name           | VARCHAR(120) | NOT NULL                 | Human-readable label          |
| description    | VARCHAR(255) |                          |                               |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |

UNIQUE constraint: `(module_code, action_code)`

### 2.5B role_action_access
table name: role_action_access
description: mapping between roles and allowed actions.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| role_id        | BIGINT       | FK → role.role_id, NOT NULL      |                             |
| action_id      | BIGINT       | FK → action_catalog.action_id, NOT NULL |                 |
| is_allowed     | BOOLEAN      | NOT NULL, DEFAULT TRUE   | Future-proofing for deny model |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |

UNIQUE constraint: `(role_id, action_id)`

### 2.6 platform_connections


### 2.6 platform_connections
table name: apiconn
Stores connection config for each external shopping platform (MerchantPro, WooCommerce, Shopify, etc.) per organization.


| Column           | Type         | Constraints              | Notes                                      |
|------------------|--------------|--------------------------|--------------------------------------------|
| apiconn_id       | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      | Auto-generated integer     |
| client_id        | BIGINT       | FK → client.client_id, NOT NULL     |                                            |
| org_id           | BIGINT       | FK → org.org_id, NOT NULL           |                                            |
| api_platform     | VARCHAR(10)  | NOT NULL                 | MP=MerchantPro, WO=WooCommerce, SH=Shopify, FS=Fiscal System |
| display_name     | VARCHAR(255) |                          | User-facing label                          |
| api_base_url     | VARCHAR(500) |                          | Platform-specific endpoint                 |
| apiauthtype      | VARCHAR(50)  | NULL                     | BASIC_AUTH, OAUTH, MTLS, NONE              |
| apikey           | VARCHAR(255) | NULL                     |                                            |
| apisecret        | VARCHAR(255) | NULL                     |                                            |
| cert_data        | BYTEA        | NULL                     | PEM/PKCS12 certificate bytes (mTLS)        |
| cert_password    | VARCHAR(255) | NULL                     | Encrypted keystore/cert password (mTLS)    |
| pac              | VARCHAR(10)  | NULL                     | Platform access code                       |
| is_active        | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                            |
| created_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |
| updated_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |
| deleted_at       | TIMESTAMPTZ  | NULL                     | Soft delete                                |

> **Security**: API keys and credentials must NEVER be stored in plain text. Store a reference to a secret manager or encrypted blob.

### 2.7 API Templates
table name: apitemplate
description: define template for api calls such as POST, GET, PATCH ...

Normalized target model (for new implementation):


| Column           | Type         | Constraints              | Notes                                      |
|------------------|--------------|--------------------------|--------------------------------------------|
| apitemplate_id   | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                            |
| apiconn_id       | BIGINT       | FK → apiconn.apiconn_id, NOT NULL   |                                            |
| operation_key    | VARCHAR(120) | NOT NULL                 | FETCH_ORDERS, CREATE_FISCAL_BILL, etc.     |
| http_method      | VARCHAR(16)  | NOT NULL                 | GET, POST, PATCH                            |
| content_type     | VARCHAR(100) | NOT NULL                 | application/json, etc.                      |
| endpoint_path    | VARCHAR(500) | NOT NULL                 | Relative endpoint path                       |
| is_active        | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                            |
| created_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |
| updated_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |

UNIQUE constraint: `(apiconn_id, operation_key)`


### 2.7A API Template Parameters
table name: apitemplate_param
description: defines dynamic query/path/body parameter mapping for each template operation.

| Column             | Type         | Constraints              | Notes                                      |
|--------------------|--------------|--------------------------|--------------------------------------------|
| apitemplate_param_id | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                            |
| apitemplate_id     | BIGINT       | FK → apitemplate.apitemplate_id, NOT NULL |                                  |
| param_key          | VARCHAR(120) | NOT NULL                 | Internal key (created_after, shipping_status) |
| provider_param_name| VARCHAR(120) | NOT NULL                 | External query key name                     |
| location           | VARCHAR(20)  | NOT NULL                 | QUERY, PATH, BODY                           |
| data_type          | VARCHAR(20)  | NOT NULL                 | STRING, DATE, NUMBER, BOOLEAN               |
| is_required        | BOOLEAN      | NOT NULL, DEFAULT FALSE  |                                            |
| default_value      | VARCHAR(255) |                          | Optional default                            |
| is_active          | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                            |
| created_at         | TIMESTAMPTZ  | NOT NULL                 |                                            |
| updated_at         | TIMESTAMPTZ  | NOT NULL                 |                                            |

UNIQUE constraint: `(apitemplate_id, param_key)`

MVP required parameters for MerchantPro order fetch template:
- created_after (DATE, QUERY, required)
- shipping_status (STRING, QUERY, required)

### 2.8 sales_orders
Sales order imported/fetched from an external shopping platform.

| Column              | Type         | Constraints              | Notes                              |
|---------------------|--------------|--------------------------|------------------------------------|
| order_id                  | UUID         | PK, NOT NULL             |                                    |
| org_id     | UUID         | FK → organizations.id    |                                    |
| client_id | UUID | FF -> client.client_id|
| platform_connection_id | UUID      | FK → platform_connections.id |                               |
| external_order_id   | VARCHAR(255) | NOT NULL                 | ID from external platform          |
| external_order_ref  | VARCHAR(255) |                          | Human-readable order number        |
| status              | VARCHAR(50)  | NOT NULL                 | NEW, PROCESSING, FISCALIZED, ERROR |
| order_data          | JSONB        |                          | Raw payload from platform          |
| order_total         | NUMERIC(12,2)| NOT NULL                 |                                    |
| currency            | VARCHAR(10)  | NOT NULL, DEFAULT 'RSD'  |                                    |
| ordered_at          | TIMESTAMPTZ  |                          | Timestamp from external platform   |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                    |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                    |

UNIQUE constraint: `(platform_connection_id, external_order_id)`

---

### 2.9 fiscal_bill
A fiscalization request submitted to Serbian Tax Authority API, linked to a sales order.

| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| fiscalbill_id       | VARCHAR(64)  | PK, NOT NULL             | Internal fiscal bill identifier          |
| order_id            | VARCHAR(64)  | NOT NULL                 | Source order identifier                  |
| status              | VARCHAR(50)  | NOT NULL                 | PENDING, SUCCESS, FAILED, RETRYING       |
| provider_reference  | VARCHAR(128) | NULL                     | Provider response reference              |
| last_error          | VARCHAR(512) | NULL                     | Last error                               |
| attempt_count       | INTEGER      | NOT NULL, DEFAULT 0      |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| client_id           | NUMERIC(10,0)| NOT NULL                 | Legacy compatibility field               |
| org_id              | NUMERIC(10,0)| NOT NULL                 | Legacy compatibility field               |
| created             | TIMESTAMP    | NOT NULL                 | Legacy compatibility field               |
| createdby           | NUMERIC(10,0)| NOT NULL                 | Legacy compatibility field               |
| efiscal_address     | VARCHAR(50)  | NULL                     |                                          |
| efiscal_businessname| VARCHAR(100) | NULL                     |                                          |
| efiscal_code        | VARCHAR(1)   | NULL                     |                                          |
| efiscal_encryptedinternaldata | TEXT | NULL                  |                                          |
| efiscal_invoicecounter | VARCHAR(22) | NULL                   |                                          |
| efiscal_invoicecounterext | VARCHAR(22) | NULL                |                                          |
| efiscal_link        | VARCHAR(2000)| NULL                     |                                          |
| efiscal_messages    | VARCHAR(22)  | NULL                     |                                          |
| efiscal_mrc         | VARCHAR(22)  | NULL                     |                                          |
| efiscal_name        | VARCHAR(50)  | NULL                     |                                          |
| efiscal_qr          | TEXT         | NULL                     |                                          |
| efiscal_requestedby | VARCHAR(50)  | NULL                     |                                          |
| efiscal_sdcdatetime | VARCHAR(50)  | NULL                     |                                          |
| efiscal_sdc_invoiceno | VARCHAR(30)| NULL                     |                                          |
| efiscal_signature   | TEXT         | NULL                     |                                          |
| efiscal_signedby    | VARCHAR(22)  | NULL                     |                                          |
| efiscal_taxgrouprevision | NUMERIC(10,0) | NULL              |                                          |
| efiscal_tin         | VARCHAR(22)  | NULL                     |                                          |
| efiscal_totalamount | NUMERIC      | NULL                     |                                          |
| efiscal_totalcounter| NUMERIC      | NULL                     |                                          |
| efiscal_transactiontypecounter | NUMERIC(10,0) | NULL        |                                          |
| efiscal_type        | VARCHAR(2)   | NULL                     |                                          |
| fiscalbill_uu       | VARCHAR(36)  | NULL                     |                                          |
| isactive            | CHAR(1)      | NOT NULL, DEFAULT 'Y'    |                                          |
| processed           | CHAR(1)      | NOT NULL, DEFAULT 'N'    |                                          |
| processedon         | NUMERIC      | NULL                     |                                          |
| updated             | TIMESTAMP    | NOT NULL                 | Legacy compatibility field               |
| updatedby           | NUMERIC(10,0)| NOT NULL                 | Legacy compatibility field               |
| value               | VARCHAR(40)  | NULL                     |                                          |
| efiscal_invoicetype | NUMERIC(10,0)| NULL                     |                                          |
| efiscal_transactiontype | NUMERIC(10,0) | NULL                |                                          |
| efiscal_customername| VARCHAR(100) | NULL                     |                                          |
| efiscal_orderid     | VARCHAR(22)  | NULL                     |                                          |

---


### 2.9A fiscalbill_idempotency_keys

### 2.9A fiscalbill_idempotency_keys
Maps request idempotency keys to fiscal bills for deduplication.

CREATE TABLE IF NOT EXISTS fiscalbill_idempotency_keys
(
    idempotency_key character varying(128) PRIMARY KEY,
    fiscalbill_id character varying(64) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT fk_fiscalbill_idempotency_bill
        FOREIGN KEY (fiscalbill_id)
        REFERENCES fiscalbill (fiscalbill_id)
)

### 2.10 fiscalbilltax


| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| fiscalbilltax_id    | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                          |
| fiscalbill_id       | VARCHAR(64)  | FK → fiscalbill.fiscalbill_id, NOT NULL |                                    |
| amount              | NUMERIC      | NOT NULL                 |                                          |
| rate                | NUMERIC      | NOT NULL                 |                                          |
| efiscal_categoryname| VARCHAR(60)  |                          |                                          |
| efiscal_taxlabel    | VARCHAR(1)   |                          |                                          |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

---

### 2.11 fiscal config per org

| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| fiscalbillconfig_id | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                          |
| org_id              | BIGINT       | FK → org.org_id, NOT NULL           |                                          |
| esirno              | VARCHAR(22)  |                          |                                          |
| is_test             | BOOLEAN      | NOT NULL, DEFAULT FALSE  |                                          |
| email_from          | VARCHAR(60)  |                          |                                          |
| email_bcc           | VARCHAR(60)  |                          |                                          |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

### 2.12 Tax
Table name: tax
List of tax code and applied rates for each tax

| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| tax_id              | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                          |
| name                | VARCHAR(60)  | NOT NULL                 |                                          |
| rate                | NUMERIC      | NOT NULL                 |                                          |
| taxcategory_id      | BIGINT       | FK → taxcategory.taxcategory_id      |                                          |
| efiscal_taxlabel    | VARCHAR(1)   |                          |                                          |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

Notes:
- Advance fiscal bill line name is resolved from tax table fields:
    efiscal_advanceprefix + efiscal_advancename
- Values should be configured on active tax rows used in fiscal mapping.

### 2.13 Tax Category
Table name: taxcategory
List of tax categories
taxcategory_id numeric(10,0) NOT NULL,
Taxcategory_code character(2)
name character varying(60) COLLATE pg_catalog."default" NOT NULL,
efiscal_categorytype numeric(10,0)

### 2.14 fiscalbillpay
Table to store payment rows per fiscal bill.


| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| fiscalbillpay_id    | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                          |
| fiscalbill_id       | VARCHAR(64)  | FK → fiscalbill.fiscalbill_id, NOT NULL |                                    |
| payment_type        | INTEGER      | NOT NULL                 | 0-Other, 1-Cash, 2-Card, etc.            |
| amount              | NUMERIC      | NOT NULL                 |                                          |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

CREATE INDEX IF NOT EXISTS idx_fiscalbillpay_bill ON fiscalbillpay (fiscalbill_id);

### 2.15 paytype_map
Payment type mapping table. Maps external payment_method_code values to Tax Authority payment type integers per client.


| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| paytype_map_id      | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                          |
| client_id           | BIGINT       | FK → client.client_id, NOT NULL     |                                          |
| payment_method_code | VARCHAR(50)  | NOT NULL                 | External code                             |
| payment_type        | INTEGER      | NOT NULL                 | Internal code                             |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

### 2.16 fiscalbillline
Table to store line items for fiscal bill when creating manual fiscal bill.
When fiscal bill is created based on sales order, then this table will be store line items after fiscal bill is issued from Tax authority based on json api request sent to tax authority.


| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| fiscalbillline_id   | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                          |
| fiscalbill_id       | VARCHAR(64)  | FK → fiscalbill.fiscalbill_id, NOT NULL |                                    |
| name                | VARCHAR(2048)| NOT NULL                 |                                          |
| quantity            | NUMERIC(14,3)| NOT NULL                 |                                          |
| unit_price          | NUMERIC(14,2)| NOT NULL                 |                                          |
| total_amount        | NUMERIC(14,2)| NOT NULL                 |                                          |
| tax_label           | VARCHAR(10)  |                          |                                          |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

CREATE INDEX IF NOT EXISTS idx_fiscalbillline_bill ON fiscalbillline (fiscalbill_id);

### 2.17 fiscalbillconfig
Organization-level fiscal configuration used during fiscal bill creation.


| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| fiscalbillconfig_id | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      |                                          |
| org_id              | BIGINT       | FK → org.org_id, NOT NULL           |                                          |
| esirno              | VARCHAR(22)  |                          |                                          |
| is_test             | BOOLEAN      | NOT NULL, DEFAULT FALSE  |                                          |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                          |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

CREATE UNIQUE INDEX IF NOT EXISTS idx_fiscalbillconfig_org ON fiscalbillconfig (org_id) WHERE isactive = 'Y';

### 2.18 product
Table name: product
Organization-scoped product catalog for fiscal bill line item lookup. Populated manually or via MerchantPro sync (`GET_PRODUCTS`).

| Column              | Type         | Constraints              | Notes                                    |
|---------------------|--------------|--------------------------|------------------------------------------|
| product_id          | BIGINT       | PK, NOT NULL, IDENTITY(1000,1) | Auto-generated integer        |
| client_id           | BIGINT       | FK → client.client_id, NOT NULL | Client scope                  |
| org_id              | BIGINT       | FK → org.org_id, NOT NULL           | Organization scope          |
| mp_product_id       | BIGINT       | NULL                     | MerchantPro product id (sync key) |
| name                | VARCHAR(500) | NOT NULL                 | Product name                  |
| sku                 | VARCHAR(255) | NULL                     | Optional SKU                  |
| ean                 | VARCHAR(100) | NULL                     | Optional EAN/barcode          |
| last_known_price    | NUMERIC(14,2)| NULL                     | Informational; live price verified at selection |
| is_active           | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                             |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                             |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                             |
| deleted_at          | TIMESTAMPTZ  | NULL                     | True delete for `MANUAL` products |
| source_type         | VARCHAR(16)  | NOT NULL, DEFAULT `MANUAL` | `MANUAL` or `MERCHANTPRO` |
| sync_status         | VARCHAR(20)  | NOT NULL, DEFAULT `ACTIVE` | `ACTIVE` or `MISSING_IN_SOURCE` (synced rows only) |
| hidden_at           | TIMESTAMPTZ  | NULL                     | Local hide/archive for `MERCHANTPRO` products |

Rules:
- At least one of `sku` or `ean` is required for manual create and for live shop price lookup.
- `last_known_price` is not authoritative for fiscal bills; use live lookup at line-item selection.
- Visible catalog rows: `deleted_at IS NULL AND hidden_at IS NULL`.
- Local delete: `MANUAL` → set `deleted_at`; `MERCHANTPRO` → set `hidden_at` (restorable via `RESET_FULL` sync).
- Catalog list/search (API `q` param on Products screen): matches visible rows across name, SKU, EAN, IDs, and price fields.
- Fiscal bill autocomplete (`GET /products/search`): matches visible, active rows where `name` contains the term OR `sku`/`ean` equals the term (case-insensitive).
- Sync upsert match order: `mp_product_id` → `sku` (case-insensitive, `MERCHANTPRO` only) → `ean` (`MERCHANTPRO` only); matches hidden rows; `RESET_FULL` clears `hidden_at`. If a `MANUAL` product already holds the same SKU or EAN, the shop row is skipped (no duplicate insert).

Migrations:
- `V31__create_product_table.sql` — creates `product` table and indexes
- `V32__seed_fiscal_products_action.sql` — seeds `FISCAL_MANAGE_PRODUCTS` action and grants for built-in admin roles
- `V33__drop_org_searchshopproducts.sql` — removes deprecated `org.is_searchshopproducts` (product search is no longer gated by org flag; use RBAC actions instead)
- `V34__add_product_sku_ean_indexes.sql` — SKU/EAN lookup indexes
- `V35__widen_mp_product_id_to_bigint.sql` — `mp_product_id` BIGINT
- `V36__create_product_sync_job.sql` — sync job tracking table
- `V37__add_product_source_ownership.sql` — `source_type`, `sync_status`, `hidden_at`; backfill synced rows; visibility indexes

### 2.19 product_sync_job
Table name: product_sync_job
Persistent MerchantPro product sync runs per organization (Option B). Progress survives page refresh; enforces one active sync per org. Stale `RUNNING` jobs (>2h) are auto-failed on status poll or sync start. User cancel marks job `FAILED` with `Cancelled by user`.

| Column         | Type         | Constraints              | Notes                                      |
|----------------|--------------|--------------------------|--------------------------------------------|
| sync_job_id    | BIGINT       | PK, IDENTITY             | Job id                                     |
| org_id         | BIGINT       | FK → org.org_id, NOT NULL| Organization scope                         |
| status         | VARCHAR(16)  | NOT NULL                 | `RUNNING`, `DONE`, `FAILED`                |
| sync_type      | VARCHAR(16)  | NOT NULL                 | `FULL`, `INCREMENTAL`, `RESET_FULL`        |
| synced         | INT          | NOT NULL, DEFAULT 0      | Products upserted so far                   |
| total          | INT          | NOT NULL, DEFAULT 0      | Reported catalog total from MP meta        |
| error_message  | TEXT         | NULL                     | Set on `FAILED`                            |
| filter_from    | TIMESTAMPTZ  | NULL                     | MP date filter lower bound; NULL for FULL  |
| started_at     | TIMESTAMPTZ  | NOT NULL                 | Job start                                  |
| finished_at    | TIMESTAMPTZ  | NULL                     | Job end                                    |

Indexes:
- `(org_id, started_at DESC)` — latest job lookup
- `(org_id) WHERE status = 'RUNNING'` — UNIQUE partial; one running job per org

### 2.20 email_template
Table name: email_template
Organization-scoped HTML email templates for fiscal bill delivery. Bodies store raw HTML with placeholder tokens (for example `{{ customername }}`); rendered at send time.

| Column            | Type         | Constraints              | Notes                                    |
|-------------------|--------------|--------------------------|------------------------------------------|
| email_template_id | BIGINT       | PK, NOT NULL, IDENTITY   | Auto-generated integer                   |
| org_id            | BIGINT       | FK → org.org_id, NOT NULL| Organization scope                       |
| template_name     | VARCHAR(120) | NOT NULL                 | Display name in admin UI                 |
| subject           | VARCHAR(255) | NOT NULL                 | Email subject line                       |
| body_html         | TEXT         | NOT NULL                 | Raw HTML template body                   |
| is_active         | BOOLEAN      | NOT NULL, DEFAULT TRUE   | Active templates are eligible for send   |
| created_at        | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at        | TIMESTAMPTZ  | NOT NULL                 |                                          |
| deleted_at        | TIMESTAMPTZ  | NULL                     | Soft delete                              |

Rules:
- List/detail APIs return only rows with `deleted_at IS NULL`.
- Send flow selects the most recently updated active template per org.

Migration:
- `V38__create_email_template_table.sql` — creates `email_template` table and org index

### 2.21 log_email
Table name: log_email
Audit log of fiscal bill email delivery attempts. Stores rendered subject/body snapshot and delivery status; not soft-deleted.

| Column          | Type         | Constraints                        | Notes                                      |
|-----------------|--------------|------------------------------------|--------------------------------------------|
| log_email_id    | BIGINT       | PK, NOT NULL, IDENTITY             | Auto-generated integer                     |
| org_id          | BIGINT       | FK → org.org_id, NOT NULL          | Organization scope                         |
| fiscalbill_id   | BIGINT       | FK → fiscalbill.fiscalbill_id, NULL| Linked bill when available                 |
| order_id        | VARCHAR(64)  | NULL                               | Source order reference                     |
| recipient_email | VARCHAR(255) | NULL                               | Customer recipient                         |
| template_name   | VARCHAR(120) | NULL                               | Template name snapshot at send time        |
| subject         | VARCHAR(255) | NULL                               | Rendered subject snapshot                  |
| body_html       | TEXT         | NULL                               | Rendered HTML body snapshot                |
| status          | VARCHAR(20)  | NOT NULL                           | `SENT`, `FAILED`, or `SKIPPED`             |
| error_message   | VARCHAR(1000)| NULL                               | Failure detail when `status = FAILED`      |
| sent_at         | TIMESTAMPTZ  | NULL                               | Set when `status = SENT`                   |
| created_at      | TIMESTAMPTZ  | NOT NULL                           |                                            |
| updated_at      | TIMESTAMPTZ  | NOT NULL                           |                                            |

Migration:
- `V39__create_log_email_table.sql` — creates `log_email` table and org/bill indexes

## 3. Entity Relationships

```
organizations
    ├── user_orgaccess (1:N)
    ├── product (1:N)
    ├── product_sync_job (1:N)
    ├── email_template (1:N)
    ├── log_email (1:N)
    ├── platform_connections (1:N)
    ├── sales_orders (1:N)
    │     └── fiscalbill (1:N)
    │           ├── fiscalbilltax (1:N)
    │           ├── fiscalbillpay (1:N)
    │           ├── fiscalbillline (1:N)
    │           └── log_email (0:N)
    └── fiscalbillconfig (1:N, active filtered by isactive='Y')

platform_connections → sales_orders (1:N)
users → fiscal_document_audit_log (0:N, optional)
client → users (1:N)
client → roles (1:N)
client → paytype_map (1:N)
users → user_orgaccess (1:N)
roles → role_action_access (1:N)
action_catalog → role_action_access (1:N)
```

---

## 4. Indexes (baseline)

| Table                      | Index                                          | Purpose                         |
|----------------------------|------------------------------------------------|---------------------------------|
| users                      | email                                          | Login lookup                    |
| users                      | (client_id, role_id)                           | Scoped access and role joins    |
| users                      | (subscription_status, subscription_expires_at) | Subscription validity checks    |
| user_orgaccess             | (user_id, org_id)                              | User organization scope lookup  |
| role                       | (client_id, role_code)                         | Role uniqueness per client      |
| action_catalog             | (module_code, action_code)                     | Permission catalog lookup       |
| role_action_access         | (role_id, action_id)                           | Effective permission lookup     |
| sales_orders               | (organization_id, status)                      | Filtered list views             |
| sales_orders               | (platform_connection_id, external_order_id)    | Deduplication on import         |
| fiscal_documents           | (organization_id, status)                      | Status dashboards               |
| fiscal_documents           | idempotency_key                                | Idempotent submission check     |
| fiscal_document_audit_log  | fiscal_document_id                             | Audit trail retrieval           |
| product                    | (org_id) WHERE deleted_at IS NULL              | Product list/search by org      |
| product                    | (org_id, mp_product_id) UNIQUE partial         | Sync upsert key                 |
| product_sync_job           | (org_id, started_at DESC)                        | Latest job per org              |
| product_sync_job           | (org_id) WHERE status = 'RUNNING' UNIQUE       | One active sync per org         |
| email_template             | (org_id)                                         | Template list by org            |
| log_email                  | (org_id)                                         | Delivery log by org             |
| log_email                  | (fiscalbill_id)                                  | Delivery log by bill            |

---

## 5. Migration Strategy
- Tool: [Flyway / Liquibase — decide before first schema creation]
- Location: `src/main/resources/db/migration/`
- Naming: `V<version>__<description>.sql` (Flyway) or equivalent
- Rule: migrations are append-only; never edit an applied migration
- Baseline: V1 creates all tables from this document
- Bootstrap seed: initial migration (or paired seed migration) must create exactly one global SuperAdmin account with full client/organization scope and role-action access to all actions.

---

## 6. Data Retention and Compliance
- `sales_orders` and `fiscal_documents`: retain indefinitely (required for tax compliance)
- `fiscal_document_audit_log`: retain indefinitely, immutable
- `users`: soft-delete only; hard delete requires explicit compliance review
- Sensitive fields (credentials, passwords): must not appear in logs or exports

---

## 7. Open Questions
- DM-001: Does Tax Authority require storing fiscal document in a specific format for re-print? — Owner: [name] — Due: [date]
- DM-002: Multi-currency support needed or RSD only for MVP? — Owner: [name] — Due: [date]
- DM-003: Should `sales_orders.order_data` JSONB be normalized further or kept as raw payload? — Owner: [name] — Due: [date]
- DM-004: Confirm secret management approach for `platform_connections.credentials_ref` — Owner: [name] — Due: [date]
