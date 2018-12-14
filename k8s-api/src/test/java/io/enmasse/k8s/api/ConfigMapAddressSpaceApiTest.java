/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpace;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


/**
 * The mock server does not emulate behaviour with respect to resourceVersion.
 */
class ConfigMapAddressSpaceApiTest {
    private static final String ADDRESS_SPACE_NAME = "myspace";
    private static final String ADDRESS_SPACE_TYPE = "mytype";
    private static final String ADDRESS_SPACE_PLAN = "myplan";
    private static final String ADDRESS_SPACE_NAMESPACE = "myproject";

    private OpenShiftServer openShiftServer = new OpenShiftServer(false, true);
    private AddressSpaceApi api;

    @BeforeEach
    void setUp() {
        openShiftServer.before();
        NamespacedOpenShiftClient client = openShiftServer.getOpenshiftClient();
        api = new ConfigMapAddressSpaceApi(client);
    }

    @AfterEach
    void tearDown() {
        openShiftServer.after();
    }

    @Test
    void create() throws Exception {
        AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);

        api.createAddressSpace(space);
        Optional<AddressSpace> readAddressSpace = api.getAddressSpaceWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);

        assertTrue(readAddressSpace.isPresent());
        AddressSpace read = readAddressSpace.get();

        assertEquals(ADDRESS_SPACE_NAME, read.getName());
        assertEquals(ADDRESS_SPACE_TYPE, read.getType());
        assertEquals(ADDRESS_SPACE_PLAN, read.getPlan());
    }

    @Test
    void replace() throws Exception {
        AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);
        final String annotationKey = "myannotation";
        String annotationValue = "value";
        AddressSpace update = new AddressSpace.Builder(space).putAnnotation(annotationKey, annotationValue).build();

        api.createAddressSpace(space);
        assertTrue(api.getAddressSpaceWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME).isPresent());

        boolean replaced = api.replaceAddressSpace(update);
        assertTrue(replaced);

        AddressSpace read = api.getAddressSpaceWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME).get();

        assertEquals(ADDRESS_SPACE_NAME, read.getName());
        assertEquals(annotationValue, read.getAnnotation(annotationKey));
    }

    @Test
    void replaceNotFound() throws Exception {
        AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);

        boolean replaced = api.replaceAddressSpace(space);
        assertFalse(replaced);
    }

    @Test
    void delete() throws Exception {
        AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);

        api.createAddressSpace(space);

        boolean deleted = api.deleteAddressSpace(space);
        assertTrue(deleted);

        assertFalse(api.getAddressSpaceWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME).isPresent());

        boolean deletedAgain = api.deleteAddressSpace(space);
        assertFalse(deletedAgain);
    }

    private AddressSpace createAddressSpace(String namespace, String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(namespace)
                .setType(ADDRESS_SPACE_TYPE)
                .setPlan(ADDRESS_SPACE_PLAN)
                .build();
    }
}
