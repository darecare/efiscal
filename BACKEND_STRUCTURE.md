# BACKEND STRUCTURE

## 1. Purpose
- Define backend structure for eFiscal implementation using Java 21 and Spring Boot 3.x.
- Keep modules clear, testable, and ready for additional API platforms.
- Align implementation with PRODUCT_REQUIREMENTS, API_CONTRACT, DATA_MODEL, and ARCHITECTURE.

## 2. Core Principles
- Modular by integration domain and business capability.
- Clear separation of API layer, application/business layer, integration adapters, and persistence.
- Contract-first behavior: API_CONTRACT is authoritative for endpoint behavior and errors.
- Reliable external communication with timeout, retry, and circuit breaker.
- Auditability and traceability for fiscal operations.
- No dependency on legacy runtime code; use legacy only as behavior reference.

## 3. High-Level Backend Layers

### 3.1 API Layer (Controllers)
- Own HTTP endpoints, request validation, auth checks, and response mapping.
- Keep controllers thin: no business rules in controllers.
- Return standardized error model as defined in API_CONTRACT.

### 3.2 Application Layer (Use Cases / Services)
- Orchestrate workflows:
  - fetch orders
  - validate data
  - issue fiscal bill
  - retry failed operations
- Handle transactional boundaries and idempotency checks.
- Coordinate domain services and integration clients.

### 3.3 Domain Layer
- Core business concepts and rules:
  - fiscal bill lifecycle
  - statuses
  - retry policy
  - validation rules
- Domain logic must stay framework-light where possible.

### 3.4 Integration Layer (Adapters)
- Use a hybrid model:
  - metadata-driven dynamic adapter engine for standard API behavior
  - provider-specific adapter plugin only for non-standard flows
- Integration runtime configuration is database-driven:
  - apiconn stores connection profile (base URL, auth type, platform, active flag)
  - apitemplate stores operation templates (endpoint, method, content type)
- Isolate external DTOs and mapping from internal domain models.

### 3.5 Persistence Layer (Repositories)
- JPA repositories and custom queries for storage/retrieval.
- Store auditable records for fiscal operations and statuses.
- Apply uniqueness and idempotency constraints from DATA_MODEL.

## 4. Suggested Package Structure

- com.efiscal.app
  - config
  - security
  - common
    - errors
    - logging
    - utils
  - api
    - auth
    - fiscalbill
    - orders
    - users
    - reporting
  - application
    - fiscalbill
    - orders
    - users
    - retry
  - domain
    - fiscalbill
    - order
    - user
    - access
    - valueobjects
  - integration
    - engine
      - operation
      - resolver
      - executor
      - template
    - auth
      - strategy
    - merchantpro
      - client
      - dto
      - mapper
    - taxauthority
      - client
      - dto
      - mapper
  - persistence
    - entity
    - repository
    - specification
  - access
    - policy
    - resolver
    - evaluator
  - audit
    - model
    - service

Notes:
- Keep integration DTOs under integration modules only.
- Do not leak external API data models into domain/application layers.
- Prefer config-first onboarding for new providers; add provider-specific code only when required by non-standard behavior.

## 5. API and Service Conventions

### 5.1 Controller Conventions
- Endpoint naming follows API_CONTRACT.
- Validate request payloads at boundary.
- Use correlation ID in logs and propagated responses where needed.

### 5.2 Service Conventions
- One service method per business use case.
- Explicit command/query inputs.
- Return structured results with status and context for audit.

### 5.3 Error Handling Conventions
- Map business and integration failures to standardized error codes.
- Distinguish:
  - validation errors
  - business rule violations
  - external API failures
  - transient infrastructure failures

## 6. External Integration Pattern

### 6.1 Adapter + Client Pattern
- Adapter translates between internal use case and external API.
- Client performs HTTP calls and handles transport-level concerns.
- Mapper converts between external DTOs and internal models.
- For standard integrations, adapter behavior is assembled dynamically from apiconn and apitemplate metadata.

### 6.2 Reliability Rules
- Timeout required for every external call.
- Retry only for safe transient failures.
- Circuit breaker for unstable dependencies.
- Structured logging for request/response metadata with secret masking.

### 6.3 Idempotency Rules
- Fiscal issuance must be idempotent.
- Use idempotency key and persistence constraints to block duplicates.
- Retry flows must not create duplicate fiscal bills.

### 6.4 Dynamic Adapter Runtime (DB-Driven)
- Source tables from DATA_MODEL:
  - apiconn: base URL, auth model, platform identity, active scope
  - apitemplate: operation endpoint, HTTP method, content type
- Dynamic fetch parameter definitions: template-linked parameter metadata for query-string construction and validation.
- Execution pipeline:
  1. Resolve active apiconn by organization and platform.
  2. Resolve apitemplate by operation key and connection.
  3. Resolve and validate allowed fetch parameters (MVP: created_after, shipping_status).
  4. Build request URL query string dynamically from provided filters and template parameter definitions.
  5. Build method, headers, and payload (if applicable).
  6. Apply auth strategy selected by apiconn auth type.
  7. Execute call with timeout/retry/circuit-breaker policies.
  8. Map response to internal result and persist audit metadata.
- Security rule: credentials are referenced securely and never stored in plaintext in operational logs.

Reference-only guidance:
- Use Kliklak Orders fetch behavior as reference for practical filter usage (date/shipping status + pagination).
- Use legacy template-driven field mapping approach as reference for dynamic parameter resolution.
- Reimplement in current backend stack; do not copy source code.

### 6.5 Provider Onboarding Modes
- Mode A: Config-only (preferred)
  - add apiconn + apitemplate rows, no new Java classes.
- Mode B: Config + custom mapper
  - add metadata plus provider-specific response/request mapper.
- Mode C: Full plugin adapter
  - add provider-specific adapter/client code only for non-standard protocols or flows.
- Target outcome for scale (for example 50 providers): most new providers should be onboarded using Mode A or Mode B.

## 7. Security and Access Control
- JWT authentication via Spring Security.
- Role-based authorization for endpoint access and actions.
- Sensitive values stored in secure configuration (not hardcoded).
- Mask secrets/PII in logs.

Subscription enforcement:
- Normal users are allowed only when subscription is active and current timestamp is before subscription expiration.
- Subscription validation must run at login and for protected operations.
- Bootstrap SuperAdmin is exempt from subscription expiration checks.

### 7.1 Authorization Model (Action-Based RBAC)
- Role Definition is data-driven and managed by admin users.
- Permissions are assigned as module actions (for example: MERCHANTPRO_FETCH_ORDERS, FISCAL_CREATE_BILL).
- Endpoint protection must validate required action code, not only role name.

### 7.2 Scope Enforcement (Client + Organization)
- User must belong to active client context.
- User must have organization access for active org.
- Final authorization decision:
  1. user role has required action
  2. user has access to active organization
  3. organization belongs to active client context
  4. normal user subscription is active and not expired

### 7.3 Dynamic Action Catalog
- New module functions register new action codes in database catalog.
- Role-to-action assignments are updated via data/admin UI.
- Authorization engine must not require code redesign for each new action.

### 7.4 Bootstrap SuperAdmin
- Initial deployment must create one bootstrap SuperAdmin account.
- Bootstrap SuperAdmin has global privileges across all clients and organizations.
- Bootstrap provisioning should be implemented through startup seed/migration process and forced password change policy on first login.

## 8. Data and Transaction Rules
- Use @Transactional at application service boundaries.
- Keep transactions short and explicit.
- Separate write flows from read-heavy queries when useful.
- Persist audit trail for status changes and key events.

## 9. Background and Async Processing
- Initial MVP can run key flows synchronously where acceptable.
- For heavier operations, introduce async job pattern:
  - enqueue operation
  - process worker-side
  - update status
  - expose status endpoint

## 10. Testing Strategy
- Unit tests:
  - domain rules
  - service orchestration
  - mappers
- Integration tests:
  - repository behavior
  - controller + security wiring
  - adapter/client contracts with mocked external APIs
- Contract tests:
  - response/error payload conformance to API_CONTRACT.

## 11. Operational Observability
- Correlation ID per request.
- Structured logs for API and integration boundaries.
- Metrics:
  - external API latency and failure rate
  - fiscalization success/failure counts
  - retry counts
- Health checks for core dependencies.

## 12. Reuse and Governance
- Legacy iDempiere code in legacy is behavior reference only.
- Kliklak frontend reference impacts UI flows, not backend runtime dependencies.
- Every PR should cite relevant sections from:
  - PRODUCT_REQUIREMENTS
  - API_CONTRACT
  - DATA_MODEL
  - LEGACY_REFERENCE (when behavior is inspired by legacy)

## 13. MVP Priorities
- Build stable auth, order fetch, fiscal issue, status tracking, and retry flows first.
- Keep module boundaries clean to support future integrations:
  - WooCommerce
  - Shopify
  - ERP/accounting platforms
- Optimize for correctness and auditability before advanced performance tuning.
