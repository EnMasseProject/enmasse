/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.util.JULInitializingTest;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;


/**
 * The mock server does not emulate behaviour with respect to resourceVersion.
 */
class ConfigMapAddressSpaceApiTest extends JULInitializingTest {

    private static final String ADDRESS_SPACE_NAME = "myspace";
    private static final String ADDRESS_SPACE_TYPE = "mytype";
    private static final String ADDRESS_SPACE_PLAN = "myplan";
    private static final String ADDRESS_SPACE_NAMESPACE = "myproject";
    private static final String TEST_UUID = "fd93dc62-197b-11e9-9e48-c85b762e5a2c";

    private KubernetesServer kubeServer = new KubernetesServer(false, true);
    private AddressSpaceApi api;

    @BeforeEach
    void setUp() {
        kubeServer.before();
        api = new ConfigMapAddressSpaceApi(kubeServer.getClient());
    }

    @AfterEach
    void tearDown() {
        kubeServer.after();
    }

    @Test
    void create() throws Exception {
        AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);

        api.createAddressSpace(space);
        Optional<AddressSpace> readAddressSpace = api.getAddressSpaceWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);

        assertTrue(readAddressSpace.isPresent());
        AddressSpace read = readAddressSpace.get();

        assertEquals(ADDRESS_SPACE_NAME, read.getMetadata().getName());
        assertEquals(ADDRESS_SPACE_TYPE, read.getSpec().getType());
        assertEquals(ADDRESS_SPACE_PLAN, read.getSpec().getPlan());
        assertEquals(TEST_UUID, read.getAnnotation(AnnotationKeys.INFRA_UUID));
    }

    @Test
    void replace() throws Exception {
        AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);
        final String annotationKey = "myannotation";
        String annotationValue = "value";
        AddressSpace update = new AddressSpaceBuilder(space).editOrNewMetadata().addToAnnotations(annotationKey, annotationValue).endMetadata().build();

        api.createAddressSpace(space);
        assertTrue(api.getAddressSpaceWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME).isPresent());

        boolean replaced = api.replaceAddressSpace(update);
        assertTrue(replaced);

        AddressSpace read = api.getAddressSpaceWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME).get();

        assertEquals(ADDRESS_SPACE_NAME, read.getMetadata().getName());
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
        return new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .addToAnnotations(AnnotationKeys.INFRA_UUID, TEST_UUID)
                .endMetadata()

                .withNewSpec()
                .withType(ADDRESS_SPACE_TYPE)
                .withPlan(ADDRESS_SPACE_PLAN)
                .endSpec()

                .build();
    }
}
