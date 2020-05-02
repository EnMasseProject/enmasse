/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class ResultWriter {
    private final Path rootDir;

    public ResultWriter(Path rootDir) {
        this.rootDir = rootDir;
    }

    public void write(Map<String, Object> result) throws Exception {
        saveResultsFile("results.json", result);
        savePlotCSV("plot.dat", result);
    }

    private void saveResultsFile(String filename, Map<String, Object> result) throws Exception {
        var mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
        Files.createDirectories(rootDir);
        Files.write(rootDir.resolve(filename), mapper.writeValueAsBytes(result));
    }

    private void savePlotCSV(String fileName, Map<String, Object> data) throws IOException {
        Files.createDirectories(rootDir);
        Path file = rootDir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            writer.write(String.join(",", data.keySet().toArray(new String[0])));
            writer.newLine();
            writer.write(String.join(",", data.values().stream().map(value -> value.toString().replaceAll("\\D+", "")).toArray(String[]::new)));
            writer.newLine();
        }
    }
}
