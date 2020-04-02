/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import static java.nio.file.Files.isRegularFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
     * Test that we didn't break basic YAML with out override method.
     */
    @Test
    public void testPlainYamlStillWorksForStatementConfig() {

        String yaml = "read: SELECT 1";

        var cfg = StatementConfiguration.empty()
                .overrideWith(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), false);

        assertEquals("SELECT 1", cfg.getRequiredStatment("read").expand().getSql());

    }

    /**
     * The creating a file via the value of a map item.
     */
    @Test
    public void testObjectCreationRejectedMapValue(@TempDir Path tempDir) {
        final Path markerFile = tempDir.resolve("testObjectCreationRejectedMapValue.marker");
        final String yaml = "read: !!java.io.FileOutputStream [" + markerFile.toAbsolutePath().toString() + "]";

        assertNoMarkerFile(markerFile, yaml);
    }

    /**
     * The creating a file via a plain value.
     */
    @Test
    public void testObjectCreationRejectedPlainValue(@TempDir Path tempDir) {
        final Path markerFile = tempDir.resolve("testObjectCreationRejectedPlainValue.marker");
        final String yaml = "!!java.io.FileOutputStream [" + markerFile.toAbsolutePath().toString() + "]";

        assertNoMarkerFile(markerFile, yaml);
    }

    private void assertNoMarkerFile(final Path markerFile, final String yaml) {
        Exception expected = null;
        try {
            var cfg = StatementConfiguration.empty();
            cfg.overrideWith(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), false);
        } catch (Exception e) {
            // delay test for later
            expected = e;
        }

        assertFalse(isRegularFile(markerFile), "Marker file must not exist");
        assertNotNull(expected);
        assertThat(expected, instanceOf(ConstructorException.class));
    }

}
