#!/usr/bin/env bash
set -euo pipefail

# Change these defaults to the paths you normally use.
INPUT_FILE="/home/benjamin.reyes/git/TrEMBL_Parsing/uniprot_trembl.xml.gz"
OUTPUT_DIRECTORY="output-csv"

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

SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if [[ ! -f "$INPUT_FILE" ]]; then
    echo "Input file not found: $INPUT_FILE" >&2
    echo "Edit INPUT_FILE in run-pipeline.sh or pass the file as the first argument." >&2
    exit 2
fi

cd "$SCRIPT_DIRECTORY"
./mvnw -q -DskipTests package
java -Xms512m -Xmx6g \
    -Djdk.xml.maxGeneralEntitySizeLimit=0 \
    -Djdk.xml.totalEntitySizeLimit=0 \
    -cp target/classes \
    org.example.biodwh2starter.uniprot.UniProtPipeline \
    "$INPUT_FILE" "$OUTPUT_DIRECTORY"
