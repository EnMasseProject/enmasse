/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.enmasse.common.model.AbstractHasMetadata;
import io.enmasse.common.model.DefaultCustomResource;
import io.enmasse.model.validation.AddressName;
import io.enmasse.model.validation.HasMetadata;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

/**
 * An EnMasse Address.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(ObjectMeta.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@DefaultCustomResource
@SuppressWarnings("serial")
@AddressName
@HasMetadata
public class Address extends AbstractHasMetadata<Address> {

    public static final String KIND = "Address";

    @NotNull @Valid
    private AddressSpec spec = new AddressSpec();
    @NotNull @Valid
    private Status status = new Status();

    public Address() {
        super(KIND, CoreCrd.API_VERSION);
        this.getMetadata().setAnnotations(new HashMap<>());
        this.getMetadata().setLabels(new HashMap<>());
    }

    public void setSpec(AddressSpec spec) {
        this.spec = spec;
    }

    public AddressSpec getSpec() {
        return spec;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("metadata=").append(getMetadata()).append(",");
        sb.append("spec=").append(spec).append(",");
        sb.append("status=").append(status);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address other = (Address) o;

        return
                Objects.equals(getMetadata().getNamespace(), other.getMetadata().getNamespace()) &&
                Objects.equals(spec, other.spec)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata().getName(), spec );
    }

    public static String generateName(String addressSpace, String address) {
        String uuid = UUID.nameUUIDFromBytes(address.getBytes(StandardCharsets.UTF_8)).toString();
        return KubeUtil.sanitizeName(addressSpace) + "." + KubeUtil.sanitizeName(address) + "." + uuid;
    }
}
