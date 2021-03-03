/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.sundr.builder.annotations.Buildable;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({"name", "credit"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceRequest extends AbstractWithAdditionalProperties {

    private String name;
    private double credit;

    public ResourceRequest() {
    }

    public ResourceRequest(final String name, final double credit) {
        setName(name);
        setCredit(credit);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    public double getCredit() {
        return credit;
    }

}
