/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.model.CustomResourceDefinitions;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserBuilder;
import io.enmasse.user.model.v1.UserCrd;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MessagingUserFinalizerControllerTest {

    private MessagingUserFinalizerController controller;
    private NamespacedKubernetesClient client;
    private KubernetesServer server;
    private MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> userClient;

    @BeforeAll
    public static void init() {
        CustomResourceDefinitions.registerAll();
    }

    @BeforeEach
    public void setup() {
        server = new KubernetesServer(false, true);
        server.before();
        client = server.getClient();
        userClient = client.customResources(UserCrd.messagingUser(), User.class, UserList.class, DoneableUser.class);
        this.controller = new MessagingUserFinalizerController(client);
    }

    @AfterEach
    public void teardown() {
        client.close();
        server.after();
    }

    @Test
    public void testFinalizerSuccess() {
        userClient.inNamespace("test").createOrReplace(createTestUser("myspace", "test"));
        assertNotNull(userClient.inNamespace("test").withName("myspace.user").get());
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace());
        assertNotNull(result);
        assertTrue(result.isFinalized());
        assertNull(userClient.inNamespace("test").withName("myspace.user").get());
    }

    @Test
    public void testFinalizerFilterNamespace() {
        userClient.inNamespace("test2").createOrReplace(createTestUser("myspace", "test2"));
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace());
        assertNotNull(result);
        assertTrue(result.isFinalized());
        assertNotNull(userClient.inNamespace("test2").withName("myspace.user").get());
    }

    @Test
    public void testFinalizerFilterAddressSpace() {
        userClient.inNamespace("test").createOrReplace(createTestUser("myspace2", "test"));
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace());
        assertNotNull(result);
        assertTrue(result.isFinalized());
        assertNotNull(userClient.inNamespace("test").withName("myspace2.user").get());
    }

    @Test
    public void testFinalizerFailed() {
        var mockClient = mock(MixedOperation.class);
        MessagingUserFinalizerController controller = new MessagingUserFinalizerController(mockClient);
        doThrow(new KubernetesClientException("ERROR")).when(mockClient).inNamespace(any());
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace());
        assertNotNull(result);
        assertFalse(result.isFinalized());
    }

    private static User createTestUser(String addressSpace, String namespace) {
        return new UserBuilder()
                .editOrNewMetadata()
                .withName(addressSpace + ".user")
                .withNamespace(namespace)
                .endMetadata()
                .editOrNewSpec()
                .withUsername("user")
                .withNewAuthentication()
                .withType(UserAuthenticationType.serviceaccount)
                .endAuthentication()
                .addNewAuthorization()
                .addToOperations(Operation.manage)
                .endAuthorization()
                .endSpec()
                .build();
    }

    private static AddressSpace createTestSpace() {
        return new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName("myspace")
                .withNamespace("test")
                .endMetadata()
                .editOrNewSpec()
                .withType("standard")
                .withPlan("standard")
                .endSpec()
                .build();
    }
}
