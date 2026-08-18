#!/bin/sh
set -e
python Add_TrEMBL.py
python Integrate_TrEMBL_BioDWH2.py

#docker compose -f /home/benjamin.reyes/TrEMBL_Parse_KG_Structure/src/main/java/org/example/biodwh2starter/TrEMBL_integration_Approach_2/docker-compose.yml up -d --wait biodwh2-neo4j
#docker compose -f /home/benjamin.reyes/TrEMBL_Parse_KG_Structure/src/main/java/org/example/biodwh2starter/TrEMBL_integration_Approach_2/docker-compose.yml --profile import run --build --rm trembl-importer
