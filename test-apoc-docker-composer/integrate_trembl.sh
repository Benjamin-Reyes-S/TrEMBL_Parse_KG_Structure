#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIRECTORY="$(cd -- "$SCRIPT_DIRECTORY/.." && pwd)"
CSV_IMPORT_DIRECTORY="${1:-$PROJECT_DIRECTORY/output-csv}"

if [[ $# -gt 1 ]]; then
    echo "Usage: $0 [csv-output-directory]" >&2
    exit 2
fi

if [[ ! -d "$CSV_IMPORT_DIRECTORY" ]]; then
    echo "CSV output directory not found: $CSV_IMPORT_DIRECTORY" >&2
    exit 2
fi

# Compose resolves bind mounts most reliably when given an absolute path.
CSV_IMPORT_DIRECTORY="$(cd -- "$CSV_IMPORT_DIRECTORY" && pwd)"
export CSV_IMPORT_DIRECTORY

required_csv_files=(
    proteins.csv
    organisms.csv
    citations.csv
    protein_organism_mapping.csv
    protein_citation_mapping.csv
)

for csv_file in "${required_csv_files[@]}"; do
    if [[ ! -s "$CSV_IMPORT_DIRECTORY/$csv_file" ]]; then
        echo "Required CSV file is missing or empty: $CSV_IMPORT_DIRECTORY/$csv_file" >&2
        exit 2
    fi
done

cd "$SCRIPT_DIRECTORY"

echo "Importing the completed pipeline output from: $CSV_IMPORT_DIRECTORY"
echo "Recreating the Neo4j data volume to ensure a clean import..."

docker compose down -v
docker compose up -d --wait neo4j
python3 queries.py

echo "Import complete. Neo4j Browser: http://localhost:7475"
