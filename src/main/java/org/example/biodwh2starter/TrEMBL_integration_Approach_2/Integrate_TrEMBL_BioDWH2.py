#!/usr/bin/env python3
"""Stream the TrEMBL CSV files into Neo4j with APOC."""

import json
import os
import sys
import urllib.error
import urllib.request


NEO4J_URL = os.getenv(
    "NEO4J_URL", "http://172.31.151.160:7479/db/neo4j/tx/commit"
)
IMPORTS = [
    (
        "protein instance nodes",
        "MATCH (concept:PROTEIN) UNWIND concept.ids AS id WITH concept, id "
        "WHERE id STARTS WITH 'UniProtKB:' "
        "RETURN concept, substring(id, size('UniProtKB:')) AS accession",
        "CREATE (i:INSTANCE_PROTEIN {accession: accession}) "
        "CREATE (i)-[:MAPPED_TO]->(concept)",
    ),
    (
        "taxon instance nodes",
        "MATCH (concept:TAXON) UNWIND concept.ids AS id WITH concept, id "
        "WHERE id STARTS WITH 'NCBITaxon:' "
        "RETURN concept, substring(id, size('NCBITaxon:')) AS taxonomy_id",
        "CREATE (i:INSTANCE_TAXON {taxonomy_id: taxonomy_id}) "
        "CREATE (i)-[:MAPPED_TO]->(concept)",
    ),
    (
        "protein-to-instance mapping",
        "MATCH (p:TrEMBL_Protein) RETURN p",
        "MATCH (i:INSTANCE_PROTEIN {accession: p.accession}) "
        "CREATE (p)-[:MAPPED_TO]->(i)",
    ),
    (
        "organism-to-instance mapping",
        "MATCH (o:TrEMBL_Organism) RETURN o",
        "MATCH (i:INSTANCE_TAXON {taxonomy_id: o.taxonomy_id}) "
        "CREATE (o)-[:MAPPED_TO]->(i)",
    ),
]


def run(statement: str, count = 0) -> dict:
    body = json.dumps({"statements": [{"statement": statement, "parameters": {"count": count}}]}).encode()
    request = urllib.request.Request(
        NEO4J_URL,
        data=body,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request) as response:
        result = json.load(response)
    if result.get("errors"):
        raise RuntimeError(json.dumps(result["errors"], indent=2))
    return result


def main() -> None:
    run("CREATE CONSTRAINT protein_accession IF NOT EXISTS "
        "FOR (p:TrEMBL_Protein) REQUIRE p.accession IS UNIQUE")
    run("CREATE CONSTRAINT organism_taxonomy_id IF NOT EXISTS "
        "FOR (o:TrEMBL_Organism) REQUIRE o.taxonomy_id IS UNIQUE")
    run("CREATE CONSTRAINT citation_title IF NOT EXISTS "
        "FOR (c:TrEMBL_Citation) REQUIRE c.title_and_date IS UNIQUE")
    run("CREATE CONSTRAINT instance_protein_accession IF NOT EXISTS "
        "FOR (i:INSTANCE_PROTEIN) REQUIRE i.accession IS UNIQUE")
    run("CREATE CONSTRAINT instance_taxon_taxonomy_id IF NOT EXISTS "
        "FOR (i:INSTANCE_TAXON) REQUIRE i.taxonomy_id IS UNIQUE")

    for name, source, action in IMPORTS:
        print(f"Importing {name}...", flush=True)
        protein_count_json = run("MATCH (p:TrEMBL_Protein) RETURN count(p) AS count")
        protein_count = protein_count_json["results"][0]["data"][0]["row"][0]
        print(f"  {protein_count} proteins in database", flush=True)
        organism_count_json = run("MATCH (o:TrEMBL_Organism) RETURN count(o) AS count")
        organism_count = organism_count_json["results"][0]["data"][0]["row"][0]
        print(f"  {organism_count} organisms in database", flush=True)
        citation_count_json = run("MATCH (c:TrEMBL_Citation) RETURN count(c) AS count")
        citation_count = citation_count_json["results"][0]["data"][0]["row"][0]
        print(f"  {citation_count} citations in database", flush=True)
        count = protein_count if "protein" in name else organism_count if "organism" in name else citation_count
        statement = (
            "CALL apoc.periodic.iterate("
            f"{json.dumps(source)}, {json.dumps(action)}, "
            "{batchSize: 10000, parallel: false, params: {count: $count}}) "
            "YIELD batches, total, failedBatches, errorMessages "
            "RETURN batches, total, failedBatches, errorMessages"
        )
        result = run(statement, count = count)
        row = result["results"][0]["data"][0]["row"]
        batches, total, failed_batches, errors = row
        print(f"  {total} rows in {batches} batches", flush=True)
        if failed_batches or errors:
            raise RuntimeError(f"Import failed: {errors}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, urllib.error.HTTPError) as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(1)
