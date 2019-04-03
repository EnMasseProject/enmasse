/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.keycloak;

import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.user.keycloak.KeycloakFactory;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Keycloak implements KeycloakApi {

    private static final Logger log = LoggerFactory.getLogger(Keycloak.class);

    private static final String OPENSHIFT_PROVIDER_ALIAS = "openshift-v3";
    private static final String IDENTITY_PROVIDER_CLIENT_SECRET = "clientSecret";
    private static final String IDENTITY_PROVIDER_CLIENT_ID = "clientId";
    private static final String IDENTITY_PROVIDER_BASE_URL = "baseUrl";

    private final KeycloakFactory keycloakFactory;
    private final Map<String, Map<String, KeycloakRealmParams>> realmState = new ConcurrentHashMap<>();
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
        return consumer.handle(keycloakMap.get(authenticationService.getMetadata().getName()));
    }

    @Override
    public Set<String> getRealmNames(AuthenticationService authenticationService) {
        return withKeycloak(authenticationService, kc -> kc.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet()));
    }

    @Override
    public void createRealm(AuthenticationService authenticationService, String namespace, String realmName, String consoleRedirectURI, KeycloakRealmParams params) {
        final RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(realmName);
        newRealm.setEnabled(true);
        newRealm.setPasswordPolicy("hashAlgorithm(scramsha1)");
        newRealm.setAttributes(new HashMap<>());
        newRealm.getAttributes().put("namespace", namespace);
        newRealm.getAttributes().put("enmasse-realm", "true");

        if (params.getBrowserSecurityHeaders() != null && !params.getBrowserSecurityHeaders().isEmpty()) {
            log.info("Setting browser security headers to {}", params.getBrowserSecurityHeaders());
            newRealm.setBrowserSecurityHeaders(params.getBrowserSecurityHeaders());
        } else {
            log.info("Not setting browser security headers");
        }

        if (params.getIdentityProviderUrl() != null && params.getIdentityProviderClientId() != null && params.getIdentityProviderClientSecret() != null) {
            IdentityProviderRepresentation openshiftIdProvider = new IdentityProviderRepresentation();

            Map<String, String> config = new HashMap<>();

            config.put(IDENTITY_PROVIDER_BASE_URL, params.getIdentityProviderUrl());
            config.put(IDENTITY_PROVIDER_CLIENT_ID, params.getIdentityProviderClientId());
            config.put(IDENTITY_PROVIDER_CLIENT_SECRET, params.getIdentityProviderClientSecret());

            openshiftIdProvider.setConfig(config);
            openshiftIdProvider.setEnabled(true);
            openshiftIdProvider.setProviderId("openshift-v3");
            openshiftIdProvider.setAlias(OPENSHIFT_PROVIDER_ALIAS);
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

        withKeycloak(authenticationService, kc -> {
            kc.realms().create(newRealm);
            Map<String, KeycloakRealmParams> state = realmState.computeIfAbsent(authenticationService.getMetadata().getName(), k -> new HashMap<>());
            state.put(realmName, params);
            return true;
        });
    }

    @Override
    public void updateRealm(AuthenticationService authenticationService, String realmName, KeycloakRealmParams updated) {
        Map<String, KeycloakRealmParams> state = realmState.computeIfAbsent(authenticationService.getMetadata().getName(), k -> new HashMap<>());
        KeycloakRealmParams current = state.getOrDefault(realmName, KeycloakRealmParams.NULL_PARAMS);
        if (!updated.equals(current)) {
            withKeycloak(authenticationService, kc -> {
                RealmResource realm = kc.realm(realmName);
                if (realm != null) {

                    IdentityProvidersResource identityProvidersResource = realm.identityProviders();
                    IdentityProviderResource identityProviderResource = identityProvidersResource.get(OPENSHIFT_PROVIDER_ALIAS);
                    Set<String> updatedProviderItems = new HashSet<>();

                    if (identityProviderResource != null) {
                        IdentityProviderRepresentation identityProviderRepresentation = identityProviderResource.toRepresentation();
                        Map<String, String> newConfig = new HashMap<>(identityProviderRepresentation.getConfig());
                        String updatedBaseUrl = updated.getIdentityProviderUrl();
                        if (!Objects.equals(updatedBaseUrl, current.getIdentityProviderUrl())) {
                            newConfig.put(IDENTITY_PROVIDER_BASE_URL, updatedBaseUrl);
                            updatedProviderItems.add(IDENTITY_PROVIDER_BASE_URL);
                        }

                        String updatedClientId = updated.getIdentityProviderClientId();
                        if (!Objects.equals(updatedClientId, current.getIdentityProviderClientId())) {
                            newConfig.put(IDENTITY_PROVIDER_CLIENT_ID, updatedClientId);
                            updatedProviderItems.add(IDENTITY_PROVIDER_CLIENT_ID);
                        }

                        String updatedSecret = updated.getIdentityProviderClientSecret();
                        if (!Objects.equals(updatedSecret, current.getIdentityProviderClientSecret())) {
                            newConfig.put(IDENTITY_PROVIDER_CLIENT_SECRET, updatedSecret);
                            updatedProviderItems.add(IDENTITY_PROVIDER_CLIENT_SECRET);
                        }

                        if (!updatedProviderItems.isEmpty()) {
                            identityProviderRepresentation.setConfig(newConfig);
                            identityProviderResource.update(identityProviderRepresentation);
                            log.info("Updated identity provider alias {}. Parameters updated: {}",
                                    OPENSHIFT_PROVIDER_ALIAS, updatedProviderItems);
                        }
                    } else {
                        log.info("Could not find identity provider with alias {}", OPENSHIFT_PROVIDER_ALIAS);
                    }

                    RealmRepresentation realmRep = realm.toRepresentation();
                    boolean browserSecurityHeadersChanged = !Objects.equals(realmRep.getBrowserSecurityHeaders(), current.getBrowserSecurityHeaders());
                    if (browserSecurityHeadersChanged) {
                        realmRep.setBrowserSecurityHeaders(updated.getBrowserSecurityHeaders());
                        realm.update(realmRep);
                        log.info("Updated browser security headers of {}", realmName);
                    }

                    if (!updatedProviderItems.isEmpty() || browserSecurityHeadersChanged) {
                        state.put(realmName, updated);
                    }
                }
                return true;
            });

        }

    }

    @Override
    public void deleteRealm(AuthenticationService authenticationService, String realmName) {
        withKeycloak(authenticationService, kc -> {

            try {
                kc.realm(realmName).remove();
            } finally {
                realmState.remove(realmName);
            }

            return true;
        });
    }
}
