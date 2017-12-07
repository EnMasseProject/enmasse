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

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.k8s.api.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KeycloakManager implements Watcher<AddressSpace>
{
    private static final Logger log = LoggerFactory.getLogger(KeycloakManager.class);
    private final KeycloakApi keycloak;

    public KeycloakManager(KeycloakApi keycloak) {
        this.keycloak = keycloak;
    }

    @Override
    public void resourcesUpdated(final Set<AddressSpace> addressSpaces) {
        Map<String, AddressSpace> standardAuthSvcSpaces =
                addressSpaces.stream()
                             .filter(x -> x.getAuthenticationService().getType() == AuthenticationServiceType.STANDARD)
                             .collect(Collectors.toMap(AddressSpace::getName, Function.identity()));

        for(String realmName : keycloak.getRealmNames()) {
            if(standardAuthSvcSpaces.remove(realmName) == null && !"master".equals(realmName)) {
                log.info("Deleting realm {}", realmName);
                keycloak.deleteRealm(realmName);
            }
        }
        for(String name : standardAuthSvcSpaces.keySet()) {
            log.info("Creating realm {}", name);
            keycloak.createRealm(name, name + "-admin");
        }
    }
}
