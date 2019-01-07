/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class User {
    private Type type;
    private String username;
    private String password;

    public User() {
        this.type = Type.PASSWORD;
    }

    public User(Type type) {
        this.type = type;
    }

    public User(Type type, String username) {
        this.type = type;
        this.username = username;
    }

    private final List<AuthorizationRule> authorization = new ArrayList<>();

    public Type getType() {
        return type;
    }

    public User setType(Type type) {
        this.type = type;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }

    public User addAuthorization(AuthorizationRule rule) {
        this.authorization.add(rule);
        return this;
    }

    public User setUserCredentials(UserCredentials credentials) {
        this.username = credentials.getUsername();
        this.password = credentials.getPassword();
        return this;
    }

    public JsonObject toJson(String addressSpace) {
        return toJson(addressSpace, getUsername(), "", "");
    }

    public JsonObject toJson(String addressSpace, String metaUserName) {
        return toJson(addressSpace, metaUserName, "", "");
    }

    public JsonObject toJson(String addressSpace, String federatedUserName, String federatedUserId) {
        return toJson(addressSpace, getUsername(), federatedUserName, federatedUserId);
    }

    public JsonObject toJson(String addressSpace, String metaUserName, String federatedUserName, String federatedUserId) {
        JsonObject config = new JsonObject();
        JsonObject metadata = new JsonObject();
        metadata.put("name", addressSpace + "." + Pattern.compile(".*:").matcher(metaUserName).replaceAll(""));
        config.put("metadata", metadata);

        JsonObject spec = new JsonObject();
        spec.put("username", getUsername());
        JsonObject authentication = new JsonObject();
        authentication.put("type", type.toString().toLowerCase());
        if (type.equals(Type.PASSWORD) && password != null) {
            authentication.put("password", Base64.getEncoder().encodeToString(getPassword().getBytes(StandardCharsets.UTF_8)));
        } else if (type.equals(Type.FEDERATED)) {
            authentication.put("provider", "openshift");
            authentication.put("federatedUsername", federatedUserName);
            authentication.put("federatedUserid", federatedUserId);
        }
        spec.put("authentication", authentication);

        JsonArray authorization = new JsonArray();

        for (User.AuthorizationRule rule : getAuthorization()) {
            JsonObject authz = new JsonObject();
            JsonArray addresses = new JsonArray();
            JsonArray operations = new JsonArray();

            for (String address : rule.getAddresses()) {
                addresses.add(address);
            }

            for (String operation : rule.getOperations()) {
                operations.add(operation);
            }

            authz.put("addresses", addresses);
            authz.put("operations", operations);
            authorization.add(authz);
        }
        spec.put("authorization", authorization);
        config.put("spec", spec);
        return config;
    }

    public JsonObject toCRDJson(String addressSpace) {
        JsonObject config = this.toJson(addressSpace);
        config.put("apiVersion", "user.enmasse.io/v1alpha1");
        config.put("kind", "MessagingUser");
        return config;
    }

    public JsonObject toCRDJson(String addressSpace, String federatedUserIp) {
        JsonObject config = this.toJson(addressSpace, username, federatedUserIp);
        config.put("apiVersion", "user.enmasse.io/v1alpha1");
        config.put("kind", "MessagingUser");
        return config;
    }

    public UserCredentials getUserCredentials() {
        return new UserCredentials(this.username, this.password);
    }

    public List<AuthorizationRule> getAuthorization() {
        return Collections.unmodifiableList(authorization);
    }

    public static class AuthorizationRule {
        private List<String> addresses = new ArrayList<>();
        private List<String> operations = new ArrayList<>();

        public AuthorizationRule addAddress(String address) {
            this.addresses.add(address);
            return this;
        }

        public AuthorizationRule addOperation(String operation) {
            this.operations.add(operation);
            return this;
        }

        public AuthorizationRule addAddresses(List<String> addresses) {
            this.addresses.addAll(addresses);
            return this;
        }

        public AuthorizationRule addAuthorizationRule(String address, String operation) {
            addAddress(address);
            addOperation(operation);
            return this;
        }

        public AuthorizationRule addAuthorizationRule(String address, String... operations) {
            addAddress(address);
            for (String operation : operations) {
                this.addOperation(operation);
            }
            return this;
        }

        public List<String> getAddresses() {
            return Collections.unmodifiableList(addresses);
        }

        public List<String> getOperations() {
            return Collections.unmodifiableList(operations);
        }
    }

    public class Operation {
        public static final String SEND = "send";
        public static final String RECEIVE = "recv";
        public static final String VIEW = "view";
        public static final String MANAGE = "manage";
    }

    public enum Type {
        PASSWORD,
        FEDERATED,
        SERVICEACCOUNT
    }
}
