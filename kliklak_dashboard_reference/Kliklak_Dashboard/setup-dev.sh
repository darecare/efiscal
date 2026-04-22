#!/bin/bash

# Development setup script for Kliklak Dashboard

echo "🚀 Setting up Kliklak Dashboard for development..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker and Docker Compose are installed${NC}"

# Setup backend environment
echo -e "${YELLOW}📦 Setting up backend environment...${NC}"
cd backend

if [ ! -f .env ]; then
    cp .env.example .env
    echo -e "${GREEN}✅ Created backend/.env from .env.example${NC}"
    echo -e "${YELLOW}⚠️  Please update SECRET_KEY in backend/.env${NC}"
    echo -e "${YELLOW}   Run: python3 -c \"import secrets; print(secrets.token_urlsafe(32))\"${NC}"
else
    echo -e "${GREEN}✅ backend/.env already exists${NC}"
fi

cd ..

# Setup frontend environment
echo -e "${YELLOW}📦 Setting up frontend environment...${NC}"
cd frontend

if [ ! -f .env ]; then
    cp .env.example .env
    echo -e "${GREEN}✅ Created frontend/.env from .env.example${NC}"
else
    echo -e "${GREEN}✅ frontend/.env already exists${NC}"
fi

cd ..

# Start services with Docker Compose
echo -e "${YELLOW}🐳 Starting services with Docker Compose...${NC}"
docker-compose up -d

# Wait for services to be ready
echo -e "${YELLOW}⏳ Waiting for services to start...${NC}"
sleep 10

# Check if services are running
echo -e "${YELLOW}🔍 Checking service status...${NC}"
docker-compose ps

# Initialize database
echo -e "${YELLOW}💾 Initializing database...${NC}"
docker-compose exec -T backend python -c "from app.db.database import init_db; init_db()" 2>/dev/null || echo -e "${YELLOW}⚠️  Database initialization skipped (may already be initialized)${NC}"

echo ""
echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo "📝 Next steps:"
echo "  1. Update backend/.env with your SECRET_KEY"
echo "  2. Configure MerchantPro API credentials in backend/.env"
echo ""
echo "🌐 Access points:"
echo "  - Frontend:    http://localhost"
echo "  - Backend API: http://localhost:8000"
echo "  - API Docs:    http://localhost:8000/docs"
echo ""
echo "📚 Commands:"
echo "  - View logs:        docker-compose logs -f"
echo "  - Stop services:    docker-compose down"
echo "  - Restart services: docker-compose restart"
echo ""
echo "🎉 Happy coding!"
