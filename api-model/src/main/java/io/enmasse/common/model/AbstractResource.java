/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.model;

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
                                value = "done"))
public abstract class AbstractResource<T> {

    private String kind;

    private String apiVersion;

    protected AbstractResource(final String kind, final String apiVersion) {
        this.kind = kind;
        this.apiVersion = apiVersion;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return this.kind;
    }

    public String getApiVersion() {
        return this.apiVersion;
    }

    public void setApiVersion(final String apiVersion) {
        this.apiVersion = apiVersion;
    }

}
