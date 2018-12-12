/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.AddressSpaceList;

import java.io.IOException;

/**
 * Deserializer for AddressSpaceList V1 format
 *
 */
class AddressSpaceListV1Deserializer extends JsonDeserializer<AddressSpaceList> {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final AddressSpaceV1Deserializer addressSpaceDeserializer;

    AddressSpaceListV1Deserializer(AddressSpaceV1Deserializer addressSpaceDeserializer) {
        this.addressSpaceDeserializer = addressSpaceDeserializer;
    }

    @Override
    public AddressSpaceList deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        AddressSpaceList retval = new AddressSpaceList();
        if (root.hasNonNull(Fields.ITEMS)) {
            ArrayNode items = (ArrayNode) root.get(Fields.ITEMS);
            for (int i = 0; i < items.size(); i++) {
                retval.add(addressSpaceDeserializer.deserialize((ObjectNode) items.get(i)));
            }
        }
        return retval;
    }
}
