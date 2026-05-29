# eFiscal – Agent Instructions

Middleware platform for automated fiscalization of online orders. React frontend, Java/Spring Boot backend, PostgreSQL.

## Reference Docs

- **ARCHITECTURE.md** – System layout, data flow, tech stack
- **PRODUCT_REQUIREMENTS.md** – Feature specs, user stories, access control
- **DATA_MODEL.md** – Database schema and entity relationships
- **FISCAL_BILL_MODULE_SPEC.md** – Fiscalization logic and integrations
- **API_CONTRACT.md** – REST API endpoint specs
- **BACKEND_STRUCTURE.md** – Java backend specific architecture
- **FRONTEND_STRUCTURE.md** – React frontend specific architecture

## Conventions

- Follow `.cursor/rules/` for backend, frontend, and integration patterns
- Use `PRODUCT_REQUIREMENTS.md` and `FISCAL_BILL_MODULE_SPEC.md` when implementing or changing features
- Use `API_CONTRACT.md` for endpoint contracts and error formats
- After making changes, review and update the relevant documentation (API_CONTRACT, BACKEND_STRUCTURE, README, DATA_MODEL, etc.) so it stays in sync with the codebase

## UI Internationalization Rules

- Treat all user-facing UI text as i18n-managed text; do not hardcode visible labels/messages in React components.
- When adding or changing a label/message, update all supported locale catalogs in the same change set (currently `en` and `sr`).
- Keep locale key sets synchronized across languages (same keys and interpolation placeholders).
- For icon-only or accessibility-only controls, keep visual glyphs as icons but provide localized accessible labels (for example via `aria-label`/screen-reader text) in every supported language.

## Stack

- Frontend: React 19, Vite, React Router, Axios, Sass
- Backend: Java 21 LTS, Spring Boot 3.x
- Database: PostgreSQL 15
- Auth: JWT (Spring Security), bcrypt
- External: MerchantPro API, Serbian Tax Authority API
