/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;

import java.io.IOException;

/**
 * Serializer for AddressList V1 format
 *
 */
class AddressListV1Serializer extends JsonSerializer<AddressList> {
    @Override
    public void serialize(AddressList addressList, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1");
        root.put(Fields.KIND, "AddressList");
        ArrayNode items = root.putArray(Fields.ITEMS);
        for (Address address : addressList) {
            ObjectNode entry = items.addObject();
            AddressV1Serializer.serialize(address, entry);
        }
        root.serialize(jsonGenerator, serializerProvider);
    }
}
