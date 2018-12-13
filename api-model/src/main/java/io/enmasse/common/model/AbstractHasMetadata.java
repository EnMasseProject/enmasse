/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.model;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
                editableEnabled = false,
                generateBuilderPackage = false,
                builderPackage = "io.fabric8.kubernetes.api.builder",
                inline = @Inline(
                                type = Doneable.class,
                                prefix = "Doneable",
                                value = "done"))
@SuppressWarnings("serial")
public abstract class AbstractHasMetadata<T> extends AbstractResource<T> implements HasMetadata {

    private ObjectMeta metadata;

    protected AbstractHasMetadata(final String kind, String apiVersion) {
        super(kind, apiVersion);
    }

    @Override
    public ObjectMeta getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(final ObjectMeta metadata) {
        this.metadata = metadata;
    }

}
