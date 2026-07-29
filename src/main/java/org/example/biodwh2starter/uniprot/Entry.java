package org.example.biodwh2starter.uniprot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Only the subset of a UniProt entry needed by this pipeline. */
public final class Entry {
    private final Protein protein;
    private final List<Organism> organisms;

    public Entry(Protein protein, List<Organism> organisms) {
        this.protein = protein;
        this.organisms = Collections.unmodifiableList(new ArrayList<>(organisms));
    }

    public Protein getProtein() { return protein; }
    public List<Organism> getOrganisms() { return organisms; }
}
