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
import java.util.List;
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
@JsonPropertyOrder({"cells", "object"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableRow {
    private final List<Object> cells;
    private final PartialObjectMetadata object;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public TableRow(@JsonProperty("cells") List<Object> cells,
                    @JsonProperty("object") PartialObjectMetadata object) {
        this.cells = cells;
        this.object = object;
    }

    public List<Object> getCells() {
        return cells;
    }

    public PartialObjectMetadata getObject() {
        return object;
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
