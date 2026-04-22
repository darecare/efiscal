** ARCHITECTURE **

- Frontend - React
- Backend - Java 21 LTS (OpenJDK: Temurin/Corretto)
- Database - Postgres
- External Integrations - MerchantPro API, Serbian Tax Authority API (future planned integrations WooCommerce, Shopify, external ERPs or any other system that has its own API)

AUTHENTICATION FLOW:
1. User submits credentials → Frontend
2. Frontend sends POST /api/auth/login → Backend
3. Backend verifies password (bcrypt) → Database
4. Backend generates JWT token → Returns to Frontend
5. Frontend stores token → LocalStorage
6. All subsequent requests include: Authorization: Bearer <token>
7. Backend validates token on each request
8. Token expires after 30 minutes

DATA FLOW:
┌─────────┐    HTTP      ┌─────────┐    SQL       ┌──────────┐
│ Browser │ ◄─────────► │ Java API│ ◄─────────► │ Postgres │
└─────────┘   JSON+JWT   └─────────┘              └──────────┘

Java API external calls:
- Java API ──HTTP+API Key──► MerchantPro API
- Java API ──HTTPS+Provider Auth──► Tax Authority Serbia API
It is planned to have other API external calls for different online shoping platforms such as WooCommerce, Shopify etc. Each 

INTEGRATION NOTE:
- Java backend calls MerchantPro API directly.
- Java backend calls Serbian Tax Authority API directly.
- MerchantPro does not call Tax Authority API for this application.

TECHNOLOGY STACK:
• Frontend: React 19, Vite, React Router, Axios, Sass
• Backend: Java 21 LTS, Spring Boot 3.x
• Database: PostgreSQL 15
• Authentication: JWT (Spring Security), bcrypt
• Deployment: Docker, Docker Compose, Apache
• Security: CORS, Password Hashing, Token Auth

OPERATIONAL BASELINE (MVP):
• Concurrent Users: 10 active users at launch
• Capacity Headroom Target: up to 30 concurrent users without architecture changes
• External API Reliability: timeout + retry with exponential backoff + circuit breaker
• Observability: request correlation ID, structured logs, masked sensitive fields

FRONTEND REFERENCE ADAPTATION (KLIKLAK DASHBOARD):
• Reference source path: kliklak_dashboard_reference/Kliklak_Dashboard/frontend/src/pages
• Reuse target pages: Account.jsx, Users.jsx, Orders.jsx
• Reuse mode: visual/interaction baseline only, with clean reimplementation in eFiscal frontend
• No direct code copy from reference pages; preserve behavior and structure while adapting domain fields/actions

Required reuse scope:
• Account page: keep baseline page shell (navbar + sidebar + content card) and account form interaction pattern, then extend with eFiscal-specific fields.
• Users page: keep baseline page shell plus users list-management pattern (header area + users table in card + create/edit/delete flow).
• Orders page (partial reuse): keep section structure only:
	- Fetch Filters section (prefetch-oriented filter block with fetch actions)
	- Actions Bar section (selection scope + action selector + apply action)
	- Summary Table view grouped by order (order-level rows with expandable item details)

Governance:
• This reference is guidance for UI/UX and page composition, not a runtime dependency.
• Final behavior contracts and data semantics remain governed by PRODUCT_REQUIREMENTS and API_CONTRACT.

ACCESS CONTROL ARCHITECTURE (RBAC + SCOPE):
• Authorization model is action-based RBAC with data scope constraints.
• Access decision = Role Permission (module action) + User Scope (client + organization).
• Roles are managed from dedicated Role Definition UI and persisted in data model tables.
• Action catalog supports extensibility: new module actions can be registered and assigned to roles without redesign of authorization flow.

Backend enforcement rules:
• Every protected endpoint is mapped to required action code (example: MERCHANTPRO_FETCH_ORDERS, FISCAL_CREATE_BILL).
• Request context must include active client and organization.
• System validates user has organization access and role permission for requested action.
• Denied permissions return authorization error with standard API error model.
