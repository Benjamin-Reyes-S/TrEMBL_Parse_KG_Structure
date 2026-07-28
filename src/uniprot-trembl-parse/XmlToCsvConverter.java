



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
    void read(String[] args) {

        try (final GZIPInputStream stream =
                    FileUtils.openGzip(filePath.toString())) {

                LOGGER.info(
                    "Start streaming without features and citations for TrEMBL (0%)"
                );

                FileUtils.streamXmlList(stream, Entry.class, entry -> {
                    exportEntry(graph, entry);

                });
            };
            } catch (IOException | XMLStreamException e) {
                throw new ExporterFormatException(e);
    }
    // parse 
    parse(Entry entry ){
        Protein protein = new Protein();
        Set<Organism> organisms = new HashSet<>();
        Set<Citation> citations = new HashSet<>();
    
        
    }

    }


