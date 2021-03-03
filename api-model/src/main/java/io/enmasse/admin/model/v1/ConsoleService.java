/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.CustomResourceWithAdditionalProperties;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

import java.util.Objects;

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
public class ConsoleService extends CustomResourceWithAdditionalProperties<ConsoleServiceSpec, ConsoleServiceStatus> implements WithAdditionalProperties, Namespaced {

    public static final String KIND = "ConsoleService";

    // for builders - probably will be fixed by https://github.com/fabric8io/kubernetes-client/pull/1346
    private ObjectMeta metadata;
    private ConsoleServiceSpec spec;
    private ConsoleServiceStatus status;

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
        ConsoleService that = (ConsoleService) o;
        return Objects.equals(getMetadata(), that.getMetadata()) &&
                Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), spec);
    }

    @Override
    public String toString() {
        return "ConsoleService{" +
                "metadata=" + getMetadata() +
                ", status='" + status + '\'' +
                ", spec=" + spec + "}";
    }

    public void setSpec(ConsoleServiceSpec spec) {
        this.spec = spec;
    }

    public ConsoleServiceSpec getSpec() {
        return spec;
    }

    public ConsoleServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ConsoleServiceStatus status) {
        this.status = status;
    }
}
