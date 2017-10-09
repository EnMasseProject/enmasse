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
import io.enmasse.address.model.*;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.Schema;
import io.enmasse.address.model.types.brokered.BrokeredAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;

import java.util.*;

/**
 * Codec that provides the object mapper for V1 format
 */
public class CodecV1 {
    private final Map<String, AddressSpaceType> typeMap = new HashMap<>();
    private final ObjectMapper mapper;


    /**
     * Note: The singleton instance is mostly for convenience, so that we don't have to create the codec
     * everywhere. There might come a time where it needs to be injected from the top level.
     */
    private static final CodecV1 instance;

    static {
        instance = new CodecV1(Arrays.asList(new StandardAddressSpaceType(), new BrokeredAddressSpaceType()), resolveAuthServiceType(System.getenv()));
    }

    public static AuthenticationServiceType resolveAuthServiceType(Map<String, String> env) {
        if (env.containsKey("STANDARD_AUTHSERVICE_SERVICE_HOST")) {
            return AuthenticationServiceType.STANDARD;
        } else {
            return AuthenticationServiceType.NONE;
        }
    }

    public static ObjectMapper getMapper() {
        return instance.getMapperInstance();
    }

    public ObjectMapper getMapperInstance() {
        return this.mapper;
    }

    private CodecV1(Collection<AddressSpaceType> addressSpaceTypes, AuthenticationServiceType defaultAuthServiceType) {
        for (AddressSpaceType type : addressSpaceTypes) {
            typeMap.put(type.getName(), type);
        }
        this.mapper = createMapper(defaultAuthServiceType);
    }

    private ObjectMapper createMapper(AuthenticationServiceType defaultAuthServiceType) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        DecodeContext context = new DecodeContext() {
            @Override
            public AddressSpaceType getAddressSpaceType(String typeName) {
                return typeMap.get(typeName);
            }

            @Override
            public AuthenticationServiceType getDefaultAuthenticationServiceType() {
                return defaultAuthServiceType;
            }
        };

        AddressV1Deserializer addressDeserializer = new AddressV1Deserializer();
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
}
