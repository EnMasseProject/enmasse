/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;

public class EqualityTest {

    /**
     * Ensure that after cloning an instance, the new instance does not influence the old one.
     */
    @Test
    public void testClone() {

        final AddressSpace s1 = new AddressSpaceBuilder()
                .withNewStatus()
                .withMessages("foo")
                .endStatus()
                .build();

        final AddressSpace s2 = new AddressSpaceBuilder(s1)
                .build();

        // right now they s1 and s2 must be equal

        assertFalse(ControllerChain.hasAddressSpaceChanged(s1, s2));
        assertFalse(ControllerChain.hasAddressSpaceChanged(s2, s1));

        // test to see if we have the messages

        assertEquals(Arrays.asList("foo"), s1.getStatus().getMessages());
        assertEquals(Arrays.asList("foo"), s2.getStatus().getMessages());

        // we make a change to s2

        s2.getStatus().clearMessages();

        // now s1 and s2 must no longer be equal

        assertTrue(ControllerChain.hasAddressSpaceChanged(s1, s2));
        assertTrue(ControllerChain.hasAddressSpaceChanged(s2, s1));

        // the lists must be different

        assertEquals(Arrays.asList("foo"), s1.getStatus().getMessages());
        assertEquals(Arrays.asList(), s2.getStatus().getMessages());
    }
}
