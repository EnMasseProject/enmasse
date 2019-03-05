/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.keycloak;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.Controller;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.enmasse.user.model.v1.Operation.recv;
import static io.enmasse.user.model.v1.Operation.send;
import static io.enmasse.user.model.v1.Operation.view;

public class RealmController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(RealmController.class);
    private static final String MASTER_REALM = "master";
    private final KeycloakApi keycloak;
    private final Map<String, KeycloakRealmParams> realmState = new ConcurrentHashMap<>();
    private volatile Set<String> currentRealmNames = new HashSet<>();
    private final UserApi userApi;
    private final UserLookupApi userLookupApi;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;

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

    @Override
    public void prepare() {
        if (!keycloak.isAvailable()) {
            return;
        }
        currentRealmNames = keycloak.getRealmNames();
    }

    @Override
    public AddressSpace reconcile(AddressSpace addressSpace) throws Exception {
        if (!keycloak.isAvailable()) {
            return addressSpace;
        }
        AuthenticationService authenticationService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService())
                .orElse(null);
        if (authenticationService == null) {
            log.warn("Error finding authentication service for address space {}, definition: {}.", addressSpace.getMetadata().getName(), addressSpace);
            return addressSpace;
        }

        if (!"standard".equals(authenticationService.getMetadata().getName())) {
            log.debug("Skipping operations on address space {}, not using standard authentication service", addressSpace.getMetadata().getName());
            return addressSpace;
        }

        if (authenticationService.getSpec().getRealm() != null) {
            log.debug("Realm specified in authentication service, not performing any per-address space realm operations");
            return addressSpace;
        }

        KeycloakRealmParams keycloakRealmParams = KeycloakRealmParams.fromAuthenticationService(authenticationService);
        String realmName = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);

        if (!currentRealmNames.contains(realmName)) {
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
                keycloak.createRealm(addressSpace.getMetadata().getNamespace(), realmName, consoleUri, keycloakRealmParams);
                realmState.put(realmName, keycloakRealmParams);
                userApi.createUser(realmName, new UserBuilder()
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

        KeycloakRealmParams lastParams = realmState.getOrDefault(realmName, KeycloakRealmParams.NULL_PARAMS);
        if (!Objects.equals(lastParams, keycloakRealmParams)) {
            log.info("Identity provider params: {}", keycloakRealmParams);
            keycloak.updateRealm(realmName, lastParams, keycloakRealmParams);
            realmState.put(realmName, keycloakRealmParams);
        }
        return addressSpace;
    }

    @Override
    public void retainAll(List<AddressSpace> addressSpaces) {
        if (!keycloak.isAvailable()) {
            return;
        }
        Map<String, AddressSpace> standardAuthSvcSpaces = addressSpaces.stream()
                .filter(x -> {
                    AuthenticationService authenticationService = authenticationServiceRegistry.findAuthenticationService(x.getSpec().getAuthenticationService()).orElse(null);
                    return authenticationService != null && "standard".equals(authenticationService.getMetadata().getName());
                }).collect(Collectors.toMap(
                        addressSpace ->
                                Optional.ofNullable(addressSpace.getAnnotation(AnnotationKeys.REALM_NAME))
                                .orElse(addressSpace.getMetadata().getName()),
                        Function.identity()));

        for(String realmName : currentRealmNames) {
            if(standardAuthSvcSpaces.remove(realmName) == null && !MASTER_REALM.equals(realmName)) {
                log.info("Deleting realm {}", realmName);
                try {
                    keycloak.deleteRealm(realmName);
                } finally {
                    realmState.remove(realmName);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "RealmController";
    }
}
