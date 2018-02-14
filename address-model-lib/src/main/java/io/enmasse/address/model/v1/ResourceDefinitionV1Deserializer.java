/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.ResourceDefinition;
import io.enmasse.address.model.ResourceRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deserializer for AddressSpacePlan V1 format
 */
class ResourceDefinitionV1Deserializer extends JsonDeserializer<ResourceDefinition> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ResourceDefinition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        return deserialize(root);
    }

    ResourceDefinition deserialize(ObjectNode root) {

        ResourceDefinition.Builder builder = new ResourceDefinition.Builder();

        ObjectNode metadata = (ObjectNode) root.get(Fields.METADATA);

        builder.setName(metadata.get(Fields.NAME).asText());

        if (root.hasNonNull(Fields.TEMPLATE)) {
            builder.setTemplateName(root.get(Fields.TEMPLATE).asText());
        }

        ArrayNode parameters = root.withArray(Fields.PARAMETERS);
        Map<String, String> parameterMap = new HashMap<>();

        for (int i = 0; i < parameters.size(); i++) {
            ObjectNode node = (ObjectNode) parameters.get(i);
            parameterMap.put(node.get(Fields.NAME).asText(), node.get(Fields.VALUE).asText());
        }

        builder.setTemplateParameters(parameterMap);

        return builder.build();
    }
}
