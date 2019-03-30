/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.keycloak;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.Controller;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static io.enmasse.user.model.v1.Operation.*;

public class RealmController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(RealmController.class);
    private static final String MASTER_REALM = "master";
    private final KeycloakApi keycloak;
    private final UserLookupApi userLookupApi;
    private final UserApi userApi;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private KeycloakRealmParams lastParams;

    public RealmController(KeycloakApi keycloak, UserLookupApi userLookupApi, UserApi userApi, AuthenticationServiceRegistry authenticationServiceRegistry) {
        this.keycloak = keycloak;
        this.userLookupApi = userLookupApi;
        this.userApi = userApi;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
    }

    private EndpointSpec getConsoleEndpoint(AddressSpace addressSpace) {
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getService().startsWith("console")) {
                return endpoint;
            }
        }
        return null;
    }

    private EndpointStatus getConsoleEndpointStatus(AddressSpace addressSpace) {
        EndpointSpec spec = getConsoleEndpoint(addressSpace);
        if (spec == null) {
            return null;
        }

        for (EndpointStatus endpoint : addressSpace.getStatus().getEndpointStatuses()) {
            if (endpoint.getName().equals(spec.getName())) {
                return endpoint;
            }
        }
        return null;
    }

    private String getConsoleUri(EndpointStatus endpoint) {
        String uri = null;
        if (endpoint.getExternalHost() != null) {
            uri = "https://" + endpoint.getExternalHost() + "/*";
            log.info("Using {} as redirect URI for enmasse-console", uri);
        }
        return uri;
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

            if (addressSpace.getSpec().getEndpoints() == null) {
                log.debug("No endpoints defined for address space, not performing any operations");
                continue;
            }

            AuthServiceEntry entry = authserviceMap.computeIfAbsent(authenticationService.getMetadata().getName(), k -> new AuthServiceEntry(authenticationService));
            entry.addAddressSpace(addressSpace);
        }

        for (AuthenticationService authenticationService : authenticationServiceRegistry.listAuthenticationServices()) {
            if (authenticationService.getSpec().getType().equals(AuthenticationServiceType.standard) && authenticationService.getSpec().getRealm() == null) {
                Set<String> realmNames = keycloak.getRealmNames(authenticationService);
                Set<String> desiredRealms = authserviceMap.getOrDefault(authenticationService.getMetadata().getName(), new AuthServiceEntry(authenticationService)).getAddressSpaces().stream()
                        .map(a -> a.getAnnotation(AnnotationKeys.REALM_NAME))
                        .collect(Collectors.toSet());

                log.info("Actual: {}, Desired: {}", realmNames, desiredRealms);
                for (String realmName : realmNames) {
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
            KeycloakRealmParams keycloakRealmParams = KeycloakRealmParams.fromAuthenticationService(authenticationService);
            if (!Objects.equals(lastParams, keycloakRealmParams)) {
                log.info("Identity provider params: {}", keycloakRealmParams);
                updateExistingRealms(authenticationService, keycloakRealmParams);
                lastParams = keycloakRealmParams;
            }

            for (AddressSpace addressSpace : addressSpaces) {
                String realmName = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
                log.info("Creating realm {}", realmName);
                String userName = addressSpace.getAnnotation(AnnotationKeys.CREATED_BY);
                String userId = addressSpace.getAnnotation(AnnotationKeys.CREATED_BY_UID);
                if (userId == null || userId.isEmpty()) {
                    userId = userLookupApi.findUserId(userName);
                }

                EndpointStatus endpointStatus = getConsoleEndpointStatus(addressSpace);
                if (endpointStatus == null) {
                    log.info("Address space {} has no endpoints defined", addressSpace.getMetadata().getName());
                } else if (endpointStatus.getExternalHost() == null && endpointStatus.getExternalPorts().isEmpty()) {
                    log.info("Address space {} console endpoint host not known, waiting", addressSpace.getMetadata().getName());
                } else {
                    String consoleUri = getConsoleUri(endpointStatus);
                    keycloak.createRealm(authenticationService, addressSpace.getMetadata().getNamespace(), realmName, consoleUri, keycloakRealmParams);
                    userApi.createUser(authenticationService, realmName, new UserBuilder()
                            .withMetadata(new ObjectMetaBuilder()
                                    .withName(addressSpace.getMetadata().getName() + "." + KubeUtil.sanitizeUserName(userName))
                                    .withNamespace(addressSpace.getMetadata().getNamespace())
                                    .build())
                            .withSpec(new UserSpecBuilder()
                                    .withUsername(KubeUtil.sanitizeUserName(userName))
                                    .withAuthentication(new UserAuthenticationBuilder()
                                            .withType(UserAuthenticationType.federated)
                                            .withProvider("openshift")
                                            .withFederatedUserid(userId)
                                            .withFederatedUsername(userName)
                                            .build())
                                    .withAuthorization(Arrays.asList(new UserAuthorizationBuilder()
                                                    .withAddresses(Collections.singletonList("*"))
                                                    .withOperations(Arrays.asList(send, recv, view))
                                                    .build(),
                                            new UserAuthorizationBuilder()
                                                    .withOperations(Collections.singletonList(Operation.manage))
                                                    .build()))
                                    .build())
                            .build());
                }
            }
        }
    }

    private void updateExistingRealms(AuthenticationService authenticationService, KeycloakRealmParams updatedParams) {
        keycloak.getRealmNames(authenticationService).stream()
                .filter(name -> !name.equals(MASTER_REALM))
                .forEach(name -> {
                    log.info("Updating realm {}", name);
                    keycloak.updateRealm(authenticationService, name, updatedParams);
                });
    }

    @Override
    public String toString() {
        return "RealmController";
    }
}
