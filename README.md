# eFiscal

eFiscal is a middleware platform for automated fiscalization of online orders.  
It connects webshop/order systems (currently MerchantPro) with Serbian Tax Authority services to issue and track fiscal bills.

## What It Does

- Authenticated, role-based access with client/organization scope
- MerchantPro order fetch with filterable sync parameters
- Fiscal bill creation (order-based and manual flows)
- Retry and status tracking for fiscalization attempts
- Admin workflows for users, roles, organizations, and API connection templates
- Localized web UI (English and Serbian) with in-app language switcher
- Extensible integration model for future modules (WooCommerce, Shopify, ERP, courier, etc.)

## Tech Stack

- Frontend: React 19 + Vite + React Router + Axios + i18next (English and Serbian UI)
- Backend: Java 17+ / Spring Boot 3.4.x
- Database: PostgreSQL 15/16 (Flyway migrations)
- Auth/Security: Bearer token auth, role/action authorization, scoped access checks
- Dev/Deployment: Docker, Docker Compose

## Repository Structure

```text
.
├── frontend/                     # React app (Vite)
├── backend/                      # Spring Boot API
├── docker-compose.yml            # Local full stack (db + backend + frontend)
├── ARCHITECTURE.md               # System and flow overview
├── PRODUCT_REQUIREMENTS.md       # Functional/non-functional requirements
├── API_CONTRACT.md               # REST contract and error model
├── DATA_MODEL.md                 # Database model and migration rules
├── BACKEND_STRUCTURE.md          # Backend architecture conventions
├── FRONTEND_STRUCTURE.md         # Frontend architecture/UI conventions
└── FISCAL_BILL_MODULE_SPEC.md    # Fiscal module behavior details
```

## Prerequisites

Choose one of the following workflows:

### Option A: Docker (recommended for quickest start)

- Docker
- Docker Compose

### Option B: Run services locally

- Java JDK 17+ (JDK 21 also supported by project scripts)
- Maven 3.9+
- Node.js 20+
- npm 10+
- PostgreSQL 15+ (if not using H2 default)

## Quick Start (Docker Compose)

From the repository root:

```bash
docker compose up --build
```

Services:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (`efiscal` / `efiscal`)

Stop:

```bash
docker compose down
```

Stop and remove DB volume:

```bash
docker compose down -v
```

## Local Development (Without Docker)

### 1) Start database (optional if using default H2)

Use a local PostgreSQL and create:

- Database: `efiscal`
- User: `efiscal`
- Password: `efiscal`

### 2) Run backend

```bash
cd backend
./run-dev.sh
```

Notes:

- `run-dev.sh` auto-detects JDK 17/21 and runs Spring with `dev` profile.
- Default backend port: `8080`.
- Flyway migrations run on startup.

### 3) Run frontend

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080` by default.

## Configuration

### Backend data source

`backend/src/main/resources/application.yml` supports env overrides:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Default fallback is a file-based H2 database in PostgreSQL compatibility mode.

### Frontend API target

`frontend/vite.config.js` supports:

- `VITE_API_PROXY_TARGET` (default: `http://localhost:8080`)

## Default Bootstrap Login (Development)

The project includes a development superadmin account:

- Email: `admin@efiscal.local`
- Password: `Admin123!`

This credential is intended for local/dev usage only. Change seeded/default credentials before any shared or production deployment.

## API Base Path

All backend endpoints are versioned under:

- `/api/v1`

See full endpoint contracts and error model in `API_CONTRACT.md`.

## Main Functional Areas

- **Auth**: login/session bootstrap for protected APIs
- **MerchantPro Sync**: fetch orders with parameterized filters
- **Fiscal Bill**: create, detail, retry, and status operations
- **RBAC and Scope**:
  - action-based permissions
  - client/org access restrictions
  - subscription validation for non-superadmin users
- **Administration**:
  - clients and organizations
  - users and roles
  - action catalog
  - API connections/templates
  - tax and payment type mappings

## Useful Commands

### Frontend

```bash
cd frontend
npm run dev
npm run build
npm run preview
npm run lint          # ESLint, including i18next literal-string checks
```

UI copy lives in `frontend/src/locales/en.json` and `frontend/src/locales/sr.json`. See `FRONTEND_STRUCTURE.md` (section 2A) for i18n conventions.

### Backend

```bash
cd backend
./run-dev.sh
mvn test
```

## Reference Documentation

- `ARCHITECTURE.md`
- `PRODUCT_REQUIREMENTS.md`
- `API_CONTRACT.md`
- `DATA_MODEL.md`
- `BACKEND_STRUCTURE.md`
- `FRONTEND_STRUCTURE.md`
- `FISCAL_BILL_MODULE_SPEC.md`

## Current Status

This repository already contains core modules and scaffolding for:

- end-to-end local stack execution
- role/user and access-control management
- MerchantPro order operations
- fiscal bill workflows and related mappings

Planned extension points include additional commerce/integration providers and broader automation modules.
