/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.List;

public class KeycloakInstance implements KeycloakClient {
    private final Keycloak keycloak;

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

        keycloak.realm(realm).users().create(userRep);
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
