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
import io.enmasse.address.model.*;
import io.enmasse.address.model.CertProvider;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.Plan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deserializer for AddressSpace V1 format
 *
 * TODO: Don't use reflection based encoding
 */
public class AddressSpaceV1Deserializer extends JsonDeserializer<AddressSpace> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DecodeContext context = new GlobalDecodeContext();

    @Override
    public AddressSpace deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        SerializeableAddressSpace addressSpace = mapper.readValue(jsonParser, SerializeableAddressSpace.class);
        return convert(addressSpace);
    }

    static AddressSpace convert(SerializeableAddressSpace addressSpace) {
        AddressSpaceType type = context.getAddressSpaceType(addressSpace.spec.type);

        Plan plan = type.getDefaultPlan();
        if (addressSpace.spec.plan != null) {
            plan = findPlan(type, addressSpace.spec.plan);
        }

        List<io.enmasse.address.model.Endpoint> endpoints = new ArrayList<>();
        if (addressSpace.spec.endpoints != null) {
            endpoints = decodeEndpoints(addressSpace.spec.endpoints);
        }
        return new AddressSpace.Builder()
                .setName(addressSpace.metadata.name)
                .setNamespace(addressSpace.metadata.namespace)
                .setType(type)
                .setEndpointList(endpoints)
                .setPlan(plan)
                .build();
    }

    private static List<io.enmasse.address.model.Endpoint> decodeEndpoints(List<Endpoint> endpoints) {
        return endpoints.stream()
                .map(endpoint -> {
                    io.enmasse.address.model.Endpoint.Builder builder = new io.enmasse.address.model.Endpoint.Builder()
                            .setName(endpoint.name)
                            .setService(endpoint.service)
                            .setHost(endpoint.host);
                    if (endpoint.certProvider != null) {
                        builder.setCertProvider(new CertProvider(endpoint.certProvider.name, endpoint.certProvider.secretName));
                    }
                    return builder.build();
                }).collect(Collectors.toList());
    }

    private static Plan findPlan(AddressSpaceType type, String planName) {
        for (Plan plan : type.getPlans()) {
            if (plan.getName().equals(planName)) {
                return plan;
            }
        }

        throw new RuntimeException("Unknown plan " + planName + " for type " + type.getName());
    }

}
