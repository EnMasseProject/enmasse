/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class UserUtils {
    public static DoneableUser createUserResource(UserCredentials cred) {
        return new DoneableUser(new UserBuilder()
                .withNewSpec()
                .withUsername(cred.getUsername())
                .withNewAuthentication()
                .withPassword(passwordToBase64(cred.getPassword()))
                .withType(UserAuthenticationType.password)
                .endAuthentication()
                .endSpec()
                .build());
    }

    public static JsonObject userToJson(String addressspace, User user) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(new DoneableUser(user)
                .editMetadata()
                .withName(String.format("%s.%s", addressspace, Pattern.compile(".*:").matcher(user.getSpec().getUsername()).replaceAll("")))
                .endMetadata()
                .done()));
    }
    public static String passwordToBase64(String password) {
        return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64ToPassword(String password) {
        return new String(Base64.getDecoder().decode(password), StandardCharsets.UTF_8);
    }

    public static UserCredentials getCredentialsFromUser(User user) {
        return new UserCredentials(user.getSpec().getUsername(), base64ToPassword(user.getSpec().getAuthentication().getPassword()));
    }

    /**
     * create users and groups for wildcard authz tests
     *
     * @param addressSpace the address space
     * @param operation    the operation
     * @return the list
     */
    public static List<User> createUsersWildcard(AddressSpace addressSpace, Operation operation) {
        List<User> users = new ArrayList<>();
        users.add(UserUtils.createUserResource(new UserCredentials("user1", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user2", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("queue/*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user3", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("topic/*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user4", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("queueA*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user5", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("topicA*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        for (User user : users) {
            IsolatedResourcesManager.getInstance().createOrUpdateUser(addressSpace, user);
        }
        return users;
    }
}
