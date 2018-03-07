/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.*;

import static org.junit.Assert.*;

public class KeycloakManagerTest {
    private KeycloakManager manager;
    private Set<String> realms;
    private Map<String, String> realmAdminUsers;

    @Before
    public void setup() {
        realms = new HashSet<>();
        realmAdminUsers = new HashMap<>();
        manager = new KeycloakManager(new KeycloakApi() {
            @Override
            public Set<String> getRealmNames() {
                return new HashSet<>(realms);
            }

            @Override
            public void createRealm(String realmName, String realmAdminUser) {
                realms.add(realmName);
                realmAdminUsers.put(realmName, realmAdminUser);
            }

            @Override
            public void deleteRealm(String realmName) {
                realms.remove(realmName);
            }
        });
    }

    @Test
    public void testAddAddressSpace() {
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
    public void testRemoveAddressSpace() {
        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));

        assertTrue(realms.contains("a1"));
        assertFalse(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());
    }

    @Test
    public void testAuthTypeChanged() {
        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a1"));
        assertEquals(1, realms.size());

        manager.onUpdate(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertFalse(realms.contains("a1"));
        assertEquals(0, realms.size());
    }

    private AddressSpace createAddressSpace(String name, AuthenticationServiceType authType) {
        return new AddressSpace.Builder().setName(name).setType("standard").setAuthenticationService(new AuthenticationService.Builder().setType(authType).build()).build();
    }
}
