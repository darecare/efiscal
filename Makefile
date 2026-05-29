.PHONY: help up down restart rebuild logs ps config pull clean branch-info up-branch down-branch restart-branch rebuild-branch logs-branch ps-branch clean-branch db-backup db-restore

PROJECT_PREFIX ?= efiscal
BRANCH_NAME ?= $(shell git rev-parse --abbrev-ref HEAD 2>/dev/null || echo default)
BRANCH_SLUG ?= $(shell echo "$(BRANCH_NAME)" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/-/g')
PROJECT_NAME ?= $(PROJECT_PREFIX)-$(BRANCH_SLUG)
COMPOSE_BRANCH = docker compose -p $(PROJECT_NAME)
BACKUP_DIR ?= .db-backups

help:
	@echo "Available commands:"
	@echo "  make up        - Build and start all services"
	@echo "  make down      - Stop and remove containers"
	@echo "  make restart   - Restart all services"
	@echo "  make rebuild   - Rebuild images and restart"
	@echo "  make logs      - Follow logs for all services"
	@echo "  make ps        - Show running service status"
	@echo "  make config    - Validate and print compose config"
	@echo "  make pull      - Pull latest base images"
	@echo "  make clean     - Stop services and remove volumes"
	@echo "  make branch-info     - Show branch-derived compose project name"
	@echo "  make up-branch       - Build and start services for current branch"
	@echo "  make down-branch     - Stop branch-specific services"
	@echo "  make restart-branch  - Restart branch-specific services"
	@echo "  make rebuild-branch  - Rebuild and start branch-specific services"
	@echo "  make logs-branch     - Follow logs for branch-specific services"
	@echo "  make ps-branch       - Show branch-specific service status"
	@echo "  make clean-branch    - Stop branch services and remove branch volumes"
	@echo "  make db-backup       - Dump branch DB into .db-backups/"
	@echo "  make db-restore FILE=<path> - Restore branch DB from SQL dump"

up:
	docker compose up --build

down:
	docker compose down

restart:
	docker compose down
	docker compose up -d

rebuild:
	docker compose up --build -d

logs:
	docker compose logs -f

ps:
	docker compose ps

config:
	docker compose config

pull:
	docker compose pull

clean:
	docker compose down -v

branch-info:
	@echo "Branch: $(BRANCH_NAME)"
	@echo "Project: $(PROJECT_NAME)"

up-branch:
	$(COMPOSE_BRANCH) up --build -d

down-branch:
	$(COMPOSE_BRANCH) down

restart-branch:
	$(COMPOSE_BRANCH) down
	$(COMPOSE_BRANCH) up -d

rebuild-branch:
	$(COMPOSE_BRANCH) up --build -d

logs-branch:
	$(COMPOSE_BRANCH) logs -f

ps-branch:
	$(COMPOSE_BRANCH) ps

clean-branch:
	$(COMPOSE_BRANCH) down -v

db-backup:
	@mkdir -p "$(BACKUP_DIR)"
	@backup_file="$(BACKUP_DIR)/$(PROJECT_NAME)-$$(date +%Y%m%d_%H%M%S).sql"; \
	$(COMPOSE_BRANCH) exec -T postgres pg_dump -U efiscal -d efiscal > "$$backup_file"; \
	echo "Backup created: $$backup_file"

db-restore:
	@test -n "$(FILE)" || (echo "Usage: make db-restore FILE=.db-backups/<dump>.sql" && exit 1)
	@test -f "$(FILE)" || (echo "File not found: $(FILE)" && exit 1)
	cat "$(FILE)" | $(COMPOSE_BRANCH) exec -T postgres psql -U efiscal -d efiscal
