/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {
                @BuildableReference(AbstractHasMetadataWithAdditionalProperties.class)
        },
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@DefaultCustomResource
@SuppressWarnings("serial")
public class BrokeredInfraConfig extends AbstractHasMetadataWithAdditionalProperties<BrokeredInfraConfig> implements InfraConfig {

    public static final String KIND = "BrokeredInfraConfig";

    private BrokeredInfraConfigSpec spec;

    public BrokeredInfraConfig() {
        super(KIND, AdminCrd.API_VERSION_V1BETA1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokeredInfraConfig that = (BrokeredInfraConfig) o;
        return Objects.equals(getMetadata(), that.getMetadata()) &&
                Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), spec);
    }

    @Override
    public String toString() {
        return "BrokeredInfraConfig{" +
                "metadata=" + getMetadata() +
                ", spec=" + spec + "}";
    }

    public void setSpec(BrokeredInfraConfigSpec spec) {
        this.spec = spec;
    }

    public BrokeredInfraConfigSpec getSpec() {
        return spec;
    }

    @Override
    @JsonIgnore
    public String getVersion() {
        return spec.getVersion();
    }

    @Override
    @JsonIgnore
    public NetworkPolicy getNetworkPolicy() {
        return spec.getNetworkPolicy();
    }

    @Override
    @JsonIgnore
    public boolean getUpdatePersistentVolumeClaim() {
        return spec.getBroker().getUpdatePersistentVolumeClaim() != null ? spec.getBroker().getUpdatePersistentVolumeClaim() : false;
    }

}
