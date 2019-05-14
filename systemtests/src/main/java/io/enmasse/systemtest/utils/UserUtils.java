/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.UserCredentials;
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

    public static JsonObject userToJson(String addressspace, String metaUserName, User user) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(new DoneableUser(user)
                .editMetadata()
                .withName(String.format("%s.%s", addressspace, Pattern.compile(".*:").matcher(metaUserName).replaceAll("")))
                .endMetadata()
                .done()));
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
