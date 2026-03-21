** ARCHITECTURE **
Frontend - React
Backend - Java
Database - Postgres

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
