/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.controller;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
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

public class KeycloakManager implements Watcher<AddressSpace>
{
    private static final Logger log = LoggerFactory.getLogger(KeycloakManager.class);
    private static final String MASTER_REALM = "master";
    private final KeycloakApi keycloak;
    private final KubeApi kube;
    private final UserApi userApi;
    private KeycloakRealmParams lastParams;

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
    public void onUpdate(List<AddressSpace> addressSpaces) throws Exception {
        KeycloakRealmParams keycloakRealmParams = this.kube.getIdentityProviderParams();
        if (!Objects.equals(lastParams, keycloakRealmParams)) {
            log.info("Identity provider params: {}", keycloakRealmParams);
            updateExistingRealms(keycloakRealmParams);
            lastParams = keycloakRealmParams;
        }

        Map<String, AddressSpace> standardAuthSvcSpaces =
                addressSpaces.stream()
                             .filter(x -> x.getSpec().getAuthenticationService().getType() == AuthenticationServiceType.STANDARD && x.getSpec().getEndpoints() != null)
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

    private void updateExistingRealms(KeycloakRealmParams updatedParams) {
        keycloak.getRealmNames().stream()
                .filter(name -> !name.equals(MASTER_REALM))
                .forEach(name -> {
                    log.info("Updating realm {}", name);
                    keycloak.updateRealm(name, updatedParams);
                });
    }
}
