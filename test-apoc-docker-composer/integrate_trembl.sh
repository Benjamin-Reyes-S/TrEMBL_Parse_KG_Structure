#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

export NEO4J_PASSWORD="${NEO4J_PASSWORD:-trembl-password}"

docker compose up -d --wait neo4j
python3 queries.py

echo "Import complete. Neo4j Browser: http://localhost:7475"
