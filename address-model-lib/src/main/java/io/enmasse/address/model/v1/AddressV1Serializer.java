/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.Address;

import java.io.IOException;
import java.util.Map;

/**
 * Serializer for Address V1 format
 */
class AddressV1Serializer extends JsonSerializer<Address> {

    @Override
    public void serialize(Address address, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1alpha1");
        root.put(Fields.KIND, "Address");
        serialize(address, root);
        root.serialize(jsonGenerator, serializerProvider);
    }

    static void serialize(Address address, ObjectNode root) {
        address.validate();
        ObjectNode metadata = root.putObject(Fields.METADATA);
        ObjectNode spec = root.putObject(Fields.SPEC);
        ObjectNode status = root.putObject(Fields.STATUS);

        metadata.put(Fields.NAME, address.getName());
        metadata.put(Fields.NAMESPACE, address.getNamespace());
        metadata.put(Fields.ADDRESS_SPACE, address.getAddressSpace());

        if (address.getUid() != null) {
            metadata.put(Fields.UID, address.getUid());
        }

        if (address.getSelfLink() != null) {
            metadata.put(Fields.SELF_LINK, address.getSelfLink());
        }

        if (address.getCreationTimestamp() != null) {
            metadata.put(Fields.CREATION_TIMESTAMP, address.getCreationTimestamp());
        }

        if (address.getResourceVersion()  != null) {
            metadata.put(Fields.RESOURCE_VERSION, address.getResourceVersion());
        }

        if (!address.getLabels().isEmpty()) {
            ObjectNode labels = metadata.putObject(Fields.LABELS);
            for (Map.Entry<String, String> entry : address.getLabels().entrySet()) {
                labels.put(entry.getKey(), entry.getValue());
            }
        }

        if (!address.getAnnotations().isEmpty()) {
            ObjectNode annotationObject = metadata.putObject(Fields.ANNOTATIONS);
            for (Map.Entry<String, String> entry : address.getAnnotations().entrySet()) {
                annotationObject.put(entry.getKey(), entry.getValue());
            }
        }

        spec.put(Fields.TYPE, address.getType());
        spec.put(Fields.PLAN, address.getPlan());
        spec.put(Fields.ADDRESS, address.getAddress());

        status.put(Fields.IS_READY, address.getStatus().isReady());
        status.put(Fields.PHASE, address.getStatus().getPhase().name());
        if (!address.getStatus().getMessages().isEmpty()) {
            ArrayNode messages = status.putArray(Fields.MESSAGES);
            for (String message : address.getStatus().getMessages()) {
                messages.add(message);
            }
        }
    }
}
