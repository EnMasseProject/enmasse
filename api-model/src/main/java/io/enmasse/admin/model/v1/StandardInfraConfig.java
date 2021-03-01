/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.enmasse.common.model.CustomResourceWithAdditionalProperties;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = {@BuildableReference(ObjectMeta.class)}
)
@DefaultCustomResource
@SuppressWarnings("serial")
@Version(AdminCrd.VERSION_V1BETA1)
@Group(AdminCrd.GROUP)
public class StandardInfraConfig extends CustomResourceWithAdditionalProperties<StandardInfraConfigSpec, StandardInfraConfigStatus> implements WithAdditionalProperties, InfraConfig, Namespaced {

    public static final String KIND = "StandardInfraConfig";

    // for builders - probably will be fixed by https://github.com/fabric8io/kubernetes-client/pull/1346
    private ObjectMeta metadata;
    private StandardInfraConfigSpec spec = new StandardInfraConfigSpec();

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public void setSpec(StandardInfraConfigSpec spec) {
        this.spec = spec;
    }

    public StandardInfraConfigSpec getSpec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfig that = (StandardInfraConfig) o;
        return Objects.equals(getMetadata(), that.getMetadata()) &&
                Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), spec);
    }

    @Override
    public String toString() {
        return "StandardInfraConfig{" +
                "metadata=" + getMetadata() +
                ", spec=" + spec + "}";
    }

    @Override
    @JsonIgnore
    public String getInfraConfigVersion() {
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
