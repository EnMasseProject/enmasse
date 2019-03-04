/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.keycloak;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.Controller;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.Watcher;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
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

public class RealmController implements Controller
{
    private static final Logger log = LoggerFactory.getLogger(RealmController.class);
    private static final String MASTER_REALM = "master";
    private final KeycloakApi keycloak;
    private final Map<String, KeycloakRealmParams> realmState = new ConcurrentHashMap<>();
    private volatile Set<String> currentRealmNames = new HashSet<>();
    private final UserApi userApi;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final KeycloakFactory keycloakFactory;

    public KeycloakManager(KeycloakApi keycloak, KubeApi kube, UserApi userApi) {
        this.keycloak = keycloak;
        this.kube = kube;
        this.userApi = userApi;
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
    public void beforeAll() {
        currentRealmNames = keycloak.getRealmNames();
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) throws Exception {
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

        if (authenticationService.getSpec().getRealm() == null) {
            log.debug("Realm specified in authentication service, not performing any per-address space realm operations");
            return addressSpace;
        }

        KeycloakRealmParams keycloakRealmParams = KeycloakRealmParams.fromAuthenticationService(authenticationService);
        String realmName = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
        KeycloakRealmParams lastParams = realmState.get(realmName);
        if (!Objects.equals(lastParams, keycloakRealmParams)) {
            log.info("Identity provider params: {}", keycloakRealmParams);
            keycloak.updateRealm(realmName, keycloakRealmParams);
            realmState.put(realmName, keycloakRealmParams);
        }

        // TODO: Ideally cache or add a 'prepare' step to all controllers
        if (!currentRealmNames.contains(realmName)) {
            log.info("Creating realm {}", realmName);
            String userName = addressSpace.getAnnotation(AnnotationKeys.CREATED_BY);
            String userId = addressSpace.getAnnotation(AnnotationKeys.CREATED_BY_UID);
            if (userId == null || userId.isEmpty()) {
                userId = kube.findUserId(userName);
            }

            EndpointStatus endpointStatus = getConsoleEndpointStatus(addressSpace);
            if (endpointStatus == null) {
                log.info("Address space {} has no endpoints defined", addressSpace.getMetadata().getName());
            } else if (endpointStatus.getExternalHost() == null && endpointStatus.getExternalPorts().isEmpty()) {
                log.info("Address space {} console endpoint host not known, waiting", addressSpace.getMetadata().getName());
            } else {
                String consoleUri = getConsoleUri(endpointStatus);
                keycloak.createRealm(addressSpace.getMetadata().getNamespace(), realmName, consoleUri, keycloakRealmParams);
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
    }

    @Override
    public void afterAll(Set<AddressSpace> addressSpaces) {

    }
}
