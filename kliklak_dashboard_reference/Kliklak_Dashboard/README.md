# Kliklak Dashboard

A web application to help online shops manage workflows that are currently too slow in the shop's admin panel. Built with React frontend, Python (FastAPI) backend, and PostgreSQL database.

## Features

- **User Management**: Register and login with JWT authentication. Three roles: **superuser** (full access, can manage users), **user** (regular dashboard and orders), **dobavljac** (supplier: orders limited to their vendor, date range max 2 weeks). Superusers can add, edit, and delete users and set roles.
- **MerchantPro Integration**: Fetch and manage data from MerchantPro platform via API
- **Workflow Management**: Streamline shop operations and improve efficiency
- **Secure**: Password hashing, JWT tokens, and role-based protected routes
- **Docker Ready**: Easy deployment with Docker and Docker Compose

## Tech Stack

### Frontend
- React 19
- React Router for navigation
- Axios for API communication
- Vite for build tooling
- Modern CSS with responsive design

### Backend
- Python 3.11
- FastAPI framework
- SQLAlchemy ORM
- PostgreSQL database
- JWT authentication
- Async HTTP client for MerchantPro API

### Deployment
- Docker & Docker Compose
- Nginx for serving frontend
- PostgreSQL in container

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Git

### Installation

1. Clone the repository:
```bash
git clone https://github.com/darecare/Kliklak_Dashboard.git
cd Kliklak_Dashboard
```

2. Set up backend environment:
```bash
cd backend
cp .env.example .env
# Edit .env and configure your settings, especially:
# - SECRET_KEY (generate a random secret key)
# - ENVIRONMENT (dev | qa | prod; selects product_statuses_<env>.json when available)
# - MERCHANTPRO_API_URL (your MerchantPro shop domain, e.g. https://probaproba.shopmania.biz)
# - MERCHANTPRO_API_USERNAME and MERCHANTPRO_API_PASSWORD for Basic Auth
```

3. Set up frontend environment (optional):
```bash
cd ../frontend
cp .env.example .env
```

4. Start the application with Docker Compose:
```bash
cd ..
docker-compose up -d
```

5. Access the application:
- Frontend: http://localhost
- Backend API: http://localhost:8000
- API Documentation: http://localhost:8000/docs

### Development Setup

**Recommended workflow** (avoids slow Docker frontend builds):

1. Start backend + database in Docker:
   ```bash
   docker compose up -d db backend
   ```

2. Run frontend locally (hot reload, no rebuilds):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Open **http://localhost:5173** — Vite proxies `/api` to the backend automatically.

#### Alternative: Backend + Frontend Both Local

```bash
# Terminal 1 - Backend
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload

# Terminal 2 - Frontend
cd frontend
npm install
npm run dev
```

The frontend will be available at http://localhost:5173

## Usage

### First Time Setup

1. Navigate to http://localhost (or http://localhost:5173 in dev mode)
2. Click "Register here" to create your first user account (this creates a regular **user**). To create a **superuser** or **dobavljac**, run the user migration then have an existing superuser create users via the Korisnici tab, or promote the first user in the database (`UPDATE users SET role = 'superuser', is_superuser = true WHERE id = 1`).
3. Fill in email, username, and password
4. After registration, you'll be automatically logged in
5. You'll see the dashboard with MerchantPro integration status. After deploying, run the user migration once: `docker-compose exec backend python scripts/migrate_users.py` (adds `role` and `vendor_name` columns; backfills existing superusers).

### Configure MerchantPro API

To enable MerchantPro integration:

1. Edit `backend/.env` file
2. Set your MerchantPro API credentials (Basic Auth username/password):
   ```
   # Use your shop domain (the backend calls /api/v2/... under this base URL)
   ENVIRONMENT=dev
   MERCHANTPRO_API_URL=https://probaproba.shopmania.biz
   MERCHANTPRO_API_USERNAME=your-username
   MERCHANTPRO_API_PASSWORD=your-password
   ```
   `ENVIRONMENT` controls product-status config lookup (`product_statuses_<ENVIRONMENT>.json`, fallback `product_statuses.json`).
3. Restart the backend service:
   ```bash
   docker-compose restart backend
   ```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/me` - Get current user info

### Users (superuser only for list/create/delete)
- `GET /api/users/` - List all users
- `POST /api/users/` - Create user (role: superuser | user | dobavljac; vendor_name required for dobavljac)
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user (role/vendor_name only by superuser)
- `DELETE /api/users/{id}` - Delete user (cannot delete self)

### Orders
- `GET /api/orders/` - List sales orders (denormalized rows; `meta.total` = order count). Dobavljac users get only their vendor's orders and a max 2-week date range.
- `POST /api/orders/bulk-update-status` - Bulk update product status for selected line items (dobavljac: only their vendor's items updated)
- `POST /api/orders/bulk-update-tags` - Add one or more tags to selected orders
- Orders UI behavior: after bulk actions refresh data, previously checked items stay checked if they still exist in refreshed results; when "Select all matching" was active, it remains active after refresh.

### Config
- `GET /api/config/product-statuses` - List product status values for bulk updates (environment-aware source via `ENVIRONMENT`; currently public)

### MerchantPro
- `GET /api/merchantpro/orders` - Get orders from MerchantPro (`order_status` query param for filtering)
- `GET /api/merchantpro/orders/{id}` - Get specific order
- `GET /api/merchantpro/products` - Get products from MerchantPro
- `PUT /api/merchantpro/orders/{id}/status` - Update order status

## Project Structure

```
Kliklak_Dashboard/
├── backend/
│   ├── app/
│   │   ├── api/           # API endpoints
│   │   ├── core/          # Core utilities (config, security)
│   │   ├── db/            # Database configuration
│   │   ├── models/        # SQLAlchemy models
│   │   ├── schemas/       # Pydantic schemas
│   │   ├── services/      # Business logic
│   │   └── main.py        # FastAPI application
│   ├── Dockerfile
│   └── requirements.txt
├── frontend/
│   ├── src/
│   │   ├── components/    # React components
│   │   ├── contexts/      # React contexts
│   │   ├── pages/         # Page components
│   │   ├── services/      # API services
│   │   └── styles/        # CSS styles
│   ├── Dockerfile
│   └── package.json
└── docker-compose.yml
```

## Security

- Passwords are hashed using bcrypt
- JWT tokens for authentication
- CORS configured for security
- Environment variables for sensitive data
- Input validation with Pydantic

## Deployment to VPS

1. Install Docker and Docker Compose on your VPS
2. Clone the repository
3. Configure environment variables
4. Run `docker-compose up -d`
5. Configure your domain/DNS to point to your VPS
6. (Optional) Set up SSL with Let's Encrypt

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

ISC

## Support

For issues and questions, please open an issue on GitHub.