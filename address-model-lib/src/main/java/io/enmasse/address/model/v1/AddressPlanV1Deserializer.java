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
import io.enmasse.address.model.AddressPlan;
import io.enmasse.address.model.ResourceRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializer for AddressSpacePlan V1 format
 */
class AddressPlanV1Deserializer extends JsonDeserializer<AddressPlan> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public AddressPlan deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        return deserialize(root);
    }

    AddressPlan deserialize(ObjectNode root) {

        AddressPlan.Builder builder = new AddressPlan.Builder();

        ObjectNode metadata = (ObjectNode) root.get(Fields.METADATA);

        builder.setName(metadata.get(Fields.NAME).asText());

        if (root.hasNonNull(Fields.DISPLAY_NAME)) {
            builder.setDisplayName(root.get(Fields.DISPLAY_NAME).asText());
        }

        if (root.hasNonNull(Fields.DISPLAY_ORDER)) {
            builder.setDisplayOrder(root.get(Fields.DISPLAY_ORDER).asInt());
        }

        if (root.hasNonNull(Fields.SHORT_DESCRIPTION)) {
            builder.setShortDescription(root.get(Fields.SHORT_DESCRIPTION).asText());
        }

        if (root.hasNonNull(Fields.LONG_DESCRIPTION)) {
            builder.setLongDescription(root.get(Fields.LONG_DESCRIPTION).asText());
        }

        if (root.hasNonNull(Fields.UUID)) {
            builder.setUuid(root.get(Fields.UUID).asText());
        }

        ArrayNode requiredResources = root.withArray(Fields.REQUIRED_RESOURCES);
        List<ResourceRequest> resourceRequests = new ArrayList<>();

        for (int i = 0; i < requiredResources.size(); i++) {
            String name = requiredResources.get(i).get(Fields.NAME).asText();
            double credit = requiredResources.get(i).get(Fields.CREDIT).asDouble();
            resourceRequests.add(new io.enmasse.address.model.ResourceRequest(name, credit));
        }
        builder.setRequestedResources(resourceRequests);


        builder.setAddressType(root.get(Fields.ADDRESS_TYPE).asText());

        return builder.build();
    }
}
