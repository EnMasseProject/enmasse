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
import io.enmasse.address.model.ResourceAllowance;
import io.enmasse.address.model.AddressSpacePlan;

import java.io.IOException;
import java.util.*;

/**
 * Deserializer for AddressSpacePlan V1 format
 */
class AddressSpacePlanV1Deserializer extends JsonDeserializer<AddressSpacePlan> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public AddressSpacePlan deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        return deserialize(root);
    }

    AddressSpacePlan deserialize(ObjectNode root) {

        AddressSpacePlan.Builder builder = new AddressSpacePlan.Builder();

        ObjectNode metadata = (ObjectNode) root.get(Fields.METADATA);

        builder.setName(metadata.get(Fields.NAME).asText());

        if (metadata.hasNonNull(Fields.ANNOTATIONS)) {
            Map<String, String> annotationsMap = new HashMap<>();
            ObjectNode annotations = metadata.with(Fields.ANNOTATIONS);

            Iterator<String> annotationIt = annotations.fieldNames();
            while (annotationIt.hasNext()) {
                String annotationKey = annotationIt.next();
                if (annotations.get(annotationKey).isTextual()) {
                    annotationsMap.put(annotationKey, annotations.get(annotationKey).asText());
                }
            }
            builder.setAnnotations(annotationsMap);
        }

        builder.setAddressSpaceType(root.get(Fields.ADDRESS_SPACE_TYPE).asText());

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

        ArrayNode resources = root.withArray(Fields.RESOURCES);
        List<ResourceAllowance> resourceAllowances = new ArrayList<>();

        for (int i = 0; i < resources.size(); i++) {
            double min = resources.get(i).get(Fields.MIN).asDouble();
            double max = resources.get(i).get(Fields.MAX).asDouble();
            String name = resources.get(i).get(Fields.NAME).asText();
            resourceAllowances.add(new ResourceAllowance(name, min, max));
        }
        builder.setResources(resourceAllowances);

        ArrayNode plans = root.withArray(Fields.ADDRESS_PLANS);
        List<String> addressPlans = new ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            addressPlans.add(plans.get(i).asText());
        }
        builder.setAddressPlans(addressPlans);

        return builder.build();
    }
}
