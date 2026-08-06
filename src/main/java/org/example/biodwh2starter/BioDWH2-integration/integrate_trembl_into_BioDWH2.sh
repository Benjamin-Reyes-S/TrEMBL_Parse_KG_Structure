set -euo pipefail


WORKSPACE= "/home/benjamin.reyes/TrEMBL_Parse_KG_Structure/workspace"
NEO4J_SEVER= "/home/benjamin.reyes/TrEMBL_Parse_KG_Structure/neo4j_server/BioDWH2-Neo4j-Server-v${NEO4J_VERSION}.jar"

NEO4J_VERSION="v${1:1.3.2}.jar"

java -jar "$NEO4J_SEVER" --start "$WORKSPACE"  -p 7476 -bp 7689