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

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"name", "credit"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceRequest {
    private final String name;
    private final double credit;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public ResourceRequest(@JsonProperty("name") String name,
                           @JsonProperty("credit") double credit) {
        this.name = name;
        this.credit = credit;
    }

    public String getName() {
        return name;
    }

    public double getCredit() {
        return credit;
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
