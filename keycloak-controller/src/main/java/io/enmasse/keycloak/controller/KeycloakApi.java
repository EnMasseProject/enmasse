/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import java.util.Set;

public interface KeycloakApi {
    Set<String> getRealmNames();
    void createRealm(String realmName, String realmAdminUser, String realmAdminUserId, String consoleRedirectURI);
    void deleteRealm(String realmName);
}
