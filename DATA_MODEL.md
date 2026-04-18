# DATA MODEL

## 1. Overview
- Database: PostgreSQL 15
- Schema strategy: single schema per environment (`public`)
- Migration tool: [e.g., Flyway / Liquibase — choose one]
- Naming convention: `snake_case` for all table and column names
- All tables include: `id` (UUID PK), `created_at`, `updated_at`
- Soft delete pattern: `deleted_at` nullable timestamp (NULL = active)

---

## 2. Entities

### 2.1 users
Represents authenticated users of the eFiscal application.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| user_id             | UUID         | PK, NOT NULL             | Generated UUID                |
| email          | VARCHAR(255) | UNIQUE, NOT NULL         |                               |
| password_hash  | VARCHAR(255) | NOT NULL                 | bcrypt hash, never plaintext  |
| role_id           | VARCHAR(50)  | NOT NULL                 | SUPER_ADMIN, USER, AUDITOR    |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| deleted_at     | TIMESTAMPTZ  | NULL                     | Soft delete                   |




---
### 2.2 Client
Table name: client
CREATE TABLE IF NOT EXISTS client
(
    client_id numeric(10,0) NOT NULL,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    created timestamp without time zone NOT NULL DEFAULT now(),
    createdby numeric(10,0) NOT NULL,
    updated timestamp without time zone NOT NULL DEFAULT now(),
    updatedby numeric(10,0) NOT NULL,
    value character varying(40) COLLATE pg_catalog."default" NOT NULL,
    name character varying(60) COLLATE pg_catalog."default" NOT NULL,
    description character varying(255) COLLATE pg_catalog."default",
)


### 2.3 organizations
Represents a merchant/client organization using eFiscal.
Table name: org
On Organization level is defined connection to mail server. From this mail address system will send mail notifications.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| org_id             | UUID         | PK, NOT NULL             |                               |
| name           | VARCHAR(255) | NOT NULL                 |                               |
| tax_id         | VARCHAR(50)  | UNIQUE, NOT NULL         | PIB (Serbia tax identifier)   |
| isactive      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated     | TIMESTAMPTZ  | NOT NULL                 |                               |
| deleted     | TIMESTAMPTZ  | NULL                     | Soft delete                   |

CREATE TABLE IF NOT EXISTS org
(
    org_id numeric(10,0) NOT NULL,
    client_id numeric(10,0) NOT NULL,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    created timestamp without time zone NOT NULL DEFAULT now(),
    createdby numeric(10,0) NOT NULL,
    updated timestamp without time zone NOT NULL DEFAULT now(),
    updatedby numeric(10,0) NOT NULL,
    value character varying(40) COLLATE pg_catalog."default" NOT NULL,
    name character varying(60) COLLATE pg_catalog."default" NOT NULL,
    description character varying(255) COLLATE pg_catalog."default",
    tax_id,
    registration_id  , //maticni broj firme
    city ,
    address ,
    postal_code ,
    smtp_out_server character varying(255),
    smtp_username character varying(255),
    smtp_port character varying(255),
    smtp_password character varying(255)      
    smtp_sender_address character varying(255)  //From address that will be visible by receiver
    smtp_secure_conn character varying(255) //will have options none, TLS, SSL

)

### 2.4 role
table name: role

CREATE TABLE IF NOT EXISTS role
(
    role_id numeric(10,0) NOT NULL,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    created timestamp without time zone NOT NULL DEFAULT now(),
    createdby numeric(10,0) NOT NULL,
    updated timestamp without time zone NOT NULL DEFAULT now(),
    updatedby numeric(10,0) NOT NULL,
    value character varying(40) COLLATE pg_catalog."default" NOT NULL,
    name character varying(60) COLLATE pg_catalog."default" NOT NULL,
    description character varying(255) COLLATE pg_catalog."default",

)

### 2.5 User organization access
table name: user_orgaccess
description: list of organizations where user has access
CREATE TABLE IF NOT EXISTS user_orgaccess
(
    user_id numeric(10,0) NOT NULL,
    org_id numeric(10,0) NOT NULL,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    created timestamp without time zone NOT NULL DEFAULT now(),
    createdby numeric(10,0) NOT NULL,
    updated timestamp without time zone NOT NULL DEFAULT now(),
    updatedby numeric(10,0) NOT NULL,
    value character varying(40) COLLATE pg_catalog."default" NOT NULL,
)


### 2.6 platform_connections
table name: apiconn
Stores connection config for each external shopping platform (MerchantPro, WooCommerce, Shopify, etc.) per organization.

| Column           | Type         | Constraints              | Notes                                      |
|------------------|--------------|--------------------------|--------------------------------------------|
| apiconn_id               | UUID         | PK, NOT NULL             |                                            |
| org_id  | UUID         | FK → organizations.id    |                                            |
| api_platform         | VARCHAR(50)  | NOT NULL                 | MERCHANTPRO, WOOCOMMERCE, SHOPIFY, etc.    |
| display_name     | VARCHAR(255) |                          | User-facing label                          |
| api_base_url     | VARCHAR(500) |                          | Platform-specific endpoint                 |
| isactive        | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                            |
| created_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |
| updated_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |
| deleted_at       | TIMESTAMPTZ  | NULL                     | Soft delete                                |

> **Security**: API keys and credentials must NEVER be stored in plain text. Store a reference to a secret manager (e.g., env variable name, Vault path, or encrypted blob).

---

CREATE TABLE IF NOT EXISTS apiconn
(
    client_id numeric(10,0) NOT NULL,
    org_id numeric(10,0) NOT NULL,
    created timestamp without time zone NOT NULL,
    createdby numeric(10,0) NOT NULL,
    apiauthtype character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying, //type of authorization, will be a dropdown list such as Basic Auth, Oauth, mTLS...
    apiconn_id numeric(10,0) NOT NULL,
    apiconn_uu character varying(36) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    apikey character varying(50) COLLATE pg_catalog."default" DEFAULT NULL::character varying, //in case of basic auth will hold api key
    apisecret character varying(50) COLLATE pg_catalog."default" DEFAULT NULL::character varying, // in case of basic auth will hold api secret
    api_platform character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying, // will be dropdown list on frontend MP - MerchantPro, EF - EFiscal, WO - Woocommerce ...
    api_base_url character varying(50) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    updated timestamp without time zone NOT NULL,
    updatedby numeric(10,0) NOT NULL,
)

### 2.7 API Templates
table name: apitemplate
description: define template for api calls such as POST, GET, PATCH ...

CREATE TABLE IF NOT EXISTS adempiere.elf_apitemplate
(
    client_id numeric(10,0) NOT NULL,
    org_id numeric(10,0) NOT NULL,
    created timestamp without time zone NOT NULL,
    createdby numeric(10,0) NOT NULL,
    apiconn_id numeric(10,0) NOT NULL DEFAULT NULL::numeric,
    apicontenttype character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying, // can be "application/json". "text/xml" ...
    apirequesttype character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying, // can be "PATCH", "POST", "GET" ..
    apitemplate_id numeric(10,0) NOT NULL,
    apitemplate_uu character varying(36) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    name character varying(60) COLLATE pg_catalog."default" NOT NULL,
    updated timestamp without time zone NOT NULL,
    updatedby numeric(10,0) NOT NULL,
    value character varying(40) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    endpoint character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
)

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
| id                  | UUID         | PK, NOT NULL             |                                          |
| organization_id     | UUID         | FK → organizations.id    |                                          |
| sales_order_id      | UUID         | FK → sales_orders.id     |                                          |
| idempotency_key     | UUID         | UNIQUE, NOT NULL         | Prevents duplicate submissions           |
| status              | VARCHAR(50)  | NOT NULL                 | PENDING, SUCCESS, FAILED, RETRYING       |
| provider_reference  | VARCHAR(255) |                          | Tax authority document ID on success     |
| request_payload     | JSONB        |                          | Payload sent to Tax Authority            |
| response_payload    | JSONB        |                          | Raw response from Tax Authority          |
| last_error          | TEXT         |                          | Last failure message                     |
| attempt_count       | INTEGER      | NOT NULL, DEFAULT 0      |                                          |
| fiscalized_at       | TIMESTAMPTZ  |                          | Timestamp of successful fiscalization    |
| created_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |
| updated_at          | TIMESTAMPTZ  | NOT NULL                 |                                          |

---

CREATE TABLE IF NOT EXISTS fiscalbill
(
    client_id numeric(10,0) NOT NULL,
    org_id numeric(10,0) NOT NULL,
    order_id numeric(10,0) DEFAULT NULL::numeric,
    created timestamp without time zone NOT NULL,
    createdby numeric(10,0) NOT NULL,
    efiscal_address character varying(50) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_businessname character varying(100) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_code character varying(1) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_encryptedinternaldata text COLLATE pg_catalog."default",
    efiscal_invoicecounter character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_invoicecounterext character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_link character varying(2000) COLLATE pg_catalog."default",
    efiscal_messages character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_mrc character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_name character varying(50) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_qr text COLLATE pg_catalog."default",
    efiscal_requestedby character varying(50) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_sdcdatetime character varying(50) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_sdc_invoiceno character varying(30) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_signature text COLLATE pg_catalog."default",
    efiscal_signedby character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_taxgrouprevision numeric(10,0) DEFAULT NULL::numeric,
    efiscal_tin character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_totalamount numeric,
    efiscal_totalcounter numeric,
    efiscal_transactiontypecounter numeric(10,0) DEFAULT NULL::numeric,
    efiscal_type character varying(2) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    fiscalbill_id numeric(10,0) NOT NULL,
    fiscalbill_uu character varying(36) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    processed character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'N'::bpchar,
    processedon numeric,
    updated timestamp without time zone NOT NULL,
    updatedby numeric(10,0) NOT NULL,
    value character varying(40) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_invoicetype numeric(10,0) DEFAULT NULL::numeric,
    efiscal_transactiontype numeric(10,0) DEFAULT NULL::numeric,
    efiscal_customername character varying(100) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_orderno character varying(22) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
   
)

### 2.10 fiscal_tax
Immutable audit trail for every status change on a fiscal document.

| Column              | Type         | Constraints              | Notes                              |
|---------------------|--------------|--------------------------|------------------------------------|
| id                  | UUID         | PK, NOT NULL             |                                    |
| fiscal_document_id  | UUID         | FK → fiscal_documents.id |                                    |
| previous_status     | VARCHAR(50)  |                          |                                    |
| new_status          | VARCHAR(50)  | NOT NULL                 |                                    |
| triggered_by        | VARCHAR(100) |                          | USER, SYSTEM, RETRY_JOB            |
| user_id             | UUID         | FK → users.id, NULL      | NULL if system-triggered           |
| note                | TEXT         |                          |                                    |
| created_at          | TIMESTAMPTZ  | NOT NULL                 | Immutable — no updated_at          |

CREATE TABLE IF NOT EXISTS fiscaltax
(
    ad_client_id numeric(10,0) NOT NULL,
    ad_org_id numeric(10,0) NOT NULL,
    amount numeric,
    efiscal_categoryname character varying(60) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    created timestamp without time zone NOT NULL,
    createdby numeric(10,0) NOT NULL,
    efiscal_taxlabel character varying(1) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    elf_fiscalbill_id numeric(10,0) DEFAULT NULL::numeric,
    elf_fiscaltax_id numeric(10,0) NOT NULL,
    elf_fiscaltax_uu character varying(36) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    processed character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'N'::bpchar,
    processedon numeric,
    rate numeric,
    updated timestamp without time zone NOT NULL,
    updatedby numeric(10,0) NOT NULL,
    value character varying(40) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    efiscal_categorytype numeric(10,0) DEFAULT NULL::numeric,

---

### 2.11 fiscal config per org
CREATE TABLE IF NOT EXISTS adempiere.elf_fiscalbillconfig
(
    ad_client_id numeric(10,0) DEFAULT NULL::numeric,
    ad_org_id numeric(10,0) DEFAULT NULL::numeric,
    ad_printformat_id numeric(10,0) DEFAULT NULL::numeric,
    created timestamp without time zone DEFAULT getdate(),
    createdby numeric(10,0) DEFAULT NULL::numeric,
    elf_fiscalbillconfig_id numeric(10,0) NOT NULL DEFAULT NULL::numeric,
    elf_fiscalbillconfig_uu character varying(36) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    isactive character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'Y'::bpchar,
    r_mailtext_id numeric(10,0) DEFAULT NULL::numeric,
    updated timestamp without time zone DEFAULT getdate(),
    updatedby numeric(10,0) DEFAULT NULL::numeric,
    email_from character varying(60) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    email_bcc character varying(60) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    email_test character varying(60) COLLATE pg_catalog."default" DEFAULT NULL::character varying,
    istest character(1) COLLATE pg_catalog."default" NOT NULL DEFAULT 'N'::bpchar,

## 3. Entity Relationships

```
organizations
  ├── users (1:N)
  ├── platform_connections (1:N)
  └── sales_orders (1:N)
        └── fiscal_documents (1:1)
              └── fiscal_document_audit_log (1:N)

platform_connections → sales_orders (1:N)
users → fiscal_document_audit_log (0:N, optional)
```

---

## 4. Indexes (baseline)

| Table                      | Index                                          | Purpose                         |
|----------------------------|------------------------------------------------|---------------------------------|
| users                      | email                                          | Login lookup                    |
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
