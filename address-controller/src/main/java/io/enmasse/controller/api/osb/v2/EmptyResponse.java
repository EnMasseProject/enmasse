/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonSerialize(using = EmptyResponse.Serializer.class)
public class EmptyResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    public EmptyResponse() {
    }

    protected static class Serializer extends JsonSerializer<EmptyResponse> {
        @Override
        public void serialize(EmptyResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            mapper.writeValue(gen, node);
        }
    }
}
