/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;

import java.io.IOException;
import java.util.Iterator;

/**
 * Deserializer for Address V1 format
 */
class AddressV1Deserializer extends JsonDeserializer<Address> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Address deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        return deserialize(root);
    }

    Address deserialize(ObjectNode root) {
        validate(root);
        ObjectNode metadata = (ObjectNode) root.get(Fields.METADATA);
        ObjectNode spec = (ObjectNode) root.get(Fields.SPEC);
        ObjectNode status = (ObjectNode) root.get(Fields.STATUS);

        String type = spec.get(Fields.TYPE).asText();

        Address.Builder builder = new Address.Builder()
                .setAddress(spec.get(Fields.ADDRESS).asText())
                .setType(type)
                .setPlan(spec.get(Fields.PLAN).asText());

        if (metadata != null) {
            if (metadata.hasNonNull(Fields.NAME)) {
                builder.setName(metadata.get(Fields.NAME).asText());
            }

            if (metadata.hasNonNull(Fields.ADDRESS_SPACE)) {
                builder.setAddressSpace(metadata.get(Fields.ADDRESS_SPACE).asText());
            }

            if (metadata.hasNonNull(Fields.UUID)) {
                builder.setUuid(metadata.get(Fields.UUID).asText());
            }

            if (metadata.hasNonNull(Fields.ANNOTATIONS)) {
                ObjectNode annotationObject = metadata.with(Fields.ANNOTATIONS);
                Iterator<String> annotationIt = annotationObject.fieldNames();
                while (annotationIt.hasNext()) {
                    String key = annotationIt.next();
                    if (annotationObject.get(key).isTextual()) {
                        builder.putAnnotation(key, annotationObject.get(key).asText());
                    }
                }
            }
        }

        if (status != null) {
            boolean isReady = status.get(Fields.IS_READY).asBoolean();
            Status s = new Status(isReady);

            if (status.hasNonNull(Fields.PHASE)) {
                s.setPhase(Status.Phase.valueOf(status.get(Fields.PHASE).asText()));
            }
            if (status.hasNonNull(Fields.MESSAGES)) {
                ArrayNode messages = (ArrayNode) status.get(Fields.MESSAGES);
                for (int i = 0; i < messages.size(); i++) {
                    s.appendMessage(messages.get(i).asText());
                }
            }
            builder.setStatus(s);
        }

        return builder.build();
    }

    private void validate(ObjectNode root) {
        JsonNode node = root.get(Fields.SPEC);
        if (node == null || !node.isObject()) {
            throw new DeserializeException("Missing 'spec' object field");
        }

        ObjectNode spec = (ObjectNode) node;
        JsonNode address = spec.get(Fields.ADDRESS);
        if (address == null || !address.isTextual()) {
            throw new DeserializeException("Missing 'address' string field in 'spec'");
        }

        JsonNode type = spec.get(Fields.TYPE);
        if (type == null || !type.isTextual()) {
            throw new DeserializeException("Missing 'type' string field in 'spec'");
        }

        JsonNode plan = spec.get(Fields.PLAN);
        if (plan == null || !plan.isTextual()) {
            throw new DeserializeException("Missing 'plan' string field in 'spec'");
        }
    }
}
