# TrEMBL importer debugging execution map

This document describes the current implementation. It separates behavior directly visible in the code from performance and operational inferences. It does not prescribe a final BioDWH2 data model.

## Execution map

```text
integrate_trembl_into_BioDWH2.sh
  |
  +-- sudo docker compose down -v
  |
  +-- start biodwh2-neo4j and wait for Bolt health check
  |
  +-- rebuild and run trembl-importer
        |
        +-- TremblImporter.main()
              |
              +-- read environment and verify Bolt connectivity
              +-- create/await source-node indexes
              +-- load PROTEIN and TAXON concept identifier maps
              +-- stream organisms.csv into source/concept nodes
              +-- stream proteins.csv into source/concept nodes
              +-- stream protein_organism_mapping.csv into relationships
```

## Facts visible in the code

### Entry point and runtime

- The operational entry point is `integrate_trembl_into_BioDWH2.sh`.
- The launcher resolves its own directory, so it can be invoked from another working directory.
- All Compose commands run through `sudo`.
- It first runs `docker compose down -v`, starts only `biodwh2-neo4j`, and then runs the importer with `--build --rm` and the `import` profile.
- The importer container entry point is `org.example.biodwh2starter.integration.TremblImporter`.
- The importer JVM maximum heap is `-Xmx15g` in the current Dockerfile.
- The Java code compiles for Java 11 and uses Neo4j Java Driver 4.4.18 and Commons CSV 1.11.0.
- The Maven JAR and exec-plugin entry points still name `Application`; the Docker entry point bypasses those declarations and invokes `TremblImporter` explicitly.

### Docker Compose data and network configuration

- The BioDWH2 workspace `/home/benjamin.reyes/BioDWH2/workspaces/no_TrEMBL_test` is bind-mounted read/write at `/workspace`.
- The CSV directory is bind-mounted read-only at `/input`.
- The Neo4j server is started with `--start /workspace`, HTTP port 7476, and Bolt port 7689.
- Host exposure is restricted to `127.0.0.1`, while the importer reaches the server over the Compose network at `bolt://biodwh2-neo4j:7689`.
- The health check waits five minutes before counting failures, then permits 120 checks at five-second intervals.
- The importer batch size is configured as 1,000 rows.
- The Compose build context resolves to the repository root, while the inspected `.dockerignore` is inside `BioDWH2-integration`, not at the context root.
- `down -v` does not revert changes inside the bind-mounted `/workspace`; it only stops/removes Compose resources and Docker volumes.

### `TremblImporter`

- Reads `NEO4J_URI`, `NEO4J_USER`, `NEO4J_PASSWORD`, `CSV_INPUT_DIRECTORY`, and `IMPORT_BATCH_SIZE`.
- Uses no authentication when `NEO4J_USER` is empty.
- Verifies the CSV directory and Bolt connection.
- Calls stages in this fixed order:

  1. `Neo4jSchemaInitializer.ensureMergeIndexes()`
  2. `ConceptNodeIndex.loadAll()`
  3. organism entity processing with prefix `NCBITaxon:`
  4. protein entity processing with prefix `UniProtKB:`
  5. protein-to-organism relationship processing

### `Neo4jSchemaInitializer`

- Creates property indexes for:

  - `TrEMBL_Protein.accession`
  - `TrEMBL_Organism.taxonomy_id`

- These are ordinary indexes, not uniqueness constraints.
- Calls `db.awaitIndexes(600)` before importing entities.
- It does not create indexes on `PROTEIN.ids` or `TAXON.ids`; those concept properties are loaded by a full concept scan before CSV processing.

### `EntityType`

| CSV entity | CSV file | Source label | Concept label | Identifier | Name | Written CSV properties |
|---|---|---|---|---|---|---|
| Protein | `proteins.csv` | `TrEMBL_Protein` | `PROTEIN` | `accession` | `name` | `accession`, `name`, `sequence` |
| Organism | `organisms.csv` | `TrEMBL_Organism` | `TAXON` | `taxonomy_id` | `scientific_name` | `taxonomy_id`, `scientific_name`, `common_name` |

### `ConceptNodeIndex`

- Runs one query for each `EntityType` concept label.
- For each `PROTEIN` or `TAXON` concept, it unwinds every element of `ids`.
- Stores `identifier -> id(c)` in a `LinkedHashMap<String, Long>`.
- Uses `putIfAbsent`, so the first returned concept wins if the same identifier occurs on multiple concept nodes.
- Consumes the Neo4j result cursor incrementally rather than calling `.list()`.
- Both complete maps remain in RAM for all later stages.
- `ORDER BY neo4jId, identifier` requires Neo4j to return the complete result in that order.

### `CsvEntityProcessor`

- Opens each CSV with a buffered reader and Commons CSV.
- Validates that every configured property column exists in the header.
- Reads CSV records one at a time but retains up to `IMPORT_BATCH_SIZE` rows before writing.
- Builds `row.properties` from all configured, non-empty CSV values.
- Keeps the raw CSV identifier as `row.identifier` for the source node.
- Builds `row.conceptIdentifier` by concatenating the supplied namespace prefix and raw identifier.
- Checks `row.conceptIdentifier` in the in-memory concept map.
- Splits each batch into rows with an existing concept and unique rows requiring a new concept.

For an existing concept, the Cypher does the following:

1. `MERGE` the source node by its raw identifier property.
2. `SET source += row.properties`.
3. Match the concept node by numeric `id(c)`.
4. `MERGE` `source-[:MAPPED_TO]->concept`.

For a missing concept, it does the following:

1. `MERGE` and update the source node in the same way.
2. `CREATE` a concept node with `ids: [row.conceptIdentifier]`, a zero-or-one-element `names` array, and `__mapped: true`.
3. `MERGE` the source-to-concept relationship.
4. Return the new numeric ID and add it to the in-memory concept map.

Important property behavior:

- The implementation does not compare `row.properties` with current source-node properties.
- Every non-empty configured property is sent over Bolt and assigned with `SET +=` on every processed row.
- Same-named existing properties are overwritten; existing properties omitted from the map are preserved.
- CSV properties are not copied into an already-existing `PROTEIN` or `TAXON` concept node.
- Empty CSV values are not sent, so they do not clear existing source-node properties.

### `CsvRelationshipProcessor`

- Streams `protein_organism_mapping.csv` in batches.
- Each row carries only a protein accession and organism taxonomy ID.
- Matches `TrEMBL_Protein` and `TrEMBL_Organism` through the indexed properties created earlier.
- Merges a `MAPPED_TO` relationship between the two source nodes.
- Does not report rows whose endpoints were not found, because a failed `MATCH` simply produces no row.

### Outputs and persistent effects

- The importer does not generate a new output file.
- Its output is graph mutation in the bind-mounted BioDWH2 workspace:

  - `TrEMBL_Protein` source nodes
  - `TrEMBL_Organism` source nodes
  - missing `PROTEIN` and `TAXON` concepts
  - source-to-concept `MAPPED_TO` relationships
  - protein-to-organism `MAPPED_TO` relationships
  - two source-property indexes

- Console output contains existing identifier counts, periodic entity progress, and final row counts.
- There is no checkpoint file, rejected-row file, phase duration, rows/second measurement, or persisted resume cursor.

## Failure points visible in the code

| Stage | Failure condition | Observable consequence |
|---|---|---|
| Launcher | sudo or Docker daemon access unavailable | Stops before containers start |
| Compose build | build context or dependency/image download failure | Importer image is not produced |
| Server startup | workspace/JAR/path/port failure or slow startup beyond health allowance | `compose up --wait` fails |
| Main setup | invalid input directory, invalid batch-size text, Bolt/auth failure | Importer exits before schema/index loading |
| Schema | unsupported index syntax/procedure, permissions, or index wait timeout | Importer exits before concept loading |
| Concept load | missing labels/properties, incompatible `ids`, query error, or heap exhaustion | Importer exits before CSV writes |
| CSV parsing | missing file/header, malformed CSV, I/O failure | Current entity phase exits |
| Entity transaction | Cypher/type/schema/disk/transaction failure | Current transaction rolls back and importer exits |
| Relationship transaction | missing file/header, Cypher/I/O/transaction failure | Relationship phase exits |
| Data integrity | duplicate source identifiers | Ordinary indexes do not prevent duplicate nodes |
| Endpoint integrity | relationship endpoint absent | That mapping row creates no relationship and is not reported |

## Performance-sensitive properties and operations

### Facts from current files and configuration

| Input | Current size | Configured fields used |
|---|---:|---|
| `proteins.csv` | 65,569,770,074 bytes | accession, name, sequence |
| `organisms.csv` | 8,721,270 bytes | taxonomy ID, scientific name, common name |
| `protein_organism_mapping.csv` | 3,227,506,420 bytes | protein accession, taxonomy ID |

- The concept maps observed during the run contained 14,725,420 protein identifiers and 2,931,848 organism identifiers.
- Protein `sequence` is passed as a string in every protein row and stored on the source node.
- The source identifier is present both as the `MERGE` key and inside `row.properties`, so it is also included in `SET +=`.
- Each batch may execute two transactions/queries logically inside one write transaction: one for existing concepts and one for missing concepts.
- Each batch opens a new Neo4j session.
- Relationship rows do two indexed node matches and one relationship merge.

### Inferences requiring measurement

- Removing `sequence` for a debugging run is likely the largest property-related speedup. It should substantially reduce CSV parsing allocation, Java object size per batch, Bolt serialization and network traffic, Neo4j transaction payload, property-store writes, database growth, and garbage collection.
- The likely improvement is not safely expressible as one multiplier without measuring row counts, average sequence length, disk throughput, Neo4j page cache, and whether nodes already exist. The 65.6 GB protein file strongly suggests sequence text dominates input bytes, so the data-transfer/write portion of protein import could fall dramatically. Total elapsed time will fall by less because `MERGE`, concept matching, relationship creation, transaction commits, and concept-map loading remain.
- Removing only `name` is likely a modest saving because names are much smaller than sequences.
- Removing `accession` from `propertyColumns` would save very little payload; the accession must still be passed separately as the source-node `MERGE` key.
- Reducing organism properties is unlikely to materially shorten the full run because `organisms.csv` is only about 8.7 MB.
- Property count does not currently reduce a comparison cost: no per-property equality comparison is performed. The savings come from not parsing, retaining, serializing, and rewriting values.
- On a rerun, `SET +=` still rewrites configured values even if they are identical. A debug mode that omits large properties may therefore be much faster than a full idempotency test.
- The concept-index stage may remain expensive even with fewer CSV properties because it scans, unwinds, orders, transfers, and retains millions of identifiers independently of entity property selection.
- The relationship phase is unaffected by reducing entity properties, except indirectly through database size/cache pressure created during the earlier protein phase.
- The 15 GiB heap may or may not be sufficient for both retained maps plus driver buffers and entity batches. Cursor streaming removes the duplicate Java result list but not server-side sorting or the maps themselves.
- Because the build context is the repository root and no root `.dockerignore` was found, Docker may inspect/send large repository directories despite the nested ignore file. This can make each forced rebuild slow before Java starts.

## A practical debugging measurement matrix

These are experiments to consider, not behavior currently implemented:

| Run | Protein properties | Purpose |
|---|---|---|
| A | accession only as the required merge key; omit name/sequence writes | Measure graph/topology and Cypher baseline |
| B | accession + name | Measure cost of small descriptive properties |
| C | accession + name + sequence | Measure full property-transfer and storage cost |

For each phase, record wall time, processed rows, rows/second, importer heap, Neo4j heap/page cache, disk write rate, and database size before/after. Comparing A, B, and C is the reliable way to quantify time saved by reducing properties.

## Questions to answer when rested

### Correctness and graph semantics

1. Is `MAPPED_TO` the intended relationship label both for source-to-concept mappings and protein-to-organism source relationships?
2. Should protein-to-organism instead connect the `PROTEIN` concept to the `TAXON` concept, or use a different relationship such as `BELONGS_TO`?
3. Is numeric `id(c)` stable enough for the duration and deployment model, or should matching use another stable identifier?
4. Can one namespaced identifier legally occur on multiple concept nodes? If yes, is “first by numeric ID” the correct choice?
5. Are all protein identifiers exactly `UniProtKB:<accession>` and all taxonomy identifiers exactly `NCBITaxon:<taxonomy_id>`?
6. Is the minimal structure created for missing concepts valid for BioDWH2, including `ids`, `names`, and `__mapped` types?
7. Should existing concept names or IDs be enriched from the CSV, or must concepts remain untouched?
8. Are empty CSV values supposed to preserve old values, or clear them?
9. Should rerunning the importer overwrite a source name/sequence with the latest CSV value?

### Reliability and restart behavior

10. What is the authoritative pristine workspace, and how will the test workspace be reset after graph mutation?
11. Is partial progress acceptable when a later batch or phase fails?
12. Should the importer resume, restart idempotently, or restore a snapshot after failure?
13. How will missing relationship endpoints and rejected CSV records be counted and investigated?
14. Do you need uniqueness constraints after confirming no duplicate source nodes exist?

### Performance debugging

15. For the first debug run, do you need protein sequences at all, or only nodes and relationships?
16. What is the measured average protein row/sequence size and total protein row count?
17. How long does each phase take separately: schema, concept maps, organisms, proteins, and relationships?
18. What are importer heap usage, GC pauses, Neo4j memory, disk throughput, and database growth during the protein phase?
19. Is batch size 1,000 optimal for the actual server, or merely a safe starting value?
20. Does `ORDER BY` in the concept query provide a required semantic guarantee, or only deterministic duplicate selection?
21. Can protein and taxon maps be released separately or loaded closer to their processing phases without breaking later requirements?
22. Should `sequence` be deferred to a second enrichment pass after graph topology is verified?
23. Is the nested `.dockerignore` actually being honored for the root build context, and how large is the reported Docker build context?
24. What concrete success criteria define a useful first run: correct sample mappings, complete counts, acceptable duration, or full property fidelity?

