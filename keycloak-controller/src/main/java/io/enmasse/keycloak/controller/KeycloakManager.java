/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.Watcher;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
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
    private IdentityProviderParams lastParams;

    public KeycloakManager(KeycloakApi keycloak, KubeApi kube, UserApi userApi) {
        this.keycloak = keycloak;
        this.kube = kube;
        this.userApi = userApi;
    }

    private EndpointSpec getConsoleEndpoint(AddressSpace addressSpace) {
        for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
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
        IdentityProviderParams identityProviderParams = this.kube.getIdentityProviderParams();
        if (!Objects.equals(lastParams, identityProviderParams)) {
            log.info("Identity provider params: {}", identityProviderParams);
            updateExistingRealms(identityProviderParams);
            lastParams = identityProviderParams;
        }

        Map<String, AddressSpace> standardAuthSvcSpaces =
                addressSpaces.stream()
                             .filter(x -> x.getAuthenticationService().getType() == AuthenticationServiceType.STANDARD && x.getEndpoints() != null)
                             .collect(Collectors.toMap(addressSpace -> Optional.ofNullable(addressSpace.getAnnotation(AnnotationKeys.REALM_NAME)).orElse(addressSpace.getName()), Function.identity()));

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
                log.info("Address space {} has no endpoints defined", addressSpace.getName());
            } else if (endpointStatus.getExternalHost() == null && endpointStatus.getExternalPorts().isEmpty()) {
                log.info("Address space {} console endpoint host not known, waiting", addressSpace.getName());
            } else {
                String consoleUri = getConsoleUri(endpointStatus);
                keycloak.createRealm(addressSpace.getNamespace(), realmName, consoleUri, identityProviderParams);
                userApi.createUser(realmName, new User.Builder()
                        .setMetadata(new UserMetadata.Builder()
                                .setName(addressSpace.getName() + "." + userName)
                                .setNamespace(addressSpace.getNamespace())
                                .build())
                        .setSpec(new UserSpec.Builder()
                                .setUsername(userName)
                                .setAuthentication(new UserAuthentication.Builder()
                                        .setType(UserAuthenticationType.federated)
                                        .setProvider("openshift")
                                        .setFederatedUserid(userId)
                                        .setFederatedUsername(userName)
                                        .build())
                                .setAuthorization(Arrays.asList(new UserAuthorization.Builder()
                                                .setAddresses(Collections.singletonList("*"))
                                                .setOperations(Arrays.asList(send, recv, view))
                                                .build(),
                                        new UserAuthorization.Builder()
                                                .setOperations(Collections.singletonList(Operation.manage))
                                                .build()))
                                .build())
                        .build());
            }
        }
    }

    private void updateExistingRealms(IdentityProviderParams updatedParams) {
        keycloak.getRealmNames().stream()
                .filter(name -> !name.equals(MASTER_REALM))
                .forEach(name -> {
                    log.info("Updating realm {}", name);
                    keycloak.updateRealm(name, updatedParams);
                });
    }
}
