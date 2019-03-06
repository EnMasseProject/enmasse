/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

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
public class ConsoleService extends AbstractHasMetadataWithAdditionalProperties<ConsoleService> {

    public static final String KIND = "ConsoleService";

    private ConsoleServiceSpec spec;
    private ConsoleServiceStatus status;

    public ConsoleService() {
        super(KIND, AdminCrd.API_VERSION_V1BETA1);
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
