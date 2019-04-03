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

public interface UserApi {
    boolean isAvailable(AuthenticationService authenticationService);
    Optional<User> getUserWithName(AuthenticationService authenticationService, String realm, String name) throws Exception;
    void createUser(AuthenticationService authenticationService, String realm, User user) throws Exception;
    boolean replaceUser(AuthenticationService authenticationService, String realm, User user) throws Exception;
    void deleteUser(AuthenticationService authenticationService, String realm, User user) throws Exception;

    boolean realmExists(AuthenticationService authenticationService, String realm);

    UserList listUsers(AuthenticationService authenticationService, String namespace);
    UserList listUsersWithLabels(AuthenticationService authenticationService, String namespace, Map<String, String> labels);
    UserList listAllUsers(AuthenticationService authenticationService);
    UserList listAllUsersWithLabels(AuthenticationService authenticationService, Map<String, String> labels);

    void deleteUsers(AuthenticationService authenticationService, String namespace);
}
