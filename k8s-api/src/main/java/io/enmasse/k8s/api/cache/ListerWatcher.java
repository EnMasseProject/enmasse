/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;

public interface ListerWatcher<T extends HasMetadata, LT extends KubernetesResourceList<T>> {
    LT list(ListOptions listOptions);
    Watch watch(Watcher<T> watcher, ListOptions listOptions);
}
