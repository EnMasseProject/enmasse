/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.api;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserApiTest extends TestBase implements ITestSharedBrokered {

    private Map<AddressSpace, User> users = new HashMap<>();
    private static final Logger LOGGER = CustomLogger.getLogger();

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
    void testCreateDeleteUserUsingCRD() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserResource(cred)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("jenda")
                                .withOperations(Operation.send, Operation.recv).build()))
                .endSpec()
                .done();

        JsonObject userDefinitionPayload = UserUtils.userToJson(getSharedAddressSpace().getMetadata().getName(), testUser);
        users.put(getSharedAddressSpace(), testUser);

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));

        //patch user
        assertThat(KubeCMDClient.patchCR(User.KIND.toLowerCase(), getSharedAddressSpace().getMetadata().getName() + "." + cred.getUsername(), "{\"spec\":{\"authentication\":{\"password\":\"aGVp\"}}}").getRetCode(), is(true));

        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testCreateUserWithWrongPayloadCRD() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserResource(cred)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("jenda")
                                .withOperations(Operation.send, Operation.recv).build()))
                .endSpec()
                .done();

        JsonObject userDefinitionPayload = UserUtils.userToJson(getSharedAddressSpace().getMetadata().getName(), testUser);
        userDefinitionPayload.getJsonObject("spec").getJsonArray("authorization").getJsonObject(0).getJsonArray("operations").add("unknown");
        users.put(getSharedAddressSpace(), testUser);

        //create user
        ExecutionResultData createUserResponse = KubeCMDClient.createCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString());
        assertThat(createUserResponse.getRetCode(), is(false));
        assertTrue(createUserResponse.getStdErr().contains("value not one of declared Enum instance names: [send, view, recv, manage]"));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));

        User testUser2 = UserUtils.createUserResource(cred)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("jenda")
                                .withOperations(Operation.send, Operation.recv).build()))
                .endSpec()
                .done();

        JsonObject userDefinitionPayload2 = UserUtils.userToJson("", testUser2);

        //create user
        ExecutionResultData createUserResponse2 = KubeCMDClient.createCR(kubernetes.getInfraNamespace(), userDefinitionPayload2.toString());
        assertThat(createUserResponse2.getRetCode(), is(false));
        assertTrue(createUserResponse2.getStdErr().contains(String.format("The name of the object (.%s) is not valid", cred.getUsername())));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testUpdateUserPermissionsCRD() throws Exception {
        UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
        User testUser = UserUtils.createUserResource(cred)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("jenda")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();

        JsonObject userDefinitionPayload = UserUtils.userToJson(getSharedAddressSpace().getMetadata().getName(), testUser);
        users.put(getSharedAddressSpace(), testUser);

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));

        testUser = new DoneableUser(testUser)
                .editSpec()
                .editFirstAuthorization()
                .removeFromOperations(Operation.send)
                .addToOperations(Operation.recv)
                .endAuthorization()
                .endSpec().done();

        userDefinitionPayload = UserUtils.userToJson(getSharedAddressSpace().getMetadata().getName(), testUser);

        //update user
        assertThat(KubeCMDClient.updateCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertTrue(resourcesManager.getUser(getSharedAddressSpace(), testUser.getSpec().getUsername()).toString().contains(Operation.recv.toString()));


        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), getSharedAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testUserWithSimilarNamesAndAlreadyExistingUser() {
        UserCredentials cred = new UserCredentials("user2", "user2");
        User testUser = resourcesManager.createOrUpdateUser(getSharedAddressSpace(), cred);

        UserCredentials cred2 = new UserCredentials("user23", "test_user23");
        User testUser2 = resourcesManager.createOrUpdateUser(getSharedAddressSpace(), cred2);

        users.put(getSharedAddressSpace(), testUser);
        users.put(getSharedAddressSpace(), testUser2);

        assertThrows(KubernetesClientException.class, () ->
                kubernetes.getUserClient(getSharedAddressSpace().getMetadata().getNamespace()).create(resourcesManager.getUser(getSharedAddressSpace(), testUser.getSpec().getUsername())));
    }

    @Test
    void testCreateUserUppercaseUsername() {
        UserCredentials cred = new UserCredentials("UserPepinator", "ff^%fh16");
        User testUser = UserUtils.createUserResource(cred)
                .editMetadata()
                .withName(getSharedAddressSpace().getMetadata().getName() + "." + "userpepinator")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send, Operation.recv).build()))
                .endSpec()
                .done();

        assertThrows(KubernetesClientException.class, () -> resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser));
    }

    @Test
    void testCreateUsersWithSymbolsInVariousPlaces() {
        //valid user is created (response HTTP:201)
        UserCredentials cred = new UserCredentials("normalusername", "password");
        User testUser = UserUtils.createUserResource(cred)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        users.put(getSharedAddressSpace(), testUser);
        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser);

        //first char of username must be a-z0-9 (other symbols respond with HTTP:400)
        UserCredentials cred2 = new UserCredentials("-hyphensymbolfirst", "password");
        User testUser2 = UserUtils.createUserResource(cred2)
                .editMetadata()
                .withName(getSharedAddressSpace().getMetadata().getName() + "." + "userpepinator")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        assertThrows(KubernetesClientException.class, () -> resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser2));

        //hyphen is allowed elsewhere in user name (response HTTP:201)
        UserCredentials cred3 = new UserCredentials("user-pepinator", "password");
        User testUser3 = UserUtils.createUserResource(cred3)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        users.put(getSharedAddressSpace(), testUser3);
        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser3);

        //underscore is also allowed in user name (response HTTP:201)
        UserCredentials cred4 = new UserCredentials("user_pepinator", "password");
        User testUser4 = UserUtils.createUserResource(cred4)
                .editMetadata()
                .withName(getSharedAddressSpace().getMetadata().getName() + "." + "testuser")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        users.put(getSharedAddressSpace(), testUser4);
        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser4);

        //last char must also be a-z (other symbols respond with HTTP:400)
        UserCredentials cred5 = new UserCredentials("hyphensymbollast-", "password");
        User testUser5 = UserUtils.createUserResource(cred5)
                .editMetadata()
                .withName(getSharedAddressSpace().getMetadata().getName() + "." + "userpepinator")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        assertThrows(KubernetesClientException.class, () ->
                resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser5));

        //username may start/end with 0-9 ()
        UserCredentials cred6 = new UserCredentials("01234usernamehere56789", "password");
        User testUser6 = UserUtils.createUserResource(cred6)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        users.put(getSharedAddressSpace(), testUser6);
        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser6);

        //Foreign symbols may not be used in username (response HTTP:400)
        UserCredentials cred7 = new UserCredentials("invalid_Ö_username", "password");
        User testUser7 = UserUtils.createUserResource(cred7)
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        testUser7.getMetadata().setName("userpepinator");
        assertThrows(KubernetesClientException.class, () ->
                resourcesManager.createOrUpdateUser(getSharedAddressSpace(), testUser5));
    }
}
