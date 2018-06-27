/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

public class KeycloakInstance implements KeycloakClient {
    private final Keycloak keycloak;
    private static final Logger log = LoggerFactory.getLogger(getClass());

    public KeycloakInstance(Keycloak keycloak) {
        this.keycloak = keycloak;
    }


    @Override
    public GroupRepresentation createGroup(String name, String group) {
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName(group);
        keycloak.realm(name).groups().add(groupRep);
        for (GroupRepresentation rep : keycloak.realm(name).groups().groups()) {
            if (rep.getName().equals(group)) {
                return rep;
            }
        }
        return groupRep;
    }

    @Override
    public List<GroupRepresentation> getGroups(String realm) {
        return keycloak.realm(realm).groups().groups();
    }

    @Override
    public UserRepresentation createUser(String realm, String username, String password) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(username);
        userRep.setEnabled(true);

        CredentialRepresentation creds = new CredentialRepresentation();
        creds.setType("password");
        creds.setValue(password);
        creds.setTemporary(false);
        userRep.setCredentials(Collections.singletonList(creds));

        Response response = keycloak.realm(realm).users().create(userRep);
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            log.warn("Error creating user in keycloak: {}: {}", response.getStatus(), response.getEntity());
            throw new RuntimeException("Error creating user in keycloak: " + response.getStatus());
        }

        return keycloak.realm(realm).users().search(username).get(0);
    }


    @Override
    public void joinGroup(String realm, String userId, String groupId) {
        keycloak.realm(realm).users().get(userId).joinGroup(groupId);
    }

    @Override
    public void close() throws Exception {
        keycloak.close();
    }
}
