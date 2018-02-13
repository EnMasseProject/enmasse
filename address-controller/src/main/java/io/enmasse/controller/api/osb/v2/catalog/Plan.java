/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.catalog;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonSerialize(using = Plan.Serializer.class)
public class Plan {
    private static final ObjectMapper mapper = new ObjectMapper();

    private UUID uuid;
    private String name;
    private String description;
    private Map<String, String> metadata = new HashMap<>();
    private boolean free;
    private boolean bindable;
    private Schemas schemas;

    public Plan() {
    }

    public Plan(UUID uuid, String name, String description, boolean free, boolean bindable) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.free = free;
        this.bindable = bindable;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public boolean isBindable() {
        return bindable;
    }

    public void setBindable(boolean bindable) {
        this.bindable = bindable;
    }

    public Schemas getSchemas() {
        return schemas;
    }

    public void setSchemas(Schemas schemas) {
        this.schemas = schemas;
    }

    protected static class Serializer extends JsonSerializer<Plan> {
        @Override
        public void serialize(Plan value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", value.getUuid().toString());
            node.put("name", value.getName());
            node.put("description", value.getDescription());
            node.put("free", value.isFree());
            node.put("bindable", value.isBindable());

            ObjectNode metadataNode = node.putObject("metadata");
            value.getMetadata().forEach(metadataNode::put);

            node.set("schemas", mapper.valueToTree(value.getSchemas()));

            mapper.writeValue(gen, node);
        }
    }
}
