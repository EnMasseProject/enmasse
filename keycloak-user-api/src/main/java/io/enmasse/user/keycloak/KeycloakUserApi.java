/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;

import io.enmasse.k8s.util.TimeUtil;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KeycloakUserApi implements UserApi  {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserApi.class);

    private final Clock clock;
    private final KeycloakFactory keycloakFactory;
    private final Duration apiTimeout;
    private volatile Keycloak keycloak;

    public KeycloakUserApi(KeycloakFactory keycloakFactory, Clock clock) {
        this(keycloakFactory, clock, Duration.ZERO);
    }

    public KeycloakUserApi(KeycloakFactory keycloakFactory, Clock clock, Duration apiTimeout) {
        this.keycloakFactory = keycloakFactory;
        this.clock = clock;
        this.apiTimeout = apiTimeout;
    }

    interface KeycloakHandler<T> {
        T handle(Keycloak keycloak);
    }

    private synchronized <T> T withKeycloak(KeycloakHandler<T> consumer) {
        if (keycloak == null) {
            keycloak = keycloakFactory.createInstance();
        }
        return consumer.handle(keycloak);
    }

    interface RealmHandler<T> {
        T handle(RealmResource realm);
    }

    private synchronized <T> T withRealm(String realmName, RealmHandler<T> consumer) throws Exception {
        if (keycloak == null) {
            keycloak = keycloakFactory.createInstance();
        }
        RealmResource realmResource = waitForRealm(keycloak, realmName, apiTimeout);
        return consumer.handle(realmResource);
    }

    private RealmResource waitForRealm(Keycloak keycloak, String realmName, Duration timeout) throws Exception {
        Instant now = clock.instant();
        Instant endTime = now.plus(timeout);
        RealmResource realmResource = null;
        while (now.isBefore(endTime)) {
            realmResource = getRealmResource(keycloak, realmName);
            if (realmResource != null) {
                break;
            }
            log.info("Waiting 1 second for realm {} to exist", realmName);
            Thread.sleep(1000);
            now = clock.instant();
        }

        if (realmResource == null) {
            realmResource = getRealmResource(keycloak, realmName);
        }

        if (realmResource != null) {
            return realmResource;
        }

        throw new WebApplicationException("Timed out waiting for realm " + realmName + " to exist", 503);
    }

    private RealmResource getRealmResource(Keycloak keycloak, String realmName) {
        List<RealmRepresentation> realms = keycloak.realms().findAll();
        for (RealmRepresentation realm : realms) {
            if (realm.getRealm().equals(realmName)) {
                return keycloak.realm(realmName);
            }
        }
        return null;
    }



    @Override
    public Optional<User> getUserWithName(String realmName, String name) throws Exception {
        log.info("Retrieving user {} in realm {}", name, realmName);
        return withRealm(realmName, realm -> realm.users().search(name).stream()
                .filter(userRep -> name.equals(userRep.getUsername()))
                .findFirst()
                .map(userRep -> {
                    List<GroupRepresentation> groupReps = realm.users().get(userRep.getId()).groups();
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
        attributes.put("creationTimestamp", Collections.singletonList(TimeUtil.formatRfc3339(now)));

        userRep.setAttributes(attributes);

        return userRep;
    }

    private boolean userExists(String username, List<UserRepresentation> userRepresentations) {
        for (UserRepresentation userRepresentation : userRepresentations) {
            if (userRepresentation.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void createUser(String realmName, User user) throws Exception {
        log.info("Creating user {} in realm {}", user.getSpec().getUsername(), realmName);
        user.validate();

        withRealm(realmName, realm -> {

            List<UserRepresentation> reps = realm.users().search(user.getSpec().getUsername());

            if (userExists(user.getSpec().getUsername(), reps)) {
                List<String> usernames = reps.stream()
                        .map(UserRepresentation::getUsername)
                        .collect(Collectors.toList());
                throw new WebApplicationException("User '" + user.getSpec().getUsername() + "' already exists in [" + usernames + "]", 409);
            }

            UserRepresentation userRep = createUserRepresentation(user);

            Response response = realm.users().create(userRep);
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                log.warn("Error creating user ({}): {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
                throw new WebApplicationException(response);
            }

            String userId = CreatedResponseUtil.getCreatedId(response);

            switch (user.getSpec().getAuthentication().getType()) {
                case password:
                    setUserPassword(realm.users().get(userId), user.getSpec().getAuthentication());
                    break;
                case federated:
                    setFederatedIdentity(realm.users().get(userId), user.getSpec().getAuthentication());
                    break;
            }

            applyAuthorizationRules(realm, user, realm.users().get(userId));

            return user;
        });
    }

    private void applyAuthorizationRules(RealmResource realm, User user, UserResource userResource) {

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
                        try {
                            String groupName = operation.name() + "_" + URLEncoder.encode(address, StandardCharsets.UTF_8.name());
                            desiredGroups.add(groupName);

                            String brokeredGroupName = groupName.replace("*", "#");
                            if (!groupName.equals(brokeredGroupName)) {
                                desiredGroups.add(brokeredGroupName);
                            }
                        } catch (UnsupportedEncodingException e) {
                            // UTF-8 must always be supported
                        }
                    }
                }
            }
        }
        List<GroupRepresentation> groups = realm.groups().groups();

        Set<String> existingGroups = userResource.groups()
                .stream()
                .map(GroupRepresentation::getName)
                .collect(Collectors.toSet());

        log.info("Changing for user {} from {} to {}", user.getMetadata().getName(), existingGroups, desiredGroups);

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
            String groupId = createGroupIfNotExists(realm, group);
            userResource.joinGroup(groupId);
        }
    }

    private Optional<UserRepresentation> getUser(String realmName, String username) throws Exception {
        return withRealm(realmName, realm -> realm.users().search(username).stream()
                .filter(userRep -> username.equals(userRep.getUsername()))
                .findFirst());
    }

    @Override
    public boolean replaceUser(String realmName, User user) throws Exception {
        log.info("Replacing user {} in realm {}", user.getSpec().getUsername(), realmName);
        user.validate();
        UserRepresentation userRep = getUser(realmName, user.getSpec().getUsername()).orElse(null);

        if (userRep == null) {
            return false;
        }

        if (user.getSpec().getAuthentication() != null) {
            String existingAuthType = userRep.getAttributes().get("authenticationType").get(0);
            if (!user.getSpec().getAuthentication().getType().name().equals(existingAuthType)) {
                throw new IllegalArgumentException("Changing authentication type of a user is not allowed (existing is " + existingAuthType + ")");
            }
        }

        return withRealm(realmName, realm -> {
            if (user.getSpec().getAuthentication() != null) {
                switch (user.getSpec().getAuthentication().getType()) {
                    case password:
                        setUserPassword(realm.users().get(userRep.getId()), user.getSpec().getAuthentication());
                        break;
                    case federated:
                        setFederatedIdentity(realm.users().get(userRep.getId()), user.getSpec().getAuthentication());
                        break;
                }
            }
            applyAuthorizationRules(realm, user, realm.users().get(userRep.getId()));
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

    private String createGroupIfNotExists(RealmResource realm, String groupName) {
        for (GroupRepresentation group : realm.groups().groups()) {
            if (group.getName().equals(groupName)) {
                return group.getId();
            }
        }

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName(groupName);
        Response response = realm.groups().add(groupRep);
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
    public void deleteUser(String realmName, User user) throws Exception {
        log.info("Deleting user {} in realm {}", user.getSpec().getUsername(), realmName);
        withRealm(realmName, realm -> {
            List<UserRepresentation> users = realm.users().search(user.getSpec().getUsername());
            for (UserRepresentation userRep : users) {
                log.info("Found user with name {}, want {}", userRep.getUsername(), user.getSpec().getUsername());
                if (user.getSpec().getUsername().equals(userRep.getUsername())) {
                    realm.users().delete(userRep.getId());
                }
            }
            return users;
        });
    }

    @Override
    public boolean realmExists(String realmName) {
        return withKeycloak(kc -> getRealmResource(kc, realmName) != null);
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
        withKeycloak(keycloak -> {
            List<RealmRepresentation> realmReps = keycloak.realms().findAll();
            for (RealmRepresentation realmRep : realmReps) {
                String realmNs = realmRep.getAttributes().get("namespace");
                if (realmNs != null && realmNs.equals(namespace)) {
                    String realm = realmRep.getRealm();
                    List<UserRepresentation> userReps = keycloak.realm(realm).users().list(0, 100);
                    while (!userReps.isEmpty()) {
                        for (UserRepresentation userRep : userReps) {
                            keycloak.realm(realm).users().delete(userRep.getId());
                        }
                        userReps = keycloak.realm(realm).users().list(0, 100);
                    }
                }
            }
            return null;
        });

    }
}
