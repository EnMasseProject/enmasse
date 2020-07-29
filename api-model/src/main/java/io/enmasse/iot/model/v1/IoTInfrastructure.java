/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import io.enmasse.common.model.AbstractHasMetadata;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(
                        type = Doneable.class,
                        prefix = "Doneable",
                        value = "done"))
@DefaultCustomResource
@SuppressWarnings("serial")
public class IoTInfrastructure extends AbstractHasMetadata<IoTInfrastructure> {

    public static final String KIND = "IoTInfrastructure";

    private IoTInfrastructureSpec spec;
    private IoTInfrastructureStatus status;

    public IoTInfrastructure() {
        super(KIND, IoTCrd.API_VERSION);
    }

    public IoTInfrastructureSpec getSpec() {
        return spec;
    }

    public void setSpec(IoTInfrastructureSpec spec) {
        this.spec = spec;
    }

    public IoTInfrastructureStatus getStatus() {
        return status;
    }

    public void setStatus(IoTInfrastructureStatus status) {
        this.status = status;
    }

}
