/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ListMeta;

@SuppressWarnings("serial")
public abstract class AbstractList<T extends HasMetadata> extends AbstractResource<T>
                implements KubernetesResource, KubernetesResourceList<T> {

    @Valid
    private ListMeta metadata = new ListMeta();

    private List<@Valid T> items = new ArrayList<>();

    protected AbstractList(final String kind, final String apiVersion) {
        super(kind, apiVersion);
    }

    public void setItems(final Collection<? extends T> items) {
        this.items = new ArrayList<>(items);
    }

    public List<T> getItems() {
        return this.items;
    }

    public void setMetadata(final ListMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public ListMeta getMetadata() {
        return this.metadata;
    }

    @Override
    public String toString() {
        return "{metadata=" + this.metadata + "," +
                        "items=" + this.items + "}";
    }
}
