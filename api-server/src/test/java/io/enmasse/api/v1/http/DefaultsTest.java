/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;

public class DefaultsTest {

    @Test
    public void test1() {
        final Address inputAddress = new AddressBuilder()
                .withNewMetadata()
                // leave out: .withAddressSpace(...)
                .endMetadata()

                .withNewSpec()
                .withAddress("address")
                // leave out: .withAddressSpace(...)
                .endSpec()

                .build();

        final Address resultAddress = HttpAddressServiceBase.setAddressDefaults("ns", "address-space", inputAddress, null);

        assertNotNull(resultAddress.getMetadata());
        assertEquals("ns", resultAddress.getMetadata().getNamespace());

        assertNotNull(resultAddress.getSpec());
        assertEquals("address", resultAddress.getSpec().getAddress());
        assertEquals("address-space", Address.extractAddressSpace(resultAddress));
    }

    @Test
    public void test2() {
        final Address existing = new AddressBuilder()
                .withNewMetadata()
                .withName("address-space.address")
                .withNamespace("ns")
                .endMetadata()
                .build();
        final Address inputAddress = new AddressBuilder()
                .withNewMetadata()
                .endMetadata()
                .build();

        final Address resultAddress = HttpAddressServiceBase.setAddressDefaults("ns", "address-space", inputAddress, existing);

        assertNotNull(resultAddress.getMetadata());
        assertEquals("ns", resultAddress.getMetadata().getNamespace());

        assertNotNull(resultAddress.getSpec());
    }

}
