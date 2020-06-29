/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.Phase;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserBuilder;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class UserUtils {
    private static Logger log = CustomLogger.getLogger();
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

    public static User waitForUserActive(User user, TimeoutBudget budget) throws Exception {
        var client = Kubernetes.getInstance().getUserClient(user.getMetadata().getNamespace());
        String name = user.getMetadata().getName();
        User foundUser = client.withName(name).get();
        while (budget.timeLeft() >= 0 && !isUserActive(foundUser)) {
            Thread.sleep(1000);
            log.info("Checking if user {} is active: {}", name, foundUser);
            foundUser = client.withName(name).get();
        }

        foundUser = client.withName(name).get();
        if (!isUserActive(foundUser)) {
            throw new IllegalStateException(String.format("User %s is not in phase Active within timeout: %s", name, foundUser));
        }
        log.info("User {} is ready for use", name);
        return foundUser;
    }

    public static void waitForUserDeleted(String namespace, String name, TimeoutBudget budget) throws InterruptedException {
        var client = Kubernetes.getInstance().getUserClient(namespace);
        User foundUser = client.withName(name).get();

        while (budget.timeLeft() >= 0 && foundUser != null) {
            Thread.sleep(1000);
            log.info("Checking if user {} is deleted: {}", name, foundUser);
            foundUser = client.withName(name).get();
        }

        foundUser = client.withName(name).get();
        if (foundUser != null) {
            throw new IllegalStateException(String.format("User %s is not deleted within timeout: %s", name, foundUser));
        }
        log.info("User {} is deleted", name);
    }

    private static boolean isUserActive(User user) {
        return user != null &&
                user.getStatus() != null &&
                Phase.Active.equals(user.getStatus().getPhase()) &&
                user.getStatus().getGeneration() != null && user.getStatus().getGeneration().equals(user.getMetadata().getGeneration());
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
