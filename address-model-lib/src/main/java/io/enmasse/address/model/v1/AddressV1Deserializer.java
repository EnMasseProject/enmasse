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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressType;
import io.enmasse.address.model.Plan;
import io.enmasse.address.model.Status;

import java.io.IOException;

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
        ObjectNode metadata = (ObjectNode) root.get(Fields.METADATA);
        ObjectNode spec = (ObjectNode) root.get(Fields.SPEC);
        ObjectNode status = (ObjectNode) root.get(Fields.STATUS);

        AddressType type = new AddressType(spec.get(Fields.TYPE).asText());


        Address.Builder builder = new Address.Builder()
                .setName(metadata.get(Fields.NAME).asText())
                .setType(type);

        if (spec.hasNonNull(Fields.PLAN)) {
            builder.setPlan(new Plan(spec.get(Fields.PLAN).asText()));
        }

        if (metadata.hasNonNull(Fields.ADDRESS_SPACE)) {
            builder.setAddressSpace(metadata.get(Fields.ADDRESS_SPACE).asText());
        }

        if (metadata.hasNonNull(Fields.UUID)) {
            builder.setUuid(metadata.get(Fields.UUID).asText());
        }

        if (spec.hasNonNull(Fields.ADDRESS)) {
            builder.setAddress(spec.get(Fields.ADDRESS).asText());
        }

        if (status != null) {
            boolean isReady = status.get(Fields.IS_READY).asBoolean();
            Status s = new Status(isReady);
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

    // TODO: This is a more low-level generator to avoid re-encoding address payload into a list
    /* Find a better way to fix this in the future
    public byte[] encodeAddressList(List<byte[]> addressList) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = mapper.getFactory().createGenerator(baos);
        generator.writeStartObject();
        generator.writeStringField("kind", "AddressList");
        generator.writeStringField("apiVersion", "enmasse.io/v1");
        generator.writeArrayFieldStart("items");
        for (byte[] address : addressList) {
            generator.writeRawValue(new String(address, "UTF-8"));
        }
        generator.writeEndArray();
        generator.writeEndObject();
        generator.close();
        baos.close();
        return baos.toByteArray();
    }
    */
}
