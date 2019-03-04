/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.api;

import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;

import java.util.Map;
import java.util.Optional;

public class UserApiWithFallback implements UserApi {
    private final UserApi primary;
    private final UserApi fallback;

    public UserApiWithFallback(UserApi primary, UserApi fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public boolean isAvailable() {
        return primary.isAvailable() || fallback.isAvailable();
    }

    @Override
    public Optional<User> getUserWithName(String realm, String name) throws Exception {
        if (primary.isAvailable()) {
            return primary.getUserWithName(realm, name);
        } else {
            return fallback.getUserWithName(realm, name);
        }
    }

    @Override
    public void createUser(String realm, User user) throws Exception {
        if (primary.isAvailable()) {
            primary.createUser(realm, user);
        } else {
            fallback.createUser(realm, user);
        }
    }

    @Override
    public boolean replaceUser(String realm, User user) throws Exception {
        if (primary.isAvailable()) {
            return primary.replaceUser(realm, user);
        } else {
            return fallback.replaceUser(realm, user);
        }
    }

    @Override
    public void deleteUser(String realm, User user) throws Exception {
        if (primary.isAvailable()) {
            primary.deleteUser(realm, user);
        } else {
            fallback.deleteUser(realm, user);
        }
    }

    @Override
    public boolean realmExists(String realm) {
        if (primary.isAvailable()) {
            return primary.realmExists(realm);
        } else {
            return fallback.realmExists(realm);
        }
    }

    @Override
    public UserList listUsers(String namespace) {
        if (primary.isAvailable()) {
            return primary.listUsers(namespace);
        } else {
            return fallback.listUsers(namespace);
        }
    }

    @Override
    public UserList listUsersWithLabels(String namespace, Map<String, String> labels) {
        if (primary.isAvailable()) {
            return primary.listUsersWithLabels(namespace, labels);
        } else {
            return fallback.listUsersWithLabels(namespace, labels);
        }
    }

    @Override
    public UserList listAllUsers() {
        if (primary.isAvailable()) {
            return primary.listAllUsers();
        } else {
            return fallback.listAllUsers();
        }
    }

    @Override
    public UserList listAllUsersWithLabels(Map<String, String> labels) {
        if (primary.isAvailable()) {
            return primary.listAllUsersWithLabels(labels);
        } else {
            return fallback.listAllUsersWithLabels(labels);
        }
    }

    @Override
    public void deleteUsers(String namespace) {
        if (primary.isAvailable()) {
            primary.deleteUsers(namespace);
        } else {
            fallback.deleteUsers(namespace);
        }
    }
}

