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
- After making changes, review and update the relevant documentation so it stays in sync with the codebase

## Stack

- Frontend: React 19, Vite, React Router, Axios, Sass
- Backend: Java 21 LTS, Spring Boot 3.x
- Database: PostgreSQL 15
- Auth: JWT (Spring Security), bcrypt
- External: MerchantPro API, Serbian Tax Authority API
