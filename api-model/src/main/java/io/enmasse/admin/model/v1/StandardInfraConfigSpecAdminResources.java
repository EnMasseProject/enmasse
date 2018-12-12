/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.*;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"memory"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpecAdminResources {
    private final String memory;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public StandardInfraConfigSpecAdminResources(@JsonProperty("memory") String memory) {
        this.memory = memory;
    }

    public String getMemory() {
        return memory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpecAdminResources that = (StandardInfraConfigSpecAdminResources) o;
        return Objects.equals(memory, that.memory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memory);
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
