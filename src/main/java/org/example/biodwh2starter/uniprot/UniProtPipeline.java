package org.example.biodwh2starter.uniprot;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Command-line entry point for the UniProt XML-to-CSV pipeline. */
public final class UniProtPipeline {
    private UniProtPipeline() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: UniProtPipeline <input.xml|input.xml.gz> [output-directory]");
            System.exit(2);
        }

        Path inputFile = Paths.get(args[0]).toAbsolutePath().normalize();
        Path outputDirectory = args.length == 2
                ? Paths.get(args[1]).toAbsolutePath().normalize()
                : Paths.get("output-csv").toAbsolutePath().normalize();

        if (!Files.isRegularFile(inputFile)) {
            System.err.println("Input file does not exist or is not a regular file: " + inputFile);
            System.exit(2);
        }

        //main block calling the XmlParser and the CsvExporter

        try {
            System.out.println("Parsing " + inputFile);
        
            ParseResult result = new XmlParser().parse(inputFile);
            new CsvExporter(outputDirectory).exportAll(result);
            System.out.printf(
                    "Finished: %d proteins, %d organisms, CSV files written to %s%n",
                    result.getProteins().size(), result.getOrganisms().size(), outputDirectory);
        } catch (IOException | XMLStreamException exception) {
            System.err.println("Pipeline failed: " + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
