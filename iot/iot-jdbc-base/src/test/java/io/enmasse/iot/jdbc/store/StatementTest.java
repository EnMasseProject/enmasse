/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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

}
