/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
                value = "done"
                )
        )
public abstract class AbstractHasMetadata<T> implements HasMetadata {

    private static final long serialVersionUID = 1L;

    private ObjectMeta metadata;

    private final String kind = CustomResources.getKind(this.getClass());
    private String apiVersion = this.getClass().getAnnotation(ApiVersion.class).value();

    @Override
    public ObjectMeta getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(final ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @JsonIgnore
    @Override
    public String getKind() {
        return this.kind;
    }

    @JsonIgnore
    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(final String version) {
        this.apiVersion = version;
    }

}
