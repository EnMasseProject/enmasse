/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.enmasse.common.model.AbstractHasMetadata;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpacePlan extends AbstractHasMetadata<AddressSpacePlan> {

    public static final String KIND = "AddressSpacePlan";

    private String shortDescription;
    private String uuid;
    private String addressSpaceType;
    private List<ResourceAllowance> resources = new LinkedList<>();
    private List<String> addressPlans = new LinkedList<>();

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public AddressSpacePlan() {
        super(KIND, AdminCrd.API_VERSION);
    }

    public void setResources(List<ResourceAllowance> resources) {
        this.resources = resources;
    }
    
    public List<ResourceAllowance> getResources() {
        return resources;
    }

    public void setAddressPlans(List<String> addressPlans) {
        this.addressPlans = addressPlans;
    }

    public List<String> getAddressPlans() {
        return addressPlans;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
    
    public String getShortDescription() {
        return shortDescription;
    }

    public void setAddressSpaceType(String addressSpaceType) {
        this.addressSpaceType = addressSpaceType;
    }
    
    public String getAddressSpaceType() {
        return addressSpaceType;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpacePlan that = (AddressSpacePlan) o;
        return Objects.equals(getMetadata(), that.getMetadata()) &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), uuid);
    }

    @Override
    public String toString() {
        return "AddressSpacePlan{" +
                "metadata='" + getMetadata()+ '\'' +
                ", uuid='" + uuid + '\'' +
                ", resources=" + resources +
                ", addressPlans=" + addressPlans +
                '}';
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
