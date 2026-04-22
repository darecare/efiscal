# Deployment Guide for Kliklak Dashboard

This guide will help you deploy the Kliklak Dashboard on your VPS.

## Prerequisites

- A VPS with Ubuntu 20.04 or later
- Docker and Docker Compose installed
- Domain name (optional, but recommended)
- SSH access to your VPS

## Step 1: Install Docker and Docker Compose

If you don't have Docker installed, run these commands on your VPS:

```bash
# Update package index
sudo apt-get update

# Install prerequisites
sudo apt-get install -y apt-transport-https ca-certificates curl software-properties-common

# Add Docker's official GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Add Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Add your user to the docker group
sudo usermod -aG docker $USER

# Start and enable Docker
sudo systemctl start docker
sudo systemctl enable docker
```

## Step 2: Clone the Repository

```bash
cd /home/$USER
git clone https://github.com/darecare/Kliklak_Dashboard.git
cd Kliklak_Dashboard
```

## Step 3: Configure Environment Variables

### Backend Configuration

```bash
cd backend
cp .env.example .env
nano .env
```

Update the following values in `.env`:

```env
# Generate a secure secret key
SECRET_KEY=your-randomly-generated-secret-key-here

# Update database credentials if needed
DATABASE_URL=postgresql://kliklak:kliklak@db:5432/kliklak_dashboard

# Configure MerchantPro API (Basic Auth credentials)
# Use your shop domain (the backend calls /api/v2/... under this base URL)
ENVIRONMENT=dev
MERCHANTPRO_API_URL=https://probaproba.shopmania.biz
MERCHANTPRO_API_USERNAME=your-actual-merchantpro-username
MERCHANTPRO_API_PASSWORD=your-actual-merchantpro-password

# Update allowed origins with your domain
ALLOWED_ORIGINS=https://yourdomain.com,http://yourdomain.com
```

`ENVIRONMENT` controls environment-specific product status config lookup:
`product_statuses_<ENVIRONMENT>.json` with fallback to `product_statuses.json`.

To generate a secure SECRET_KEY:
```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### Frontend Configuration (Optional)

```bash
cd ../frontend
cp .env.example .env
# Edit if needed
```

## Step 4: Deploy with Docker Compose

```bash
cd /home/$USER/Kliklak_Dashboard

# Build and start all services
docker-compose up -d

# Check if services are running
docker-compose ps

# View logs
docker-compose logs -f
```

## Step 5: Initialize the Database and Run Migrations

The database will be automatically created when you first start the services. To verify and apply user management schema (role, vendor_name):

```bash
# Create tables if not already created
docker-compose exec backend python -c "from app.db.database import init_db; init_db()"

# Run user management migration (adds role and vendor_name columns; safe to run multiple times)
docker-compose exec backend python scripts/migrate_users.py
exit
```

Alternatively, run the migration as a one-off: `docker-compose run --rm backend python scripts/migrate_users.py`

## Step 6: Access Your Application

- Frontend: http://your-vps-ip
- Backend API: http://your-vps-ip:8000
- API Documentation: http://your-vps-ip:8000/docs

## Step 7: Set Up SSL with Let's Encrypt (Recommended)

### Install Certbot

```bash
sudo apt-get install -y certbot python3-certbot-nginx
```

### Update docker-compose.yml for SSL

Add nginx as a reverse proxy in front of your application. Create `nginx.conf`:

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name yourdomain.com www.yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /api {
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Obtain SSL Certificate

```bash
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
```

## Step 8: Set Up Automatic Backups

Create a backup script:

```bash
#!/bin/bash
# /home/$USER/backup-kliklak.sh

BACKUP_DIR="/home/$USER/kliklak-backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup database
docker-compose exec -T db pg_dump -U kliklak kliklak_dashboard > $BACKUP_DIR/db_backup_$DATE.sql

# Keep only last 7 days of backups
find $BACKUP_DIR -name "db_backup_*.sql" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/db_backup_$DATE.sql"
```

Make it executable and add to cron:

```bash
chmod +x /home/$USER/backup-kliklak.sh

# Add to crontab (daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * /home/$USER/backup-kliklak.sh") | crontab -
```

## Step 9: Monitoring and Maintenance

### View Application Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f db
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart backend
```

### Update the Application

```bash
cd /home/$USER/Kliklak_Dashboard

# Pull latest changes
git pull origin main

# Rebuild and restart services
docker-compose down
docker-compose up -d --build
```

## Troubleshooting

### Backend won't start
- Check logs: `docker-compose logs backend`
- Verify environment variables in `backend/.env`
- Check database connection

### Frontend shows 502 error
- Check if backend is running: `docker-compose ps`
- Verify backend is healthy: `curl http://localhost:8000/health`

### Database connection failed
- Check if database is running: `docker-compose ps db`
- Verify DATABASE_URL in backend/.env

### Can't create user / login
- Check API logs: `docker-compose logs backend`
- Verify SECRET_KEY is set in backend/.env
- Check database tables are created

## Security Checklist

- [ ] Change default SECRET_KEY
- [ ] Use strong database password
- [ ] Enable SSL/HTTPS
- [ ] Set up firewall (UFW)
- [ ] Regular backups
- [ ] Keep Docker images updated
- [ ] Monitor application logs
- [ ] Configure MerchantPro API credentials securely

## Performance Optimization

### For production, update docker-compose.yml:

```yaml
services:
  backend:
    command: uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
    
  db:
    deploy:
      resources:
        limits:
          memory: 1G
```

## Support

For issues or questions:
- Check application logs
- Review the README.md
- Open an issue on GitHub
