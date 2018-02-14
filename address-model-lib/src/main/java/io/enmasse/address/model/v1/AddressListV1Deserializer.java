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
import io.enmasse.address.model.AddressList;

import java.io.IOException;

/**
 * Deserializer for AddressList V1 format
 *
 */
class AddressListV1Deserializer extends JsonDeserializer<AddressList> {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final AddressV1Deserializer addressDeserializer;

    public AddressListV1Deserializer(AddressV1Deserializer addressDeserializer) {
        this.addressDeserializer = addressDeserializer;
    }

    @Override
    public AddressList deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        return deserialize(root);
    }

    AddressList deserialize(ObjectNode root) {
        AddressList retval = new AddressList();
        if (root.hasNonNull(Fields.ITEMS)) {
            ArrayNode items = (ArrayNode) root.get(Fields.ITEMS);
            for (int i = 0; i < items.size(); i++) {
                retval.add(addressDeserializer.deserialize((ObjectNode) items.get(i)));
            }
        }
        return retval;

    }
}
