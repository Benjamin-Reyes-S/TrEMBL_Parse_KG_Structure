#!/usr/bin/env python3
"""Stream the TrEMBL CSV files into Neo4j with APOC."""

import json
import os
import sys
import urllib.error
import urllib.request


NEO4J_URL = os.getenv(
    "NEO4J_URL", "http://localhost:7475/db/neo4j/tx/commit"
)
IMPORTS = [
    (
        "proteins",
        "LOAD CSV WITH HEADERS FROM 'file:///proteins.csv' AS row RETURN row",
        "CREATE (p:Protein {accession: row.accession}) "
        "SET p.name = row.name, p.sequence = row.sequence",
    ),
    (
        "organisms",
        "LOAD CSV WITH HEADERS FROM 'file:///organisms.csv' AS row RETURN row",
        "CREATE (o:Organism {taxonomy_id: row.taxonomy_id}) "
        "SET o.scientific_name = row.scientific_name, o.common_name = row.common_name",
    ),
    (
        "citations",
        "LOAD CSV WITH HEADERS FROM 'file:///citations.csv' AS row RETURN row",
        "CREATE (c:Citation {title_and_date: row.title_and_date}) "
        "SET c.authors = row.authors, c.db_references = row.db_references",
    ),
    (
        "protein-to-organism relationships",
        "LOAD CSV WITH HEADERS FROM 'file:///protein_organism_mapping.csv' AS row "
        "RETURN row",
        "MATCH (p:Protein {accession: row.protein_accession}) "
        "MATCH (o:Organism {taxonomy_id: row.organism_taxonomy_id}) "
        "CREATE (p)-[:MAPPED_TO]->(o)",
    ),
    (
        "protein-to-citation relationships",
        "LOAD CSV WITH HEADERS FROM 'file:///protein_citation_mapping.csv' AS row "
        "RETURN row",
        "MATCH (p:Protein {accession: row.protein_accession}) "
        "MATCH (c:Citation {title_and_date: row.citation_title_and_date}) "
        "CREATE (p)-[:MAPPED_TO]->(c)",
    ),
]


def run(statement: str) -> dict:
    body = json.dumps({"statements": [{"statement": statement}]}).encode()
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
        "FOR (p:Protein) REQUIRE p.accession IS UNIQUE")
    run("CREATE CONSTRAINT organism_taxonomy_id IF NOT EXISTS "
        "FOR (o:Organism) REQUIRE o.taxonomy_id IS UNIQUE")
    run("CREATE CONSTRAINT citation_title IF NOT EXISTS "
        "FOR (c:Citation) REQUIRE c.title_and_date IS UNIQUE")

    for name, source, action in IMPORTS:
        print(f"Importing {name}...", flush=True)
        statement = (
            "CALL apoc.periodic.iterate("
            f"{json.dumps(source)}, {json.dumps(action)}, "
            "{batchSize: 10000, parallel: false}) "
            "YIELD batches, total, failedBatches, errorMessages "
            "RETURN batches, total, failedBatches, errorMessages"
        )
        result = run(statement)
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
