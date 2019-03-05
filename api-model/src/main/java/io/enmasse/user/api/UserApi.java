/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.api;

import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;

import java.util.Map;
import java.util.Optional;

public interface UserApi {
    boolean isAvailable();
    Optional<User> getUserWithName(String realm, String name) throws Exception;
    void createUser(String realm, User user) throws Exception;
    boolean replaceUser(String realm, User user) throws Exception;
    void deleteUser(String realm, User user) throws Exception;

    boolean realmExists(String realm);

    UserList listUsers(String namespace);
    UserList listUsersWithLabels(String namespace, Map<String, String> labels);
    UserList listAllUsers();
    UserList listAllUsersWithLabels(Map<String, String> labels);

    void deleteUsers(String namespace);
}
