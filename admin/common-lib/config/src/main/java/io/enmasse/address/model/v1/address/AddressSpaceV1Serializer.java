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

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Serializer for AddressSpace V1 format
 *
 * TODO: Don't use reflection based encoding
 */
public class AddressSpaceV1Serializer extends JsonSerializer<AddressSpace> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(AddressSpace addressSpace, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        mapper.writeValue(jsonGenerator, convert(addressSpace));
    }

    static SerializeableAddressSpace convert(AddressSpace addressSpace) {
        SerializeableAddressSpace serialized = new SerializeableAddressSpace();
        serialized.metadata = new AddressSpaceMeta();
        serialized.metadata.name = addressSpace.getName();

        serialized.spec = new AddressSpaceSpec();
        serialized.spec.type = addressSpace.getType().getName();
        serialized.spec.plan = addressSpace.getPlan().getName();
        serialized.spec.endpoints = addressSpace.getEndpoints().stream()
                .map(endpoint -> {
                    Endpoint e = new Endpoint();
                    e.name = endpoint.getName();
                    e.service = endpoint.getService();
                    e.host = endpoint.getHost().orElse(null);
                    endpoint.getCertProvider().ifPresent(certProvider -> {
                        e.certProvider = new io.enmasse.address.model.v1.address.CertProvider();
                        e.certProvider.name = certProvider.getName();
                        e.certProvider.secretName = certProvider.getSecretName();
                    });
                    return e;
                }).collect(Collectors.toList());
        return serialized;
    }

}
