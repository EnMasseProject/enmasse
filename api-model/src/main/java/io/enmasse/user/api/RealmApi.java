/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.api;

import io.enmasse.admin.model.v1.AuthenticationService;

import java.util.Set;

public interface RealmApi {
    Set<String> getRealmNames(AuthenticationService authenticationService) throws Exception;
    void createRealm(AuthenticationService authenticationService, String namespace, String realmName) throws Exception;
    void deleteRealm(AuthenticationService authenticationService, String realmName) throws Exception;
}
