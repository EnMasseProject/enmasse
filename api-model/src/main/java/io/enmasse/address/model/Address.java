/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.enmasse.admin.model.v1.WithAdditionalProperties;
import io.enmasse.common.model.CustomResourceWithAdditionalProperties;
import io.enmasse.common.model.DefaultCustomResource;
import io.enmasse.model.validation.AddressName;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

/**
 * An EnMasse Address.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(ObjectMeta.class)}
        )
@DefaultCustomResource
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
@AddressName
@Version(CoreCrd.VERSION)
@Group(CoreCrd.GROUP)
public class Address extends CustomResourceWithAdditionalProperties<AddressSpec,  AddressStatus> implements WithAdditionalProperties, Namespaced {

    public static final String KIND = "Address";

    @NotNull @Valid
    private AddressSpec spec = new AddressSpec();
    @NotNull @Valid
    private AddressStatus status = new AddressStatus();
    private ObjectMeta metadata = new ObjectMeta();


    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }
    public void setSpec(AddressSpec spec) {
        this.spec = spec;
    }

    public AddressSpec getSpec() {
        return spec;
    }

    public void setStatus(AddressStatus status) {
        this.status = status;
    }

    public AddressStatus getStatus() {
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

    private static String [] extractAddressInformation(final Address address) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(address.getMetadata());
        Objects.requireNonNull(address.getMetadata().getName());

        final String name = address.getMetadata().getName();
        final String toks [] = name.split("\\.", 2);
        if ( toks.length != 2 ) {
            throw new IllegalArgumentException("Address space name in illegal format. Expected: <address-space>.<address>, was: " + name);
        }

        return toks;
    }

    public static String extractAddressSpace(final Address address) {
       return extractAddressInformation(address)[0];
    }

    public static String extractAddressFromName(final Address address) {
        return extractAddressInformation(address)[1];
    }

    public static String extractAddress(final Address address) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(address.getSpec());
        final String addressValue = address.getSpec().getAddress();
        if ( addressValue != null ) {
            return addressValue;
        }
        return extractAddressFromName(address);
    }

    @JsonIgnore
    public String getForwarderLinkName(AddressSpecForwarder forwarder) {
        return String.format("%s.%s.%s", getMetadata().getName(), forwarder.getName(), forwarder.getDirection().name());
    }

}
