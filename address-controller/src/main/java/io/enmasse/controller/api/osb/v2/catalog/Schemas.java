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

@JsonSerialize(using = Schemas.Serializer.class)
public class Schemas {
    private static final ObjectMapper mapper = new ObjectMapper();

    private ServiceInstanceSchema serviceInstance;
    private ServiceBindingSchema serviceBinding;

    public Schemas() {
    }

    public Schemas(ServiceInstanceSchema serviceInstance, ServiceBindingSchema serviceBinding) {
        this.serviceInstance = serviceInstance;
        this.serviceBinding = serviceBinding;
    }

    public ServiceInstanceSchema getServiceInstance() {
        return serviceInstance;
    }

    public ServiceBindingSchema getServiceBinding() {
        return serviceBinding;
    }

    protected static class Serializer extends JsonSerializer<Schemas> {
        @Override
        public void serialize(Schemas value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            node.set("service_instance", mapper.valueToTree(value.getServiceInstance()));
            node.set("service_binding", mapper.valueToTree(value.getServiceBinding()));
            mapper.writeValue(gen, node);
        }
    }
}
