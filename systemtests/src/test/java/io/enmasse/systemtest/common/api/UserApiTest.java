/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.selenium.SeleniumContainers;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
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
    private AddressSpace brokered = new AddressSpace("user-api-address-space-brokered", AddressSpaceType.BROKERED, AuthService.STANDARD);
    private AddressSpace standard = new AddressSpace("user-api-address-space-standard", AddressSpaceType.STANDARD, AuthService.STANDARD);

    @BeforeEach
    void deployAddressSpaces() throws Exception {
        if (!TestUtils.existAddressSpace(addressApiClient, brokered.getName())) {
            createAddressSpace(brokered);
        }
        if (!TestUtils.existAddressSpace(addressApiClient, standard.getName())) {
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
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE));

        JsonObject userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));

        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testCreateUserWithWrongPayloadCRD() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE)
                        .addOperation("unknown"));

        JsonObject userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //create user
        ExecutionResultData createUserResponse = KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString());
        assertThat(createUserResponse.getRetCode(), is(false));
        assertTrue(createUserResponse.getStdErr().contains("value not one of declared Enum instance names: [send, view, recv, manage]"));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));

        User testUser2 = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND)
                        .addOperation(User.Operation.RECEIVE));

        JsonObject userDefinitionPayload2 = testUser2.toCRDJson("");

        //create user
        ExecutionResultData createUserResponse2 = KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload2.toString());
        assertThat(createUserResponse2.getRetCode(), is(false));
        assertTrue(createUserResponse2.getStdErr().contains(String.format("The name of the object (.%s) is not valid", cred.getUsername())));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testCreateUserWrongPayloadUserAPI() throws Exception {
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
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.SEND));

        JsonObject userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));

        UserCredentials updatedCred = new UserCredentials("pepanatestovani", null);
        testUser = new User().setUserCredentials(updatedCred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("jenda")
                        .addOperation(User.Operation.RECEIVE));

        userDefinitionPayload = testUser.toCRDJson(brokered.getName());

        //update user
        assertThat(KubeCMDClient.updateCR(kubernetes.getNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));
        assertTrue(getUserApiClient().getUser(brokered.getName(), testUser.getUsername()).toString().contains(User.Operation.RECEIVE));


        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), cred.getUsername()).getRetCode(), is(false));
    }


    @Test
    void testUpdateUserPermissionsUserAPI() throws Exception {
        Destination queue = Destination.queue("myqueue", DestinationPlan.STANDARD_SMALL_QUEUE);
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
        UserCredentials cred = new UserCredentials("UserPepinator", "ff^%fh16");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));

        assertThrows(ExecutionException.class, () -> getUserApiClient().createUser(brokered.getName(), testUser, HTTP_BAD_REQUEST));
    }

    @Test
    void testCreateUsersWithSymbolsInVariousPlaces() throws Exception {
        //valid user is created (response HTTP:201)
        UserCredentials cred = new UserCredentials("normalusername", "password");
        User testUser = new User().setUserCredentials(cred).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));
        getUserApiClient().createUser(brokered.getName(), testUser.toJson(brokered.getName(), "userpepinator"), HTTP_CREATED);

        //first char of username must be a-z0-9 (other symbols respond with HTTP:400)
        UserCredentials cred2 = new UserCredentials("-hyphensymbolfirst", "password");
        User testUser2 = new User().setUserCredentials(cred2).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));
        assertThrows(ExecutionException.class, () ->
                getUserApiClient().createUser(brokered.getName(), testUser2.toJson(brokered.getName(), "userpepinator"), HTTP_BAD_REQUEST));

        //hyphen is allowed elsewhere in user name (response HTTP:201)
        UserCredentials cred3 = new UserCredentials("user-pepinator", "password");
        User testUser3 = new User().setUserCredentials(cred3).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));
        getUserApiClient().createUser(brokered.getName(), testUser3.toJson(brokered.getName(), "userpepinator"), HTTP_CREATED);

        //underscore is also allowed in user name (response HTTP:201)
        UserCredentials cred4 = new UserCredentials("user_pepinator", "password");
        User testUser4 = new User().setUserCredentials(cred4).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));

        getUserApiClient().createUser(brokered.getName(), testUser4.toJson(brokered.getName(), "userpepinator"), HTTP_CREATED);

        //last char must also be a-z (other symbols respond with HTTP:400)
        UserCredentials cred5 = new UserCredentials("hyphensymbollast-", "password");
        User testUser5 = new User().setUserCredentials(cred5).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));
        assertThrows(ExecutionException.class, () ->
                getUserApiClient().createUser(brokered.getName(), testUser5.toJson(brokered.getName(), "userpepinator"), HTTP_BAD_REQUEST));

        //username may start/end with 0-9 ()
        UserCredentials cred6 = new UserCredentials("01234usernamehere56789", "password");
        User testUser6 = new User().setUserCredentials(cred6).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));

        getUserApiClient().createUser(brokered.getName(), testUser6.toJson(brokered.getName(), "userpepinator"), HTTP_CREATED);

        //Foreign symbols may not be used in username (response HTTP:400)
        UserCredentials cred7 = new UserCredentials("invalid_Ö_username", "password");
        User testUser7 = new User().setUserCredentials(cred7).addAuthorization(
                new User.AuthorizationRule()
                        .addAddress("*")
                        .addOperation(User.Operation.RECEIVE));
        assertThrows(ExecutionException.class, () ->
                getUserApiClient().createUser(brokered.getName(), testUser7.toJson(brokered.getName(), "userpepinator"), HTTP_BAD_REQUEST));
    }

    @Test
    void testFederatedUsers() throws Exception {
        UserCredentials ocUser = new UserCredentials("test-federated-user", "testovaci");
        createUserFederated(brokered, ocUser);

        assertThat(KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), ocUser.getUsername()).getRetCode(), is(true));

        assertCanOpenWebconsole(brokered, ocUser);

        //delete user
        assertThat("User deleting failed using oc cmd",
                KubeCMDClient.deleteUser(kubernetes.getNamespace(), brokered.getName(), ocUser.getUsername()).getRetCode(), is(true));
        assertThat("User is still present",
                KubeCMDClient.getUser(kubernetes.getNamespace(), brokered.getName(), ocUser.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testServiceaccountUser() throws Exception {
        Destination queue = Destination.queue("test-queue", DestinationPlan.STANDARD_SMALL_QUEUE);
        setAddresses(standard, queue);
        UserCredentials serviceAccount = new UserCredentials("test-service-account", "");
        createUserServiceAccount(standard, serviceAccount, environment.namespace());
        UserCredentials messagingUser = new UserCredentials("@@serviceaccount@@",
                kubernetes.getServiceaccountToken(serviceAccount.getUsername(), environment.namespace()));
        log.info("username: {}, password: {}", messagingUser.getUsername(), messagingUser.getPassword());

        assertCanConnect(standard, messagingUser, Collections.singletonList(queue));

        //delete user
        assertThat("User deleting failed using oc cmd",
                KubeCMDClient.deleteUser(kubernetes.getNamespace(), standard.getName(), serviceAccount.getUsername()).getRetCode(), is(true));
        assertThat("User is still present",
                KubeCMDClient.getUser(kubernetes.getNamespace(), standard.getName(), serviceAccount.getUsername()).getRetCode(), is(false));

        assertCannotConnect(standard, messagingUser, Collections.singletonList(queue));

        kubernetes.deleteServiceAccount("test-service-account", environment.namespace());
    }

    private void assertCanOpenWebconsole(AddressSpace addressSpace, UserCredentials credentials) throws Exception {
        SeleniumProvider selenium = new SeleniumProvider();
        try {
            SeleniumContainers.deployFirefoxContainer();
            selenium.setupDriver(environment, kubernetes, TestUtils.getFirefoxDriver());
            ConsoleWebPage page = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace, credentials);
            page.openWebConsolePage(credentials, true, true);
        } finally {
            selenium.saveScreenShots("UserApiTests", "testFederatedUsers");
            selenium.tearDownDrivers();
            SeleniumContainers.stopAndRemoveFirefoxContainer();
        }
    }
}
