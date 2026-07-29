package org.example.biodwh2starter.uniprot;

import java.util.Objects;

public final class Organism {
    private final String taxonomyId;
    private final String scientificName;
    private final String commonName;

    public Organism(String taxonomyId, String scientificName, String commonName) {
        this.taxonomyId = Objects.requireNonNull(taxonomyId, "taxonomyId");
        this.scientificName = scientificName;
        this.commonName = commonName;
    }

    public String getTaxonomyId() { return taxonomyId; }
    public String getScientificName() { return scientificName; }
    public String getCommonName() { return commonName; }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Organism
                && taxonomyId.equals(((Organism) other).taxonomyId);
    }

    @Override
    public int hashCode() { return taxonomyId.hashCode(); }
}
