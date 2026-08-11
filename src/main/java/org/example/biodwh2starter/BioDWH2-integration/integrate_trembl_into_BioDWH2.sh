#!/usr/bin/env bash
set -euo pipefail

#Implemented for 1 run{launcher-independent}
SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE=(sudo docker compose --project-directory "$SCRIPT_DIRECTORY" -f "$SCRIPT_DIRECTORY/docker-compose.yml")

# Stop containers from a previous run. Bind-mounted workspace data is preserved.
"${COMPOSE[@]}" down -v

# Start the BioDWH2 Neo4j server and wait until its Bolt endpoint is healthy.
"${COMPOSE[@]}" up -d --wait biodwh2-neo4j

#Implemented for 1 run{importer-image-rebuild}
"${COMPOSE[@]}" --profile import run --build --rm trembl-importer
