/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private String username;
    private String password;
    // TODO: Support federated user

    private final List<AuthorizationRule> authorization = new ArrayList<>();

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

        public List<String> getAddresses() {
            return Collections.unmodifiableList(addresses);
        }

        public List<String> getOperations() {
            return Collections.unmodifiableList(operations);
        }
    }
}
