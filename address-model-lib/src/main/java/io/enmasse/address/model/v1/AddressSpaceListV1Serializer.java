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
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;

import java.io.IOException;

/**
 * Serializer for AddressSpaceList V1 format
 */
class AddressSpaceListV1Serializer extends JsonSerializer<AddressSpaceList> {
    @Override
    public void serialize(AddressSpaceList addressSpaceList, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1alpha1");
        root.put(Fields.KIND, "AddressSpaceList");
        ArrayNode items = root.putArray(Fields.ITEMS);
        for (AddressSpace addressSpace : addressSpaceList) {
            ObjectNode entry = items.addObject();
            AddressSpaceV1Serializer.serialize(addressSpace, entry);
        }
        root.serialize(jsonGenerator, serializerProvider);
    }
}
