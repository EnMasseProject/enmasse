/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.util.JULInitializingTest;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;


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

    @Test
    void listReturnsOne() throws Exception {
        AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);

        AddressSpaceList addressSpaceList = api.listAddressSpaces(ADDRESS_SPACE_NAMESPACE, null);
        assertNotNull(addressSpaceList.getItems());
        assertEquals(0, addressSpaceList.getItems().size());

        api.createAddressSpace(space);

        addressSpaceList = api.listAddressSpaces(ADDRESS_SPACE_NAMESPACE, null);
        assertEquals(1, addressSpaceList.getItems().size());
        assertEquals(space, addressSpaceList.getItems().get(0));

        api.deleteAddressSpace(space);

        addressSpaceList = api.listAddressSpaces(ADDRESS_SPACE_NAMESPACE, null);
        assertEquals(0, addressSpaceList.getItems().size());
    }

    @Test
    void listReturnsMany() throws Exception {
        AddressSpace space1 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-1");
        AddressSpace space2 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-2");
        AddressSpace different = createAddressSpace(ADDRESS_SPACE_NAMESPACE +"-1", ADDRESS_SPACE_NAME);

        api.createAddressSpace(space1);
        api.createAddressSpace(space2);
        api.createAddressSpace(different);

        AddressSpaceList addressSpaceList = api.listAddressSpaces(ADDRESS_SPACE_NAMESPACE, null);
        assertEquals(2, addressSpaceList.getItems().size());

        assertTrue(addressSpaceList.getItems().contains(space1));
        assertTrue(addressSpaceList.getItems().contains(space2));
    }
    @Test
    void listByLabels() throws Exception {
        AddressSpace space1 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-1");
        space1.putLabelIfAbsent("foo", "bar");
        AddressSpace space2 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-2");

        api.createAddressSpace(space1);
        api.createAddressSpace(space2);

        AddressSpaceList addressSpaceList = api.listAddressSpaces(ADDRESS_SPACE_NAMESPACE, Collections.singletonMap("foo", "bar"));
        assertEquals(1, addressSpaceList.getItems().size());

        assertTrue(addressSpaceList.getItems().contains(space1));

        addressSpaceList = api.listAddressSpaces(ADDRESS_SPACE_NAMESPACE, Collections.singletonMap("baz", "bob"));
        assertEquals(0, addressSpaceList.getItems().size());
    }

    @Test
    void listAll() throws Exception {
        AddressSpace space1 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-1");
        AddressSpace space2 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-2");
        AddressSpace different = createAddressSpace(ADDRESS_SPACE_NAMESPACE +"-1", ADDRESS_SPACE_NAME);

        api.createAddressSpace(space1);
        api.createAddressSpace(space2);
        api.createAddressSpace(different);

        AddressSpaceList addressSpaceList = api.listAllAddressSpaces(null);
        assertEquals(3, addressSpaceList.getItems().size());

        assertTrue(addressSpaceList.getItems().contains(space1));
        assertTrue(addressSpaceList.getItems().contains(space2));
        assertTrue(addressSpaceList.getItems().contains(different));
    }

    @Test
    void listAllByLabels() throws Exception {
        AddressSpace space1 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-1");
        space1.putLabelIfAbsent("foo", "bar");
        AddressSpace space2 = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME + "-2");
        AddressSpace different = createAddressSpace(ADDRESS_SPACE_NAMESPACE +"-1", ADDRESS_SPACE_NAME);
        different.putLabelIfAbsent("foo", "bar");

        api.createAddressSpace(space1);
        api.createAddressSpace(space2);
        api.createAddressSpace(different);

        AddressSpaceList addressSpaceList = api.listAllAddressSpaces(Collections.singletonMap("foo", "bar"));
        assertEquals(2, addressSpaceList.getItems().size());

        assertTrue(addressSpaceList.getItems().contains(space1));
        assertTrue(addressSpaceList.getItems().contains(different));
    }

    @Test
    @Disabled("https://github.com/fabric8io/kubernetes-client/issues/1456")
    void watch() throws Exception {
        final BlockingDeque<Action> events = new LinkedBlockingDeque<>();
        try (Closeable ignored = api.watch(new Watcher<>() {
            @Override
            public void eventReceived(Action action, AddressSpace resource) {
                events.offer(action);
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        }, null, null, null)) {

            AddressSpace space = createAddressSpace(ADDRESS_SPACE_NAMESPACE, ADDRESS_SPACE_NAME);
            api.createAddressSpace(space);

            Action addEvent = events.poll(10, TimeUnit.SECONDS);
            assertEquals(Action.ADDED, addEvent);

            api.deleteAddressSpace(space);

            Action delEvent = events.poll(10, TimeUnit.SECONDS);
            assertEquals(Action.ADDED, delEvent);
        }
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
