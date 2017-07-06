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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.enmasse.address.model.Address;

import java.io.IOException;

/**
 * Serializer for Address V1 format
 *
 * TODO: Don't use reflection based encoding
 */
public class AddressV1Serializer extends JsonSerializer<Address> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(Address address, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        mapper.writeValue(jsonGenerator, convert(address));
    }

    static SerializeableAddress convert(Address address) {
        SerializeableAddress serialized = new SerializeableAddress();

        serialized.metadata = new AddressMeta();
        serialized.metadata.name = address.getName();
        serialized.metadata.uuid = address.getUuid();
        serialized.metadata.addressSpace = address.getAddressSpace();

        serialized.spec = new AddressSpec();
        serialized.spec.plan = address.getPlan().getName();
        serialized.spec.type = address.getType().getName();
        serialized.spec.address = address.getAddress();

        serialized.status = new AddressStatus();
        serialized.status.isReady = address.getStatus().isReady();
        serialized.status.messages = address.getStatus().getMessages();

        return serialized;
    }
}
