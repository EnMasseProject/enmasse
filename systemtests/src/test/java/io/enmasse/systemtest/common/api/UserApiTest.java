/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CRDCmdClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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

        UserCredentials cred = new UserCredentials("pepaNaTestovani", "pepaNaTestovani");
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

        UserCredentials cred = new UserCredentials("pepaNaTestovani", "pepaNaTestovani");
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

        UserCredentials cred = new UserCredentials("pepaNaTestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE)
                        .addOperation("posilani"));

        try {
            getUserApiClient().createUser(brokered.getName(), testUser, HTTP_INTERNAL_ERROR);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("value not one of declared Enum instance names: [send, view, recv, manage]"));
        }

        User testUser2 = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE));

        try {
            getUserApiClient().createUser("", testUser2, HTTP_INTERNAL_ERROR);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains(String.format("The name of the object (.%s) is not valid", cred.getUsername())));
        }
    }

    @Test
    void testUpdateUserPermissions() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-update-user", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        Destination queue = Destination.queue("myqueue", "brokered-queue");
        setAddresses(brokered, queue);

        UserCredentials cred = new UserCredentials("pepaNaTestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress(queue.getAddress())
                        .addOperation(User.Operation.SEND));

        createUser(brokered, testUser);

        AmqpClient client = amqpClientFactory.createQueueClient(brokered);
        client.getConnectOptions().setCredentials(cred);
        assertThat(client.sendMessages(queue.getAddress(), Arrays.asList("kuk", "puk")).get(1, TimeUnit.MINUTES), is(2));

        testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress(queue.getAddress())
                        .addOperation(User.Operation.RECEIVE));

        updateUser(brokered, testUser);
        assertThat(client.sendMessages(queue.getAddress(), Arrays.asList("kuk", "puk")).get(1, TimeUnit.MINUTES), is(2));
        assertThat(client.recvMessages(queue.getAddress(), 2).get(1, TimeUnit.MINUTES).size(), is(2));
    }

    @Test
    void testUpdateNoExistsUser() throws Exception {
        AddressSpace brokered = new AddressSpace("user-api-space-update-noexists-user", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        UserCredentials cred = new UserCredentials("pepaNaTestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("unknown")
                        .addOperation(User.Operation.RECEIVE));

        try {
            getUserApiClient().updateUser(brokered.getName(), testUser, HTTP_NOT_FOUND);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains(String.format("User %s.%s not found", brokered.getName(), cred.getUsername())));
        }
    }
}
