/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A class containing a name and optional description.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpacePlanDescription extends AbstractWithAdditionalProperties {

    @NotNull
    private String name;
    private String displayName;
    private String description;
    private Map<String, Double> resourceLimits = new HashMap<>();

    public AddressSpacePlanDescription() {
    }

    public AddressSpacePlanDescription(final String name, final String displayName, final String description, Map<String, Double> resourceLimits) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.resourceLimits = resourceLimits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpacePlanDescription that = (AddressSpacePlanDescription) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(description, that.description) &&
                Objects.equals(resourceLimits, that.resourceLimits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, displayName, description, resourceLimits);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("name=").append(this.name);
        sb.append(", displayName=").append(this.displayName);
        sb.append(", description=").append(this.description);
        sb.append(", resourceLimits=").append(this.resourceLimits);
        return sb.append("}").toString();
    }

    public Map<String, Double> getResourceLimits() {
        return Collections.unmodifiableMap(resourceLimits);
    }

    public void setResourceLimits(Map<String, Double> resourceLimits) {
        this.resourceLimits = new HashMap<>(resourceLimits);
    }
}
