# eFiscal – Agent Instructions

Middleware platform for automated fiscalization of online orders. React 19 frontend, Java 17 / Spring Boot 3.4 backend, PostgreSQL 15.

## Priority of Instructions

When rules conflict, follow this order:
1. This file (AGENTS.md)
2. Scoped `.cursor/rules/` (applied per file glob)
3. Feature/contract docs (`PRODUCT_REQUIREMENTS.md`, `API_CONTRACT.md`, `FISCAL_BILL_MODULE_SPEC.md`)
4. Structure docs (`BACKEND_STRUCTURE.md`, `FRONTEND_STRUCTURE.md`, `ARCHITECTURE.md`)

If behavior is unspecified by any of the above, ask before inventing a solution.

## Reference Docs

- **ARCHITECTURE.md** – System layout, data flow, tech stack, access-control model
- **PRODUCT_REQUIREMENTS.md** – Feature specs, user stories, access control
- **DATA_MODEL.md** – Database schema and entity relationships
- **FISCAL_BILL_MODULE_SPEC.md** – Fiscalization logic and integrations
- **API_CONTRACT.md** – REST API endpoint specs and error model
- **BACKEND_STRUCTURE.md** – Java package layout and patterns
- **FRONTEND_STRUCTURE.md** – React component and routing structure
- **docs/MERCHANTPRO_ORDER_NORMALIZATION.md** – Order normalization pipeline
- **docs/PRODUCT_SYNC_JOBS_AND_INCREMENTAL_HANDOFF.md** – Product sync and incremental fetch jobs

## Before Coding

1. Identify which reference docs cover the area being changed.
2. Check `API_CONTRACT.md` if touching any endpoint request/response/error shape.
3. Check `DATA_MODEL.md` if touching persisted data.
4. Check `FISCAL_BILL_MODULE_SPEC.md` if touching fiscalization logic or external API calls.

## Rules by Area

Apply the relevant `.cursor/rules/` file based on what you touch:

| Area | Rule file |
|---|---|
| Backend Java | `.cursor/rules/api-conventions.mdc` |
| Frontend React/JSX/TSX | `.cursor/rules/frontend-patterns.mdc` |
| External integrations | `.cursor/rules/integrations.mdc` |

## Non-Negotiables

### Security
- Never log raw secrets, API keys, JWT tokens, or credentials.
- Never commit `.env` files or files containing real credentials.
- Tax Authority communication uses mTLS; do not bypass certificate config.

### API & Contract Discipline
- Every new or changed endpoint must be reflected in `API_CONTRACT.md` in the same change.
- All error responses must follow the API error model defined in `API_CONTRACT.md`.
- Idempotency-Key support is required on all mutating fiscal endpoints.

### Database & Migrations
- Every schema change requires a Flyway migration under `backend/src/main/resources/db/migration/`.
- Update `DATA_MODEL.md` alongside any schema change.
- User deletion is soft-delete only (`deleted_at`); never hard-delete user rows.

### External Integrations
- All external API calls must have explicit timeouts, safe retries with exponential backoff, and a circuit breaker.
- Every request must carry a Correlation ID in logs.
- New integration modules go in the existing `service/` package, following the MerchantPro module pattern; do not create parallel structures.

### RBAC & Authorization
- Every new protected endpoint must be mapped to an action code (e.g. `FISCAL_CREATE_BILL`).
- Access decisions require: valid JWT + active client/organization scope + role permission + active subscription (for normal users).

### UI Internationalization
- All user-visible strings and `aria-label`s must go through `react-i18next` (`t()`); never hardcode them in JSX.
- Any label/message change must update both `frontend/src/locales/en.json` and `sr.json` in the same change.
- Run `npm run lint` in `frontend/` — `eslint-plugin-i18next` will catch hardcoded text.

## Done Checklist

Before finishing a task, verify:

- [ ] Backend compiles (`mvn compile` / `mvn test`)
- [ ] Frontend lints clean (`npm run lint` in `frontend/`)
- [ ] No hardcoded UI strings (both locale files updated if text was added/changed)
- [ ] API contract updated if endpoint behavior changed
- [ ] Flyway migration added and `DATA_MODEL.md` updated if schema changed
- [ ] Relevant structure docs updated (`BACKEND_STRUCTURE.md`, `FRONTEND_STRUCTURE.md`)
- [ ] Relevant `.cursor/rules/*.mdc` updated if the change established or invalidated an agent-facing convention

## Doc Sync Matrix

| If you change… | Also update… |
|---|---|
| Any endpoint request/response/error | `API_CONTRACT.md` |
| Database schema | `DATA_MODEL.md` + add Flyway migration |
| Fiscalization logic | `FISCAL_BILL_MODULE_SPEC.md` |
| Backend package structure | `BACKEND_STRUCTURE.md` |
| Frontend page/component structure | `FRONTEND_STRUCTURE.md` |
| Any UI text or aria-label | `en.json` + `sr.json` |
| A cross-cutting convention (package layout, auth flow, integration pattern, UI pattern, i18n rule) | Relevant `.cursor/rules/*.mdc` — only if the existing rule would now mislead an agent |

## Stack (authoritative)

- Frontend: React 19, Vite 6, React Router 7, Axios, Sass, i18next (en/sr)
- Backend: Java 17, Spring Boot 3.4, Maven
- Database: PostgreSQL 15, Flyway migrations
- Auth: JWT (Spring Security), bcrypt, 30-minute token expiry
- Deployment: Docker, Docker Compose, Apache
- External: MerchantPro API (Basic/API Key auth), Serbian Tax Authority API (mTLS)
