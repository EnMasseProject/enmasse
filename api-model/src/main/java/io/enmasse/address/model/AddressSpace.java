/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.enmasse.admin.model.v1.WithAdditionalProperties;
import io.enmasse.common.model.CustomResourceWithAdditionalProperties;
import io.enmasse.common.model.DefaultCustomResource;
import io.enmasse.model.validation.AddressSpaceName;
import io.enmasse.model.validation.KubeMetadataName;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

/**
 * An EnMasse AddressSpace.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = {@BuildableReference(ObjectMeta.class)}
        )
@DefaultCustomResource
@SuppressWarnings("serial")
@AddressSpaceName
@KubeMetadataName
@JsonIgnoreProperties(ignoreUnknown = true)
@Version(CoreCrd.VERSION)
@Group(CoreCrd.GROUP)
public class AddressSpace extends CustomResourceWithAdditionalProperties<AddressSpaceSpec,  AddressSpaceStatus> implements WithAdditionalProperties, Namespaced {

    public static final String KIND = "AddressSpace";

    @NotNull @Valid
    private AddressSpaceSpec spec = new AddressSpaceSpec();
    @Valid
    private AddressSpaceStatus status = new AddressSpaceStatus(false);

    public void setSpec(AddressSpaceSpec spec) {
        this.spec = spec;
    }

    public AddressSpaceSpec getSpec() {
        return spec;
    }

    public void setStatus(AddressSpaceStatus status) {
        this.status = status;
    }

    public AddressSpaceStatus getStatus() {
        return status;
    }
    private ObjectMeta metadata;

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressSpace that = (AddressSpace) o;

        return Objects.equals(getMetadata().getName(), that.getMetadata().getName())
                && Objects.equals(getMetadata().getNamespace(), that.getMetadata().getNamespace());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata().getName(), getMetadata().getNamespace());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb
                .append("metadata=").append(getMetadata()).append(",")
                .append("spec=").append(spec).append(",")
                .append("status=").append(status).append("}");
        return sb.toString();
    }
}
