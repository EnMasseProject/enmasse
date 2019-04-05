/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.apiclients.UserApiClient;
import io.enmasse.user.model.v1.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class UserUtils {
    private static Logger log = CustomLogger.getLogger();

    public static User createUserObject(UserCredentials cred) {
        return createUserResource(cred, UserAuthenticationType.password).done();
    }

    public static User createUserObject(UserAuthenticationType type, UserCredentials cred) {
        return createUserResource(cred, type).done();
    }

    public static User createUserObject(UserAuthenticationType type) {
        return new UserBuilder().withNewSpec().withNewAuthentication().withType(type).endAuthentication().endSpec().build();
    }

    public static User createUserObject(UserAuthenticationType type, String username) {
        return new UserBuilder().withNewSpec().withUsername(username).withNewAuthentication().withType(type).endAuthentication().endSpec().build();
    }

    public static User createUserObject(UserCredentials cred, List<UserAuthorization> authz) {
        return new UserBuilder()
                .withNewSpec()
                .withUsername(cred.getUsername())
                .withNewAuthentication()
                .withPassword(passwordToBase64(cred.getPassword()))
                .withType(UserAuthenticationType.password)
                .endAuthentication()
                .withAuthorization(authz)
                .endSpec()
                .build();
    }

    public static User createUserObject(String username, String password) {
        return new UserBuilder()
                .withNewSpec()
                .withUsername(username)
                .withNewAuthentication()
                .withPassword(passwordToBase64(password))
                .withType(UserAuthenticationType.password)
                .endAuthentication()
                .endSpec()
                .build();
    }

    public static User createUserObject(String username, String password, List<UserAuthorization> authz) {
        return new UserBuilder()
                .withNewSpec()
                .withUsername(username)
                .withNewAuthentication()
                .withPassword(passwordToBase64(password))
                .withType(UserAuthenticationType.password)
                .endAuthentication()
                .withAuthorization(authz)
                .endSpec()
                .build();
    }


    private static DoneableUser createUserResource(UserCredentials cred, UserAuthenticationType type) {
        return new DoneableUser(new UserBuilder()
                .withNewSpec()
                .withUsername(cred.getUsername())
                .withNewAuthentication()
                .withPassword(passwordToBase64(cred.getPassword()))
                .withType(type)
                .endAuthentication()
                .withAuthorization(Arrays.asList(
                        new UserAuthorizationBuilder()
                                .withAddresses("*")
                                .withOperations(Arrays.asList(
                                        Operation.send,
                                        Operation.recv,
                                        Operation.view
                                ))
                                .build(),
                        new UserAuthorizationBuilder()
                                .withOperations(Operation.manage)
                                .build()
                ))
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

    public static JsonObject userToJson(String addressspace, String metaUserName, User user) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(new DoneableUser(user)
                .editMetadata()
                .withName(String.format("%s.%s", addressspace, Pattern.compile(".*:").matcher(metaUserName).replaceAll("")))
                .endMetadata()
                .done()));
    }

    public static User userToJson(String addressSpace, String metaUserName, String federatedUserName, String federatedUserId, User user) {
        return new DoneableUser(user)
                .editMetadata()
                .withName(String.format("%s.%s", addressSpace, Pattern.compile(".*:").matcher(metaUserName).replaceAll("")))
                .endMetadata()
                .editSpec()
                .withNewAuthentication()
                .withType(UserAuthenticationType.federated)
                .withFederatedUsername(federatedUserName)
                .withFederatedUserid(federatedUserId)
                .withProvider("openshift")
                .endAuthentication()
                .endSpec()
                .done();
    }

    public static User getUserObject(UserApiClient apiClient, String addressSpace, String username) throws Exception {
        JsonObject response = apiClient.getUser(addressSpace, username);
        return jsonResponseToUser(response).stream().findFirst().orElse(null);
    }

    public static Future<List<User>> getAllUsersObjects(UserApiClient apiClient) throws Exception {
        JsonObject response = apiClient.getAllUsers();
        CompletableFuture<List<User>> listOfUsers = new CompletableFuture<>();
        listOfUsers.complete(jsonResponseToUser(response));
        return listOfUsers;
    }

    private static List<User> jsonResponseToUser(JsonObject userJson) throws IOException {
        if (userJson == null) {
            throw new IllegalArgumentException("null response can't be converted to User");
        }
        log.info("Got User object: {}", userJson.toString());
        String kind = userJson.getString("kind");
        List<User> resultUser = new ArrayList<>();
        switch (kind) {
            case "MessagingUser":
                resultUser.add(jsonToUser(userJson));
                break;
            case "MessagingUserList":
                JsonArray items = userJson.getJsonArray("items");
                for (int i = 0; i < items.size(); i++) {
                    resultUser.add(jsonToUser(items.getJsonObject(i)));
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown kind: '%s'", kind));
        }
        return resultUser;
    }

    public static User jsonToUser(JsonObject userJson) throws IOException {
        return new ObjectMapper().readValue(userJson.toString(), User.class);
    }

    public static String passwordToBase64(String password) {
        return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64ToPassword(String password) {
        return new String(Base64.getDecoder().decode(password), StandardCharsets.UTF_8);
    }

    public static UserCredentials getCredentialsFromUser(User user) {
        return new UserCredentials(user.getSpec().getUsername(), base64ToPassword(user.getSpec().getAuthentication().getPassword()));
    }
}
