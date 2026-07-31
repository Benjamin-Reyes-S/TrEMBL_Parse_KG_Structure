package org.example.biodwh2starter.uniprot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Only the subset of a UniProt entry needed by this pipeline. */
public final class Entry {
    private final Protein protein;
    private final List<Organism> organisms;
    private final List<Citation> citations;

    public Entry(Protein protein, List<Organism> organisms, List<Citation> citations) {
        this.protein = protein;
        this.organisms = Collections.unmodifiableList(new ArrayList<>(organisms));
        this.citations = Collections.unmodifiableList(new ArrayList<>(citations));
    }

    public Protein getProtein() { return protein; }
    public List<Organism> getOrganisms() { return organisms; }
    public List<Citation> getCitations() { return citations; }
}
