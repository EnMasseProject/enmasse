/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.AddressSpecForwarder;
import io.enmasse.address.model.AddressSpecForwarderBuilder;
import io.enmasse.address.model.AddressSpecForwarderDirection;
import org.junit.jupiter.api.Test;

import static io.enmasse.address.model.validation.ValidationMatchers.isNotValid;
import static io.enmasse.address.model.validation.ValidationMatchers.isValid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressSpecForwarderTest {
    @Test
    public void testForwarderValidation() {
        AddressSpecForwarder forwarder = new AddressSpecForwarderBuilder()
                .withName("c1")
                .build();
        assertThat(forwarder, isNotValid());

        assertFalse(validForwarder("c", null, null));
        assertFalse(validForwarder("c", AddressSpecForwarderDirection.in, null));
        assertTrue(validForwarder("c", AddressSpecForwarderDirection.in, "r1"));
        assertTrue(validForwarder("c1", AddressSpecForwarderDirection.in, "r1"));

        assertFalse(validForwarder("", AddressSpecForwarderDirection.in, "r1"));
        assertFalse(validForwarder(".c", AddressSpecForwarderDirection.in, "r1"));
        assertFalse(validForwarder("c.", AddressSpecForwarderDirection.in, "r1"));
        assertFalse(validForwarder("c/", AddressSpecForwarderDirection.in, "r1"));
        assertFalse(validForwarder("/c", AddressSpecForwarderDirection.in, "r1"));
        assertFalse(validForwarder("c*", AddressSpecForwarderDirection.in, "r1"));
        assertFalse(validForwarder("c", AddressSpecForwarderDirection.in, ""));
    }

    private boolean validForwarder(String name, AddressSpecForwarderDirection dir, String remoteAddress) {
        AddressSpecForwarder c = new AddressSpecForwarderBuilder()
                .withName(name)
                .withDirection(dir)
                .withRemoteAddress(remoteAddress)
                .build();
        return isValid().matches(c);
    }
}
