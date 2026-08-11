package org.example.biodwh2starter.integration;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

/** Creates and confirms the indexes needed by the source-node MERGE queries. */
public final class Neo4jSchemaInitializer {
    private static final String PROTEIN_INDEX =
            "CREATE INDEX trembl_protein_accession IF NOT EXISTS "
                    + "FOR (node:TrEMBL_Protein) ON (node.accession)";
    private static final String ORGANISM_INDEX =
            "CREATE INDEX trembl_organism_taxonomy_id IF NOT EXISTS "
                    + "FOR (node:TrEMBL_Organism) ON (node.taxonomy_id)";
    private static final String AWAIT_INDEXES = "CALL db.awaitIndexes(600)";

    private final Driver driver;

    public Neo4jSchemaInitializer(Driver driver) {
        this.driver = driver;
    }

    // #Implemented for 1 run{merge-property-indexes}
    public void ensureMergeIndexes() {
        try (Session session = driver.session()) {
            session.run(PROTEIN_INDEX).consume();
            session.run(ORGANISM_INDEX).consume();
            session.run(AWAIT_INDEXES).consume();
        }
    }
}
