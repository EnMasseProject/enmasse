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
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Serializer for AddressSpaceList V1 format
 *
 * TODO: Don't use reflection based encoding
 */
public class AddressSpaceListV1Serializer extends JsonSerializer<AddressSpaceList> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(AddressSpaceList addressSpaceList, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        SerializeableAddressSpaceList serialized = new SerializeableAddressSpaceList();
        serialized.items = new ArrayList<>();
        for (AddressSpace addressSpace : addressSpaceList) {
            serialized.items.add(AddressSpaceV1Serializer.convert(addressSpace));
        }
        mapper.writeValue(jsonGenerator, serialized);
    }
}
