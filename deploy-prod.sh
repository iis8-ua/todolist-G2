#!/usr/bin/env bash
set -eo pipefail

cd "$(dirname "$0")"

NET=network-equipo02
DB=db-equipo02
APP=mads-todolist
IMG=israelizqdo9/mads-todolist-equipo02:1.3.0-snapshot
PORT=8080
SEED=${SEED:-sql/schema-1.2.0.sql}
MIGRATION=${MIGRATION:-}

./mvnw clean package
docker build -t "$IMG" .

docker network inspect "$NET" >/dev/null 2>&1 || docker network create "$NET"

if docker ps -a --format '{{.Names}}' | grep -qx "$DB"; then
    docker start "$DB" >/dev/null
else
    docker run -d --name "$DB" --network "$NET" --network-alias postgres -p 5432:5432 \
        -v "$PWD:/mi-host" -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads \
        -e POSTGRES_DB=mads postgres:13
fi

docker network connect --alias postgres "$NET" "$DB" 2>/dev/null || true

for i in {1..30}; do docker exec "$DB" pg_isready -U mads >/dev/null 2>&1 && break; sleep 1; done

docker exec "$DB" psql -U mads -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='mads'" | grep -q 1 || docker exec "$DB" createdb -U mads mads

TABLES=$(docker exec "$DB" psql -U mads -d mads -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'" | tr -d '[:space:]')
if [ "$TABLES" -eq 0 ]; then docker exec -i "$DB" psql -U mads -d mads < "$SEED"; fi

mkdir -p sql
docker exec "$DB" pg_dump -U mads --clean mads > "sql/backup$(date +%d%m%Y-%H%M%S).sql"

docker rm -f "$APP" >/dev/null 2>&1 || true

if [ -n "$MIGRATION" ]; then docker exec -i "$DB" psql -U mads -d mads < "$MIGRATION"; fi

docker run -d --name "$APP" --network "$NET" --restart unless-stopped -p "$PORT:8080" "$IMG" --spring.profiles.active=postgres-prod --POSTGRES_HOST=postgres

for i in {1..30}; do curl -fsS "http://localhost:$PORT/" >/dev/null 2>&1 && break; sleep 2; done

docker logs --tail 30 "$APP"
echo "http://localhost:$PORT"
