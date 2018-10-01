/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.dsl.BuildConfigResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeycloakManagerTest {
    private static final String KEYCLOAK_CONFIG_NAME = "keycloakConfigName";

    private KeycloakManager manager;
    private Set<String> realms;
    private List<String> updatedRealms;
    private Map<String, String> realmAdminUsers;
    private KubeApi mockKubeApi;
    private NamespacedOpenShiftClient mockClient;

    @Before
    public void setup() {
        realms = new HashSet<>();
        updatedRealms = new LinkedList<>();
        realmAdminUsers = new HashMap<>();
        mockKubeApi = mock(KubeApi.class);
        mockClient = mock(NamespacedOpenShiftClient.class);
        when(mockKubeApi.findUserId(any())).thenReturn("");

        ConfigMap keycloakConfigMap = createKeyCloakConfigMapMock("http://example.com", "id", "secret");
        configClientToReturn(KEYCLOAK_CONFIG_NAME, keycloakConfigMap);

        manager = new KeycloakManager(new KeycloakApi() {
            @Override
            public Set<String> getRealmNames() {
                return new HashSet<>(realms);
            }

            @Override
            public void createRealm(String namespace, String realmName, String consoleRedirectURI, IdentityProviderParams params) {
                realms.add(realmName);
            }

            @Override
            public void updateRealm(String realmName, IdentityProviderParams updated) {
                updatedRealms.add(realmName);
            }

            @Override
            public void deleteRealm(String realmName) {
                realms.remove(realmName);
            }
        }, mockKubeApi, new UserApi() {
            @Override
            public Optional<User> getUserWithName(String realm, String name) {
                return Optional.empty();
            }

            @Override
            public void createUser(String realm, User user) {
                realmAdminUsers.put(realm, user.getSpec().getUsername());
            }

            @Override
            public boolean replaceUser(String realm, User user) {
                return false;
            }

            @Override
            public void deleteUser(String realm, User user) {

            }

            @Override
            public UserList listUsers(String realm) {
                return null;
            }

            @Override
            public UserList listUsersWithLabels(String realm, Map<String, String> labels) {
                return null;
            }

            @Override
            public void deleteUsers(String namespace) {

            }
        }, mockClient, KEYCLOAK_CONFIG_NAME);
    }

    @Test
    public void testAddAddressSpace() throws Exception {
        manager.onUpdate(Collections.singleton(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertTrue(realms.isEmpty());

        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));

        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());

        assertTrue(realmAdminUsers.get("a2").length() > 0);
        assertTrue(realmAdminUsers.get("a3").length() > 0);
    }

    @Test
    public void testRemoveAddressSpace() throws Exception {
        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));

        assertTrue(realms.contains("a1"));
        assertFalse(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());
    }

    @Test
    public void testAuthTypeChanged() throws Exception {
        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a1"));
        assertEquals(1, realms.size());

        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertFalse(realms.contains("a1"));
        assertEquals(0, realms.size());
    }

    @Test
    public void testUpdateRealm() throws Exception {
        Set<AddressSpace> spaces = Collections.singleton(createAddressSpace("a1", AuthenticationServiceType.STANDARD));
        manager.onUpdate(spaces);
        assertTrue(realms.contains("a1"));
        assertTrue(updatedRealms.isEmpty());

        manager.onUpdate(spaces);
        assertTrue(updatedRealms.isEmpty());

        ConfigMap keycloakConfigMap = createKeyCloakConfigMapMock("http://example.com", "id", "secret1");
        configClientToReturn(KEYCLOAK_CONFIG_NAME, keycloakConfigMap);

        manager.onUpdate(spaces);
        assertEquals(1, updatedRealms.size());
    }

    private AddressSpace createAddressSpace(String name, AuthenticationServiceType authType) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace("myns")
                .setPlan("myplan")
                .setType("standard")
                .putAnnotation(AnnotationKeys.CREATED_BY, "developer")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("console")
                        .setService("console")
                        .setServicePort("https")
                        .build())
                .setStatus(new AddressSpaceStatus(true)
                        .appendEndpointStatus(new EndpointStatus.Builder()
                                .setName("console")
                                .setServiceHost("console.svc")
                                .setPort(443)
                                .setHost("console.example.com")
                                .build()))
                .setAuthenticationService(new AuthenticationService.Builder().setType(authType).build()).build();
    }

    private ConfigMap createKeyCloakConfigMapMock(String url, String id, String secret) {
        ConfigMap keycloakConfigMap = mock(ConfigMap.class);
        Map<String, String> map = new HashMap<>();
        map.put("identityProviderUrl", url);
        map.put("identityProviderClientId", id);
        map.put("identityProviderClientSecret", secret);

        when(keycloakConfigMap.getData()).thenReturn(map);
        return keycloakConfigMap;
    }

    private void configClientToReturn(String keycloakConfigName, ConfigMap keycloakConfigMap) {
        BuildConfigResource buildConfigResource = mock(BuildConfigResource.class);
        when(buildConfigResource.get()).thenReturn(keycloakConfigMap);
        MixedOperation configMapsOperation = mock(MixedOperation.class);
        when(configMapsOperation.withName(keycloakConfigName)).thenReturn(buildConfigResource);
        when(mockClient.configMaps()).thenReturn(configMapsOperation);
    }
}
