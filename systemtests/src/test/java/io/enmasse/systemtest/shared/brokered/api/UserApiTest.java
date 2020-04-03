/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.api;

import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Collections;

import static io.enmasse.systemtest.TestTag.SHARED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(SHARED)
public class UserApiTest extends TestBase {

    private static final Logger LOGGER = CustomLogger.getLogger();

    @BeforeAll
    void initMessaging() throws Exception {
        resourceManager.createDefaultMessaging(AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED);
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

        JsonObject userDefinitionPayload = UserUtils.userToJson(resourceManager.getDefaultAddressSpace().getMetadata().getName(), testUser);

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));

        //patch user
        assertThat(KubeCMDClient.patchCR(User.KIND.toLowerCase(), resourceManager.getDefaultAddressSpace().getMetadata().getName() + "." + cred.getUsername(), "{\"spec\":{\"authentication\":{\"password\":\"aGVp\"}}}").getRetCode(), is(true));

        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
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

        JsonObject userDefinitionPayload = UserUtils.userToJson(resourceManager.getDefaultAddressSpace().getMetadata().getName(), testUser);
        userDefinitionPayload.getJsonObject("spec").getJsonArray("authorization").getJsonObject(0).getJsonArray("operations").add("unknown");

        //create user
        ExecutionResultData createUserResponse = KubeCMDClient.createCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString());
        LOGGER.info("RESPONSE stdout: {}", createUserResponse.getStdOut());
        LOGGER.info("RESPONSE stderr: {}", createUserResponse.getStdErr());
        assertThat(createUserResponse.getRetCode(), is(false));
        assertTrue(createUserResponse.getStdErr().contains("Unsupported value: \"unknown\": supported values: \"send\", \"recv\", \"view\", \"manage\"") ||
                createUserResponse.getStdErr().contains("operations in body should be one of [send recv view manage]"));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));

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
        assertTrue(createUserResponse2.getStdErr().contains("DNS-1123 subdomain must consist of lower case alphanumeric characters"));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
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

        JsonObject userDefinitionPayload = UserUtils.userToJson(resourceManager.getDefaultAddressSpace().getMetadata().getName(), testUser);

        //create user
        assertThat(KubeCMDClient.createCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));

        testUser = new DoneableUser(testUser)
                .editSpec()
                .editFirstAuthorization()
                .removeFromOperations(Operation.send)
                .addToOperations(Operation.recv)
                .endAuthorization()
                .endSpec().done();

        userDefinitionPayload = UserUtils.userToJson(resourceManager.getDefaultAddressSpace().getMetadata().getName(), testUser);

        //update user
        assertThat(KubeCMDClient.updateCR(kubernetes.getInfraNamespace(), userDefinitionPayload.toString()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertTrue(resourceManager.getUser(resourceManager.getDefaultAddressSpace(), testUser.getSpec().getUsername()).toString().contains(Operation.recv.toString()));


        //delete user
        assertThat(KubeCMDClient.deleteUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(true));
        assertThat(KubeCMDClient.getUser(kubernetes.getInfraNamespace(), resourceManager.getDefaultAddressSpace().getMetadata().getName(), cred.getUsername()).getRetCode(), is(false));
    }

    @Test
    void testUserWithSimilarNamesAndAlreadyExistingUser() throws Exception {
        UserCredentials cred = new UserCredentials("user2", "user2");
        User testUser = resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), cred);

        UserCredentials cred2 = new UserCredentials("user23", "test_user23");
        User testUser2 = resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), cred2);


        assertThrows(KubernetesClientException.class, () ->
                kubernetes.getUserClient(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace()).create(resourceManager.getUser(resourceManager.getDefaultAddressSpace(), testUser.getSpec().getUsername())));
    }

    @Test
    void testCreateUserUppercaseUsername() throws Exception {
        UserCredentials cred = new UserCredentials("UserPepinator", "ff^%fh16");
        User testUser = UserUtils.createUserResource(cred)
                .editMetadata()
                .withName(resourceManager.getDefaultAddressSpace().getMetadata().getName() + "." + "userpepinator")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send, Operation.recv).build()))
                .endSpec()
                .done();

        assertThrows(KubernetesClientException.class, () -> resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser));
    }

    @Test
    void testCreateUsersWithSymbolsInVariousPlaces() throws Exception {
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
        resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser);

        //first char of username must be a-z0-9 (other symbols respond with HTTP:400)
        UserCredentials cred2 = new UserCredentials("-hyphensymbolfirst", "password");
        User testUser2 = UserUtils.createUserResource(cred2)
                .editMetadata()
                .withName(resourceManager.getDefaultAddressSpace().getMetadata().getName() + "." + "userpepinator")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        assertThrows(KubernetesClientException.class, () -> resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser2));

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
        resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser3);

        //underscore is also allowed in user name (response HTTP:201)
        UserCredentials cred4 = new UserCredentials("user_pepinator", "password");
        User testUser4 = UserUtils.createUserResource(cred4)
                .editMetadata()
                .withName(resourceManager.getDefaultAddressSpace().getMetadata().getName() + "." + "testuser")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser4);

        //last char must also be a-z (other symbols respond with HTTP:400)
        UserCredentials cred5 = new UserCredentials("hyphensymbollast-", "password");
        User testUser5 = UserUtils.createUserResource(cred5)
                .editMetadata()
                .withName(resourceManager.getDefaultAddressSpace().getMetadata().getName() + "." + "userpepinator")
                .endMetadata()
                .editSpec()
                .withAuthorization(Collections.singletonList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Operation.send).build()))
                .endSpec()
                .done();
        assertThrows(KubernetesClientException.class, () ->
                resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser5));

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
        resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser6);

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
                resourceManager.createOrUpdateUser(resourceManager.getDefaultAddressSpace(), testUser5));
    }
}
