/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CRDCmdClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
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

    @Test
    void testCreateDeleteUserUsingCRD() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-create-delete-crd", AddressSpaceType.BROKERED, AuthService.STANDARD);
        addToAddressSpacess(brokered);
        JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());

        //create addressspace
        assertThat(CRDCmdClient.createCR(kubernetes.getNamespace(), addressSpacePayloadJson.toString()).getRetCode(), is(true));
        waitForAddressSpaceReady(brokered);

        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE));

        JsonObject userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //create user
        assertThat(CRDCmdClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(CRDCmdClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));

        //delete user
        assertThat(CRDCmdClient.deleteUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(CRDCmdClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testCreateUserWithWrongPayloadCRD() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-wrong-payload-crd", AddressSpaceType.BROKERED, AuthService.STANDARD);
        addToAddressSpacess(brokered);
        JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());

        //create addressspace
        assertThat(CRDCmdClient.createCR(kubernetes.getNamespace(), addressSpacePayloadJson.toString()).getRetCode(), is(true));
        waitForAddressSpaceReady(brokered);

        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE)
                        .addOperation("unknown"));

        JsonObject userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //create user
        ExecutionResultData createUserResponse = CRDCmdClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString());
        assertThat(createUserResponse.getRetCode(), is(false));
        assertTrue(createUserResponse.getStdErr().contains("value not one of declared Enum instance names: [send, view, recv, manage]"));
        assertThat(CRDCmdClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));

        User testUser2 = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE));

        JsonObject userDefinitionPayload2 = testUser2.toCRDJson("");

        //create user
        ExecutionResultData createUserResponse2 = CRDCmdClient.createCR(kubernetes.getNamespace(), userDefinitionPayload2.toString());
        assertThat(createUserResponse2.getRetCode(), is(false));
        assertTrue(createUserResponse2.getStdErr().contains(String.format("The name of the object (.%s) is not valid", cred.getUsername())));
        assertThat(CRDCmdClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testCreateUserWrongPayloadUserAPI() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-wrong-payload-api", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE)
                        .addOperation("posilani"));

        Throwable exception = assertThrows(ExecutionException.class, () -> getUserApiClient().createUser(brokered.getName(), testUser, HTTP_INTERNAL_ERROR));
        assertTrue(exception.getMessage().contains("value not one of declared Enum instance names: [send, view, recv, manage]"));

        User testUser2 = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE));


        exception = assertThrows(ExecutionException.class, () -> getUserApiClient().createUser("", testUser2, HTTP_INTERNAL_ERROR));
        assertTrue(exception.getMessage().contains(String.format("The name of the object (.%s) is not valid", cred.getUsername())));
    }

    @Test
    void testUpdateUserPermissionsCRD() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-update-crd", AddressSpaceType.BROKERED, AuthService.STANDARD);
        addToAddressSpacess(brokered);
        JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());

        //create addressspace
        assertThat(CRDCmdClient.createCR(kubernetes.getNamespace(), addressSpacePayloadJson.toString()).getRetCode(), is(true));
        waitForAddressSpaceReady(brokered);

        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND));

        JsonObject userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //create user
        assertThat(CRDCmdClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(CRDCmdClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));

        testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.RECEIVE));

        userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //update user
        assertThat(CRDCmdClient.updateCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(CRDCmdClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));
        assertTrue(getUserApiClient().getUser(brokered.getName(), testUser.getUsername()).toString().contains(User.Operation.RECEIVE));


        //delete user
        assertThat(CRDCmdClient.deleteUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(CRDCmdClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));
    }


    @Test
    void testUpdateUserPermissionsUserAPI() throws Exception {
        AddressSpace standard = new AddressSpace("user-api-space-update-user", AddressSpaceType.STANDARD, AuthService.STANDARD);
        standard.setPlan("unlimited-standard-without-mqtt");

        createAddressSpace(standard);

        Destination queue = Destination.queue("myqueue", "pooled-queue");
        setAddresses(standard, queue);

        UserCredentials cred = new UserCredentials("pepa", "pepapw");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress(queue.getAddress())
                        .addOperation(User.Operation.SEND));

        createUser(standard, testUser);

        AmqpClient client = amqpClientFactory.createQueueClient(standard);
        client.getConnectOptions().setCredentials(cred);
        assertThat(client.sendMessages(queue.getAddress(), Arrays.asList("kuk", "puk")).get(1, TimeUnit.MINUTES), is(2));

        testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress(queue.getAddress())
                        .addOperation(User.Operation.RECEIVE));

        updateUser(standard, testUser);
        Throwable exception = assertThrows(ExecutionException.class,
                () -> client.sendMessages(queue.getAddress(), Arrays.asList("kuk", "puk")).get(10, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof UnauthorizedAccessException);
        assertThat(client.recvMessages(queue.getAddress(), 2).get(1, TimeUnit.MINUTES).size(), is(2));
    }

    @Test
    void testUpdateUserWrongPayload() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-update-user-wrong-payload", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        UserCredentials cred = new UserCredentials("pepa", "pepapw");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.SEND));

        createUser(brokered, testUser);

        testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation("admin"));

        User finalTestUser = testUser;
        Throwable exception = assertThrows(ExecutionException.class,
                () -> getUserApiClient().updateUser(brokered.getName(), finalTestUser, HTTP_BAD_REQUEST));
        assertTrue(exception.getMessage().contains("Bad Request"));
    }

    @Test
    void testUpdateNoExistsUser() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-update-noexists-user", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("unknown")
                        .addOperation(User.Operation.RECEIVE));

        Throwable exception = assertThrows(ExecutionException.class, () -> getUserApiClient().updateUser(brokered.getName(), testUser, HTTP_NOT_FOUND));
        assertTrue(exception.getMessage().contains(String.format("User %s.%s not found", brokered.getName(), cred.getUsername())));
    }

    @Test
    void testUserWithSimilarNamesAndAlreadyExistingUser() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-similar-user", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        UserCredentials cred = new UserCredentials("user2", "user2");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));

        UserCredentials cred2 = new UserCredentials("user23", "test_user23");
        User testUser2 = new User().setUserCredentials(cred2).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));

        createUser(brokered, testUser);
        createUser(brokered, testUser2);

        Throwable exception = assertThrows(ExecutionException.class, () -> getUserApiClient().createUser(brokered.getName(), testUser, HTTP_CONFLICT));
        assertTrue(exception.getMessage().contains(String.format("User '%s' already exists", cred.getUsername())));
    }

    @Test
    void testCreateUserUppercaseUsername() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-uppercase-username", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        UserCredentials cred = new UserCredentials("UserPepinator", "ff^%fh16");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));

        assertThrows(ExecutionException.class, () -> getUserApiClient().createUser(brokered.getName(), testUser, HTTP_BAD_REQUEST));
    }

    @Test
    void testCreateUserUnderscoreUsername() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-underscore-username", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        UserCredentials cred = new UserCredentials("user_pepinator", "password");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));

        getUserApiClient().createUser(brokered.getName(), testUser.toJson(brokered.getName(), "userpepinator"), HTTP_CREATED);
    }
}
