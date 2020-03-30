/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;

public class StatementTest {

    @Test
    public void testValidateFields() {
        final Statement statement = Statement.statement("SELECT foo FROM bar WHERE baz=:baz");
        statement.validateParameters("baz");
    }

    @Test
    public void testValidateMissingField() {
        final Statement statement = Statement.statement("SELECT foo FROM bar WHERE baz=:baz");
        assertThrows(IllegalStateException.class, () -> {
            statement.validateParameters("bar");
        });
    }

    @Test
    public void testValidateAdditionalField() {
        final Statement statement = Statement.statement("SELECT foo FROM bar WHERE baz=:baz");
        statement.validateParameters("baz", "bar");
    }

    @Test
    public void testValidateAdditionalField2() {
        final Statement statement = Statement.statement("UPDATE devices\n" +
                "SET\n" +
                "   data=?::jsonb,\n" +
                "   version=?\n" +
                "WHERE\n" +
                "   tenant_id=?\n" +
                "AND\n" +
                "   device_id=?");
        statement.validateParameters("data", "device_id", "next_version", "tenant_id");
    }

    /**
     * Test that we didn't break basic YAML loading.
     */
    @Test
    public void testPlainYamlStillWorks() {
        final Yaml parser = StatementConfiguration.createYamlParser();
        parser.load("foo: bar");
    }

    /**
     * Test that we create a YAML parser which reject class instantiation.
     */
    @Test
    public void testObjectCreationRejected(@TempDir Path tempDir) {
        final Path markerFile = tempDir.resolve("testObjectCreationRejected.marker");
        final String yaml = "!!java.io.FileOutputStream [" + markerFile.toAbsolutePath().toString() + "]";

        assertThrows(ConstructorException.class, () -> {
            final Yaml parser = StatementConfiguration.createYamlParser();
            parser.load(yaml);
        });

        assertFalse(Files.isRegularFile(markerFile), "Marker file must not exist");
    }

    /**
     * Test that we actually make use of the parser, preventing class instantiation.
     */
    @Test
    public void testObjectCreationRejectedFull(@TempDir Path tempDir) {
        final Path markerFile = tempDir.resolve("testObjectCreationRejectedFull.marker");
        final String yaml = "!!java.io.FileOutputStream [" + markerFile.toAbsolutePath().toString() + "]";

        assertThrows(ConstructorException.class, () -> {
            var cfg = StatementConfiguration.empty();
            cfg.overrideWith(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), false);
        });

        assertFalse(Files.isRegularFile(markerFile), "Marker file must not exist");
    }

}
