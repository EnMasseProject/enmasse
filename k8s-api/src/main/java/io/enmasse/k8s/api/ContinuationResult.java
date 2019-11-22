/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;

public class ContinuationResult<T> implements Iterable<T> {
    private Collection<T> items;
    private String continuation;

    public static <T> ContinuationResult<T> from(final Collection<T> items, final String continuation) {
        Objects.requireNonNull(items);
        return new ContinuationResult<>(items, continuation);
    }

    public static <T extends HasMetadata> ContinuationResult<T> from(final KubernetesResourceList<T> list) {
        Objects.requireNonNull(list);
        return new ContinuationResult<>(list.getItems(), list.getMetadata().getContinue());
    }

    private ContinuationResult(final Collection<T> items, final String continuation) {
        this.items = items;
        this.continuation = continuation;
    }

    public String getContinuation() {
        return this.continuation;
    }

    public Collection<T> getItems() {
        return this.items;
    }

    public boolean canContinue() {
        return this.continuation != null && !this.continuation.isBlank();
    }

    @Override
    public Iterator<T> iterator() {
        return this.items.iterator();
    }

}
