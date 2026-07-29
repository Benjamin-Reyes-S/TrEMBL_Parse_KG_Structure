package org.example.biodwh2starter.uniprot;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/** Streaming UniProt XML parser; it never loads the complete document in memory. */
public final class XmlParser {
    public ParseResult parse(Path inputFile) throws IOException, XMLStreamException {
        ParseResult result = new ParseResult();
        try (InputStream input = open(inputFile)) {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT
                            && "entry".equals(reader.getLocalName())) {
                        result.add(readEntry(reader));
                    }
                }
            } finally {
                reader.close();
            }
        }
        return result;
    }

    private Entry readEntry(XMLStreamReader reader) throws XMLStreamException {
        String accession = null;
        String entryName = null;
        String proteinName = null;
        String sequence = null;
        List<Organism> organisms = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String element = reader.getLocalName();
                if ("accession".equals(element) && accession == null) {
                    accession = reader.getElementText().trim();
                } else if ("name".equals(element) && entryName == null) {
                    entryName = reader.getElementText().trim();
                } else if ("protein".equals(element)) {
                    proteinName = readProteinName(reader);
                } else if ("organism".equals(element) || "organismHost".equals(element)) {
                    Organism organism = readOrganism(reader, element);
                    if (organism != null) organisms.add(organism);
                } else if ("sequence".equals(element)) {
                    sequence = reader.getElementText().replaceAll("\\s+", "");
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "entry".equals(reader.getLocalName())) {
                break;
            }
        }
        Protein protein = accession == null ? null
                : new Protein(accession, proteinName == null ? entryName : proteinName, sequence);
        return new Entry(protein, organisms);
    }

    private String readProteinName(XMLStreamReader reader) throws XMLStreamException {
        int depth = 1;
        String fullName = null;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
                if ("fullName".equals(reader.getLocalName()) && fullName == null) {
                    fullName = reader.getElementText().trim();
                    depth--;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) depth--;
        }
        return fullName;
    }

    private Organism readOrganism(XMLStreamReader reader, String container)
            throws XMLStreamException {
        String scientific = null;
        String common = null;
        String taxonomyId = null;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("name".equals(reader.getLocalName())) {
                    String type = reader.getAttributeValue(null, "type");
                    String value = reader.getElementText().trim();
                    if ("scientific".equals(type)) scientific = value;
                    else if ("common".equals(type)) common = value;
                } else if ("dbReference".equals(reader.getLocalName())
                        && "NCBI Taxonomy".equals(reader.getAttributeValue(null, "type"))) {
                    taxonomyId = reader.getAttributeValue(null, "id");
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && container.equals(reader.getLocalName())) break;
        }
        return taxonomyId == null ? null : new Organism(taxonomyId, scientific, common);
    }

    private InputStream open(Path inputFile) throws IOException {
        BufferedInputStream input = new BufferedInputStream(Files.newInputStream(inputFile));
        input.mark(2);
        int first = input.read();
        int second = input.read();
        input.reset();
        if ((first == 0x1f && second == 0x8b)
                || inputFile.toString().toLowerCase(Locale.ROOT).endsWith(".gz")) {
            return new GZIPInputStream(input);
        }
        return input;
    }
}
