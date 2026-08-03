#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

docker compose down -v
docker compose up -d --wait neo4j
python3 queries.py

echo "Import complete. Neo4j Browser: http://localhost:7475"
