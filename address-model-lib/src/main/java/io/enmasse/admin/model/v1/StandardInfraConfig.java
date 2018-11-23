/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfig implements InfraConfig {
    public static final String STANDARD_INFRA_CONFIG = "StandardInfraConfig";
    private String apiVersion = "admin.enmasse.io/v1alpha1";
    private String kind = STANDARD_INFRA_CONFIG;

    private ObjectMeta metadata;
    private final StandardInfraConfigSpec spec;

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public StandardInfraConfig(@JsonProperty("metadata") ObjectMeta metadata,
                               @JsonProperty("spec") StandardInfraConfigSpec spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public ObjectMeta getMetadata() {
        return metadata;
    }

    public StandardInfraConfigSpec getSpec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfig that = (StandardInfraConfig) o;
        return Objects.equals(metadata, that.metadata) &&
                Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, spec);
    }

    @Override
    public String toString() {
        return "StandardInfraConfig{" +
                "metadata=" + metadata +
                ", spec=" + spec + "}";
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
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

    @Override
    @JsonIgnore
    public String getVersion() {
        return spec.getVersion();
    }

    @Override
    @JsonIgnore
    public NetworkPolicy getNetworkPolicy() {
        return spec.getNetworkPolicy();
    }
}
