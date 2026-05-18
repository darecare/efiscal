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
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| deleted_at     | TIMESTAMPTZ  | NULL                     | Soft delete                   |


### 2.4 role
table name: role

Represents reusable access profile definitions per client.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| role_id        | BIGINT       | PK, NOT NULL, IDENTITY(1000,1)      | Auto-generated integer        |
| role_code      | VARCHAR(100) | NOT NULL, UNIQUE         | Unique code                   |
| name           | VARCHAR(120) | NOT NULL                 | Display label                 |
| description    | VARCHAR(255) |                          |                               |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |

UNIQUE constraint: `(role_code)`


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


## 3. Entity Relationships

```
organizations
    ├── user_orgaccess (1:N)
    ├── platform_connections (1:N)
    ├── sales_orders (1:N)
    │     └── fiscalbill (1:N)
    │           ├── fiscalbilltax (1:N)
    │           ├── fiscalbillpay (1:N)
    │           └── fiscalbillline (1:N)
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
