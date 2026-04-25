.PHONY: help up down restart rebuild logs ps config pull clean

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
