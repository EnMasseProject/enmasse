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
package io.enmasse.address.model.impl.k8s.v1.address;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressType;
import io.enmasse.address.model.Plan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kubernetes resource codec for {@link Address}
 */
public class AddressCodec {
    private static final ObjectMapper mapper = new ObjectMapper();

    public io.enmasse.address.model.Address decodeAddress(DecodeContext context, byte [] json) throws IOException {
        Address address = mapper.readValue(json, io.enmasse.address.model.impl.k8s.v1.address.Address.class);
        return decode(context, address);
    }



    public Object decode(DecodeContext context, byte [] json) throws IOException {
        AddressResource resource = mapper.readValue(json, AddressResource.class);
        if (resource instanceof Address) {
            return decode(context, (Address) resource);
        } else if (resource instanceof AddressList) {
            return ((AddressList)resource).items.stream()
                    .map(a -> decode(context, a))
                    .collect(Collectors.toSet());
        }
        throw new RuntimeException("Unknown type " + resource.kind);
    }

    private static Plan findPlan(AddressType type, String planName) {
        for (Plan plan : type.getPlans()) {
            if (plan.getName().equals(planName)) {
                return plan;
            }
        }
        throw new RuntimeException("Unknown plan " + planName + " for type " + type.getName());
    }

    private static io.enmasse.address.model.Address decode(DecodeContext context, Address address) {
        AddressType type = context.getAddressType(address.spec.type);
        return new io.enmasse.address.model.impl.Address.Builder()
            .setName(address.metadata.name)
            .setAddress(address.spec.address)
            .setAddressSpace(address.metadata.addressSpace)
            .setType(type)
            .setUuid(address.metadata.uuid)
            .setPlan(findPlan(type, address.spec.plan))
            .build();
    }

    public byte [] encodeAddress(io.enmasse.address.model.Address address) throws JsonProcessingException {
        return mapper.writeValueAsBytes(encode(address));
    }

    private static Address encode(io.enmasse.address.model.Address address) {
        Address serialized = new Address();
        serialized.kind = "Address";
        serialized.metadata = new Metadata();
        serialized.metadata.name = address.getName();
        serialized.metadata.uuid = address.getUuid();
        serialized.metadata.addressSpace = address.getAddressSpace();

        serialized.spec = new Spec();
        serialized.spec.plan = address.getPlan().getName();
        serialized.spec.type = address.getType().getName();
        serialized.spec.address = address.getAddress();
        return serialized;
    }

    public byte[] encodeAddressList(Collection<io.enmasse.address.model.Address> addressList) throws JsonProcessingException {
        AddressList serialized = new AddressList();
        serialized.kind = "AddressList";
        for (io.enmasse.address.model.Address address : addressList) {
            serialized.items.add(encode(address));
        }
        return mapper.writeValueAsBytes(serialized);
    }

    // TODO: This is a more low-level generator to avoid re-encoding address payload into a list
    // Find a better way to fix this in the future
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
}
