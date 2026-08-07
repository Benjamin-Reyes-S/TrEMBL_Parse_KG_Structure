set -euo pipefail

#take remaining docker open down
docker compose down -v

#start docker compose for neo4j server
docker compose up -d --wait biodwh2-neo4j
#start docker compose for trembl integration
docker compose --profile import run --rm trembl-importer