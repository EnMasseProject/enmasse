/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.keycloak;

import java.util.Set;

public interface KeycloakApi {
    Set<String> getRealmNames();
    boolean isAvailable();
    void createRealm(String namespace, String realmName, String consoleRedirectURI, KeycloakRealmParams params);
    void updateRealm(String realmName, KeycloakRealmParams current, KeycloakRealmParams updated);
    void deleteRealm(String realmName);
}
