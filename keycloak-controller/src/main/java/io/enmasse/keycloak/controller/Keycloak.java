/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.enmasse.user.keycloak.KeycloakFactory;
import org.keycloak.representations.idm.*;

import java.util.*;
import java.util.stream.Collectors;

public class Keycloak implements KeycloakApi {

    private final IdentityProviderParams params;
    private final KeycloakFactory keycloakFactory;
    private volatile org.keycloak.admin.client.Keycloak keycloak;

    public Keycloak(KeycloakFactory keycloakFactory, IdentityProviderParams params) {
        this.keycloakFactory = keycloakFactory;
        this.params = params;
    }

    interface Handler<T> {
        T handle(org.keycloak.admin.client.Keycloak keycloak);
    }

    private synchronized <T> T withKeycloak(Handler<T> consumer) {
        if (keycloak == null) {
            keycloak = keycloakFactory.createInstance();
        }
        return consumer.handle(keycloak);
    }

    @Override
    public Set<String> getRealmNames() {
        return withKeycloak(kc -> kc.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet()));
    }

    @Override
    public void createRealm(String namespace, String realmName, String consoleRedirectURI) {
        final RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(realmName);
        newRealm.setEnabled(true);
        newRealm.setPasswordPolicy("hashAlgorithm(scramsha1)");
        newRealm.setAttributes(new HashMap<>());
        newRealm.getAttributes().put("namespace", namespace);
        newRealm.getAttributes().put("enmasse-realm", "true");

        if (params.getIdentityProviderUrl() != null && params.getIdentityProviderClientId() != null && params.getIdentityProviderClientSecret() != null) {
            IdentityProviderRepresentation openshiftIdProvider = new IdentityProviderRepresentation();

            Map<String, String> config = new HashMap<>();

            config.put("baseUrl", params.getIdentityProviderUrl());
            config.put("clientId", params.getIdentityProviderClientId());
            config.put("clientSecret", params.getIdentityProviderClientSecret());

            openshiftIdProvider.setConfig(config);
            openshiftIdProvider.setEnabled(true);
            openshiftIdProvider.setProviderId("openshift-v3");
            openshiftIdProvider.setAlias("openshift-v3");
            openshiftIdProvider.setDisplayName("OpenShift");
            openshiftIdProvider.setTrustEmail(true);
            openshiftIdProvider.setFirstBrokerLoginFlowAlias("direct grant");

            newRealm.addIdentityProvider(openshiftIdProvider);
        }

        if (consoleRedirectURI != null) {
            final ClientRepresentation console = new ClientRepresentation();
            console.setClientId("enmasse-console");
            console.setPublicClient(true);
            console.setRedirectUris(Collections.singletonList(consoleRedirectURI));
            newRealm.setClients(Collections.singletonList(console));
        }

        withKeycloak(kc -> {
            kc.realms().create(newRealm);
            return true;
        });
    }

    @Override
    public void deleteRealm(String realmName) {
        withKeycloak(kc -> {
            kc.realm(realmName).remove();
            return true;
        });
    }
}
