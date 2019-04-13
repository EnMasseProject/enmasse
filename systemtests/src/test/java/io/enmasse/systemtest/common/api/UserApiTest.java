/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static java.net.HttpURLConnection.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(isolated)
class UserApiTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("user-api-address-space-brokered", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
    private AddressSpace standard = AddressSpaceUtils.createAddressSpaceObject("user-api-address-space-standard", AddressSpaceType.STANDARD, AddressSpacePlans.STANDARD_SMALL, AuthenticationServiceType.STANDARD);

    @BeforeEach
    void deployAddressSpaces() throws Exception {
        if (!AddressSpaceUtils.existAddressSpace(addressApiClient, brokered.getMetadata().getName())) {
            createAddressSpace(brokered);
        }
        if (!AddressSpaceUtils.existAddressSpace(addressApiClient, standard.getMetadata().getName())) {
            createAddressSpace(standard);
        }
    }

    @BeforeAll
    void beforeAll() {
        setReuseAddressSpace();
    }

    @AfterAll
    void removeAddressSpaces() throws Exception {
        unsetReuseAddressSpace();
        deleteAddressspacesFromList();
    }


    @Test
    void testCreateDeleteUserUsingCRD() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("jenda")
                        .withOperations(Operation.send, Operation.recv).build()));

        JsonObject userDefinitionPayload = UserUtils.userToJson(brokered.getMetadata().getName(), testUser);

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));

        //patch user
        assertThat(KubeCMDClient.patchCR(User.KIND.toLowerCase(), brokered.getMetadata().getName() + "." + cred.getUsername(), "{\"spec\":{\"authentication\":{\"password\":\"aGVp\"}}}").getRetCode(), is(true));

        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testCreateUserWithWrongPayloadCRD() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("jenda")
                        .withOperations(Operation.send, Operation.recv).build()));

        JsonObject userDefinitionPayload = UserUtils.userToJson(brokered.getMetadata().getName(), testUser);
        userDefinitionPayload.getJsonObject("spec").getJsonArray("authorization").getJsonObject(0).getJsonArray("operations").add("unknown");

        //create user
        ExecutionResultData createUserResponse = KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString());
        assertThat(createUserResponse.getRetCode(), is(false));
        assertTrue(createUserResponse.getStdErr().contains("value not one of declared Enum instance names: [send, view, recv, manage]"));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));

        User testUser2 = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("jenda")
                        .withOperations(Operation.send, Operation.recv).build()));

        JsonObject userDefinitionPayload2 = UserUtils.userToJson("", testUser2);

        //create user
        ExecutionResultData createUserResponse2 = KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload2.toString());
        assertThat(createUserResponse2.getRetCode(), is(false));
        assertTrue(createUserResponse2.getStdErr().contains(String.format("The name of the object (.%s) is not valid", cred.getUsername())));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testCreateUserWrongPayloadUserAPI() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("jenda")
                        .withOperations(Operation.send, Operation.recv).build()));

        JsonObject userDefinitionPayload = UserUtils.userToJson(brokered.getMetadata().getName(), testUser);
        userDefinitionPayload.getJsonObject("spec").getJsonArray("authorization").getJsonObject(0).getJsonArray("operations").add("posilani");

        Throwable exception = assertThrows(ExecutionException.class, () -> getUserApiClient().createUser(userDefinitionPayload, HTTP_INTERNAL_ERROR));
        assertTrue(exception.getMessage().contains("value not one of declared Enum instance names: [send, view, recv, manage]"));

        User testUser2 = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("jenda")
                        .withOperations(Operation.send, Operation.recv).build()));


        exception = assertThrows(ExecutionException.class, () -> getUserApiClient().createUser("", testUser2, HTTP_INTERNAL_ERROR));
        assertTrue(exception.getMessage().contains(String.format("The name of the object (.%s) is not valid", cred.getUsername())));
    }

    @Test
    void testUpdateUserPermissionsCRD() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("jenda")
                        .withOperations(Operation.send).build()));

        JsonObject userDefinitionPayload = UserUtils.userToJson(brokered.getMetadata().getName(), testUser);

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));

        UserCredentials updatedCred = new UserCredentials("pepanatestovani", null);
        testUser = new DoneableUser(testUser)
                .editSpec()
                .editFirstAuthorization()
                .removeFromOperations(Operation.send)
                .addToOperations(Operation.recv)
                .endAuthorization()
                .endSpec().done();

        userDefinitionPayload = UserUtils.userToJson(brokered.getMetadata().getName(), testUser);

        //update user
        assertThat(KubeCMDClient.updateCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertTrue(getUserApiClient().getUser(brokered.getMetadata().getName(), testUser.getSpec().getUsername()).toString().contains(Operation.recv.toString()));


        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
    }


    @Test
    void testUpdateUserPermissionsUserAPI() throws Exception {
        Address queue = AddressUtils.createQueueAddressObject("myqueue", DestinationPlan.STANDARD_SMALL_QUEUE);
        setAddresses(standard, queue);

        UserCredentials cred = new UserCredentials("pepa", "pepapw");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses(queue.getSpec().getAddress())
                        .withOperations(Operation.send).build()));

        createUser(standard, testUser);

        AmqpClient client = amqpClientFactory.createQueueClient(standard);
        client.getConnectOptions().setCredentials(cred);
        assertThat(client.sendMessages(queue.getSpec().getAddress(), Arrays.asList("kuk", "puk")).get(1, TimeUnit.MINUTES), is(2));

        testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses(queue.getSpec().getAddress())
                        .withOperations(Operation.recv).build()));

        updateUser(standard, testUser);
        Throwable exception = assertThrows(ExecutionException.class,
                () -> client.sendMessages(queue.getSpec().getAddress(), Arrays.asList("kuk", "puk")).get(10, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof UnauthorizedAccessException);
        assertThat(client.recvMessages(queue.getSpec().getAddress(), 2).get(1, TimeUnit.MINUTES).size(), is(2));
    }

    @Test
    void testUpdateUserWrongPayload() throws Exception {
        UserCredentials cred = new UserCredentials("pepa", "pepapw");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));

        createUser(brokered, testUser);

        testUser = new DoneableUser(testUser).editSpec().withUsername("").endSpec().done();

        User finalTestUser = testUser;
        Throwable exception = assertThrows(ExecutionException.class,
                () -> getUserApiClient().updateUser(brokered.getMetadata().getName(), finalTestUser, HTTP_BAD_REQUEST));
        assertTrue(exception.getMessage().contains("Bad Request"));
    }

    @Test
    void testUpdateNoExistsUser() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("unknown")
                        .withOperations(Operation.send, Operation.recv).build()));

        Throwable exception = assertThrows(ExecutionException.class, () -> getUserApiClient().updateUser(brokered.getMetadata().getName(), testUser, HTTP_NOT_FOUND));
        assertTrue(exception.getMessage().contains(String.format("User %s.%s not found", brokered.getMetadata().getName(), cred.getUsername())));
    }

    @Test
    void testUserWithSimilarNamesAndAlreadyExistingUser() throws Exception {
        UserCredentials cred = new UserCredentials("user2", "user2");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send, Operation.recv).build()));

        UserCredentials cred2 = new UserCredentials("user23", "test_user23");
        User testUser2 = UserUtils.createUserObject(cred2, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send, Operation.recv).build()));

        createUser(brokered, testUser);
        createUser(brokered, testUser2);

        Throwable exception = assertThrows(ExecutionException.class, () -> getUserApiClient().createUser(brokered.getMetadata().getName(), testUser, HTTP_CONFLICT));
        assertTrue(exception.getMessage().contains(String.format("User '%s' already exists", cred.getUsername())));
    }

    @Test
    void testCreateUserUppercaseUsername() throws Exception {
        UserCredentials cred = new UserCredentials("UserPepinator", "ff^%fh16");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send, Operation.recv).build()));

        assertThrows(ExecutionException.class, () -> getUserApiClient().createUser(brokered.getMetadata().getName(), testUser, HTTP_BAD_REQUEST));
    }

    @Test
    void testCreateUsersWithSymbolsInVariousPlaces() throws Exception {
        //valid user is created (response HTTP:201)
        UserCredentials cred = new UserCredentials("normalusername", "password");
        User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));
        getUserApiClient().createUser(UserUtils.userToJson(brokered.getMetadata().getName(), "userpepinator", testUser), HTTP_CREATED);

        //first char of username must be a-z0-9 (other symbols respond with HTTP:400)
        UserCredentials cred2 = new UserCredentials("-hyphensymbolfirst", "password");
        User testUser2 = UserUtils.createUserObject(cred2, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));
        assertThrows(ExecutionException.class, () ->
                getUserApiClient().createUser(UserUtils.userToJson(brokered.getMetadata().getName(), "userpepinator", testUser2), HTTP_BAD_REQUEST));

        //hyphen is allowed elsewhere in user name (response HTTP:201)
        UserCredentials cred3 = new UserCredentials("user-pepinator", "password");
        User testUser3 = UserUtils.createUserObject(cred3, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));
        getUserApiClient().createUser(UserUtils.userToJson(brokered.getMetadata().getName(), "userpepinator", testUser3), HTTP_CREATED);

        //underscore is also allowed in user name (response HTTP:201)
        UserCredentials cred4 = new UserCredentials("user_pepinator", "password");
        User testUser4 = UserUtils.createUserObject(cred4, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));

        getUserApiClient().createUser(UserUtils.userToJson(brokered.getMetadata().getName(), "userpepinator", testUser4), HTTP_CREATED);

        //last char must also be a-z (other symbols respond with HTTP:400)
        UserCredentials cred5 = new UserCredentials("hyphensymbollast-", "password");
        User testUser5 = UserUtils.createUserObject(cred5, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));
        assertThrows(ExecutionException.class, () ->
                getUserApiClient().createUser(UserUtils.userToJson(brokered.getMetadata().getName(), "userpepinator", testUser5), HTTP_BAD_REQUEST));

        //username may start/end with 0-9 ()
        UserCredentials cred6 = new UserCredentials("01234usernamehere56789", "password");
        User testUser6 = UserUtils.createUserObject(cred6, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));

        getUserApiClient().createUser(UserUtils.userToJson(brokered.getMetadata().getName(), "userpepinator", testUser6), HTTP_CREATED);

        //Foreign symbols may not be used in username (response HTTP:400)
        UserCredentials cred7 = new UserCredentials("invalid_Ö_username", "password");
        User testUser7 = UserUtils.createUserObject(cred7, Collections.singletonList(
                new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send).build()));
        assertThrows(ExecutionException.class, () ->
                getUserApiClient().createUser(UserUtils.userToJson(brokered.getMetadata().getName(), "userpepinator", testUser7), HTTP_BAD_REQUEST));
    }

    @Test
    void testServiceaccountUser() throws Exception {
        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.STANDARD_SMALL_QUEUE);
        setAddresses(standard, queue);
        UserCredentials serviceAccount = new UserCredentials("test-service-account", "");
        createUserServiceAccount(standard, serviceAccount, environment.namespace());
        UserCredentials messagingUser = new UserCredentials("@@serviceaccount@@",
                kubernetes.getServiceaccountToken(serviceAccount.getUsername(), environment.namespace()));
        log.info("username: {}, password: {}", messagingUser.getUsername(), messagingUser.getPassword());

        assertCanConnect(standard, messagingUser, Collections.singletonList(queue));

        //delete user
        assertThat("User deleting failed using oc cmd",
                KubeCMDClient.deleteUser(kubernetes.getNamespace(), standard.getMetadata().getName(), serviceAccount.getUsername()).getRetCode(), is(true));
        assertThat("User is still present",
                KubeCMDClient.getUser(kubernetes.getNamespace(), standard.getMetadata().getName(), serviceAccount.getUsername()).getRetCode(), is(false));

        assertCannotConnect(standard, messagingUser, Collections.singletonList(queue));

        kubernetes.deleteServiceAccount("test-service-account", environment.namespace());
    }
}
