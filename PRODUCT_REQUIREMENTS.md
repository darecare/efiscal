# PRODUCT REQUIREMENTS

## 1. Product Overview
- Product Name: eFiscal
- Purpose: App will be used to issue fiscal bills based on sales orders from online shop. App will be able to connect to different online shopping platforms.
App will be later extended to different platforms and functions for each platform. Example connect online shop with courier service. 
Example 2: connect online shop with ERP system.
App will serve as a bridge between 2 systems which both have API, but have no direct connection, and exchange data between system based on various functions.
- Primary Market: [e.g., Serbia small/medium online merchants]
- app will be made as modular, so that new API modules can be added. Example: connect app to various platforms for online shops, such as WooCommerce, MerchantPro, Shopify etc.
First module to be implemented is MerchantPro.
- Other API modules will be Tax Authorities, API Courier services, ERPs, or external accounting apps.
- For every API module (connection) it will be possible to create functions that operate per defined module. 
Example 1: Issue fiscal bill for module Tax Authority, based on sales order from MerchantPro.
Example 2: Create shipment for module "Slanje paketa" (courier service, ), based on sales order from MerchantPro.
Example 3: Create product for module "Slanje paketa", based on product from MerchantPro.
- Each user will be linked to its own Client and Organizations. Client can have more organizations, example one user has 2 online shops, each shop will be one organization.



## 2. Goals and Success Metrics
- Goal 1: [e.g., Issue compliant fiscal bills from webshop sales orders
- Goal 2: [e.g., ]
- Goal 3: [e.g., Improve traceability of fiscal operations]

### KPI Targets (MVP)
- Active users supported concurrently: 10 (target headroom: 30)
- API error rate: [target %]
- Fiscal document processing time (P95): [target seconds]
- External API success rate (MerchantPro/Tax API): [target %]

## 3. User Roles
- SuperAdmin
  - [all permisions]
- User
  - Change account
  - Sales Order list page
  - Fiscal bill page
  - Fiscal bill list page
  - Reports
  - per Organization
- Read-only/Auditor (optional)
  - [permissions]

## 4. In Scope (MVP)
- User authentication and authorization
- MerchantPro order fetch/sync
- MerchantPro products fetch/sync
- Fiscalization flow (fiscal bill issue) through Serbian Tax Authority API
- Status tracking for submitted fiscal bills
- Basic reporting/export

## 5. Out of Scope (MVP)
- [example: multi-tenant white-labeling]
- [example: advanced BI dashboards]
- [example: mobile native apps]

## 6. Core User Flows

### 6.1 Login
1. User submits credentials
2. Backend validates and issues JWT
3. User accesses protected modules

### 6.2 Process Merchant Order
1. App receives or fetches order from MerchantPro - data will be in json format, kept in memory. Only if fiscall bill is issued succesfully for order, then some of order data will be stored in database. Main source of data for orders and products is MerchantPro shop, not local tables.
2. User/system validates required fiscal data
3. User selects orders for fiscalization
3. Backend sends request to Tax Authority API per each selected order
4. App stores response and status - if response is 200, fiscal bill data will be stored in database tables
5. User sees final status (success/failure/pending)

### 6.3 Retry Failed Fiscalization
1. User opens failed transaction
2. User/system triggers retry
3. Backend executes safe retry policy
4. Status and audit trail are updated

### 6.4 Manual entry of fiscal bill
1. User will access page to manually add fiscal bill
2. User enters header data and line items(products)
3. System sends request to Tax Authority API to issue fiscal bill
4. App stores response and status - if response is 200, fiscal bill data will be stored in database tables

## 7. Functional Requirements
- FR-001: System must authenticate users and protect all non-public endpoints.
- FR-002: System must integrate with MerchantPro API for order data.
- FR-003: System must integrate with Serbian Tax Authority API for fiscalization.
- FR-004: System must persist all request/response references required for audit.
- FR-005: System must provide transaction status visibility to users.
- FR-006: System must allow safe retry for transient external API failures.

## 8. Non-Functional Requirements (Product-Level)
- NFR-001: MVP supports 10 concurrent active users.
- NFR-002: System is designed to scale to 30 concurrent users without redesign.
- NFR-003: External API calls use timeout, retries with backoff, and circuit breaker.
- NFR-004: Sensitive data is protected in transit and at rest.
- NFR-005: Logs include correlation ID and exclude secrets/PII.

## 9. Acceptance Criteria (MVP)
- [ ] User can log in and access protected pages.
- [ ] MerchantPro order can be processed end-to-end to Tax Authority submission.
- [ ] Failed external API calls are visible and can be retried safely.
- [ ] Every fiscal bill issuing has auditable status history.
- [ ] Basic KPI and error visibility available for operations.

## 10. Open Questions
- OQ-001: [question] — Owner: [name] — Due: [date]
- OQ-002: [question] — Owner: [name] — Due: [date]
