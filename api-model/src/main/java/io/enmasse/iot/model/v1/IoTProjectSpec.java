/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IoTProjectSpec {

    private DownstreamStrategy downstreamStrategy;

    private ObjectNode configuration;

    public DownstreamStrategy getDownstreamStrategy() {
        return this.downstreamStrategy;
    }

    public void setDownstreamStrategy(final DownstreamStrategy downstreamStrategy) {
        this.downstreamStrategy = downstreamStrategy;
    }

    public ObjectNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final ObjectNode configuration) {
        this.configuration = configuration;
    }

}
