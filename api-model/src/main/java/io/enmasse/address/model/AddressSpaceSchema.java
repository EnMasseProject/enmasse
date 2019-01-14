/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
        refs= {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpaceSchema extends AbstractHasMetadata<AddressSpaceSchema> {

    public static final String KIND = "AddressSpaceSchema";

    @NotNull @Valid
    private AddressSpaceType spec;

    public AddressSpaceSchema() {
        super(KIND, CoreCrd.API_VERSION);
    }

    public void setSpec(final AddressSpaceType spec) {
        this.spec = spec;
    }

    public AddressSpaceType getSpec() {
        return this.spec;
    }
}
