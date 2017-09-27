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
package enmasse.systemtest;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KeycloakClient implements AutoCloseable {
    private final Keycloak keycloak;

    public KeycloakClient(Endpoint endpoint, KeycloakCredentials credentials) {
        Logging.log.info("Logging into keycloak with {}/{}", credentials.getUsername(), credentials.getPassword());
        this.keycloak = Keycloak.getInstance("http://" + endpoint.getHost() + ":" + endpoint.getPort() + "/auth",
                "master", credentials.getUsername(), credentials.getPassword(), "admin-cli");
    }

    public void createUser(String realm, String userName, String password, long timeout, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        RealmResource realmResource = waitForRealm(realm, timeout, timeUnit);

        if (realmResource.users().search(userName).isEmpty()) {
            UserRepresentation userRep = new UserRepresentation();
            userRep.setUsername(userName);
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(password);
            cred.setTemporary(false);
            userRep.setCredentials(Arrays.asList(cred));
            userRep.setEnabled(true);
            Response response = keycloak.realm(realm).users().create(userRep);
            if (response.getStatus() != 201) {
                throw new RuntimeException("Unable to create user: " + response.getStatus());
            }
        } else {
            Logging.log.info("User " + userName + " already created, skipping");
        }
    }

    private RealmResource waitForRealm(String realmName, long timeout, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        RealmResource realm = keycloak.realm(realmName);
        while (realm == null && System.currentTimeMillis() < endTime) {
            Thread.sleep(5000);
        }
        if (realm == null) {
            throw new TimeoutException("Timed out waiting for realm " + realmName + " to exist");
        }
        return realm;
    }

    public void deleteUser(String realm, String userName) {
        keycloak.realm(realm).users().delete(userName);
    }

    @Override
    public void close() throws Exception {
        keycloak.close();
    }
}
