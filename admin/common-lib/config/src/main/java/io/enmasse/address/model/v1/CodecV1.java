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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.types.Schema;

/**
 * Codec that provides the object mapper for V1 format
 */
public class CodecV1 {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Address.class, new AddressV1Deserializer());
        module.addSerializer(Address.class, new AddressV1Serializer());
        module.addDeserializer(AddressSpace.class, new AddressSpaceV1Deserializer());
        module.addSerializer(AddressSpace.class, new AddressSpaceV1Serializer());

        module.addDeserializer(AddressList.class, new AddressListV1Deserializer());
        module.addSerializer(AddressList.class, new AddressListV1Serializer());
        module.addDeserializer(AddressSpaceList.class, new AddressSpaceListV1Deserializer());
        module.addSerializer(AddressSpaceList.class, new AddressSpaceListV1Serializer());

        module.addSerializer(Schema.class, new SchemaV1Serializer());
        mapper.registerModule(module);
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }
}
