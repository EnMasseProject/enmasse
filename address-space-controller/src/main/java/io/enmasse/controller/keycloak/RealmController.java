/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.keycloak;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.Controller;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RealmController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(RealmController.class);
    private static final String MASTER_REALM = "master";
    private final KeycloakApi keycloak;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;

    public RealmController(KeycloakApi keycloak, AuthenticationServiceRegistry authenticationServiceRegistry) {
        this.keycloak = keycloak;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
    }

    private static class AuthServiceEntry {
        private final List<AddressSpace> addressSpaces = new ArrayList<>();
        private final AuthenticationService authenticationService;

        private AuthServiceEntry(AuthenticationService authenticationService) {
            this.authenticationService = authenticationService;
        }

        public void addAddressSpace(AddressSpace addressSpace) {
            this.addressSpaces.add(addressSpace);
        }

        public List<AddressSpace> getAddressSpaces() {
            return Collections.unmodifiableList(addressSpaces);
        }

        public AuthenticationService getAuthenticationService() {
            return authenticationService;
        }
    }

    @Override
    public void reconcileAll(List<AddressSpace> spaces) throws Exception {
        Map<String, AuthServiceEntry> authserviceMap = new HashMap<>();

        for (AddressSpace addressSpace : spaces) {
            AuthenticationService authenticationService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService()).orElse(null);
            if (authenticationService == null) {
                continue;
            }

            if (authenticationService.getStatus() == null) {
                log.debug("Standard authentication service not configured, not performing any operations");
                continue;
            }

            if (authenticationService.getSpec().getRealm() != null) {
                log.debug("Standard authentication service overriding realm, not performing any operations");
                continue;
            }

            AuthServiceEntry entry = authserviceMap.computeIfAbsent(authenticationService.getMetadata().getName(), k -> new AuthServiceEntry(authenticationService));
            entry.addAddressSpace(addressSpace);
        }

        for (AuthenticationService authenticationService : authenticationServiceRegistry.listAuthenticationServices()) {
            if (authenticationService.getSpec().getType().equals(AuthenticationServiceType.standard) && authenticationService.getSpec().getRealm() == null) {
                Set<String> actualRealms = keycloak.getRealmNames(authenticationService);
                Set<String> desiredRealms = authserviceMap.getOrDefault(authenticationService.getMetadata().getName(), new AuthServiceEntry(authenticationService)).getAddressSpaces().stream()
                        .map(a -> a.getAnnotation(AnnotationKeys.REALM_NAME))
                        .collect(Collectors.toSet());

                log.info("Actual: {}, Desired: {}", actualRealms, desiredRealms);
                for (String realmName : actualRealms) {
                    if (!desiredRealms.contains(realmName) && !MASTER_REALM.equals(realmName)) {
                        log.info("Deleting realm {}", realmName);
                        keycloak.deleteRealm(authenticationService, realmName);
                    }
                }
            }
        }

        for (AuthServiceEntry entry : authserviceMap.values()) {
            AuthenticationService authenticationService = entry.getAuthenticationService();
            List<AddressSpace> addressSpaces = entry.getAddressSpaces();

            Set<String> actualRealms = keycloak.getRealmNames(authenticationService);

            for (AddressSpace addressSpace : addressSpaces) {
                String realmName = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
                if (actualRealms.contains(realmName)) {
                    continue;
                }
                log.info("Creating realm {} in authentication service {}", realmName, authenticationService.getMetadata().getName());
                keycloak.createRealm(authenticationService, addressSpace.getMetadata().getNamespace(), realmName);
            }
        }
    }

    @Override
    public String toString() {
        return "RealmController";
    }
}
