/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.api;

import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;

import java.util.Map;
import java.util.Optional;

public class DelegateUserApi implements UserApi {
    private final Map<AuthenticationServiceType, UserApi> userApiMap;

    public DelegateUserApi(Map<AuthenticationServiceType, UserApi> userApiMap) {
        this.userApiMap = userApiMap;
    }

    @Override
    public boolean isAvailable(AuthenticationService authenticationService) {
        return userApiMap.get(authenticationService.getSpec().getType()).isAvailable(authenticationService);
    }

    @Override
    public Optional<User> getUserWithName(AuthenticationService authenticationService, String realm, String name) throws Exception {
        return userApiMap.get(authenticationService.getSpec().getType()).getUserWithName(authenticationService, realm, name);
    }

    @Override
    public void createUser(AuthenticationService authenticationService, String realm, User user) throws Exception {
        userApiMap.get(authenticationService.getSpec().getType()).createUser(authenticationService, realm, user);
    }

    @Override
    public boolean replaceUser(AuthenticationService authenticationService, String realm, User user) throws Exception {
        return userApiMap.get(authenticationService.getSpec().getType()).replaceUser(authenticationService, realm, user);
    }

    @Override
    public void deleteUser(AuthenticationService authenticationService, String realm, User user) throws Exception {
        userApiMap.get(authenticationService.getSpec().getType()).deleteUser(authenticationService, realm, user);
    }

    @Override
    public boolean realmExists(AuthenticationService authenticationService, String realm) {
        return userApiMap.get(authenticationService.getSpec().getType()).realmExists(authenticationService, realm);
    }

    @Override
    public UserList listUsers(AuthenticationService authenticationService, String namespace) {
        return userApiMap.get(authenticationService.getSpec().getType()).listUsers(authenticationService, namespace);
    }

    @Override
    public UserList listUsersWithLabels(AuthenticationService authenticationService, String namespace, Map<String, String> labels) {
        return userApiMap.get(authenticationService.getSpec().getType()).listUsersWithLabels(authenticationService, namespace, labels);
    }

    @Override
    public UserList listAllUsers(AuthenticationService authenticationService) {
        return userApiMap.get(authenticationService.getSpec().getType()).listAllUsers(authenticationService);
    }

    @Override
    public UserList listAllUsersWithLabels(AuthenticationService authenticationService, Map<String, String> labels) {
        return userApiMap.get(authenticationService.getSpec().getType()).listAllUsersWithLabels(authenticationService, labels);
    }

    @Override
    public void deleteUsers(AuthenticationService authenticationService, String namespace) {
        userApiMap.get(authenticationService.getSpec().getType()).deleteUsers(authenticationService, namespace);
    }
}

