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

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Keycloak implements KeycloakApi {

    private final KeycloakParams params;

    public Keycloak(KeycloakParams params) {
        this.params = params;
    }

    @Override
    public Set<String> getRealmNames() {
        try (CloseableKeycloak wrapper = new CloseableKeycloak(params)) {
            return wrapper.get().realms().findAll().stream()
                    .map(RealmRepresentation::getRealm)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public void createRealm(String realmName, String realmAdminUser) {
        final RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(realmName);
        newRealm.setEnabled(true);
        newRealm.setPasswordPolicy("hashAlgorithm(scramsha1)");

        final UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername(realmAdminUser);

        newUser.setEnabled(true);
        newUser.setClientRoles(Collections.singletonMap("realm-management", Collections.singletonList("manage-users")));

        newRealm.setUsers(Collections.singletonList(newUser));

        try (CloseableKeycloak wrapper = new CloseableKeycloak(params)) {
            wrapper.get().realms().create(newRealm);
        }
    }

    @Override
    public void deleteRealm(String realmName) {
        try (CloseableKeycloak wrapper = new CloseableKeycloak(params)) {
            wrapper.get().realm(realmName).remove();
        }
    }

    public static class CloseableKeycloak implements AutoCloseable {

        private final org.keycloak.admin.client.Keycloak keycloak;

        CloseableKeycloak(KeycloakParams params) {
            this.keycloak = KeycloakBuilder.builder()
                .serverUrl("https://" + params.getHost() + ":" + params.getHttpPort() + "/auth")
                .realm("master")
                .username(params.getAdminUser())
                .password(params.getAdminPassword())
                .clientId("admin-cli")
                .resteasyClient(new ResteasyClientBuilder()
                        .trustStore(params.getKeyStore())
                        .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                        .build())
                .build();
        }

        org.keycloak.admin.client.Keycloak get() {
            return keycloak;
        }

        @Override
        public void close() {
            keycloak.close();
        }
    }
}
