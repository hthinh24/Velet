#!/usr/bin/env bash
# Runs all services (Maven). Assumes .env files and infra (docker-compose) are already up.
# Usage: ./scripts/dev-up.sh
set -e

SERVICES=(gateway identity payment wallet)

echo "==> Starting services (Ctrl+C to stop all)"
for s in "${SERVICES[@]}"; do
  (cd "services/$s" && mvn -q spring-boot:run) &
done

wait
