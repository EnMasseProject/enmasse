/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Keycloak implements KeycloakApi {

    private final KeycloakParams params;

    public Keycloak(KeycloakParams params) {
        this.params = params;
    }

    @Override
    public Set<String> getRealmNames() {
        try (CloseableKeycloak wrapper = new CloseableKeycloak(params)) {
            return wrapper.get().realms().findAll().stream()
                    .map(RealmRepresentation::getRealm)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public void createRealm(String realmName, String realmAdminUser, String realmAdminUserId, String consoleRedirectURI) {
        final RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(realmName);
        newRealm.setEnabled(true);
        newRealm.setPasswordPolicy("hashAlgorithm(scramsha1)");

        if (realmAdminUser != null) {

            final UserRepresentation newUser = new UserRepresentation();
            newUser.setUsername(realmAdminUser);
            newUser.setEnabled(true);
            newUser.setClientRoles(Collections.singletonMap("realm-management", Collections.singletonList("manage-users")));

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

                FederatedIdentityRepresentation openshiftFederatedIdentity = new FederatedIdentityRepresentation();
                openshiftFederatedIdentity.setUserName(realmAdminUser);
                openshiftFederatedIdentity.setUserId(realmAdminUserId);
                openshiftFederatedIdentity.setIdentityProvider("openshift-v3");
                newUser.setFederatedIdentities(Collections.singletonList(openshiftFederatedIdentity));
            }

            newRealm.setUsers(Collections.singletonList(newUser));
        }

        if (consoleRedirectURI != null) {
            final ClientRepresentation console = new ClientRepresentation();
            console.setClientId("enmasse-console");
            console.setPublicClient(true);
            console.setRedirectUris(Collections.singletonList(consoleRedirectURI));
            newRealm.setClients(Collections.singletonList(console));
        }

        try (CloseableKeycloak wrapper = new CloseableKeycloak(params)) {
            wrapper.get().realms().create(newRealm);
            String realmAdminKcUserId = wrapper.get().realm(realmName).users().search(realmAdminUser).stream()
                    .findFirst()
                    .map(UserRepresentation::getId)
                    .orElse(null);

            String adminGroupId = addGroup(wrapper, realmName, "admin");
            String manageGroupId = addGroup(wrapper, realmName, "manage");
            addGroup(wrapper, realmName, "send_*");
            addGroup(wrapper, realmName, "recv_*");
            addGroup(wrapper, realmName, "view_*");
            addGroup(wrapper, realmName, "browse_*");
            if (realmAdminKcUserId != null) {
                wrapper.get().realm(realmName).users().get(realmAdminKcUserId).joinGroup(adminGroupId);
                wrapper.get().realm(realmName).users().get(realmAdminKcUserId).joinGroup(manageGroupId);
            }
        }
    }

    private String addGroup(CloseableKeycloak keycloak, String realmName, String groupName) {
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName(groupName);
        keycloak.get().realm(realmName).groups().add(groupRep);
        for (GroupRepresentation group : keycloak.get().realm(realmName).groups().groups()) {
            if (group.getName().equals(groupName)) {
                return group.getId();
            }
        }
        return null;
    }

    @Override
    public void deleteRealm(String realmName) {
        try (CloseableKeycloak wrapper = new CloseableKeycloak(params)) {
            wrapper.get().realm(realmName).remove();
        }
    }

    public static class CloseableKeycloak implements AutoCloseable {

        private final org.keycloak.admin.client.Keycloak keycloak;

        CloseableKeycloak(KeycloakParams params) {
            this.keycloak = KeycloakBuilder.builder()
                .serverUrl(params.getKeycloakUri())
                .realm("master")
                .username(params.getAdminUser())
                .password(params.getAdminPassword())
                .clientId("admin-cli")
                .resteasyClient(new ResteasyClientBuilder()
                        .establishConnectionTimeout(30, TimeUnit.SECONDS)
                        .trustStore(params.getKeyStore())
                        .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                        .build())
                .build();
        }

        org.keycloak.admin.client.Keycloak get() {
            return keycloak;
        }

        @Override
        public void close() {
            keycloak.close();
        }
    }
}
