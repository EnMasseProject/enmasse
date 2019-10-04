/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.k8s.util.TimeUtil;
import io.enmasse.user.api.RealmApi;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.empty;

public class KeycloakUserApi implements UserApi, RealmApi {

    private static final String ATTR_ANNOTATIONS = "annotations";
    private static final String ATTR_OWNER_REFERENCES = "ownerReferences";

    private static final String BASE = "io.enmasse.user.keycloak";
    private static final int MAX_ANNOTATION_VALUE_LENGTH = Integer.getInteger( BASE + ".maxUserAnnotationValueLength", 255);

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserApi.class);

    private static final String DEFAULT_ANNOTATION_BLACKLIST = "";
    private static final String DEFAULT_ANNOTATION_WHITELIST = "(.*\\.|)enmasse\\.io/.*";

    private static final List<Pattern> ANNOTATION_BLACKLIST;
    private static final List<Pattern> ANNOTATION_WHITELIST;

    static {
        ANNOTATION_BLACKLIST = Arrays.stream(System.getProperty(BASE + ".annotationBlacklist", DEFAULT_ANNOTATION_BLACKLIST)
                .split(","))
                .filter(Predicate.not(String::isBlank))
                .map(Pattern::compile)
                .collect(Collectors.toUnmodifiableList());
        ANNOTATION_WHITELIST = Arrays.stream(System.getProperty(BASE + ".annotationWhitelist", DEFAULT_ANNOTATION_WHITELIST)
                .split(","))
                .filter(Predicate.not(String::isBlank))
                .map(Pattern::compile)
                .collect(Collectors.toUnmodifiableList());
        log.info("Annotation blacklist: {}", ANNOTATION_BLACKLIST);
        log.info("Annotation whitelist: {}", ANNOTATION_WHITELIST);
    }

    private final Clock clock;
    private final KeycloakFactory keycloakFactory;
    private final Duration apiTimeout;
    private final Map<String, Keycloak> keycloakMap = new HashMap<>();

    public KeycloakUserApi(KeycloakFactory keycloakFactory, Clock clock, Duration apiTimeout) {
        this.keycloakFactory = keycloakFactory;
        this.clock = clock;
        this.apiTimeout = apiTimeout;
    }

    public synchronized void retainAuthenticationServices(List<AuthenticationService> items) {
        Set<String> desired = items.stream().map(a -> a.getMetadata().getName()).collect(Collectors.toSet());
        Set<String> toRemove = new HashSet<>(keycloakMap.keySet());
        toRemove.removeAll(desired);
        for (String authService : toRemove) {
            Keycloak keycloak = keycloakMap.remove(authService);
            if (keycloak != null && !keycloak.isClosed()) {
                keycloak.close();
            }
        }
    }

    interface KeycloakHandler<T> {
        T handle(Keycloak keycloak) throws Exception;
    }

    private synchronized <T> T withKeycloak(AuthenticationService authenticationService, KeycloakHandler<T> consumer) throws Exception {
        if (keycloakMap.get(authenticationService.getMetadata().getName()) == null) {
            keycloakMap.put(authenticationService.getMetadata().getName(), keycloakFactory.createInstance(authenticationService));
        }
        Keycloak keycloak = keycloakMap.get(authenticationService.getMetadata().getName());
        try {
            return consumer.handle(keycloak);
        } catch (Exception e) {
            keycloakMap.remove(authenticationService.getMetadata().getName());
            keycloak.close();
            throw e;
        }
    }

    interface RealmHandler<T> {
        T handle(RealmResource realm);
    }

    private synchronized <T> T withRealm(AuthenticationService authenticationService, String realmName, RealmHandler<T> consumer) throws Exception {
        return withKeycloak(authenticationService, keycloak -> {
            RealmResource realmResource = waitForRealm(keycloak, realmName, apiTimeout);
            return consumer.handle(realmResource);
        });
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
    public synchronized boolean isAvailable(AuthenticationService authenticationService) throws Exception {
        return withKeycloak(authenticationService, Objects::nonNull);
    }

    @Override
    public Optional<User> getUserWithName(AuthenticationService authenticationService, String realmName, String resourceName) throws Exception {
        log.info("Retrieving user {} in realm {}", resourceName, realmName);
        return withRealm(authenticationService, realmName, realm -> realm.users().list().stream()
                .filter(userRep -> {
                    Map<String, List<String>> attributes = userRep.getAttributes();
                    return attributes != null && attributes.get("resourceName") != null && resourceName.equals(attributes.get("resourceName").get(0));
                })
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

        final List<String> ownerReferences = ownerReferencesToString(user.getMetadata().getOwnerReferences());
        attributes.put(ATTR_OWNER_REFERENCES, ownerReferences);

        Instant now = clock.instant();
        attributes.put("creationTimestamp", Collections.singletonList(TimeUtil.formatRfc3339(now)));
        attributes.put(ATTR_ANNOTATIONS, annotationsToString(user.getMetadata().getAnnotations()));

        userRep.setAttributes(attributes);

        return userRep;
    }

    static List<String> annotationsToString(final Map<String, String> annotations) {

        if (annotations == null) {
            return null;
        }

        final ObjectMapper mapper = new ObjectMapper();

        return annotations.entrySet().stream()
                .filter(KeycloakUserApi::filterUserAnnotation)
                .map(entry -> {
                    try {
                        return mapper.writeValueAsString(entry);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

    }

    private static List<String> ownerReferencesToString(final List<OwnerReference> references) {

        if (references == null) {
            return null;
        }

        final ObjectMapper mapper = new ObjectMapper();

        return references
                .stream()
                .map(ownerReference -> {
                    try {
                        return mapper.writeValueAsString(ownerReference);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

    }

    static Map<String, String> annotationsFromString(final List<String> annotationValues) {

        if (annotationValues == null || annotationValues.isEmpty()) {
            return null;
        }

        final ObjectMapper mapper = new ObjectMapper();

        return annotationValues
                .stream().<Map.Entry<String, String>>map(ref -> {
                    try {
                        return mapper.readValue(ref, new TypeReference<Map.Entry<String, String>>() {});
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.<Map.Entry<String, String>, String, String>toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    private static List<OwnerReference> ownerReferencesFromString(final List<String> attributeValues) {

        if (attributeValues == null || attributeValues.isEmpty() ) {
            return null;
        }

        final ObjectMapper mapper = new ObjectMapper();

        return attributeValues.stream()
                .map(ref -> {
                    try {
                        return mapper.readValue(ref, OwnerReference.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
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
    public void createUser(AuthenticationService authenticationService, String realmName, User user) throws Exception {
        log.info("Creating user {} in realm {}", user.getSpec().getUsername(), realmName);
        user.validate();
        validateForCreation(user);

        withRealm(authenticationService, realmName, realm -> {

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
                case serviceaccount:
                    // nothing to do
                    break;
                default:
                    log.error("Authentication type {} requested, but not properly implemented", user.getSpec().getAuthentication().getType());
                    throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

            }

            applyAuthorizationRules(realm, user, realm.users().get(userId));

            return user;
        });
    }

    /**
     * Check if the user is valid for creating a new instance.
     * @param user The user to check.
     */
    private void validateForCreation(final User user) {
        final UserAuthentication auth = user.getSpec().getAuthentication();
        switch (auth.getType()) {
            case password:
                Objects.requireNonNull(auth.getPassword(), "'password' must be set for 'password' type");
                break;
            case federated:
                Objects.requireNonNull(auth.getProvider(), "'provider' must be set for 'federated' type");
                Objects.requireNonNull(auth.getFederatedUserid(), "'federatedUserid' must be set for 'federated' type");
                Objects.requireNonNull(auth.getFederatedUsername(), "'federatedUsername' must be set for 'federated' type");
                break;
        }
    }

    private void applyAuthorizationRules(RealmResource realm, User user, UserResource userResource) {

        Set<String> desiredGroups = createDesiredGroupsSet(user.getSpec().getAuthorization());
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

    /**
     * Create the set of desired groups.
     *
     * @param authorization The authorization rule list to create the groups for.
     * @return A set of groups.
     */
    static Set<String> createDesiredGroupsSet(final List<UserAuthorization> authorization) {

        final Set<String> desiredGroups = new HashSet<>();

        for (UserAuthorization userAuthorization : authorization) {

            for (Operation operation : userAuthorization.getOperations()) {

                switch (operation) {
                    case manage:
                        desiredGroups.add("manage");
                        break;
                    case view:
                        desiredGroups.add("monitor");
                        break;
                    default:
                        if (userAuthorization.getAddresses() != null && !userAuthorization.getAddresses().isEmpty()) {
                            for (String address : userAuthorization.getAddresses()) {
                                String groupName = operation.name() + "_" + encodePart(address);
                                // normal name
                                desiredGroups.add(groupName);
                            }
                        }
                        break;
                }
            }
        }

        return desiredGroups;
    }

    private Optional<UserRepresentation> getUser(AuthenticationService authenticationService, String realmName, String username) throws Exception {
        return withRealm(authenticationService, realmName, realm -> realm.users().search(username).stream()
                .filter(userRep -> username.equals(userRep.getUsername()))
                .findFirst());
    }

    @Override
    public boolean replaceUser(AuthenticationService authenticationService, String realmName, User user) throws Exception {
        log.info("Replacing user {} in realm {}", user.getSpec().getUsername(), realmName);
        user.validate();
        UserRepresentation userRep = getUser(authenticationService, realmName, user.getSpec().getUsername()).orElse(null);

        if (userRep == null) {
            return false;
        }

        if (user.getSpec().getAuthentication() != null) {
            String existingAuthType = userRep.getAttributes().get("authenticationType").get(0);
            if (!user.getSpec().getAuthentication().getType().name().equals(existingAuthType)) {
                throw new IllegalArgumentException("Changing authentication type of a user is not allowed (existing is " + existingAuthType + ")");
            }
        }

        return withRealm(authenticationService, realmName, realm -> {
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
            applyAttributes(realm, user, userRep);
            return true;
        });
    }

    private void applyAttributes(RealmResource realm, User user, UserRepresentation userRep) {
        boolean changed = false;
        if (updateAttribute(userRep, ATTR_ANNOTATIONS, annotationsToString(user.getMetadata().getAnnotations()))) {
            changed = true;
        }
        if (updateAttribute(userRep, ATTR_OWNER_REFERENCES, ownerReferencesToString(user.getMetadata().getOwnerReferences()))) {
            changed = true;
        }
        if (changed) {
            realm.users().get(userRep.getId()).update(userRep);
        }
    }

    private static boolean updateAttribute(final UserRepresentation userRep, final String key, List<String> value) {

        final String[] currentValue = Optional.ofNullable(userRep.getAttributes().get(key))
                .map(v -> v.toArray(String[]::new))
                .orElseGet(()-> new String[] {});

        final String[] newValue = Optional.ofNullable(value)
                .map(v-> v.toArray(String[]::new))
                .orElseGet(()-> new String[] {});

        if (Arrays.equals(currentValue, newValue)) {
            return false;
        }

        if (value.isEmpty()) {
            userRep.getAttributes().remove(key);
        } else {
            userRep.getAttributes().put(key, value);
        }

        return true;
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
    public void deleteUser(AuthenticationService authenticationService, String realmName, User user) throws Exception {
        log.info("Deleting user {} in realm {}", user.getSpec().getUsername(), realmName);
        withRealm(authenticationService, realmName, realm -> {
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
    public boolean realmExists(AuthenticationService authenticationService, String realmName) throws Exception {
        return withKeycloak(authenticationService, kc -> getRealmResource(kc, realmName) != null);
    }

    private UserList queryUsers(AuthenticationService authenticationService, final Predicate<RealmRepresentation> realmPredicate, final Predicate<UserRepresentation> userPredicate) throws Exception {
        return withKeycloak(authenticationService, keycloak -> {

            List<RealmRepresentation> realmReps = keycloak.realms().findAll();
            UserList userList = new UserList();

            for (RealmRepresentation realmRep : realmReps) {
                if (realmPredicate.test(realmRep)) {
                    String realm = realmRep.getRealm();

                    keycloak.realm(realm).users().list()
                                    .stream()
                                    .filter(userPredicate)
                                    .forEachOrdered(userRep -> {
                                        List<GroupRepresentation> groupReps = keycloak.realm(realm).users().get(userRep.getId()).groups();
                                        userList.getItems().add(buildUser(userRep, groupReps));
                                    });
                }
            }

            return userList;
        });
    }

    /**
     * List all users, from all namespaces.
     */
    @Override
    public UserList listAllUsers(AuthenticationService authenticationService) throws Exception {
        return queryUsers(authenticationService,
                        realm -> getAttribute(realm, "namespace").isPresent(),
                        user -> true);
    }

    /**
     * List users from a single namespace.
     */
    @Override
    public UserList listUsers(AuthenticationService authenticationService, final String namespace) throws Exception {
        return queryUsers(authenticationService,
                        realm -> hasAttribute(realm, "namespace", namespace),
                        user -> true);
    }

    static Optional<String> getAttribute(final RealmRepresentation realm, final String attributeName) {

        if (realm == null || attributeName == null) {
            // if either the realm is null or the attribute name, then we simply return nothing
            return empty();
        }

        final Map<String, String> attributes = realm.getAttributes();
        if (attributes == null) {
            // there are no attributes, so no chance for "true"
            return empty();
        }

        return Optional.ofNullable(attributes.get(attributeName));
    }

    /**
     * Test if a realm as has specific attribute.
     *
     * @param realm The realm to test.
     * @param attributeName The attribute to test.
     * @param attributeValue The expected value. May be {@code null}, in which case the attribute value
     *        is to be expected {@code null} or not set.
     * @return The methods return {@code true} either if the value attributeName parameter is
     *         {@code null} and the attribute is either missing or {@code null}, or if the attribute
     *         value is set and equal to the parameter "attributeValue".
     */
    static boolean hasAttribute(final RealmRepresentation realm, final String attributeName, final String attributeValue) {

        if (realm == null || attributeName == null) {
            return false;
        }

        final Optional<String> value = getAttribute(realm, attributeName);

        final String v = value.orElse(null);

        if (v == attributeValue) {
            // do a check on the object reference, catches "null == null"
            return true;
        }

        if (v == null) {
            // attribute value is null, but due to the previous check we know
            // that the required attribute value is non-null, so it doesn't match
            return false;
        }

        // do a proper check

        return v.equals(attributeValue);

    }

    static User buildUser(UserRepresentation userRep, List<GroupRepresentation> groupReps) {
        log.debug("Creating user from user representation id {}, name {} part of groups {}", userRep.getId(), userRep.getUsername(), userRep.getGroups());
        Map<String, Set<Operation>> operationsByAddress = new LinkedHashMap<>();
        Set<Operation> globalOperations = new LinkedHashSet<>();
        for (GroupRepresentation groupRep : groupReps) {
            log.debug("Checking group id {} name {}", groupRep.getId(), groupRep.getName());
            if (groupRep.getName().startsWith("manage")) {
                globalOperations.add(Operation.manage);
            } else if (groupRep.getName().startsWith("monitor")) {
                globalOperations.add(Operation.view);
            } else if (groupRep.getName().contains("_")) {
                String[] parts = groupRep.getName().split("_", 2);
                Operation operation = Operation.valueOf(parts[0]);
                String address = decodePart(parts[1]);
                operationsByAddress.computeIfAbsent(address, k -> new LinkedHashSet<>())
                        .add(operation);
            }
        }

        Map<Set<Operation>, Set<String>> operations = new LinkedHashMap<>();
        for (Map.Entry<String, Set<Operation>> byAddressEntry : operationsByAddress.entrySet()) {
            if (!operations.containsKey(byAddressEntry.getValue())) {
                operations.put(byAddressEntry.getValue(), new LinkedHashSet<>());
            }
            operations.get(byAddressEntry.getValue()).add(byAddressEntry.getKey());
        }

        for (Operation operation : globalOperations) {
            if (operation == Operation.manage || operation == Operation.view) {
                operations.put(Collections.singleton(operation), Collections.emptySet());
            }
        }

        List<UserAuthorization> authorizations = new ArrayList<>();
        for (Map.Entry<Set<Operation>, Set<String>> operationsEntry : operations.entrySet()) {
            authorizations.add(new UserAuthorizationBuilder()
                    .withAddresses(new ArrayList<>(operationsEntry.getValue()))
                    .withOperations(new ArrayList<>(operationsEntry.getKey()))
                    .build());
        }

        String name = userRep.getAttributes().get("resourceName").get(0);
        String namespace = userRep.getAttributes().get("resourceNamespace").get(0);

        return new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .withNamespace(namespace)
                        .withSelfLink("/apis/user.enmasse.io/v1beta1/namespaces/" + namespace + "/messagingusers/" + name)
                        .withCreationTimestamp(userRep.getAttributes().get("creationTimestamp").get(0))
                        .withOwnerReferences(ownerReferencesFromString(userRep.getAttributes().get(ATTR_OWNER_REFERENCES)))
                        .withAnnotations(annotationsFromString(userRep.getAttributes().get(ATTR_ANNOTATIONS)))
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername(userRep.getUsername())
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.valueOf(userRep.getAttributes().get("authenticationType").get(0)))
                                .build())
                        .withAuthorization(authorizations)
                        .build())
                .build();
    }

    private static boolean matchesLabels (final UserRepresentation userRep, final Map<String,String> labels) {
        for (final Map.Entry<String, String> label : labels.entrySet()) {
            if (userRep.getAttributes().get(label.getKey()) == null || !label.getValue().equals(userRep.getAttributes().get(label.getKey()).get(0))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public UserList listUsersWithLabels(AuthenticationService authenticationService, String namespace, Map<String, String> labels) throws Exception {
        return queryUsers(authenticationService,
                        realm -> hasAttribute(realm, "namespace", namespace),
                        user -> matchesLabels(user, labels));
    }

    @Override
    public UserList listAllUsersWithLabels(AuthenticationService authenticationService, final Map<String, String> labels) throws Exception {
        return queryUsers(authenticationService,
                        realm -> getAttribute(realm, "namespace").isPresent(),
                        user -> matchesLabels(user, labels));
    }

    @Override
    public void deleteUsers(AuthenticationService authenticationService, String namespace) throws Exception {
        withKeycloak(authenticationService, keycloak -> {
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

    @Override
    public Set<String> getRealmNames(AuthenticationService authenticationService) throws Exception {
        return withKeycloak(authenticationService, kc -> kc.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet()));
    }

    @Override
    public void createRealm(AuthenticationService authenticationService, String namespace, String realmName) throws Exception {
        final RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(realmName);
        newRealm.setEnabled(true);
        newRealm.setPasswordPolicy("hashAlgorithm(scramsha1)");
        newRealm.setAttributes(new HashMap<>());
        newRealm.getAttributes().put("namespace", namespace);
        newRealm.getAttributes().put("enmasse-realm", "true");

        withKeycloak(authenticationService, kc -> {
            kc.realms().create(newRealm);
            return true;
        });
    }

    @Override
    public void deleteRealm(AuthenticationService authenticationService, String realmName) throws Exception {
        withKeycloak(authenticationService, kc -> {
            kc.realm(realmName).remove();
            return true;
        });
    }

    public static String decodePart(final String part) {
        return URLDecoder.decode(part, StandardCharsets.UTF_8);
    }

    public static String encodePart(final String part) {
        return URLEncoder.encode(part, StandardCharsets.UTF_8);
    }

    private static boolean filterUserAnnotation(final Map.Entry<String, String> entry) {

        // first check for the value length ...

        if (entry.getValue() != null && entry.getValue().length() > MAX_ANNOTATION_VALUE_LENGTH) {
            // ... value is too long, and will cause a problem
            log.error("Found annotation value that exceeds limit - limit: {}, actualLengt: {}, key: {}, value: {}", MAX_ANNOTATION_VALUE_LENGTH, entry.getValue().length(), entry.getKey(), entry.getValue());
            return false;
        }

        // next check the two lists ...

        final String key = entry.getKey();

        for (final Pattern pattern : ANNOTATION_BLACKLIST) {
            if (pattern.matcher(key).matches()) {
                return false;
            }
        }

        for (final Pattern pattern : ANNOTATION_WHITELIST) {
            if (pattern.matcher(key).matches()) {
                return true;
            }
        }

        // ... nothing left, reject!

        return false;
    }
}
