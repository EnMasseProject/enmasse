/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.HashMap;
import java.util.List;
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
public class AddressPlan implements HasMetadata {

    public static final String ADDRESS_PLAN = "AddressPlan";
    @JsonProperty("apiVersion")
    private String apiVersion = "admin.enmasse.io/v1alpha1";

    @JsonProperty("kind")
    private String kind = ADDRESS_PLAN;

    private ObjectMeta metadata;

    private final String shortDescription;
    private final String uuid;
    private final String addressType;

    private final List<ResourceRequest> requiredResources;

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public AddressPlan(@JsonProperty("shortDescription") String shortDescription,
                       @JsonProperty("uuid") String uuid,
                       @JsonProperty("addressType") String addressType,
                       @JsonProperty("requiredResources") List<ResourceRequest> requiredResources) {
        this.shortDescription = shortDescription;
        this.uuid = uuid;
        this.addressType = addressType;
        this.requiredResources = requiredResources;
    }

    public List<ResourceRequest> getRequiredResources() {
        return requiredResources;
    }

    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getKind() {
        return kind;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(String s) {
        this.apiVersion = s;
    }

    public String getAddressType() {
        return addressType;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void validate() {
        Objects.requireNonNull(shortDescription);
        Objects.requireNonNull(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressPlan that = (AddressPlan) o;
        return Objects.equals(metadata, that.metadata) &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, uuid);
    }

    @Override
    public String toString() {
        return "AddressPlan{" +
                "metadata='" + metadata+ '\'' +
                ", uuid='" + uuid + '\'' +
                ", requiredResources=" + requiredResources +
                '}';
    }

    public String getUuid() {
        return uuid;
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
