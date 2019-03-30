/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.api;

import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;

import java.util.Map;
import java.util.Optional;

public class NullUserApi implements UserApi {
    @Override
    public boolean isAvailable(AuthenticationService authenticationService) {
        return true;
    }

    @Override
    public Optional<User> getUserWithName(AuthenticationService authenticationService, String realm, String name) {
        return Optional.empty();
    }

    @Override
    public void createUser(AuthenticationService authenticationService, String realm, User user) {
    }

    @Override
    public boolean replaceUser(AuthenticationService authenticationService, String realm, User user) {
        return true;
    }

    @Override
    public void deleteUser(AuthenticationService authenticationService, String realm, User user) {
    }

    @Override
    public boolean realmExists(AuthenticationService authenticationService, String realm) {
        return true;
    }

    @Override
    public UserList listUsers(AuthenticationService authenticationService, String namespace) {
        return new UserList();
    }

    @Override
    public UserList listUsersWithLabels(AuthenticationService authenticationService, String namespace, Map<String, String> labels) {
        return new UserList();
    }

    @Override
    public UserList listAllUsers(AuthenticationService authenticationService) {
        return new UserList();
    }

    @Override
    public UserList listAllUsersWithLabels(AuthenticationService authenticationService, Map<String, String> labels) {
        return new UserList();
    }

    @Override
    public void deleteUsers(AuthenticationService authenticationService, String namespace) {
    }
}
