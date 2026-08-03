#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TREMBL_URL="https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_trembl.xml.gz"
INPUT_FILE="$SCRIPT_DIRECTORY/INPUT_FILE/uniprot_trembl.xml.gz"
OUTPUT_DIRECTORY="$SCRIPT_DIRECTORY/output-csv"

# Optional arguments override the defaults above:
#   ./run-pipeline.sh /data/uniprot_trembl.xml.gz /data/csv-output
if [[ $# -ge 1 ]]; then
    INPUT_FILE="$1"
fi
if [[ $# -ge 2 ]]; then
    OUTPUT_DIRECTORY="$2"
fi
if [[ $# -gt 2 ]]; then
    echo "Usage: $0 [input.xml.gz] [output-directory]" >&2
    exit 2
fi

if [[ "$INPUT_FILE" != /* ]]; then
    INPUT_FILE="$(pwd)/$INPUT_FILE"
fi
if [[ "$OUTPUT_DIRECTORY" != /* ]]; then
    OUTPUT_DIRECTORY="$(pwd)/$OUTPUT_DIRECTORY"
fi

mkdir -p "$(dirname "$INPUT_FILE")"
wget --continue "$TREMBL_URL" --output-document="$INPUT_FILE"

cd "$SCRIPT_DIRECTORY"
./mvnw -q -DskipTests package
java -Xms512m -Xmx6g \
    -Djdk.xml.maxGeneralEntitySizeLimit=0 \
    -Djdk.xml.totalEntitySizeLimit=0 \
    -cp target/classes \
    org.example.biodwh2starter.uniprot.UniProtPipeline \
    "$INPUT_FILE" "$OUTPUT_DIRECTORY"

#run docker compose to load the csv files into neo4j
CSV_IMPORT_DIRECTORY="$OUTPUT_DIRECTORY" \
    "$SCRIPT_DIRECTORY/test-apoc-docker-composer/integrate_trembl.sh"
