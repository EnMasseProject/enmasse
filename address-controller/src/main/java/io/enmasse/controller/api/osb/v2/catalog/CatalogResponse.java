/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.catalog;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonSerialize(using = CatalogResponse.Serializer.class)
public class CatalogResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    private List<Service> services;

    public CatalogResponse(List<Service> services) {
        this.services = services;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    protected static class Serializer extends JsonSerializer<CatalogResponse> {
        @Override
        public void serialize(CatalogResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();

            ArrayNode services = node.putArray("services");

            value.getServices().stream().forEach(service -> {
                JsonNode jsonNode = mapper.valueToTree(service);
                services.add(jsonNode);
            });

            mapper.writeValue(gen, node);
        }
    }
}
