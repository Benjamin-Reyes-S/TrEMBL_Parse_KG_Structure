package org.example.biodwh2starter.uniprot;

import java.util.Objects;

public final class Protein {
    private final String accession;
    private final String name;
    private final String sequence;

    public Protein(String accession, String name, String sequence) {
        this.accession = Objects.requireNonNull(accession, "accession");
        this.name = name;
        this.sequence = sequence;
    }

    public String getAccession() { return accession; }
    public String getName() { return name; }
    public String getSequence() { return sequence; }
}
