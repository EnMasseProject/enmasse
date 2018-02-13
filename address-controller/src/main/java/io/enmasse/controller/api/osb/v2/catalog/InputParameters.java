/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.catalog;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import java.io.IOException;

@JsonSerialize(using = InputParameters.Serializer.class)
public class InputParameters {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonSchema parameters;

    public InputParameters() {
    }

    public InputParameters(JsonSchema parameters) {
        this.parameters = parameters;
    }

    public JsonSchema getParameters() {
        return parameters;
    }

    protected static class Serializer extends JsonSerializer<InputParameters> {
        @Override
        public void serialize(InputParameters value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            node.set("parameters", mapper.valueToTree(value.getParameters()));
            mapper.writeValue(gen, node);
        }
    }
}
