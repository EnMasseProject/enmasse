/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.keycloak;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RealmControllerTest {

    private RealmController manager;
    private Set<String> realms;
    private List<String> updatedRealms;
    private Map<String, String> realmAdminUsers;
    private UserLookupApi mockUserLookupApi;
    private AuthenticationServiceRegistry mockAuthenticationServiceRegistry;

    @BeforeEach
    public void setup() {
        realms = new HashSet<>();
        updatedRealms = new LinkedList<>();
        realmAdminUsers = new HashMap<>();
        mockUserLookupApi = mock(UserLookupApi.class);
        when(mockUserLookupApi.findUserId(any())).thenReturn("");

        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_URL, "http://bar.com")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_ID, "i")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_SECRET, "secret1")
                .addToAnnotations(AnnotationKeys.BROWSER_SECURITY_HEADERS, "{\"hdr2\":\"value2\"}")
                .endMetadata()
                .withNewSpec()
                .withType(io.enmasse.admin.model.v1.AuthenticationServiceType.standard)
                .endSpec()
                .withNewStatus()
                .withHost("example.com")
                .withPort(5671)
                .endStatus()
                .build();
        mockAuthenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        io.enmasse.address.model.AuthenticationService standardSvc = new io.enmasse.address.model.AuthenticationServiceBuilder()
                .withName("standard")
                .build();
        when(mockAuthenticationServiceRegistry.findAuthenticationService(standardSvc)).thenReturn(Optional.of(authenticationService));
        when(mockAuthenticationServiceRegistry.findAuthenticationServiceByType(eq(io.enmasse.admin.model.v1.AuthenticationServiceType.standard))).thenReturn(Arrays.asList(authenticationService));
        when(mockAuthenticationServiceRegistry.listAuthenticationServices()).thenReturn(Collections.singletonList(authenticationService));

        manager = new RealmController(new KeycloakApi() {
            @Override
            public Set<String> getRealmNames(AuthenticationService auth) {
                return new HashSet<>(realms);
            }

            @Override
            public void createRealm(AuthenticationService auth, String namespace, String realmName, String consoleRedirectURI, KeycloakRealmParams params) {
                realms.add(realmName);
            }

            @Override
            public void updateRealm(AuthenticationService auth, String realmName, KeycloakRealmParams updated) {
                updatedRealms.add(realmName);
            }

            @Override
            public void deleteRealm(AuthenticationService auth, String realmName) {
                realms.remove(realmName);
            }
        }, mockUserLookupApi, new UserApi() {
            @Override
            public boolean isAvailable(AuthenticationService auth) {
                return true;
            }

            @Override
            public Optional<User> getUserWithName(AuthenticationService auth, String realm, String name) {
                return Optional.empty();
            }

            @Override
            public void createUser(AuthenticationService auth, String realm, User user) {
                realmAdminUsers.put(realm, user.getSpec().getUsername());
            }

            @Override
            public boolean replaceUser(AuthenticationService auth, String realm, User user) {
                return false;
            }

            @Override
            public void deleteUser(AuthenticationService auth, String realm, User user) {

            }

            @Override
            public boolean realmExists(AuthenticationService auth, String realm) {
                return true;
            }

            @Override
            public UserList listUsers(AuthenticationService auth, String realm) {
                return null;
            }

            @Override
            public UserList listUsersWithLabels(AuthenticationService auth, String realm, Map<String, String> labels) {
                return null;
            }

            @Override
            public UserList listAllUsers(AuthenticationService auth) {
                return null;
            }

            @Override
            public UserList listAllUsersWithLabels(AuthenticationService auth, Map<String, String> labels) {
                return null;
            }

            @Override
            public void deleteUsers(AuthenticationService auth, String namespace) {

            }
        }, mockAuthenticationServiceRegistry);
    }

    @Test
    public void testAddAddressSpace() throws Exception {
        manager.reconcileAll(Collections.singletonList(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertTrue(realms.isEmpty());

        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));

        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());

        assertTrue(realmAdminUsers.get("a2").length() > 0);
        assertTrue(realmAdminUsers.get("a3").length() > 0);
    }

    @Test
    public void testRemoveAddressSpace() throws Exception {
        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));

        assertTrue(realms.contains("a1"));
        assertFalse(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());
    }

    @Test
    public void testAuthTypeChanged() throws Exception {
        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a1"));
        assertEquals(1, realms.size());

        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertFalse(realms.contains("a1"));
        assertEquals(0, realms.size());
    }

    @Test
    public void testUpdateRealm() throws Exception {
        List<AddressSpace> spaces = Collections.singletonList(createAddressSpace("a1", AuthenticationServiceType.STANDARD));
        manager.reconcileAll(spaces);
        assertTrue(realms.contains("a1"));
        assertTrue(updatedRealms.isEmpty());

        manager.reconcileAll(spaces);
        assertTrue(updatedRealms.isEmpty());

        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_URL, "http://example.com")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_ID, "id")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_SECRET, "secret2")
                .addToAnnotations(AnnotationKeys.BROWSER_SECURITY_HEADERS, "{\"hdr1\":\"value1\"}")
                .endMetadata()
                .withNewSpec()
                .withType(io.enmasse.admin.model.v1.AuthenticationServiceType.standard)
                .endSpec()
                .withNewStatus()
                .withHost("example.com")
                .withPort(5671)
                .endStatus()
                .build();
        when(mockAuthenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));
        when(mockAuthenticationServiceRegistry.findAuthenticationServiceByType(eq(io.enmasse.admin.model.v1.AuthenticationServiceType.standard))).thenReturn(Arrays.asList(authenticationService));
        when(mockAuthenticationServiceRegistry.listAuthenticationServices()).thenReturn(Collections.singletonList(authenticationService));

        manager.reconcileAll(spaces);
        assertEquals(1, updatedRealms.size());
    }

    private AddressSpace createAddressSpace(String name, AuthenticationServiceType authType) {
        return new AddressSpaceBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .withNamespace("myns")
                        .addToAnnotations(AnnotationKeys.CREATED_BY, "developer")
                        .addToAnnotations(AnnotationKeys.REALM_NAME, name)
                        .build())

                .withNewSpec()
                .withPlan("myplan")
                .withType("standard")

                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("console")
                        .withService("console")
                        .build())
                .withAuthenticationService(new io.enmasse.address.model.AuthenticationServiceBuilder().withName(authType.getName()).build())
                .endSpec()

                .withNewStatus()
                    .withReady(true)
                    .addToEndpointStatuses(new EndpointStatusBuilder()
                            .withName("console")
                            .withServiceHost("console.svc")
                            .withExternalPorts(Collections.singletonMap("http", 443))
                            .withExternalHost("console.example.com")
                            .build())
                .endStatus()

                .build();
    }
}
