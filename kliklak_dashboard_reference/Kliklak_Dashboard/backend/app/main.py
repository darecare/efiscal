from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging
from app.api import auth, users, merchantpro, orders, config
from app.core.config import settings
from app.db.database import init_db

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

app = FastAPI(title=settings.PROJECT_NAME, version="1.0.0")

# Initialize database on startup
@app.on_event("startup")
async def startup_event():
    init_db()

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router, prefix="/api/auth", tags=["auth"])
app.include_router(users.router, prefix="/api/users", tags=["users"])
app.include_router(merchantpro.router, prefix="/api/merchantpro", tags=["merchantpro"])
app.include_router(orders.router, prefix="/api/orders", tags=["orders"])
app.include_router(config.router, prefix="/api/config", tags=["config"])

@app.get("/")
async def root():
    return {"message": "Kliklak Dashboard API"}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}
