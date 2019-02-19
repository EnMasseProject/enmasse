/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
//FIXME: implement missing fields and remove ignore annotation
@JsonIgnoreProperties(ignoreUnknown = true)
public class IoTProject extends AbstractHasMetadata<IoTProject> {

    public static final String KIND = "IoTProject";

    private IoTProjectSpec spec;

    public IoTProject() {
        super(KIND, Version.API_VERSION);
    }

    public void setSpec(final IoTProjectSpec spec) {
        this.spec = spec;
    }

    public IoTProjectSpec getSpec() {
        return this.spec;
    }

}
