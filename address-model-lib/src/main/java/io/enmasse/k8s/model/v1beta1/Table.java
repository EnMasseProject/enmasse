/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.model.v1beta1;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.ListMeta;
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
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "columnDefinitions", "rows"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Table {
    private String apiVersion = "meta.k8s.io/v1beta1";
    private String kind = "Table";

    private ListMeta metadata;
    private final List<TableColumnDefinition> columnDefinitions;
    private final List<TableRow> rows;

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public Table(@JsonProperty("metadata") ListMeta metadata,
                 @JsonProperty("columnDefinitions") List<TableColumnDefinition> columnDefinitions,
                 @JsonProperty("rows") List<TableRow> rows) {
        this.metadata = metadata;
        this.columnDefinitions = columnDefinitions;
        this.rows = rows;
    }

    public ListMeta getMetadata() {
        return metadata;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setMetadata(ListMeta metadata) {
        this.metadata = metadata;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public List<TableColumnDefinition> getColumnDefinitions() {
        return columnDefinitions;
    }

    public List<TableRow> getRows() {
        return rows;
    }
}
