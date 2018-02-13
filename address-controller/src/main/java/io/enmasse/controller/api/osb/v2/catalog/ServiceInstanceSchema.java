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

@JsonSerialize(using = ServiceInstanceSchema.Serializer.class)
public class ServiceInstanceSchema {
    private static final ObjectMapper mapper = new ObjectMapper();

    private InputParameters createParameters;
    private InputParameters updateParameters;

    public ServiceInstanceSchema() {
    }

    public ServiceInstanceSchema(InputParameters createParameters, InputParameters updateParameters) {
        this.createParameters = createParameters;
        this.updateParameters = updateParameters;
    }

    public InputParameters getCreateParameters() {
        return createParameters;
    }

    public InputParameters getUpdateParameters() {
        return updateParameters;
    }

    protected static class Serializer extends JsonSerializer<ServiceInstanceSchema> {
        @Override
        public void serialize(ServiceInstanceSchema value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            node.set("create", mapper.valueToTree(value.getCreateParameters()));
            node.set("update", mapper.valueToTree(value.getUpdateParameters()));
            mapper.writeValue(gen, node);
        }
    }
}
