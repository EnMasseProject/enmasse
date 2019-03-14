/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class DownstreamStrategy {

    private ExternalDownstreamStrategy externalStrategy;
    private ProvidedDownstreamStrategy providedStrategy;
    private ManagedDownstreamStrategy managedStrategy;

    public ExternalDownstreamStrategy getExternalStrategy() {
        return this.externalStrategy;
    }

    public void setExternalStrategy(final ExternalDownstreamStrategy externalStrategy) {
        this.externalStrategy = externalStrategy;
    }

    public ProvidedDownstreamStrategy getProvidedStrategy() {
		return providedStrategy;
	}

	public void setProvidedStrategy(ProvidedDownstreamStrategy providedStrategy) {
		this.providedStrategy = providedStrategy;
	}

	public ManagedDownstreamStrategy getManagedStrategy() {
        return managedStrategy;
    }

    public void setManagedStrategy(ManagedDownstreamStrategy managedStrategy) {
        this.managedStrategy = managedStrategy;
    }

}
