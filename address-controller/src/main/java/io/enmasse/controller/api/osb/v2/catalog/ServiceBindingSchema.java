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

import java.io.IOException;

@JsonSerialize(using = ServiceBindingSchema.Serializer.class)
public class ServiceBindingSchema {
    private static final ObjectMapper mapper = new ObjectMapper();

    private InputParameters createParameters;

    public ServiceBindingSchema() {
    }

    public ServiceBindingSchema(InputParameters createParameters) {
        this.createParameters = createParameters;
    }

    public InputParameters getCreateParameters() {
        return createParameters;
    }

    protected static class Serializer extends JsonSerializer<ServiceBindingSchema> {
        @Override
        public void serialize(ServiceBindingSchema value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            node.set("create", mapper.valueToTree(value.getCreateParameters()));
            mapper.writeValue(gen, node);
        }
    }
}
