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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.CertProvider;
import io.enmasse.address.model.GlobalDecodeContext;
import io.enmasse.address.model.Status;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.Plan;

import java.io.IOException;

/**
 * Deserializer for AddressSpace V1 format
 */
class AddressSpaceV1Deserializer extends JsonDeserializer<AddressSpace> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DecodeContext context = new GlobalDecodeContext();

    @Override
    public AddressSpace deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        return deserialize(root);
    }

    static AddressSpace deserialize(ObjectNode root) {

        ObjectNode metadata = (ObjectNode) root.get(Fields.METADATA);
        ObjectNode spec = (ObjectNode) root.get(Fields.SPEC);

        AddressSpaceType type = context.getAddressSpaceType(spec.get(Fields.TYPE).asText());

        AddressSpace.Builder builder = new AddressSpace.Builder()
                .setName(metadata.get(Fields.NAME).asText())
                .setType(type);

        if (spec.hasNonNull(Fields.PLAN)) {
            builder.setPlan(findPlan(type, spec.get(Fields.PLAN).asText()));
        }

        if (metadata.hasNonNull(Fields.NAMESPACE)) {
            builder.setNamespace(metadata.get(Fields.NAMESPACE).asText());
        }

        if (spec.hasNonNull(Fields.ENDPOINTS)) {
            ArrayNode endpoints = (ArrayNode) spec.get(Fields.ENDPOINTS);
            for (int i = 0; i < endpoints.size(); i++) {
                ObjectNode endpoint = (ObjectNode) endpoints.get(i);
                io.enmasse.address.model.Endpoint.Builder b = new io.enmasse.address.model.Endpoint.Builder()
                        .setName(endpoint.get(Fields.NAME).asText())
                        .setService(endpoint.get(Fields.NAME).asText());

                if (endpoint.hasNonNull(Fields.HOST)) {
                    b.setHost(endpoint.get(Fields.HOST).asText());
                }

                if (endpoint.hasNonNull(Fields.CERT_PROVIDER)) {
                    ObjectNode certProvider = (ObjectNode) endpoint.get(Fields.CERT_PROVIDER);
                    b.setCertProvider(new CertProvider(
                            certProvider.get(Fields.NAME).asText(),
                            certProvider.get(Fields.SECRET_NAME).asText()));
                }
                builder.appendEndpoint(b.build());
            }
        }

        ObjectNode status = (ObjectNode) root.get(Fields.STATUS);
        if (status != null) {
            boolean isReady = status.get(Fields.IS_READY).asBoolean();
            Status s = new Status(isReady);
            if (status.hasNonNull(Fields.MESSAGES)) {
                ArrayNode messages = (ArrayNode) status.get(Fields.MESSAGES);
                for (int i = 0; i < messages.size(); i++) {
                    s.appendMessage(messages.get(i).asText());
                }
            }
            builder.setStatus(s);
        }
        return builder.build();
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
