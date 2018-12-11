/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.api.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ListMeta;

public abstract class AbstractList<T extends HasMetadata>
        extends ArrayList<T>
        implements KubernetesResource<T>, KubernetesResourceList<T>, Iterable<T> {

    private static final long serialVersionUID = 1L;

    private final String kind = CustomResources.getKind(this.getClass());
    private String apiVersion = this.getClass().getAnnotation(ApiVersion.class).value();

    private ListMeta metadata;

    public String getKind() {
        return this.kind;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(final String version) {
        this.apiVersion = version;
    }

    public void setItems(final Collection<? extends T> items) {
        super.clear();
        super.addAll(items);
    }

    public List<T> getItems() {
        return this;
    }

    public void setMetadata(final ListMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public ListMeta getMetadata() {
        return this.metadata;
    }

}
