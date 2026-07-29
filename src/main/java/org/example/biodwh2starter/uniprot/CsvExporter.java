package org.example.biodwh2starter.uniprot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class CsvExporter {
    private final Path outputDirectory;

    public CsvExporter(Path outputDirectory) { this.outputDirectory = outputDirectory; }

    public void exportAll(ParseResult result) throws IOException {
        Files.createDirectories(outputDirectory);
        CsvGenerator.export(outputDirectory.resolve("proteins.csv"),
                Arrays.asList("accession", "name", "sequence"), result.getProteins(),
                protein -> Arrays.asList(protein.getAccession(), protein.getName(), protein.getSequence()));
        CsvGenerator.export(outputDirectory.resolve("organisms.csv"),
                Arrays.asList("taxonomy_id", "scientific_name", "common_name"), result.getOrganisms(),
                organism -> Arrays.asList(organism.getTaxonomyId(), organism.getScientificName(),
                        organism.getCommonName()));

        List<List<String>> edges = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry
                : result.getProteinOrganismMapping().entrySet()) {
            for (String organismId : entry.getValue()) {
                edges.add(Arrays.asList(entry.getKey(), organismId));
            }
        }
        CsvGenerator.export(outputDirectory.resolve("protein_organism_mapping.csv"),
                Arrays.asList("protein_accession", "organism_taxonomy_id"), edges, row -> row);
    }

    public void exportAll(List<Protein> proteins, java.util.Set<Organism> organisms,
            Map<String, List<String>> proteinOrganismMapping) throws IOException {
        Files.createDirectories(outputDirectory);
        CsvGenerator.export(outputDirectory.resolve("proteins.csv"),
                Arrays.asList("accession", "name", "sequence"), proteins,
                p -> Arrays.asList(p.getAccession(), p.getName(), p.getSequence()));
        CsvGenerator.export(outputDirectory.resolve("organisms.csv"),
                Arrays.asList("taxonomy_id", "scientific_name", "common_name"), organisms,
                o -> Arrays.asList(o.getTaxonomyId(), o.getScientificName(), o.getCommonName()));
        List<List<String>> edges = new ArrayList<>();
        proteinOrganismMapping.forEach((protein, ids) ->
                ids.forEach(id -> edges.add(Arrays.asList(protein, id))));
        CsvGenerator.export(outputDirectory.resolve("protein_organism_mapping.csv"),
                Arrays.asList("protein_accession", "organism_taxonomy_id"), edges, row -> row);
    }
}
