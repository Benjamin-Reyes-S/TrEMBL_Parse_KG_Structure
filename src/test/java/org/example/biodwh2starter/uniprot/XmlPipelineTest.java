package org.example.biodwh2starter.uniprot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlPipelineTest {
    @TempDir Path temporaryDirectory;

    @Test
    void parsesEntryAndExportsAllCsvFiles() throws Exception {
        String xml = "<uniprot xmlns=\"https://uniprot.org/uniprot\"><entry>"
                + "<accession>P0C9F1</accession><name>1001R_ASFM2</name>"
                + "<protein><recommendedName><fullName>Protein MGF 100-1R</fullName>"
                + "</recommendedName></protein>"
                + "<organism><name type=\"scientific\">African swine fever virus</name>"
                + "<name type=\"common\">ASFV</name>"
                + "<dbReference type=\"NCBI Taxonomy\" id=\"10500\"/></organism>"
                + "<organismHost><name type=\"scientific\">Sus scrofa</name>"
                + "<name type=\"common\">Pig</name>"
                + "<dbReference type=\"NCBI Taxonomy\" id=\"9823\"/></organismHost>"
                + "<reference key=\"1\"><citation date=\"1990\">"
                + "<title>A cited paper.</title><authorList><person name=\"A. Author\"/>"
                + "<person name=\"B. Author\"/></authorList>"
                + "<dbReference type=\"PubMed\" id=\"1234\"/>"
                + "<dbReference type=\"DOI\" id=\"10.1/example\"/></citation>"
                + "<scope>SEQUENCE</scope></reference>"
                + "<sequence>MVRL FYNP\nIKYL</sequence></entry></uniprot>";
        Path input = temporaryDirectory.resolve("input.xml");
        Files.write(input, xml.getBytes(StandardCharsets.UTF_8));

        ParseResult result = new XmlParser().parse(input);

        assertEquals(1, result.getProteins().size());
        assertEquals("Protein MGF 100-1R", result.getProteins().get(0).getName());
        assertEquals("MVRLFYNPIKYL", result.getProteins().get(0).getSequence());
        assertEquals(2, result.getOrganisms().size());
        assertEquals(2, result.getProteinOrganismMapping().get("P0C9F1").size());
        assertEquals(1, result.getCitations().size());
        Citation citation = result.getCitations().iterator().next();
        assertEquals("A cited paper. (1990)", citation.getTitleAndDate());
        assertEquals(2, citation.getAuthorList().size());
        assertEquals(2, citation.getDbReferences().size());
        assertEquals("A cited paper. (1990)",
                result.getProteinCitationMapping().get("P0C9F1").get(0));

        Path output = temporaryDirectory.resolve("csv");
        new CsvExporter(output).exportAll(result);
        assertTrue(Files.readString(output.resolve("proteins.csv")).contains("P0C9F1"));
        assertEquals(3, Files.readAllLines(output.resolve("organisms.csv")).size());
        assertEquals(3, Files.readAllLines(output.resolve("protein_organism_mapping.csv")).size());
        assertEquals(2, Files.readAllLines(output.resolve("citations.csv")).size());
        assertEquals(2, Files.readAllLines(output.resolve("protein_citation_mapping.csv")).size());
    }

    @Test
    void commandLinePipelineWritesCsvFiles() throws Exception {
        Path input = temporaryDirectory.resolve("minimal.xml");
        Files.writeString(input, "<uniprot><entry><accession>P1</accession>"
                + "<protein><recommendedName><fullName>Test protein</fullName>"
                + "</recommendedName></protein><organism>"
                + "<name type=\"scientific\">Test organism</name>"
                + "<dbReference type=\"NCBI Taxonomy\" id=\"123\"/>"
                + "</organism><sequence>ABC</sequence></entry></uniprot>");
        Path output = temporaryDirectory.resolve("command-line-output");

        UniProtPipeline.main(new String[]{input.toString(), output.toString()});

        assertTrue(Files.isRegularFile(output.resolve("proteins.csv")));
        assertTrue(Files.isRegularFile(output.resolve("organisms.csv")));
        assertTrue(Files.isRegularFile(output.resolve("protein_organism_mapping.csv")));
        assertTrue(Files.isRegularFile(output.resolve("citations.csv")));
        assertTrue(Files.isRegularFile(output.resolve("protein_citation_mapping.csv")));
    }
}
