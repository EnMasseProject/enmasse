/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.keycloak;

import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.user.keycloak.KeycloakFactory;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Keycloak implements KeycloakApi {

    private final KeycloakFactory keycloakFactory;
    private final Map<String, org.keycloak.admin.client.Keycloak> keycloakMap = new HashMap<>();

    public Keycloak(KeycloakFactory keycloakFactory) {
        this.keycloakFactory = keycloakFactory;
    }

    interface Handler<T> {
        T handle(org.keycloak.admin.client.Keycloak keycloak);
    }

    private synchronized <T> T withKeycloak(AuthenticationService authenticationService, Handler<T> consumer) {
        if (keycloakMap.get(authenticationService.getMetadata().getName()) == null) {
            keycloakMap.put(authenticationService.getMetadata().getName(), keycloakFactory.createInstance(authenticationService));
        }
        org.keycloak.admin.client.Keycloak keycloak = keycloakMap.get(authenticationService.getMetadata().getName());
        try {
            return consumer.handle(keycloak);
        } catch (Exception e) {
            keycloakMap.remove(authenticationService.getMetadata().getName());
            keycloak.close();
            throw e;
        }
    }

    @Override
    public Set<String> getRealmNames(AuthenticationService authenticationService) {
        return withKeycloak(authenticationService, kc -> kc.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet()));
    }

    @Override
    public void createRealm(AuthenticationService authenticationService, String namespace, String realmName) {
        final RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(realmName);
        newRealm.setEnabled(true);
        newRealm.setPasswordPolicy("hashAlgorithm(scramsha1)");
        newRealm.setAttributes(new HashMap<>());
        newRealm.getAttributes().put("namespace", namespace);
        newRealm.getAttributes().put("enmasse-realm", "true");

        withKeycloak(authenticationService, kc -> {
            kc.realms().create(newRealm);
            return true;
        });
    }

    @Override
    public void deleteRealm(AuthenticationService authenticationService, String realmName) {
        withKeycloak(authenticationService, kc -> {
            kc.realm(realmName).remove();
            return true;
        });
    }
}
