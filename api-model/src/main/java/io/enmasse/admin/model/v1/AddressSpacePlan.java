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

import java.util.*;

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
public class AddressSpacePlan implements HasMetadata {
    public static final String ADDRESS_SPACE_PLAN = "AddressSpacePlan";
    private static final long serialVersionUID = 1L;

    @JsonProperty("apiVersion")
    private String apiVersion = "admin.enmasse.io/v1alpha1";

    @JsonProperty("kind")
    private String kind = ADDRESS_SPACE_PLAN;

    private ObjectMeta metadata;

    private final String shortDescription;
    private final String uuid;
    private final String addressSpaceType;
    private final List<ResourceAllowance> resources;
    private final List<String> addressPlans;

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public AddressSpacePlan(@JsonProperty("shortDescription") String shortDescription,
                            @JsonProperty("uuid") String uuid,
                            @JsonProperty("addressSpaceType") String addressSpaceType,
                            @JsonProperty("resources") List<ResourceAllowance> resources,
                            @JsonProperty("addressPlans") List<String> addressPlans) {
        this.shortDescription = shortDescription;
        this.uuid = uuid;
        this.addressSpaceType = addressSpaceType;
        this.resources = resources;
        this.addressPlans = addressPlans;
    }

    public List<ResourceAllowance> getResources() {
        return resources;
    }

    public List<String> getAddressPlans() {
        return addressPlans;
    }

    public String getShortDescription() {
        return shortDescription;
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
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getAddressSpaceType() {
        return addressSpaceType;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpacePlan that = (AddressSpacePlan) o;
        return Objects.equals(metadata, that.metadata) &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, uuid);
    }

    @Override
    public String toString() {
        return "AddressSpacePlan{" +
                "metadata='" + metadata+ '\'' +
                ", uuid='" + uuid + '\'' +
                ", resources=" + resources +
                ", addressPlans=" + addressPlans +
                '}';
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
