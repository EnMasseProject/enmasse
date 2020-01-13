/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;


import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.user.api.RealmApi;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KeycloakRealmApi implements RealmApi {

    private static final Logger log = LoggerFactory.getLogger(KeycloakRealmApi.class);

    private final Clock clock;
    private final KeycloakFactory keycloakFactory;
    private final Duration apiTimeout;
    private final Map<String, Keycloak> keycloakMap = new HashMap<>();

    public KeycloakRealmApi(KeycloakFactory keycloakFactory, Clock clock, Duration apiTimeout) {
        this.keycloakFactory = keycloakFactory;
        this.clock = clock;
        this.apiTimeout = apiTimeout;
    }

    public synchronized void retainAuthenticationServices(List<AuthenticationService> items) {
        Set<String> desired = items.stream().map(a -> a.getMetadata().getName()).collect(Collectors.toSet());
        Set<String> toRemove = new HashSet<>(keycloakMap.keySet());
        toRemove.removeAll(desired);
        for (String authService : toRemove) {
            Keycloak keycloak = keycloakMap.remove(authService);
            if (keycloak != null && !keycloak.isClosed()) {
                keycloak.close();
            }
        }
    }

    interface KeycloakHandler<T> {
        T handle(Keycloak keycloak) throws Exception;
    }

    private synchronized <T> T withKeycloak(AuthenticationService authenticationService, KeycloakHandler<T> consumer) throws Exception {
        if (keycloakMap.get(authenticationService.getMetadata().getName()) == null) {
            keycloakMap.put(authenticationService.getMetadata().getName(), keycloakFactory.createInstance(authenticationService));
        }
        Keycloak keycloak = keycloakMap.get(authenticationService.getMetadata().getName());
        try {
            return consumer.handle(keycloak);
        } catch (Exception e) {
            keycloakMap.remove(authenticationService.getMetadata().getName());
            keycloak.close();
            throw e;
        }
    }

    interface RealmHandler<T> {
        T handle(RealmResource realm);
    }

    private synchronized <T> T withRealm(AuthenticationService authenticationService, String realmName, RealmHandler<T> consumer) throws Exception {
        return withKeycloak(authenticationService, keycloak -> {
            RealmResource realmResource = waitForRealm(keycloak, realmName, apiTimeout);
            return consumer.handle(realmResource);
        });
    }

    private RealmResource waitForRealm(Keycloak keycloak, String realmName, Duration timeout) throws Exception {
        Instant now = clock.instant();
        Instant endTime = now.plus(timeout);
        RealmResource realmResource = null;
        while (now.isBefore(endTime)) {
            realmResource = getRealmResource(keycloak, realmName);
            if (realmResource != null) {
                break;
            }
            log.info("Waiting 1 second for realm {} to exist", realmName);
            Thread.sleep(1000);
            now = clock.instant();
        }

        if (realmResource == null) {
            realmResource = getRealmResource(keycloak, realmName);
        }

        if (realmResource != null) {
            return realmResource;
        }

        throw new WebApplicationException("Timed out waiting for realm " + realmName + " to exist", 503);
    }

    private RealmResource getRealmResource(Keycloak keycloak, String realmName) {
        List<RealmRepresentation> realms = keycloak.realms().findAll();
        for (RealmRepresentation realm : realms) {
            if (realm.getRealm().equals(realmName)) {
                return keycloak.realm(realmName);
            }
        }
        return null;
    }

    @Override
    public Set<String> getRealmNames(AuthenticationService authenticationService) throws Exception {
        return withKeycloak(authenticationService, kc -> kc.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet()));
    }

    @Override
    public void createRealm(AuthenticationService authenticationService, String namespace, String realmName) throws Exception {
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
    public void deleteRealm(AuthenticationService authenticationService, String realmName) throws Exception {
        withKeycloak(authenticationService, kc -> {
            kc.realm(realmName).remove();
            return true;
        });
    }
}
