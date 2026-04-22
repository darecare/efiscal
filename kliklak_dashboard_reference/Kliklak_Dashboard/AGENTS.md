# Kliklak Dashboard – Agent Instructions

Workflow management system for online shops. React frontend, FastAPI backend, PostgreSQL.

## Reference Docs

- **ARCHITECTURE.md** – System layout, data flow, tech stack
- **API_DOCS.md** – Endpoint specs, request/response formats
- **FUNCTIONS_SPECS.md** – Feature specs, user stories, UI mockups, data models
- **FUNCTION_OVERVIEW.md** – High-level feature list
- **DEPLOYMENT.md** – VPS deployment steps
- **docs/** – Bug reports and incident notes

## Conventions

- Follow `.cursor/rules/` for backend, frontend, and MerchantPro patterns
- Use FUNCTIONS_SPECS.md when implementing or changing features
- Use API_DOCS.md for endpoint contracts and error formats
- After making changes, review and update the relevant documentation (API_DOCS, FUNCTIONS_SPECS, README, ARCHITECTURE, etc.) so it stays in sync with the codebase

## Stack

- Frontend: React 19, Vite, React Router, Axios
- Backend: Python 3.11, FastAPI, SQLAlchemy, Pydantic
- Auth: JWT (python-jose), bcrypt
- External: MerchantPro API (Basic Auth)
