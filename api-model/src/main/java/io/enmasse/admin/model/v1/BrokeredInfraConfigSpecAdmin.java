/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"resources"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokeredInfraConfigSpecAdmin extends AbstractWithAdditionalProperties {
    private BrokeredInfraConfigSpecAdminResources resources;

    public BrokeredInfraConfigSpecAdmin() {
    }

    public BrokeredInfraConfigSpecAdmin(final BrokeredInfraConfigSpecAdminResources resources) {
        setResources(resources);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokeredInfraConfigSpecAdmin that = (BrokeredInfraConfigSpecAdmin) o;
        return Objects.equals(resources, that.resources);
    }

    @Override
    public String toString() {
        return "BrokeredInfraConfigSpecAdmin{" +
                "resources=" + resources +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources);
    }

    public void setResources(BrokeredInfraConfigSpecAdminResources resources) {
        this.resources = resources;
    }

    public BrokeredInfraConfigSpecAdminResources getResources() {
        return resources;
    }

}
