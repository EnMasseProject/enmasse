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
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.Schema;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;

import java.util.HashMap;
import java.util.Map;

/**
 * Codec that provides the object mapper for V1 format
 */
public class CodecV1 {
    private static final Map<String, ObjectMapper> serializerMap = new HashMap<>();
    private static final Map<String, AddressSpaceType> typeMap = new HashMap<>();

    static {
        StandardAddressSpaceType type = new StandardAddressSpaceType();
        serializerMap.put(type.getName(), createMapper(type));
        typeMap.put(type.getName(), type);
    }

    private static ObjectMapper createMapper(AddressSpaceType addressSpaceType) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        DecodeContext context = typeMap::get;

        AddressV1Deserializer addressDeserializer = new AddressV1Deserializer(addressSpaceType);
        AddressSpaceV1Deserializer addressSpaceDeserializer = new AddressSpaceV1Deserializer(context);

        module.addDeserializer(Address.class, addressDeserializer);
        module.addSerializer(Address.class, new AddressV1Serializer());
        module.addDeserializer(AddressSpace.class, addressSpaceDeserializer);
        module.addSerializer(AddressSpace.class, new AddressSpaceV1Serializer());

        module.addDeserializer(AddressList.class, new AddressListV1Deserializer(addressDeserializer));
        module.addSerializer(AddressList.class, new AddressListV1Serializer());
        module.addDeserializer(AddressSpaceList.class, new AddressSpaceListV1Deserializer(addressSpaceDeserializer));
        module.addSerializer(AddressSpaceList.class, new AddressSpaceListV1Serializer());

        module.addSerializer(Schema.class, new SchemaV1Serializer());
        mapper.registerModule(module);
        return mapper;
    }

    // TODO: Parameterize this by type
    public static ObjectMapper getMapper() {
        return serializerMap.get(new StandardAddressSpaceType().getName());
    }
}
