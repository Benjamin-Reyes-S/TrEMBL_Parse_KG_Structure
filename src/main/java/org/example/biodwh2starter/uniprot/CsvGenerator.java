package org.example.biodwh2starter.uniprot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class CsvGenerator {
    private CsvGenerator() { }

    public static <T> void export(Path outputFile, List<String> headers,
            Collection<T> objects, Function<T, List<String>> rowMapper) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writeRow(writer, headers);
            for (T object : objects) writeRow(writer, rowMapper.apply(object));
        }
    }

    static void writeRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) writer.write(',');
            String value = values.get(index);
            if (value != null) writer.write('"' + value.replace("\"", "\"\"") + '"');
        }
        writer.newLine();
    }
}
