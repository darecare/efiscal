** ARCHITECTURE **

- Frontend - React
- Backend - Java 21 LTS (OpenJDK: Temurin/Corretto)
- Database - Postgres
- External Integrations - MerchantPro API, Serbian Tax Authority API

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
• Frontend: React 19, Vite, React Router, Axios
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
