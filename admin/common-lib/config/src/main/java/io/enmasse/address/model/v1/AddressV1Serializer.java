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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.v1.Fields;

import java.io.IOException;

/**
 * Serializer for Address V1 format
 */
class AddressV1Serializer extends JsonSerializer<Address> {

    @Override
    public void serialize(Address address, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1");
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
        metadata.put(Fields.ADDRESS_SPACE, address.getAddressSpace());
        metadata.put(Fields.UUID, address.getUuid());

        spec.put(Fields.TYPE, address.getType().getName());
        spec.put(Fields.PLAN, address.getPlan().getName());
        spec.put(Fields.ADDRESS, address.getAddress());

        status.put(Fields.IS_READY, address.getStatus().isReady());
        if (!address.getStatus().getMessages().isEmpty()) {
            ArrayNode messages = status.putArray(Fields.MESSAGES);
            for (String message : address.getStatus().getMessages()) {
                messages.add(message);
            }
        }
    }
}
