/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.enmasse.address.model.MessageRedelivery;
import io.enmasse.address.model.MessageTtl;
import io.enmasse.common.model.CustomResourceWithAdditionalProperties;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

import java.util.Map;
import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = {@BuildableReference(ObjectMeta.class)}
)
@DefaultCustomResource
@SuppressWarnings("serial")
@Version(AdminCrd.VERSION_V1BETA2)
@Group(AdminCrd.GROUP)

public class AddressPlan extends CustomResourceWithAdditionalProperties<AddressPlanSpec, AddressPlanStatus> implements WithAdditionalProperties, io.enmasse.admin.model.AddressPlan, Namespaced {

    public static final String KIND = "AddressPlan";

    private AddressPlanSpec spec;
    private AddressPlanStatus status;
    private ObjectMeta metadata; // for builder

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
        AddressPlan that = (AddressPlan) o;
        return Objects.equals(getMetadata(), that.getMetadata()) &&
                Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), spec);
    }

    @Override
    public String toString() {
        return "AddressPlan{" +
                "metadata='" + getMetadata() + '\'' +
                ", spec ='" + spec + '\'' +
                '}';
    }

    public void setSpec(AddressPlanSpec spec) {
        this.spec = spec;
    }

    public AddressPlanSpec getSpec() {
        return spec;
    }

    @Override
    @JsonIgnore
    public String getShortDescription() {
        return spec.getShortDescription();
    }

    @Override
    @JsonIgnore
    public String getAddressType() {
        return spec.getAddressType();
    }

    @Override
    @JsonIgnore
    public Map<String, Double> getResources() {
        return spec.getResources();
    }

    @Override
    @JsonIgnore
    public int getPartitions() {
        if (spec.getPartitions() != null) {
            return spec.getPartitions();
        } else {
            return 1;
        }
    }

    @Override
    @JsonIgnore
    public MessageTtl getTtl() {
        return spec == null ? null : spec.getMessageTtl();
    }

    @Override
    @JsonIgnore
    public MessageRedelivery getMessageRedelivery() {
        return spec == null ? null : spec.getMessageRedelivery();
    }

    public AddressPlanStatus getStatus() {
        return status;
    }

    public void setStatus(AddressPlanStatus status) {
        this.status = status;
    }
}
