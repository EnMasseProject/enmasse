/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.model.v1beta1;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"description", "format", "name", "priority", "type"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableColumnDefinition {
    private final String description;
    private final String format;
    private final String name;
    private final int priority;
    private final String type;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public TableColumnDefinition(@JsonProperty("description") String description,
                                 @JsonProperty("format") String format,
                                 @JsonProperty("name") String name,
                                 @JsonProperty("priority") int priority,
                                 @JsonProperty("type") String type) {
        this.description = description;
        this.format = format;
        this.name = name;
        this.priority = priority;
        this.type = type;
    }

    public TableColumnDefinition(String name, String type) {
        this(name, "", name, 0, type);
    }

    public String getDescription() {
        return description;
    }

    public String getFormat() {
        return format;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public String getType() {
        return type;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
