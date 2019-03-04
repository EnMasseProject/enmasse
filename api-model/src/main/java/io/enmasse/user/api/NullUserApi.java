/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.api;

import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;

import java.util.Map;
import java.util.Optional;

public class NullUserApi implements UserApi {
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<User> getUserWithName(String realm, String name) {
        return Optional.empty();
    }

    @Override
    public void createUser(String realm, User user) {
    }

    @Override
    public boolean replaceUser(String realm, User user) {
        return true;
    }

    @Override
    public void deleteUser(String realm, User user) {
    }

    @Override
    public boolean realmExists(String realm) {
        return true;
    }

    @Override
    public UserList listUsers(String namespace) {
        return new UserList();
    }

    @Override
    public UserList listUsersWithLabels(String namespace, Map<String, String> labels) {
        return new UserList();
    }

    @Override
    public UserList listAllUsers() {
        return new UserList();
    }

    @Override
    public UserList listAllUsersWithLabels(Map<String, String> labels) {
        return new UserList();
    }

    @Override
    public void deleteUsers(String namespace) {
    }
}
