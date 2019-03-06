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
import io.enmasse.k8s.api.Watcher;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.enmasse.user.model.v1.Operation.recv;
import static io.enmasse.user.model.v1.Operation.send;
import static io.enmasse.user.model.v1.Operation.view;

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

    @Override
    public void reconcileAll(List<AddressSpace> addressSpaces) throws Exception {
        AuthenticationService authenticationService = authenticationServiceRegistry.findAuthenticationServiceByName(AuthenticationServiceType.STANDARD.getName()).orElse(null);
        if (authenticationService == null) {
            log.debug("Unable to find standard authentication service, not performing any operations");
            return;
        }

        if (authenticationService.getSpec().getRealm() != null) {
            log.info("Standard authentication service overriding realm, not performing any opereations");
            return;
        }

        KeycloakRealmParams keycloakRealmParams = KeycloakRealmParams.fromAuthenticationService(authenticationService);
        if (!Objects.equals(lastParams, keycloakRealmParams)) {
            log.info("Identity provider params: {}", keycloakRealmParams);
            updateExistingRealms(keycloakRealmParams);
            lastParams = keycloakRealmParams;
        }

        Map<String, AddressSpace> standardAuthSvcSpaces =
                addressSpaces.stream()
                             .filter(x -> {
                                 AuthenticationService authService = authenticationServiceRegistry.findAuthenticationService(x.getSpec().getAuthenticationService()).orElse(null);
                                 return authenticationService.equals(authService) && x.getSpec().getEndpoints() != null;
                             })
                             .collect(Collectors.toMap(
                                     addressSpace -> Optional
                                         .ofNullable(addressSpace.getAnnotation(AnnotationKeys.REALM_NAME))
                                         .orElse(addressSpace.getMetadata().getName()),
                                     Function.identity()));

        Set<String> realmNames = keycloak.getRealmNames();
        log.info("Actual: {}, Desired: {}", realmNames, standardAuthSvcSpaces.keySet());
        for(String realmName : realmNames) {
            if(standardAuthSvcSpaces.remove(realmName) == null && !MASTER_REALM.equals(realmName)) {
                log.info("Deleting realm {}", realmName);
                keycloak.deleteRealm(realmName);
            }
        }
        for(Map.Entry<String, AddressSpace> entry : standardAuthSvcSpaces.entrySet()) {
            AddressSpace addressSpace = entry.getValue();
            String realmName = entry.getKey();
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

    private void updateExistingRealms(KeycloakRealmParams updatedParams) {
        keycloak.getRealmNames().stream()
                .filter(name -> !name.equals(MASTER_REALM))
                .forEach(name -> {
                    log.info("Updating realm {}", name);
                    keycloak.updateRealm(name, updatedParams);
                });
    }

    @Override
    public String toString() {
        return "RealmController";
    }
}
