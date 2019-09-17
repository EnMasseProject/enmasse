/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static io.enmasse.address.model.validation.ValidationMatchers.isNotValid;
import static io.enmasse.address.model.validation.ValidationMatchers.isValid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressSpaceSpecConnectorAddressRuleTest {
    @Test
    public void testAddressRuleValidation() {
        assertTrue(validRule("a", "a"));
        assertTrue(validRule("a", "b"));
        assertTrue(validRule("a", "*"));
        assertTrue(validRule("a", "a/b"));
        assertTrue(validRule("a", "a/#"));
        assertTrue(validRule("a", "#/a"));
        assertTrue(validRule("a", "a/*"));
        assertTrue(validRule("a", "*/a"));
        assertTrue(validRule("a", "a/*/b"));
        assertTrue(validRule("a", "*/a/#"));
        assertTrue(validRule("a", "*/a/*/b"));

        assertFalse(validRule(null, "a"));
        assertFalse(validRule("", "a"));
        assertFalse(validRule("a/", "a"));
        assertFalse(validRule("a.", "a"));
        assertFalse(validRule("..", "a"));
        assertFalse(validRule("a", "/"));
        assertFalse(validRule("a", "/a"));
        assertFalse(validRule("a", "/*"));
        assertFalse(validRule("a", "*/"));
        assertFalse(validRule("a", "*//"));
        assertFalse(validRule("a", "//"));
    }

    private boolean validRule(String name, String pattern) {
        AddressSpaceSpecConnectorAddressRule c = new AddressSpaceSpecConnectorAddressRuleBuilder()
                .withName(name)
                .withPattern(pattern)
                .build();
        return isValid().matches(c);
    }
}
