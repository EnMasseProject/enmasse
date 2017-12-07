/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.keycloak.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        manager.resourcesUpdated(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertTrue(realms.isEmpty());

        manager.resourcesUpdated(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));

        manager.resourcesUpdated(Sets.newSet(createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());

        assertTrue(realmAdminUsers.get("a2").length() > 0);
        assertTrue(realmAdminUsers.get("a3").length() > 0);
    }

    @Test
    public void testRemoveAddressSpace() {
        manager.resourcesUpdated(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        manager.resourcesUpdated(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));

        assertTrue(realms.contains("a1"));
        assertFalse(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());
    }

    @Test
    public void testAuthTypeChanged() {
        manager.resourcesUpdated(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a1"));
        assertEquals(1, realms.size());

        manager.resourcesUpdated(Sets.newSet(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertFalse(realms.contains("a1"));
        assertEquals(0, realms.size());
    }

    private AddressSpace createAddressSpace(String name, AuthenticationServiceType authType) {
        return new AddressSpace.Builder().setName(name).setType(new StandardAddressSpaceType()).setAuthenticationService(new AuthenticationService.Builder().setType(authType).build()).build();
    }
}
