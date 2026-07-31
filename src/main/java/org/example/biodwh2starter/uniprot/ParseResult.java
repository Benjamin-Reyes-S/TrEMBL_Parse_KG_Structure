package org.example.biodwh2starter.uniprot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ParseResult {
    private final List<Protein> proteins = new ArrayList<>();
    private final Set<Organism> organisms = new LinkedHashSet<>();
    private final Set<Citation> citations= new LinkedHashSet<>();
    private final Map<String, List<String>> proteinOrganismMapping = new LinkedHashMap<>();
    private final Map<String, List<String>> proteinCitationMapping = new LinkedHashMap <>();

    void add(Entry entry) {
        Protein protein = entry.getProtein();
        if (protein == null) return;
        proteins.add(protein);

        List<String> organismIds = proteinOrganismMapping.computeIfAbsent(
                protein.getAccession(), ignored -> new ArrayList<>());
        for (Organism organism : entry.getOrganisms()) {
            organisms.add(organism);
            if (!organismIds.contains(organism.getTaxonomyId())) {
                organismIds.add(organism.getTaxonomyId());
            }
        }
        List<String> citationIds = proteinCitationMapping.computeIfAbsent(
                protein.getAccession(), ignored -> new ArrayList<>());
        for (Citation citation : entry.getCitations()) {
            citations.add(citation);
            if (!citationIds.contains(citation.getTitleAndDate())) {
                citationIds.add(citation.getTitleAndDate());
            }
        }
    }

    public List<Protein> getProteins() {
        return Collections.unmodifiableList(proteins);
    }

    public Set<Organism> getOrganisms() {
        return Collections.unmodifiableSet(organisms);
    }

    public Set<Citation> getCitations(){
        return Collections.unmodifiableSet(citations);

    }

    public Map<String, List<String>> getProteinOrganismMapping() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        proteinOrganismMapping.forEach((key, value) ->
                copy.put(key, Collections.unmodifiableList(value)));
        return Collections.unmodifiableMap(copy);
    }
    public Map<String, List<String>> getProteinCitationMapping() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        proteinCitationMapping.forEach((key, value) ->
                copy.put(key, Collections.unmodifiableList(value)));
        return Collections.unmodifiableMap(copy);
    }
}
