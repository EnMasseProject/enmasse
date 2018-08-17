/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;

import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeycloakUserApiTest {
    @Test
    public void testConversion() {
        UserRepresentation userRep = new UserRepresentation();

        userRep.setUsername("user1");
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("resourceName", Arrays.asList("myspace.user1"));
        attributes.put("resourceNamespace", Arrays.asList("ns1"));
        attributes.put("authenticationType", Arrays.asList(UserAuthenticationType.password.name()));
        attributes.put("creationTimestamp", Arrays.asList("12345"));
        userRep.setAttributes(attributes);

        GroupRepresentation sendQueue1 = new GroupRepresentation();
        sendQueue1.setId("sq1");
        sendQueue1.setName("send_queue1");

        GroupRepresentation recvQueue1 = new GroupRepresentation();
        recvQueue1.setId("rq1");
        recvQueue1.setName("recv_queue1");

        GroupRepresentation allDirect1 = new GroupRepresentation();
        allDirect1.setId("mg");
        allDirect1.setName("manage");

        userRep.setGroups(Arrays.asList("sq1", "rq1", "mg"));

        User user = KeycloakUserApi.buildUser(userRep, Arrays.asList(sendQueue1, recvQueue1, allDirect1));
        assertEquals("myspace.user1", user.getMetadata().getName());
        assertEquals("ns1", user.getMetadata().getNamespace());
        assertEquals("user1", user.getSpec().getUsername());
        assertEquals(UserAuthenticationType.password, user.getSpec().getAuthentication().getType());

        assertEquals(2, user.getSpec().getAuthorization().size());
    }
}
