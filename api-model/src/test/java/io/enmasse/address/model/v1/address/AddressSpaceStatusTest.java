/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpaceStatus;
import io.enmasse.address.model.AddressSpaceStatusBuilder;

public class AddressSpaceStatusTest {

    @Test
    public void testEqual1 () {
        final AddressSpaceStatus s1 = new AddressSpaceStatusBuilder()
                .build();
        final AddressSpaceStatus s2 = new AddressSpaceStatusBuilder()
                .build();

        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
    }

    @Test
    public void testNotEqual1 () {
        final AddressSpaceStatus s1 = new AddressSpaceStatusBuilder()
                .withMessages("Foo")
                .build();
        final AddressSpaceStatus s2 = new AddressSpaceStatusBuilder()
                .build();

        assertFalse(s1.equals(s2));
        assertFalse(s2.equals(s1));
    }

}