# Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Kliklak Dashboard                         │
│                    Workflow Management System                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                          FRONTEND                                │
│                     React + Vite (Port 80)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │    Login     │  │   Register   │  │   Dashboard  │         │
│  │     Page     │  │     Page     │  │     Page     │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│                                                                  │
│  ┌──────────────────────────────────────────────────┐          │
│  │         React Router (Protected Routes)          │          │
│  └──────────────────────────────────────────────────┘          │
│                                                                  │
│  ┌──────────────────────────────────────────────────┐          │
│  │      Auth Context (JWT Token Management)         │          │
│  └──────────────────────────────────────────────────┘          │
│                                                                  │
│  ┌──────────────────────────────────────────────────┐          │
│  │          API Client (Axios + Interceptors)       │          │
│  └──────────────────────────────────────────────────┘          │
│                          │                                       │
└──────────────────────────┼───────────────────────────────────────┘
                           │ HTTP/REST
                           │ JSON + JWT Bearer Token
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                          BACKEND                                 │
│                   FastAPI + Python (Port 8000)                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                    API Endpoints                        │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────┐     │    │
│  │  │   Auth   │  │  Users   │  │  Orders  │  │ MerchantPro│     │    │
│  │  │  /login  │  │  /users  │  │ /orders  │  │/merchantpro│     │    │
│  │  │ /register│  │          │  │          │  │ /orders    │     │    │
│  │  │   /me    │  │          │  │          │  │ /products  │     │    │
│  │  └──────────┘  └──────────┘  └──────────┘  └────────────┘     │    │
│  └────────────────────────────────────────────────────────┘    │
│                          │                                       │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Security & Middleware                      │    │
│  │  • JWT Authentication (python-jose)                     │    │
│  │  • CORS Middleware                                      │    │
│  │  • Password Hashing (bcrypt)                            │    │
│  └────────────────────────────────────────────────────────┘    │
│                          │                                       │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                Business Logic Layer                     │    │
│  │  • User Management (CRUD; role-based)                   │    │
│  │  • Orders access control (dobavljac: vendor + 2-week)   │    │
│  │  • MerchantPro Service (HTTP Client)                    │    │
│  └────────────────────────────────────────────────────────┘    │
│                          │                                       │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                  Data Access Layer                      │    │
│  │  • SQLAlchemy ORM                                       │    │
│  │  • Pydantic Schemas                                     │    │
│  │  • Database Models                                      │    │
│  └────────────────────────────────────────────────────────┘    │
│                          │                                       │
└──────────────────────────┼───────────────────────────────────────┘
                           │ SQL
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         DATABASE                                 │
│                   PostgreSQL 15 (Port 5432)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Tables:                                                         │
│  ┌─────────────────┐                                            │
│  │     users       │                                            │
│  ├─────────────────┤                                            │
│  │ • id            │                                            │
│  │ • email         │                                            │
│  │ • username      │                                            │
│  │ • hashed_pwd    │                                            │
│  │ • is_active     │                                            │
│  │ • is_superuser  │                                            │
│  │ • role          │  (superuser | user | dobavljac)            │
│  │ • vendor_name   │  (nullable; for dobavljac only)            │
│  │ • created_at    │                                            │
│  │ • updated_at    │                                            │
│  └─────────────────┘                                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    EXTERNAL SERVICES                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              MerchantPro Platform API                   │    │
│  │  • Orders Management                                    │    │
│  │  • Products Catalog                                     │    │
│  │  • Status Updates                                       │    │
│  └────────────────────────────────────────────────────────┘    │
│                          ▲                                       │
└──────────────────────────┼───────────────────────────────────────┘
                           │ HTTPS/REST
                           │ Basic Auth (API user/password)
                           │
                    (from Backend Service)

┌─────────────────────────────────────────────────────────────────┐
│                     DEPLOYMENT (Docker)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Docker Compose Services:                                       │
│  • frontend  → Nginx serving React build                        │
│  • backend   → Uvicorn running FastAPI                          │
│  • db        → PostgreSQL database                              │
│                                                                  │
│  Volumes:                                                        │
│  • postgres_data → Persistent database storage                  │
│                                                                  │
│  Networks:                                                       │
│  • default → Internal container network                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

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
│ Browser │ ◄─────────► │ FastAPI │ ◄─────────► │ Postgres │
└─────────┘   JSON+JWT   └─────────┘              └──────────┘
                              │
                              │ HTTP+API Key
                              ▼
                         ┌────────────┐
                         │ MerchantPro│
                         └────────────┘

TECHNOLOGY STACK:
• Frontend: React 19, Vite, React Router, Axios
• Backend: Python 3.11, FastAPI, SQLAlchemy, Pydantic
• Database: PostgreSQL 15
• Authentication: JWT (python-jose), bcrypt
• Deployment: Docker, Docker Compose, Nginx
• Security: CORS, Password Hashing, Token Auth
