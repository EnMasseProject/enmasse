/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.api.model;

public abstract class AbstractResource<T> {

    private final String kind;

    private String apiVersion;

    protected AbstractResource(final String kind, final String apiVersion) {
        this.kind = kind;
        this.apiVersion = apiVersion;
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
