/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.sundr.builder.annotations.Buildable;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({"name", "max"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceAllowance extends AbstractWithAdditionalProperties {

    private String name;
    private double max;

    public ResourceAllowance() {
    }

    public ResourceAllowance(final String name, final double max) {
        setName(name);
        setMax(max);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMax() {
        return max;
    }

}
