/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class ConfigMapAddressApiTest {
    private static final String ADDRESS = "myaddress";
    private static final String ADDRESS_TYPE = "mytype";
    private static final String ADDRESS_PLAN = "myplan";
    private static final String ADDRESS_SPACE_NAMESPACE = "myproject";
    private static final String ADDRESS_SPACE = "myspace";
    private static final String ADDRESS_NAME = String.format("%s.%s", ADDRESS_SPACE, ADDRESS);

    private OpenShiftServer openShiftServer = new OpenShiftServer(false, true);
    private AddressApi api;

    @BeforeEach
    void setUp() {
        openShiftServer.before();
        NamespacedOpenShiftClient client = openShiftServer.getOpenshiftClient();
        api = new ConfigMapAddressApi(client, UUID.randomUUID().toString());
    }

    @AfterEach
    void tearDown() {
        openShiftServer.after();
    }

    @Test
    void create() {
        Address address = createAddress(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);

        api.createAddress(address);
        Optional<Address> readAddress = api.getAddressWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);

        assertTrue(readAddress.isPresent());
        Address read = readAddress.get();

        assertEquals(ADDRESS, read.getAddress());
        assertEquals(ADDRESS_SPACE, read.getAddressSpace());
        assertEquals(ADDRESS_NAME, read.getName());
        assertEquals(ADDRESS_TYPE, read.getType());
        assertEquals(ADDRESS_PLAN, read.getPlan());
    }

    @Test
    void replace() {
        Address adddress = createAddress(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);
        final String annotationKey = "myannotation";
        String annotationValue = "value";
        Address update = new Address.Builder(adddress).putAnnotation(annotationKey, annotationValue).build();

        api.createAddress(adddress);
        assertTrue(api.getAddressWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME).isPresent());

        boolean replaced = api.replaceAddress(update);
        assertTrue(replaced);

        Address read = api.getAddressWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME).get();

        assertEquals(ADDRESS_NAME, read.getName());
        assertEquals(annotationValue, read.getAnnotation(annotationKey));
    }

    @Test
    void replaceNotFound() {
        Address address = createAddress(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);

        boolean replaced = api.replaceAddress(address);
        assertFalse(replaced);
    }

    @Test
    void delete() {
        Address space = createAddress(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);

        api.createAddress(space);

        boolean deleted = api.deleteAddress(space);
        assertTrue(deleted);

        assertFalse(api.getAddressWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME).isPresent());

        boolean deletedAgain = api.deleteAddress(space);
        assertFalse(deletedAgain);
    }

    private Address createAddress(String namespace, String name) {
        return new Address.Builder()
                .setName(name)
                .setNamespace(namespace)
                .setAddress(ADDRESS)
                .setAddressSpace(ADDRESS_SPACE)
                .setType(ADDRESS_TYPE)
                .setPlan(ADDRESS_PLAN)
                .build();
    }
}
