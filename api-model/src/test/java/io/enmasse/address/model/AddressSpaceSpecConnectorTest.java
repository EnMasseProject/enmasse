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

public class AddressSpaceSpecConnectorTest {
    @Test
    public void testConnectorValidation() {
        AddressSpaceSpecConnector connector = new AddressSpaceSpecConnectorBuilder()
                .withName("c1")
                .build();
        assertThat(connector, isNotValid());

        connector = new AddressSpaceSpecConnectorBuilder()
                .withName("c1")
                .withEndpointHosts(new ArrayList<>())
                .build();
        assertThat(connector, isNotValid());


        connector = new AddressSpaceSpecConnectorBuilder()
                .withName("c1")
                .addNewEndpointHost()
                    .withHost("example.com")
                    .endEndpointHost()
                .build();
        assertThat(connector, isValid());

        assertTrue(validConnectorName("a"));
        assertTrue(validConnectorName("1"));
        assertTrue(validConnectorName("a1"));
        assertFalse(validConnectorName(null));
        assertFalse(validConnectorName(""));
        assertFalse(validConnectorName("a1."));
        assertFalse(validConnectorName("a1.."));
        assertFalse(validConnectorName("a1/.."));
    }

    private boolean validConnectorName(String name) {
        AddressSpaceSpecConnector c = new AddressSpaceSpecConnectorBuilder()
                .withName(name)
                .addNewEndpointHost()
                .withHost("example.com")
                .endEndpointHost()
                .build();
        return isValid().matches(c);
    }
}
