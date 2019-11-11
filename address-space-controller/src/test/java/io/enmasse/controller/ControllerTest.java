/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;

public class ControllerTest {

    @Test
    public void testIsDeleted() {

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("foo")
                .withNamespace("bar")
                .endMetadata()
                .build();

        assertFalse(Controller.isDeleted(addressSpace));

        addressSpace = new AddressSpaceBuilder(addressSpace)
                .editOrNewMetadata()
                .withDeletionTimestamp(ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(UTC)))
                .endMetadata()
                .build();

        assertTrue(Controller.isDeleted(addressSpace));

    }

}
