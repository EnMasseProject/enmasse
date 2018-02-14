/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.enmasse.address.model.*;

import java.util.*;

/**
 * Codec that provides the object mapper for V1 format
 */
public class CodecV1 {
    private final ObjectMapper mapper;


    /**
     * Note: The singleton instance is mostly for convenience, so that we don't have to create the codec
     * everywhere. There might come a time where it needs to be injected from the top level.
     */
    private static final CodecV1 instance;

    static {
        instance = new CodecV1(resolveAuthServiceType(System.getenv()));
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

    private CodecV1(AuthenticationServiceType defaultAuthServiceType) {
        this.mapper = createMapper(defaultAuthServiceType);
    }

    private ObjectMapper createMapper(AuthenticationServiceType defaultAuthServiceType) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        DecodeContext context = () -> defaultAuthServiceType;

        AddressV1Deserializer addressDeserializer = new AddressV1Deserializer();
        AddressListV1Deserializer addressListDeserializer = new AddressListV1Deserializer(addressDeserializer);
        AddressSpaceV1Deserializer addressSpaceDeserializer = new AddressSpaceV1Deserializer(context);

        module.addDeserializer(Address.class, addressDeserializer);
        module.addSerializer(Address.class, new AddressV1Serializer());
        module.addDeserializer(AddressSpace.class, addressSpaceDeserializer);
        module.addSerializer(AddressSpace.class, new AddressSpaceV1Serializer());

        module.addDeserializer(Either.class, new AddressAndAddressListDeserializer(addressDeserializer, addressListDeserializer));
        module.addDeserializer(AddressList.class, addressListDeserializer);
        module.addSerializer(AddressList.class, new AddressListV1Serializer());
        module.addDeserializer(AddressSpaceList.class, new AddressSpaceListV1Deserializer(addressSpaceDeserializer));
        module.addSerializer(AddressSpaceList.class, new AddressSpaceListV1Serializer());

        module.addDeserializer(AddressSpacePlan.class, new AddressSpacePlanV1Deserializer());
        module.addDeserializer(AddressPlan.class, new AddressPlanV1Deserializer());
        module.addDeserializer(ResourceDefinition.class, new ResourceDefinitionV1Deserializer());
        module.addSerializer(Schema.class, new SchemaV1Serializer());

        mapper.registerModule(module);
        return mapper;
    }
}
