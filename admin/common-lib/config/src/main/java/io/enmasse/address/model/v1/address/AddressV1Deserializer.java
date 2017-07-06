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
package io.enmasse.address.model.v1.address;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.Plan;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;

import java.io.IOException;

/**
 * Deserializer for Address V1 format
 *
 * TODO: Don't use reflection based decoding
 */
public class AddressV1Deserializer extends JsonDeserializer<Address> {
    private static final ObjectMapper mapper = new ObjectMapper();

    // TODO: Make this generic
    private static final AddressSpaceType addressSpaceType = new StandardAddressSpaceType();

    @Override
    public Address deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        SerializeableAddress address = mapper.readValue(jsonParser, SerializeableAddress.class);
        return convert(address);
    }

    static Address convert(SerializeableAddress address) {
        AddressType type = findAddressType(addressSpaceType, address.spec.type);
        Plan plan = type.getDefaultPlan();
        if (address.spec.plan != null) {
            plan = findPlan(type, address.spec.plan);
        }

        Address.Builder builder = new Address.Builder()
                .setName(address.metadata.name)
                .setAddressSpace(address.metadata.addressSpace)
                .setUuid(address.metadata.uuid)
                .setPlan(plan)
                .setType(type);

        if (address.status != null) {
            Status status = new Status(address.status.isReady);
            status.setMessages(address.status.messages);
            builder.setStatus(status);
        }

        return builder.build();
    }

    private static Plan findPlan(AddressType type, String planName) {
        for (Plan plan : type.getPlans()) {
            if (plan.getName().equals(planName)) {
                return plan;
            }
        }
        throw new RuntimeException("Unknown plan " + planName + " for type " + type.getName());
    }

    private static AddressType findAddressType(AddressSpaceType type, String addressTypeName) {
        for (AddressType atype : type.getAddressTypes()) {
            if (atype.getName().equals(addressTypeName)) {
                return atype;
            }
        }
        throw new RuntimeException("Unknown address type " + addressTypeName + " for address space type " + type.getName());
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
