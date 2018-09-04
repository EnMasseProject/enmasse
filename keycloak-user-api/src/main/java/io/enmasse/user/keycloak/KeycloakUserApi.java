/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;

import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KeycloakUserApi implements UserApi  {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserApi.class);

    private final String keycloakUri;
    private final String adminUser;
    private final String adminPassword;
    private final KeyStore keyStore;
    private final Clock clock;
    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));

    public KeycloakUserApi(String keycloakUri, String adminUser, String adminPassword, KeyStore keyStore, Clock clock) {
        this.keycloakUri = keycloakUri;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.keyStore = keyStore;
        this.clock = clock;
    }

    interface Handler<T> {
        T handle(Keycloak keycloak);
    }

    private synchronized <T> T withKeycloak(Handler<T> consumer) {
        Keycloak keycloak = null;
        try {
            keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakUri)
                    .realm("master")
                    .username(adminUser)
                    .password(adminPassword)
                    .clientId("admin-cli")
                    .resteasyClient(new ResteasyClientBuilder()
                            .establishConnectionTimeout(30, TimeUnit.SECONDS)
                            .connectionPoolSize(1)
                            .trustStore(keyStore)
                            .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                            .build())
                    .build();
            return consumer.handle(keycloak);
        } catch (Exception e) {
            log.warn("ExceptioN", e);
            throw new RuntimeException(e);
        } finally {
            keycloak.close();
        }
    }

    @Override
    public Optional<User> getUserWithName(String realm, String name) {
        log.debug("Retrieving user {} in realm {}", name, realm);
        return withKeycloak(keycloak -> keycloak.realm(realm).users().search(name).stream()
                .findFirst()
                .map(userRep -> {
                    List<GroupRepresentation> groupReps = keycloak.realm(realm).users().get(userRep.getId()).groups();
                    return buildUser(userRep, groupReps);
                }));
    }

    private UserRepresentation createUserRepresentation(User user) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(user.getSpec().getUsername());
        userRep.setEnabled(true);
        Map<String, List<String>> attributes = new HashMap<>();

        attributes.put("resourceName", Collections.singletonList(user.getMetadata().getName()));
        attributes.put("resourceNamespace", Collections.singletonList(user.getMetadata().getNamespace()));
        attributes.put("authenticationType", Collections.singletonList(user.getSpec().getAuthentication().getType().name()));

        Instant now = clock.instant();
        attributes.put("creationTimestamp", Collections.singletonList(formatter.format(now)));

        userRep.setAttributes(attributes);

        return userRep;
    }

    @Override
    public void createUser(String realm, User user) {
        log.debug("Creating user {} in realm {}", user.getSpec().getUsername(), realm);
        user.validate();

        UserRepresentation userRep = createUserRepresentation(user);

        withKeycloak(keycloak -> {

            if (keycloak.realm(realm).users().search(user.getSpec().getUsername()).stream().findAny().isPresent()) {
                throw new WebApplicationException("User '" + user.getSpec().getUsername() + "' already exists", 409);
            }

            Response response = keycloak.realm(realm).users().create(userRep);
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                log.warn("Error creating user ({}): {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
                throw new WebApplicationException(response);
            }

            String userId = CreatedResponseUtil.getCreatedId(response);

            switch (user.getSpec().getAuthentication().getType()) {
                case password:
                    setUserPassword(keycloak.realm(realm).users().get(userId), user.getSpec().getAuthentication());
                    break;
                case federated:
                    setFederatedIdentity(keycloak.realm(realm).users().get(userId), user.getSpec().getAuthentication());
                    break;
            }

            applyAuthorizationRules(keycloak, realm, user, keycloak.realm(realm).users().get(userId));

            return user;
        });
    }

    private void applyAuthorizationRules(Keycloak keycloak, String realm, User user, UserResource userResource) {

        Set<String> desiredGroups = new HashSet<>();
        for (UserAuthorization userAuthorization : user.getSpec().getAuthorization()) {
            for (Operation operation : userAuthorization.getOperations()) {
                if (userAuthorization.getAddresses() == null || userAuthorization.getAddresses().isEmpty()) {
                    String groupName = operation.name();
                    desiredGroups.add(groupName);
                    if (groupName.equals("manage")) {
                        desiredGroups.add("manage_#");
                    }
                } else {
                    for (String address : userAuthorization.getAddresses()) {
                        String groupName = operation.name() + "_" + address;
                        desiredGroups.add(groupName);

                        String brokeredGroupName = groupName.replace("*", "#");
                        if (!groupName.equals(brokeredGroupName)) {
                            desiredGroups.add(brokeredGroupName);
                        }
                    }
                }
            }
        }
        List<GroupRepresentation> groups = keycloak.realm(realm).groups().groups();

        Set<String> existingGroups = userResource.groups()
                .stream()
                .map(GroupRepresentation::getName)
                .collect(Collectors.toSet());

        log.debug("Changing for user {} from {} to {}", user.getMetadata().getName(), existingGroups, desiredGroups);

        // Remove membership of groups no longer specified
        Set<String> membershipsToRemove = new HashSet<>(existingGroups);
        membershipsToRemove.removeAll(desiredGroups);
        log.debug("Removing groups {} from user {}", membershipsToRemove, user.getMetadata().getName());
        for (String group : membershipsToRemove) {
            getGroupId(groups, group).ifPresent(userResource::leaveGroup);
        }

        // Add membership of new groups
        Set<String> membershipsToAdd = new HashSet<>(desiredGroups);
        membershipsToAdd.removeAll(existingGroups);
        log.debug("Adding groups {} to user {}", membershipsToRemove, user.getMetadata().getName());
        for (String group : membershipsToAdd) {
            String groupId = createGroupIfNotExists(keycloak, realm, group);
            userResource.joinGroup(groupId);
        }
    }

    private Optional<UserRepresentation> getUser(String realm, String username) {
        return withKeycloak(keycloak -> keycloak.realm(realm).users().search(username).stream().findFirst());
    }

    @Override
    public boolean replaceUser(String realm, User user) {
        log.debug("Replacing user {} in realm {}", user.getSpec().getUsername(), realm);
        user.validate();
        UserRepresentation userRep = getUser(realm, user.getSpec().getUsername()).orElse(null);

        if (userRep == null) {
            return false;
        }

        String existingAuthType = userRep.getAttributes().get("authenticationType").get(0);
        if (!user.getSpec().getAuthentication().getType().name().equals(existingAuthType)) {
            throw new IllegalArgumentException("Changing authentication type of a user is not allowed (existing is " + existingAuthType + ")");
        }

        withKeycloak(keycloak -> {
            switch (user.getSpec().getAuthentication().getType()) {
                case password:
                    setUserPassword(keycloak.realm(realm).users().get(userRep.getId()), user.getSpec().getAuthentication());
                    break;
                case federated:
                    setFederatedIdentity(keycloak.realm(realm).users().get(userRep.getId()), user.getSpec().getAuthentication());
                    break;
            }
            applyAuthorizationRules(keycloak, realm, user, keycloak.realm(realm).users().get(userRep.getId()));
            return true;
        });
    }

    private Optional<String> getGroupId(List<GroupRepresentation> groupRepresentations, String groupName) {
        for (GroupRepresentation groupRepresentation : groupRepresentations) {
            if (groupName.equals(groupRepresentation.getName())) {
                return Optional.of(groupRepresentation.getId());
            }
        }
        return Optional.empty();
    }

    private String createGroupIfNotExists(Keycloak keycloak, String realm, String groupName) {
        for (GroupRepresentation group : keycloak.realm(realm).groups().groups()) {
            if (group.getName().equals(groupName)) {
                return group.getId();
            }
        }

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName(groupName);
        Response response = keycloak.realm(realm).groups().add(groupRep);
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            log.warn("Error creating group ({}): {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
            throw new WebApplicationException(response);
        }
        return CreatedResponseUtil.getCreatedId(response);
    }

    private void setFederatedIdentity(UserResource userResource, UserAuthentication authentication) {
        String provider = authentication.getProvider();
        if ("openshift".equals(provider)) {
            provider = "openshift-v3";
        }

        // Remove existing instance of provider
        for (FederatedIdentityRepresentation existing : userResource.getFederatedIdentity()) {
            if (existing.getIdentityProvider().equals(provider)) {
                userResource.removeFederatedIdentity(provider);
                break;
            }
        }

        FederatedIdentityRepresentation federatedIdentity = new FederatedIdentityRepresentation();

        federatedIdentity.setUserName(authentication.getFederatedUsername());
        federatedIdentity.setUserId(authentication.getFederatedUserid());
        federatedIdentity.setIdentityProvider(provider);
        userResource.addFederatedIdentity(provider, federatedIdentity);
    }

    private void setUserPassword(UserResource userResource, UserAuthentication authentication) {
        // Only set password if specified
        if (authentication.getPassword() != null) {
            byte[] decoded = java.util.Base64.getDecoder().decode(authentication.getPassword());
            CredentialRepresentation creds = new CredentialRepresentation();
            creds.setType("password");
            creds.setValue(new String(decoded, Charset.forName("UTF-8")));
            creds.setTemporary(false);
            userResource.resetPassword(creds);
        }
    }

    @Override
    public void deleteUser(String realm, User user) {
        withKeycloak(keycloak -> {
            List<UserRepresentation> users = keycloak.realm(realm).users().search(user.getSpec().getUsername());
            for (UserRepresentation userRep : users) {
                keycloak.realm(realm).users().delete(userRep.getId());
            }
            return users;
        });
    }

    @Override
    public UserList listUsers(String namespace) {
        return withKeycloak(keycloak -> {
            List<RealmRepresentation> realmReps = keycloak.realms().findAll();
            UserList userList = new UserList();
            for (RealmRepresentation realmRep : realmReps) {
                String realmNs = realmRep.getAttributes().get("namespace");
                if (realmNs != null && realmNs.equals(namespace)) {
                    String realm = realmRep.getRealm();
                    List<UserRepresentation> userReps = keycloak.realm(realm).users().list();
                    for (UserRepresentation userRep : userReps) {
                        List<GroupRepresentation> groupReps = keycloak.realm(realm).users().get(userRep.getId()).groups();
                        userList.add(buildUser(userRep, groupReps));
                    }
                }
            }
            return userList;
        });
    }

    static User buildUser(UserRepresentation userRep, List<GroupRepresentation> groupReps) {
        log.debug("Creating user from user representation id {}, name {} part of groups {}", userRep.getId(), userRep.getUsername(), userRep.getGroups());
        Map<String, Set<Operation>> operationsByAddress = new HashMap<>();
        Set<Operation> globalOperations = new HashSet<>();
        for (GroupRepresentation groupRep : groupReps) {
            log.debug("Checking group id {} name {}", groupRep.getId(), groupRep.getName());
            if (groupRep.getName().contains("_")) {
                String[] parts = groupRep.getName().split("_");
                Operation operation = Operation.valueOf(parts[0]);
                String address = parts[1];
                operationsByAddress.computeIfAbsent(address, k -> new HashSet<>())
                        .add(operation);
            } else {
                Operation operation = Operation.valueOf(groupRep.getName());
                globalOperations.add(operation);
            }
        }

        Map<Set<Operation>, Set<String>> operations = new HashMap<>();
        for (Map.Entry<String, Set<Operation>> byAddressEntry : operationsByAddress.entrySet()) {
            if (!operations.containsKey(byAddressEntry.getValue())) {
                operations.put(byAddressEntry.getValue(), new HashSet<>());
            }
            operations.get(byAddressEntry.getValue()).add(byAddressEntry.getKey());
        }

        for (Operation operation : globalOperations) {
            if (operation == Operation.manage) {
                operations.put(Collections.singleton(operation), Collections.emptySet());
            }
        }

        List<UserAuthorization> authorizations = new ArrayList<>();
        for (Map.Entry<Set<Operation>, Set<String>> operationsEntry : operations.entrySet()) {
            authorizations.add(new UserAuthorization.Builder()
                    .setAddresses(new ArrayList<>(operationsEntry.getValue()))
                    .setOperations(new ArrayList<>(operationsEntry.getKey()))
                    .build());
        }

        String name = userRep.getAttributes().get("resourceName").get(0);
        String namespace = userRep.getAttributes().get("resourceNamespace").get(0);

        return new User.Builder()
                .setMetadata(new UserMetadata.Builder()
                        .setName(name)
                        .setNamespace(namespace)
                        .setSelfLink("/apis/user.enmasse.io/v1alpha1/namespaces/" + namespace + "/messagingusers/" + name)
                        .setCreationTimestamp(userRep.getAttributes().get("creationTimestamp").get(0))
                        .build())
                .setSpec(new UserSpec.Builder()
                        .setUsername(userRep.getUsername())
                        .setAuthentication(new UserAuthentication.Builder()
                                .setType(UserAuthenticationType.valueOf(userRep.getAttributes().get("authenticationType").get(0)))
                                .build())
                        .setAuthorization(authorizations)
                        .build())
                .build();
    }

    @Override
    public UserList listUsersWithLabels(String namespace, Map<String, String> labels) {
        return withKeycloak(keycloak -> {

            List<RealmRepresentation> realmReps = keycloak.realms().findAll();
            UserList userList = new UserList();
            for (RealmRepresentation realmRep : realmReps) {
                String realmNs = realmRep.getAttributes().get("namespace");
                if (realmNs != null && realmNs.equals(namespace)) {
                    String realm = realmRep.getRealm();
                    List<UserRepresentation> userReps = keycloak.realm(realm).users().list().stream()
                            .filter(userRep -> {
                                for (Map.Entry<String, String> label : labels.entrySet()) {
                                    if (userRep.getAttributes().get(label.getKey()) == null || !label.getValue().equals(userRep.getAttributes().get(label.getKey()).get(0))) {
                                        return false;
                                    }
                                }
                                return true;
                            }).collect(Collectors.toList());

                    for (UserRepresentation userRep : userReps) {
                        List<GroupRepresentation> groupReps = keycloak.realm(realm).users().get(userRep.getId()).groups();
                        userList.add(buildUser(userRep, groupReps));
                    }
                }
            }
            return userList;
        });
    }

    @Override
    public void deleteUsers(String namespace) {

    }
}
