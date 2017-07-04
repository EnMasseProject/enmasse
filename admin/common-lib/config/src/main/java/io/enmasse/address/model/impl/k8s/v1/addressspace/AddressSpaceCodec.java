package io.enmasse.address.model.impl.k8s.v1.addressspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Kubernetes resource codec for {@link AddressSpace}
 */
public class AddressSpaceCodec {
    private static final ObjectMapper mapper = new ObjectMapper();

    io.enmasse.address.model.AddressSpace decodeAddressSpace(DecodeContext context, byte [] json) throws IOException {
        AddressSpace addressSpace = mapper.readValue(json, AddressSpace.class);

        return new io.enmasse.address.model.impl.AddressSpace.Builder()
                .setName(addressSpace.metadata.name)
                .setType(context.getAddressSpaceType(addressSpace.spec.type))
   //             .setPlan(context.getAddressSpacePlan(addressSpace.spec.plan))
                .build();
    }



    byte [] encodeAddressSpace(io.enmasse.address.model.AddressSpace addressSpace) throws JsonProcessingException {
        AddressSpace serialized = new AddressSpace();
        serialized.metadata = new Metadata();
        serialized.metadata.name = addressSpace.getName();

        serialized.spec = new Spec();
        serialized.spec.type = addressSpace.getType().getName();
        serialized.spec.plan = addressSpace.getPlan().getName();
        serialized.spec.endpoints = addressSpace.getEndpoints().stream()
                .map(endpoint -> {
                    Endpoint e = new Endpoint();
                    e.name = endpoint.getName();
                    e.service = endpoint.getService();
                    e.host = endpoint.getHost();
                    e.certProvider = endpoint.getCertProvider().getName();
                    return e;
                }).collect(Collectors.toList());
        return mapper.writeValueAsBytes(serialized);
    }
}
