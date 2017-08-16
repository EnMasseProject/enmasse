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

package io.enmasse.controller.auth;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.RealmRepresentation;


public class KeycloakAuthServiceManager implements AuthServiceManager
{
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    private KeycloakAuthServiceManager(final String host,
                                       final int port,
                                       final String username,
                                       final String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public static AuthServiceManager create(String host,
                                            int port,
                                            String username,
                                            String password) {
        return new KeycloakAuthServiceManager(host, port, username, password);
    }

    @Override
    public void updateAuthServices(final Set<AddressSpace> addressSpaces) {
        Keycloak keycloak = Keycloak.getInstance(
                "http://" + host + ":" + port + "/auth",
                "master",
                username,
                password,
                "admin-cli");
        RealmsResource realms = keycloak.realms();
        Map<String, AddressSpace> standardAuthSvcSpaces =
                addressSpaces.stream()
                             .filter(x -> x.getAuthenticationService().getType() == AuthenticationServiceType.STANDARD)
                             .collect(Collectors.toMap(AddressSpace::getName, Function.identity()));
        for(RealmRepresentation realm : realms.findAll()) {
            String realmName = realm.getRealm();
            if(standardAuthSvcSpaces.remove(realmName) == null && !"master".equals(realmName)) {
                keycloak.realm(realmName).remove();
            }
        }
        for(String name : standardAuthSvcSpaces.keySet()) {
            final RealmRepresentation newrealm = new RealmRepresentation();
            newrealm.setRealm(name);
            newrealm.setPasswordPolicy("hashAlgorithm(scramsha1)");
            keycloak.realms().create(newrealm);
        }
    }
}
