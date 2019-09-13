/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
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

        final Address resultAddress = HttpAddressServiceBase.setAddressDefaults("ns", createAddressSpace("address-space"), inputAddress, null);

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

        final Address resultAddress = HttpAddressServiceBase.setAddressDefaults("ns", createAddressSpace("address-space"), inputAddress, existing);

        assertNotNull(resultAddress.getMetadata());
        assertEquals("ns", resultAddress.getMetadata().getNamespace());

        assertNotNull(resultAddress.getSpec());
    }

    @Test
    public void testHandleNullMaps () {
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

        inputAddress.getMetadata().setAnnotations(null);
        inputAddress.getMetadata().setLabels(null);

        final Address resultAddress = HttpAddressServiceBase.setAddressDefaults("ns", createAddressSpace("address-space"), inputAddress, existing);

        assertNotNull(resultAddress.getMetadata());
        assertEquals("ns", resultAddress.getMetadata().getNamespace());

        assertNotNull(resultAddress.getSpec());
    }

    @Test
    public void testMergeMaps () {
        final Address existing = new AddressBuilder()
                .withNewMetadata()
                .withName("address-space.address")
                .withNamespace("ns")
                .addToAnnotations("foo1", "bar1")
                .addToAnnotations("foo2", "bar2")
                .endMetadata()
                .build();
        final Address inputAddress = new AddressBuilder()
                .withNewMetadata()
                .addToAnnotations("foo2", "bar2a")
                .addToAnnotations("foo3", "bar3")
                .endMetadata()
                .build();

        final Address resultAddress = HttpAddressServiceBase.setAddressDefaults("ns", createAddressSpace("address-space"), inputAddress, existing);

        assertNotNull(resultAddress.getMetadata());
        assertEquals("ns", resultAddress.getMetadata().getNamespace());

        assertThat(resultAddress.getMetadata().getAnnotations().size(), is(3));

        final Map<String,String> expectedAnnotations = new HashMap<>();
        expectedAnnotations.put("foo1", "bar1");
        expectedAnnotations.put("foo2", "bar2a");
        expectedAnnotations.put("foo3", "bar3");
        assertThat(resultAddress.getMetadata().getAnnotations(), is(expectedAnnotations));

        assertNotNull(resultAddress.getSpec());
    }

    private static AddressSpace createAddressSpace(String name) {
        return new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName(name)
                .endMetadata()
                .editOrNewSpec()
                .withType("standard")
                .withPlan("small")
                .endSpec()
                .build();
    }
}
