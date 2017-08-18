/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.keycloak.controller;

import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Set;
import java.util.stream.Collectors;

public class Keycloak implements KeycloakApi {
    private final org.keycloak.admin.client.Keycloak keycloak;

    public Keycloak(KeycloakParams params) {
        this.keycloak = org.keycloak.admin.client.Keycloak.getInstance(
            "http://" + params.getHost() + ":" + params.getHttpPort() + "/auth",
                "master",
                params.getAdminUser(),
                params.getAdminPassword(),
                "admin-cli");
    }

    @Override
    public Set<String> getRealmNames() {
        return keycloak.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet());
    }

    @Override
    public void createRealm(String realmName) {
        final RealmRepresentation newrealm = new RealmRepresentation();
        newrealm.setRealm(realmName);
        newrealm.setPasswordPolicy("hashAlgorithm(scramsha1)");
        keycloak.realms().create(newrealm);
    }

    @Override
    public void deleteRealm(String realmName) {
        keycloak.realm(realmName).remove();
    }
}
