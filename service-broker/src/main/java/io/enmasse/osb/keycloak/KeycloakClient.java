/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.keycloak;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface KeycloakClient extends AutoCloseable {
    GroupRepresentation createGroup(String realm, String group);
    List<GroupRepresentation> getGroups(String realm);
    UserRepresentation createUser(String realm, String username, String password);
    void joinGroup(String realm, String userId, String groupId);
}
