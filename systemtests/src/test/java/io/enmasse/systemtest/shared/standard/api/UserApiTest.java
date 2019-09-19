/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;
import io.enmasse.user.model.v1.UserCrd;
import io.enmasse.user.model.v1.UserList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserApiTest extends TestBase implements ITestSharedStandard {

    private Map<AddressSpace, User> users = new HashMap<>();
    private Logger LOGGER = CustomLogger.getLogger();

    @AfterEach
    void cleanUsers() {
        users.forEach((addressSpace, user) -> {
            try {
                resourcesManager.removeUser(addressSpace, user);
            } catch (Exception e) {
                LOGGER.info("Clean: User not exists {}", user.getSpec().getUsername());
            }
        });
    }

    @Test
    void testUpdateUserPermissionsUserAPI() throws Exception {
        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        resourcesManager.setAddresses(queue);

        UserCredentials cred = new UserCredentials("pepa", "pepapw");
        User testUser = UserUtils.createUserResource(cred)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses(queue.getSpec().getAddress())
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();

        users.put(getSharedAddressSpace(), testUser);
        testUser = resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser);

        AmqpClient client = getAmqpClientFactory().createQueueClient(getSharedAddressSpace());
        client.getConnectOptions().setCredentials(cred);
        assertThat(client.sendMessages(queue.getSpec().getAddress(), Arrays.asList("kuk", "puk")).get(1, TimeUnit.MINUTES), is(2));

        testUser = new DoneableUser(testUser)
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses(queue.getSpec().getAddress())
                        .withOperations(Operation.recv).build()))
                .endSpec()
                .done();

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser);
        Throwable exception = assertThrows(ExecutionException.class,
                () -> client.sendMessages(queue.getSpec().getAddress(), Arrays.asList("kuk", "puk")).get(10, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof UnauthorizedAccessException);
        assertThat(client.recvMessages(queue.getSpec().getAddress(), 2).get(1, TimeUnit.MINUTES).size(), is(2));
    }

    @Test
    void testServiceaccountUser() throws Exception {
        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        resourcesManager.setAddresses(queue);
        UserCredentials serviceAccount = new UserCredentials("test-service-account", "");
        try {
            resourcesManager.createUserServiceAccount(getSharedAddressSpace(), serviceAccount);
            UserCredentials messagingUser = new UserCredentials("@@serviceaccount@@",
                    kubernetes.getServiceaccountToken(serviceAccount.getUsername(), environment.namespace()));
            LOGGER.info("username: {}, password: {}", messagingUser.getUsername(), messagingUser.getPassword());

            getClientUtils().assertCanConnect(getSharedAddressSpace(), messagingUser, Collections.singletonList(queue), resourcesManager);

            //delete user
            assertThat("User deleting failed using oc cmd",
                    KubeCMDClient.deleteUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), serviceAccount.getUsername()).getRetCode(), is(true));
            assertThat("User is still present",
                    KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), serviceAccount.getUsername()).getRetCode(), is(false));

            getClientUtils().assertCannotConnect(getSharedAddressSpace(), messagingUser, Collections.singletonList(queue), resourcesManager);
        } finally {
            kubernetes.deleteServiceAccount("test-service-account", environment.namespace());
        }
    }

    @Test
    void testSetAnnotations() {

        var client = Kubernetes.getInstance().getClient();
        var userCrd = client.customResources(UserCrd.messagingUser()   , User.class, UserList.class, DoneableUser.class);

        var name = getSharedAddressSpace().getMetadata().getName()+ ".foo";
        var user = new UserBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(name)
                .addToAnnotations("annotation1", "value1")
                .addToAnnotations("annotation2", "value2")
                .endMetadata()
                .build();

        // create
        userCrd.create(user);

        // read again
        var actualUser = userCrd.inNamespace(getSharedAddressSpace().getMetadata().getNamespace()).withName(name).get();

        assertNotNull(actualUser);
        assertThat(actualUser.getAnnotation("annotation1"), is("value1"));
        assertThat(actualUser.getAnnotation("annotation2"), is("value2"));

        var updateUser = new UserBuilder(actualUser)
                .editOrNewMetadata()
                .addToAnnotations("annotation1", "value1a")
                .removeFromAnnotations("annotation2")
                .addToAnnotations("annotation3", "value3")
                .endMetadata()
                .build();

        userCrd.createOrReplace(updateUser);

        // read again
        actualUser = userCrd.inNamespace(getSharedAddressSpace().getMetadata().getNamespace()).withName(name).get();

        assertNotNull(actualUser);
        assertThat(actualUser.getAnnotation("annotation1"), is("value1a"));
        assertThat(actualUser.getAnnotation("annotation2"), nullValue());
        assertThat(actualUser.getAnnotation("annotation3"), is("value3"));

    }
}
