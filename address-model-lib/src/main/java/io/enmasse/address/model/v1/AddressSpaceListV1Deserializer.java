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
