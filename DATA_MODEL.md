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
| id             | UUID         | PK, NOT NULL             | Generated UUID                |
| email          | VARCHAR(255) | UNIQUE, NOT NULL         |                               |
| password_hash  | VARCHAR(255) | NOT NULL                 | bcrypt hash, never plaintext  |
| role           | VARCHAR(50)  | NOT NULL                 | SUPER_ADMIN, USER, AUDITOR    |
| organization_id| UUID         | FK → organizations.id    | NULL for SuperAdmin           |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| deleted_at     | TIMESTAMPTZ  | NULL                     | Soft delete                   |

---

### 2.2 organizations
Represents a merchant/client organization using eFiscal.

| Column         | Type         | Constraints              | Notes                         |
|----------------|--------------|--------------------------|-------------------------------|
| id             | UUID         | PK, NOT NULL             |                               |
| name           | VARCHAR(255) | NOT NULL                 |                               |
| tax_id         | VARCHAR(50)  | UNIQUE, NOT NULL         | PIB (Serbia tax identifier)   |
| is_active      | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                               |
| created_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| updated_at     | TIMESTAMPTZ  | NOT NULL                 |                               |
| deleted_at     | TIMESTAMPTZ  | NULL                     | Soft delete                   |

---

### 2.3 platform_connections
Stores connection config for each external shopping platform (MerchantPro, WooCommerce, Shopify, etc.) per organization.

| Column           | Type         | Constraints              | Notes                                      |
|------------------|--------------|--------------------------|--------------------------------------------|
| id               | UUID         | PK, NOT NULL             |                                            |
| organization_id  | UUID         | FK → organizations.id    |                                            |
| platform         | VARCHAR(50)  | NOT NULL                 | MERCHANTPRO, WOOCOMMERCE, SHOPIFY, etc.    |
| display_name     | VARCHAR(255) |                          | User-facing label                          |
| api_base_url     | VARCHAR(500) |                          | Platform-specific endpoint                 |
| credentials_ref  | VARCHAR(500) | NOT NULL                 | Reference to secret store, NOT raw secret  |
| is_active        | BOOLEAN      | NOT NULL, DEFAULT TRUE   |                                            |
| created_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |
| updated_at       | TIMESTAMPTZ  | NOT NULL                 |                                            |
| deleted_at       | TIMESTAMPTZ  | NULL                     | Soft delete                                |

> **Security**: API keys and credentials must NEVER be stored in plain text. Store a reference to a secret manager (e.g., env variable name, Vault path, or encrypted blob).

---

### 2.4 sales_orders
Sales order imported/fetched from an external shopping platform.

| Column              | Type         | Constraints              | Notes                              |
|---------------------|--------------|--------------------------|------------------------------------|
| id                  | UUID         | PK, NOT NULL             |                                    |
| organization_id     | UUID         | FK → organizations.id    |                                    |
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

### 2.5 fiscal_documents
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

### 2.6 fiscal_document_audit_log
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

> **Audit requirement**: rows in this table must NEVER be updated or deleted.

---

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
