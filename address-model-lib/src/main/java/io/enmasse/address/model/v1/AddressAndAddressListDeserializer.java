/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;

import java.io.IOException;

public class AddressAndAddressListDeserializer extends JsonDeserializer<Either> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final AddressV1Deserializer addressV1Deserializer;
    private final AddressListV1Deserializer addressListV1Deserializer;

    public AddressAndAddressListDeserializer(AddressV1Deserializer addressV1Deserializer, AddressListV1Deserializer addressListV1Deserializer) {
        this.addressV1Deserializer = addressV1Deserializer;
        this.addressListV1Deserializer = addressListV1Deserializer;
    }

    @Override
    public Either deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        String kind = root.get(Fields.KIND).asText();
        if ("AddressList".equals(kind)) {
            return Either.<Address, AddressList>createRight(addressListV1Deserializer.deserialize(root));
        } else if ("Address".equals(kind)) {
            return Either.<Address, AddressList>createLeft(addressV1Deserializer.deserialize(root));
        } else {
            throw new DeserializeException("Unknown kind " + kind + ", expected Address or AddressList");
        }
    }
}
