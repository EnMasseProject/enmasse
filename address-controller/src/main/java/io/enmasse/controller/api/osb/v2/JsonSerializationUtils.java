/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import java.util.UUID;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonSerializationUtils {

    public static UUID getUuid(ObjectNode node, String field) {
        JsonNode jsonNode = getRequiredField(node, field);
        try {
            return UUID.fromString(jsonNode.asText());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid UUID " + jsonNode.asText() + " in field " + field);
        }
    }

    public static JsonNode getRequiredField(ObjectNode node, String name) {
        JsonNode organizationGuid = node.get(name);
        if (organizationGuid == null) {
            throw new BadRequestException("Field " + name + " is required");
        }
        return organizationGuid;
    }
}
