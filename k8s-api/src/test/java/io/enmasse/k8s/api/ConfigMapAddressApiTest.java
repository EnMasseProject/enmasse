/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.k8s.util.JULInitializingTest;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class ConfigMapAddressApiTest extends JULInitializingTest {

    private static final String ADDRESS = "myaddress";
    private static final String ADDRESS_TYPE = "mytype";
    private static final String ADDRESS_PLAN = "myplan";
    private static final String ADDRESS_SPACE_NAMESPACE = "myproject";
    private static final String ADDRESS_SPACE = "myspace";
    private static final String ADDRESS_NAME = String.format("%s.%s", ADDRESS_SPACE, ADDRESS);

    private KubernetesServer kubeServer = new KubernetesServer(false, true);
    private AddressApi api;

    @BeforeEach
    void setUp() {
        kubeServer.before();
        NamespacedKubernetesClient client = kubeServer.getClient();
        api = new ConfigMapAddressApi(client, UUID.randomUUID().toString(), null);
    }

    @AfterEach
    void tearDown() {
        kubeServer.after();
    }

    @Test
    void create() {
        Address address = createAddress(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);

        api.createAddress(address);
        Optional<Address> readAddress = api.getAddressWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);

        assertTrue(readAddress.isPresent());
        Address read = readAddress.get();

        assertEquals(ADDRESS, read.getSpec().getAddress());
        assertEquals(ADDRESS_SPACE, Address.extractAddressSpace(read));
        assertEquals(ADDRESS_NAME, read.getMetadata().getName());
        assertEquals(ADDRESS_TYPE, read.getSpec().getType());
        assertEquals(ADDRESS_PLAN, read.getSpec().getPlan());
    }

    @Test
    void replace() {
        Address address = createAddress(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME);
        final String annotationKey = "myannotation";
        String annotationValue = "value";
        Address update = new AddressBuilder(address).editOrNewMetadata().addToAnnotations(annotationKey, annotationValue).endMetadata().build();

        api.createAddress(address);
        assertTrue(api.getAddressWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME).isPresent());

        boolean replaced = api.replaceAddress(update);
        assertTrue(replaced);

        Address read = api.getAddressWithName(ADDRESS_SPACE_NAMESPACE, ADDRESS_NAME).get();

        assertEquals(ADDRESS_NAME, read.getMetadata().getName());
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
        return new AddressBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()

                .withNewSpec()
                .withAddress(ADDRESS)
                .withAddressSpace(ADDRESS_SPACE)
                .withType(ADDRESS_TYPE)
                .withPlan(ADDRESS_PLAN)
                .endSpec()

                .build();
    }
}
