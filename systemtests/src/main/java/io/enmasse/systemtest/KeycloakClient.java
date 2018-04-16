/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class KeycloakClient {

    private static Logger log = CustomLogger.getLogger();
    private final Endpoint endpoint;
    private final KeycloakCredentials credentials;
    private final KeyStore trustStore;

    public KeycloakClient(Endpoint endpoint, KeycloakCredentials credentials, String keycloakCaCert) throws Exception {
        this.endpoint = endpoint;
        this.credentials = credentials;
        this.trustStore = createTrustStore(keycloakCaCert);
    }

    private static KeyStore createTrustStore(String keycloakCaCert) throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keyStore.setCertificateEntry("standard-authservice",
                    cf.generateCertificate(new ByteArrayInputStream(keycloakCaCert.getBytes("UTF-8"))));

            return keyStore;
        } catch (Exception ignored) {
            log.warn("Error creating keystore for authservice CA", ignored);
            throw ignored;
        }
    }

    public void joinGroup(String realm, String groupName, String username) throws Exception {
        groupOperation(realm, groupName, username, 3, TimeUnit.MINUTES, (realmResource, clientId, groupId) -> {
            realmResource.users().get(clientId).joinGroup(groupId);
            log.info("User '{}' successfully joined group '{}'", username, groupName);
        });
    }

    public void leaveGroup(String realm, String groupName, String username) throws Exception {
        groupOperation(realm, groupName, username, 3, TimeUnit.MINUTES, (realmResource, clientId, groupId) -> {
            realmResource.users().get(clientId).leaveGroup(groupId);
            log.info("User '{}' successfully removed from group '{}'", username, groupName);
        });
    }

    public void leaveGroups(String realm, String username, String... groups) throws Exception {
        for (String group : groups) {
            leaveGroup(realm, group, username);
        }
    }

    public void groupOperation(String realm, String groupName, String username, long timeout, TimeUnit timeUnit,
                               GroupMethod<RealmResource, String, String> groupMethod) throws Exception {
        int maxRetries = 10;
        try (CloseableKeycloak keycloak = new CloseableKeycloak(endpoint, credentials, trustStore)) {
            RealmResource realmResource = waitForRealm(keycloak.get(), realm, timeout, timeUnit);
            for (int retries = 0; retries < maxRetries; retries++) {
                try {
                    groupMethod.apply(
                            realmResource,
                            getClientId(keycloak, realm, username),
                            getGroupId(keycloak, realm, groupName));
                    break;
                } catch (Exception e) {
                    log.info("Exception querying keycloak ({}), retrying", e.getMessage());
                    Thread.sleep(2000);
                }
            }
        }
    }

    private String getClientId(CloseableKeycloak keycloak, String realm, String username) {
        List<UserRepresentation> users = keycloak.get().realm(realm).users().search(username);
        if (!users.isEmpty()) {
            return users.get(0).getId();
        }
        throw new RuntimeException("Unable to find user: " + username);
    }

    private String getGroupId(CloseableKeycloak keycloak, String realm, String groupName) {
        List<GroupRepresentation> groups =
                keycloak.get().realm(realm).groups()
                        .groups()
                        .stream()
                        .filter(group -> group.getName().equals(groupName))
                        .collect(Collectors.toList());
        if (!groups.isEmpty()) {
            return groups.get(0).getId();
        }
        throw new RuntimeException("Unable to find group: " + groupName);
    }

    private boolean groupExist(CloseableKeycloak keycloak, String realm, String groupName) {
        List<GroupRepresentation> groups =
                keycloak.get().realm(realm).groups()
                        .groups()
                        .stream()
                        .filter(group -> group.getName().equals(groupName))
                        .collect(Collectors.toList());
        return !groups.isEmpty();
    }

    public void createGroup(String realm, String groupName) throws Exception {
        createGroup(realm, groupName, 3, TimeUnit.MINUTES);
    }

    public void createGroup(String realm, String groupName, long timeout, TimeUnit timeUnit) throws Exception {
        int maxRetries = 10;
        try (CloseableKeycloak keycloak = new CloseableKeycloak(endpoint, credentials, trustStore)) {
            waitForRealm(keycloak.get(), realm, timeout, timeUnit);
            if (!groupExist(keycloak, realm, groupName)) {
                for (int retries = 0; retries < maxRetries; retries++) {
                    try {
                        GroupRepresentation groupRep = new GroupRepresentation();
                        groupRep.setName(groupName);
                        Response response = keycloak.get().realm(realm).groups().add(groupRep);
                        if (response.getStatus() != 201) {
                            throw new RuntimeException("Unable to create group: " + response.getStatus());
                        }
                        break;
                    } catch (Exception e) {
                        log.info("Exception querying keycloak ({}), retrying", e.getMessage());
                        Thread.sleep(2000);
                    }
                }
            }
        }
    }

    public void createUser(String realm, String userName, String password) throws Exception {
        createUser(realm, userName, password, 3, TimeUnit.MINUTES,
                Group.SEND_ALL_BROKERED.toString(),
                Group.RECV_ALL_BROKERED.toString(),
                Group.MANAGE_ALL_BROKERED.toString(),
                Group.SEND_ALL_STANDARD.toString(),
                Group.RECV_ALL_STANDARD.toString(),
                Group.MANAGE.toString());
    }

    public void createUser(String realm, String userName, String password, String... groups) throws Exception {
        createUser(realm, userName, password, 3, TimeUnit.MINUTES, groups);
    }

    public void createUser(String realm, String userName, String password, long timeout, TimeUnit timeUnit)
            throws Exception {
        createUser(realm, userName, password, timeout, timeUnit,
                Group.SEND_ALL_BROKERED.toString(),
                Group.RECV_ALL_BROKERED.toString(),
                Group.MANAGE_ALL_BROKERED.toString(),
                Group.SEND_ALL_STANDARD.toString(),
                Group.RECV_ALL_STANDARD.toString(),
                Group.MANAGE.toString());
    }

    public void createUser(String realm, String userName, String password, long timeout, TimeUnit timeUnit, String... groups)
            throws Exception {

        int maxRetries = 10;
        try (CloseableKeycloak keycloak = new CloseableKeycloak(endpoint, credentials, trustStore)) {
            RealmResource realmResource = waitForRealm(keycloak.get(), realm, timeout, timeUnit);

            for (int retries = 0; retries < maxRetries; retries++) {
                try {
                    if (realmResource.users().search(userName).isEmpty()) {
                        UserRepresentation userRep = new UserRepresentation();
                        userRep.setUsername(userName);
                        CredentialRepresentation cred = new CredentialRepresentation();
                        cred.setType(CredentialRepresentation.PASSWORD);
                        cred.setValue(password);
                        cred.setTemporary(false);
                        userRep.setCredentials(Arrays.asList(cred));
                        userRep.setEnabled(true);
                        Response response = keycloak.get().realm(realm).users().create(userRep);
                        if (response.getStatus() != 201) {
                            throw new RuntimeException("Unable to create user: " + response.getStatus());
                        }
                    } else {
                        log.info("User " + userName + " already created, skipping");
                    }
                    break;
                } catch (Exception e) {
                    log.info("Exception querying keycloak ({}), retrying", e.getMessage());
                    Thread.sleep(2000);
                }
            }
        }

        for (String group : groups) {
            createGroup(realm, group);
            joinGroup(realm, group, userName);
        }
    }

    private RealmResource waitForRealm(Keycloak keycloak, String realmName, long timeout, TimeUnit timeUnit) throws Exception {
        log.info("Waiting for realm {} to exist", realmName);
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        RealmResource realmResource = null;
        while (System.currentTimeMillis() < endTime) {
            realmResource = getRealmResource(keycloak, realmName);
            if (realmResource != null) {
                return realmResource;
            }
            Thread.sleep(10000);
        }

        if (realmResource == null) {
            realmResource = getRealmResource(keycloak, realmName);
        }

        if (realmResource != null) {
            return realmResource;
        }

        throw new TimeoutException("Timed out waiting for realm " + realmName + " to exist");
    }

    private RealmResource getRealmResource(Keycloak keycloak, String realmName) throws Exception {
        return TestUtils.doRequestNTimes(10, () -> {
            List<RealmRepresentation> realms = keycloak.realms().findAll();
            for (RealmRepresentation realm : realms) {
                if (realm.getRealm().equals(realmName)) {
                    return keycloak.realm(realmName);
                }
            }
            return null;
        });
    }

    public void deleteUser(String realm, String userName) throws Exception {
        try (CloseableKeycloak keycloak = new CloseableKeycloak(endpoint, credentials, trustStore)) {
            TestUtils.doRequestNTimes(10, () -> keycloak.get().realm(realm).users().delete(userName));
        }
    }

    @FunctionalInterface
    interface GroupMethod<T, U, V> {
        void apply(T t, U u, V v);
    }

    private static class CloseableKeycloak implements AutoCloseable {

        private final Keycloak keycloak;

        private CloseableKeycloak(Endpoint endpoint, KeycloakCredentials credentials, KeyStore trustStore) {
            this.keycloak = KeycloakBuilder.builder()
                    .serverUrl("https://" + endpoint.getHost() + ":" + endpoint.getPort() + "/auth")
                    .realm("master")
                    .username(credentials.getUsername())
                    .password(credentials.getPassword())
                    .clientId("admin-cli")
                    .resteasyClient(new ResteasyClientBuilder()
                            .disableTrustManager()
                            .trustStore(trustStore)
                            .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                            .build())
                    .build();
        }

        public Keycloak get() {
            return keycloak;
        }

        @Override
        public void close() {
            keycloak.close();
        }
    }
}
