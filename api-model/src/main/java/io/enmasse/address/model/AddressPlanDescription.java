/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.sundr.builder.annotations.Buildable;

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
        builderPackage = "io.fabric8.kubernetes.api.builder"
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressPlanDescription extends AbstractWithAdditionalProperties {

    @NotNull
    private String name;
    private String description;
    private Map<String, Double> resources = new HashMap<>();

    public AddressPlanDescription() {
    }

    public AddressPlanDescription(final String name, final String description, Map<String, Double> resources) {
        this.name = name;
        this.description = description;
        this.resources = new HashMap<>(resources);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        AddressPlanDescription that = (AddressPlanDescription) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, resources);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("name=").append(this.name);
        sb.append(",");
        sb.append("description=").append(this.description);
        sb.append("resources=").append(this.resources);
        return sb.append("}").toString();
    }

    public Map<String, Double> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    public void setResources(Map<String, Double> resources) {
        this.resources = new HashMap<>(resources);
    }
}
