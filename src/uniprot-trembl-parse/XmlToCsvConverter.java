



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class XmlToCsvConverter {

    //constructor
    private XmlToCsvConverter() {
    }

    // unwrappe and read file
    void exportUniProtFile(
            final String fileName
    ) throws ExporterException {
        LOGGER.info("Starting UniProt XML export for '{}'", fileName);

        final File filePath = dataSource.resolveSourceFilePath(fileName).toFile();

        if (!filePath.exists()) {
            throw new ExporterException(
                    "Failed to parse the file '" + fileName + "'"
            );
        }
    }
    public class EntryReader {

        private final Path filePath;

        public EntryReader(Path filePath) {
            this.filePath = filePath;
        }

        // Reads the compressed XML file and passes each Entry to entryConsumer.

        public void read(Consumer<Entry> entryConsumer) {
            // collect the organisms and Proteins
            List<Protein> proteins= new ArrayList<>();
            Set<Organisms> organisms= new HashSet<>();
            //collect mapping ids
            Map<String, List<String>> proteinOrganismMapping = new HashMap<>();

            try (GZIPInputStream stream =
                        FileUtils.openGzip(filePath.toString())) {
                LOGGER.info("Start processing Entries");

                FileUtils.streamXmlList(
                        stream,
                        Entry.class,
                        entry -> {
                            if (entry != null) {
                                entryConsumer.accept(entry entry);
                                protein.parseProtein(Entry entry) -> {proteins //store object in proteins list
                                }
                                organism.parseOrganism(Entry entry) -> { organisms; //store objects in organims Hashset

                                    for (Organism organism : entry.getOrganisms()) {
                                        String proteinId = protein.getAccession();
                                        String organismId = organism.getDbReference().getId();

                                        proteinOrganismMapping
                                                .computeIfAbsent(proteinId, key -> new HashSet<>())
                                                .add(organismId);
                                    
                                }
                            }
                        }
                );

            } catch (IOException | XMLStreamException e) {
                throw new ExporterFormatException(
                        "Could not read XML file: " + filePath, e
                );
            }
        }
    }

    // parse 
    public List<ProteinCsvRow> transformProteins(
            Collection<? extends Protein> proteins) {

        return proteins.stream()
                .map(protein -> new ProteinCsvRow(
                        protein.getAccession(),
                        protein.getName(),
                        protein.getSequence()
                ))
                .toList();
    }

}



