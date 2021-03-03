/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.common.model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;

public class CustomResourceWithAdditionalPropertiesTest {

    @Test
    public void testPutAnnotation() {
        final Address address = new AddressBuilder()
                .build();

        assertNull(address.getAnnotation("foo"));
        address.putAnnotation("foo", "bar");

        assertEquals(address.getAnnotation("foo"), "bar");

        assertEquals(address.getAnnotation("foo"), "bar");
    }
}
