package org.example.biodwh2starter.uniprot;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class CsvExporter {
    private final Path outputDirectory;

    public CsvExporter(Path outputDirectory) { this.outputDirectory = outputDirectory; }

    /**
     * Exports a large XML document with bounded heap usage. Protein and relationship
     * rows are written immediately. Entity rows are partitioned on disk and exactly
     * deduplicated one partition at a time.
     */
    public ExportStatistics exportStreaming(Path inputFile, XmlParser parser)
            throws IOException, javax.xml.stream.XMLStreamException {
        Files.createDirectories(outputDirectory);
        try (StreamingOutput output = new StreamingOutput(outputDirectory)) {
            parser.parseEntries(inputFile, output::write);
            output.finish();
            return output.statistics();
        }
    }

        // accepts results collection (protein + organism + mapping set)
    public void exportAll(ParseResult result) throws IOException {
        Files.createDirectories(outputDirectory);
        CsvGenerator.export(outputDirectory.resolve("proteins.csv"),
                Arrays.asList("accession", "name", "sequence"), result.getProteins(),
                protein -> Arrays.asList(protein.getAccession(), protein.getName(), protein.getSequence()));
        CsvGenerator.export(outputDirectory.resolve("organisms.csv"),
                Arrays.asList("taxonomy_id", "scientific_name", "common_name"), result.getOrganisms(),
                organism -> Arrays.asList(organism.getTaxonomyId(), organism.getScientificName(),
                        organism.getCommonName()));
        CsvGenerator.export(outputDirectory.resolve("citations.csv"),
                Arrays.asList("title_and_date", "authors", "db_references"),
                result.getCitations(), citation -> Arrays.asList(citation.getTitleAndDate(),
                        String.join("; ", citation.getAuthorList()),
                        String.join("; ", citation.getDbReferences())));

        List<List<String>> edges = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry
                : result.getProteinOrganismMapping().entrySet()) {
            for (String organismId : entry.getValue()) {
                edges.add(Arrays.asList(entry.getKey(), organismId));
            }
        }
        CsvGenerator.export(outputDirectory.resolve("protein_organism_mapping.csv"),
                Arrays.asList("protein_accession", "organism_taxonomy_id"), edges, row -> row);

        List<List<String>> citationEdges = new ArrayList<>();
        result.getProteinCitationMapping().forEach((protein, citationIds) ->
                citationIds.forEach(id -> citationEdges.add(Arrays.asList(protein, id))));
        CsvGenerator.export(outputDirectory.resolve("protein_citation_mapping.csv"),
                Arrays.asList("protein_accession", "citation_title_and_date"),
                citationEdges, row -> row);
    }

        // accepts separated collections ( protein, organisms, mappingset)
    public void exportAll(List<Protein> proteins, java.util.Set<Organism> organisms,
            Map<String, List<String>> proteinOrganismMapping) throws IOException {
        exportAll(proteins, organisms, proteinOrganismMapping,
                java.util.Collections.emptySet(), java.util.Collections.emptyMap());
    }

    public void exportAll(List<Protein> proteins, java.util.Set<Organism> organisms,
            Map<String, List<String>> proteinOrganismMapping,
            java.util.Set<Citation> citations,
            Map<String, List<String>> proteinCitationMapping) throws IOException {
        Files.createDirectories(outputDirectory);
        CsvGenerator.export(outputDirectory.resolve("proteins.csv"),
                Arrays.asList("accession", "name", "sequence"), proteins,
                p -> Arrays.asList(p.getAccession(), p.getName(), p.getSequence()));
        CsvGenerator.export(outputDirectory.resolve("organisms.csv"),
                Arrays.asList("taxonomy_id", "scientific_name", "common_name"), organisms,
                o -> Arrays.asList(o.getTaxonomyId(), o.getScientificName(), o.getCommonName()));
        CsvGenerator.export(outputDirectory.resolve("citations.csv"),
                Arrays.asList("title_and_date", "authors", "db_references"), citations,
                c -> Arrays.asList(c.getTitleAndDate(), String.join("; ", c.getAuthorList()),
                        String.join("; ", c.getDbReferences())));
        List<List<String>> edges = new ArrayList<>();
        proteinOrganismMapping.forEach((protein, ids) ->
                ids.forEach(id -> edges.add(Arrays.asList(protein, id))));
        CsvGenerator.export(outputDirectory.resolve("protein_organism_mapping.csv"),
                Arrays.asList("protein_accession", "organism_taxonomy_id"), edges, row -> row);
        List<List<String>> citationEdges = new ArrayList<>();
        proteinCitationMapping.forEach((protein, ids) ->
                ids.forEach(id -> citationEdges.add(Arrays.asList(protein, id))));
        CsvGenerator.export(outputDirectory.resolve("protein_citation_mapping.csv"),
                Arrays.asList("protein_accession", "citation_title_and_date"),
                citationEdges, row -> row);
    }

    public static final class ExportStatistics {
        private final long proteins;
        private final long organisms;
        private final long citations;

        private ExportStatistics(long proteins, long organisms, long citations) {
            this.proteins = proteins;
            this.organisms = organisms;
            this.citations = citations;
        }

        public long getProteins() { return proteins; }
        public long getOrganisms() { return organisms; }
        public long getCitations() { return citations; }
    }

    private static final class StreamingOutput implements AutoCloseable {
        private static final int PARTITION_COUNT = 256;
        private final Path outputDirectory;
        private final Path temporaryDirectory;
        private final BufferedWriter proteins;
        private final BufferedWriter proteinOrganisms;
        private final BufferedWriter proteinCitations;
        private final Map<Path, BufferedWriter> partitionWriters = new HashMap<>();
        private long proteinCount;
        private long organismCount;
        private long citationCount;
        private boolean finished;

        private StreamingOutput(Path outputDirectory) throws IOException {
            this.outputDirectory = outputDirectory;
            temporaryDirectory = outputDirectory.resolve(".dedup-" + UUID.randomUUID());
            Files.createDirectories(temporaryDirectory);
            proteins = open("proteins.csv", Arrays.asList("accession", "name", "sequence"));
            proteinOrganisms = open("protein_organism_mapping.csv",
                    Arrays.asList("protein_accession", "organism_taxonomy_id"));
            proteinCitations = open("protein_citation_mapping.csv",
                    Arrays.asList("protein_accession", "citation_title_and_date"));
        }

        private BufferedWriter open(String file, List<String> header) throws IOException {
            BufferedWriter writer = Files.newBufferedWriter(outputDirectory.resolve(file),
                    StandardCharsets.UTF_8);
            CsvGenerator.writeRow(writer, header);
            return writer;
        }

        private void write(Entry entry) throws IOException {
            Protein protein = entry.getProtein();
            if (protein == null) return;
            CsvGenerator.writeRow(proteins, Arrays.asList(protein.getAccession(),
                    protein.getName(), protein.getSequence()));
            proteinCount++;
            if (proteinCount % 1_000_000 == 0) {
                System.out.printf("Processed %,d proteins%n", proteinCount);
            }

            Set<String> entryOrganisms = new HashSet<>();
            for (Organism organism : entry.getOrganisms()) {
                if (!entryOrganisms.add(organism.getTaxonomyId())) continue;
                writePartition("organism", organism.getTaxonomyId(), Arrays.asList(
                        organism.getTaxonomyId(), organism.getScientificName(),
                        organism.getCommonName()));
                CsvGenerator.writeRow(proteinOrganisms,
                        Arrays.asList(protein.getAccession(), organism.getTaxonomyId()));
            }

            Set<String> entryCitations = new HashSet<>();
            for (Citation citation : entry.getCitations()) {
                String id = citation.getTitleAndDate();
                if (!entryCitations.add(id)) continue;
                writePartition("citation", id, Arrays.asList(id,
                        String.join("; ", citation.getAuthorList()),
                        String.join("; ", citation.getDbReferences())));
                CsvGenerator.writeRow(proteinCitations,
                        Arrays.asList(protein.getAccession(), id));
            }
        }

        private void writePartition(String type, String key, List<String> fields)
                throws IOException {
            int partition = (key.hashCode() & 0x7fffffff) % PARTITION_COUNT;
            Path path = temporaryDirectory.resolve(type + "-" + partition + ".part");
            BufferedWriter writer = partitionWriters.get(path);
            if (writer == null) {
                writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
                partitionWriters.put(path, writer);
            }
            writer.write(encode(key));
            for (String field : fields) {
                writer.write('\t');
                writer.write(encode(field));
            }
            writer.newLine();
        }

        private void finish() throws IOException {
            if (finished) return;
            closeDirectWriters();
            closePartitionWriters();
            organismCount = mergePartitions("organism", "organisms.csv",
                    Arrays.asList("taxonomy_id", "scientific_name", "common_name"));
            citationCount = mergePartitions("citation", "citations.csv",
                    Arrays.asList("title_and_date", "authors", "db_references"));
            finished = true;
            deleteTemporaryDirectory();
        }

        private long mergePartitions(String type, String outputFile, List<String> header)
                throws IOException {
            long count = 0;
            try (BufferedWriter output = Files.newBufferedWriter(
                    outputDirectory.resolve(outputFile), StandardCharsets.UTF_8)) {
                CsvGenerator.writeRow(output, header);
                for (int partition = 0; partition < PARTITION_COUNT; partition++) {
                    Path path = temporaryDirectory.resolve(type + "-" + partition + ".part");
                    if (!Files.exists(path)) continue;
                    Set<String> seen = new HashSet<>();
                    try (BufferedReader input = Files.newBufferedReader(path,
                            StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = input.readLine()) != null) {
                            String[] columns = line.split("\\t", -1);
                            String key = decode(columns[0]);
                            if (!seen.add(key)) continue;
                            List<String> row = new ArrayList<>(columns.length - 1);
                            for (int index = 1; index < columns.length; index++) {
                                row.add(decode(columns[index]));
                            }
                            CsvGenerator.writeRow(output, row);
                            count++;
                        }
                    }
                }
            }
            return count;
        }

        private static String encode(String value) {
            if (value == null) return "";
            return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decode(String value) {
            if (value.isEmpty()) return null;
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        }

        private ExportStatistics statistics() {
            return new ExportStatistics(proteinCount, organismCount, citationCount);
        }

        private void closeDirectWriters() throws IOException {
            proteins.close();
            proteinOrganisms.close();
            proteinCitations.close();
        }

        private void closePartitionWriters() throws IOException {
            IOException failure = null;
            for (BufferedWriter writer : partitionWriters.values()) {
                try {
                    writer.close();
                } catch (IOException exception) {
                    failure = exception;
                }
            }
            partitionWriters.clear();
            if (failure != null) throw failure;
        }

        private void deleteTemporaryDirectory() throws IOException {
            if (!Files.exists(temporaryDirectory)) return;
            try (Stream<Path> paths = Files.walk(temporaryDirectory)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    Files.deleteIfExists(path);
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (!finished) {
                IOException failure = null;
                try { closeDirectWriters(); } catch (IOException exception) { failure = exception; }
                try { closePartitionWriters(); } catch (IOException exception) { failure = exception; }
                try { deleteTemporaryDirectory(); } catch (IOException exception) { failure = exception; }
                if (failure != null) throw failure;
            }
        }
    }
}
