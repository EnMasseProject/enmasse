/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;

import io.enmasse.k8s.api.cache.Controller;
import io.enmasse.k8s.api.cache.EventCache;
import io.enmasse.k8s.api.cache.HasMetadataFieldExtractor;
import io.enmasse.k8s.api.cache.ListOptions;
import io.enmasse.k8s.api.cache.ListerWatcher;
import io.enmasse.k8s.api.cache.Reflector;
import io.enmasse.k8s.api.cache.WorkQueue;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;

public class KubeCrdApi<T extends HasMetadata, LT extends KubernetesResourceList<T>, DT extends Doneable<T>> implements CrdApi<T>, ListerWatcher<T, LT> {

    private final NamespacedKubernetesClient client;
    private final String namespace;
    private final Class<T> tClazz;
    private final Class<LT> ltClazz;
    private final Class<DT> dtClazz;
    private final CustomResourceDefinition customResourceDefinition;

    public KubeCrdApi(NamespacedKubernetesClient client, String namespace, CustomResourceDefinition customResourceDefinition,
                      Class<T> tClazz,
                      Class<LT> ltClazz,
                      Class<DT> dtClazz) {
        this.client = client;
        this.namespace = namespace;
        this.tClazz = tClazz;
        this.ltClazz = ltClazz;
        this.dtClazz = dtClazz;
        this.customResourceDefinition = customResourceDefinition;
    }

    @Override
    public LT list(ListOptions listOptions) {
        if (namespace != null) {
            return client.customResources(customResourceDefinition, tClazz, ltClazz, dtClazz).inNamespace(namespace).list();
        } else {
            return client.customResources(customResourceDefinition, tClazz, ltClazz, dtClazz).inAnyNamespace().list();
        }
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<T> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        if (namespace != null) {
            return client.withRequestConfig(requestConfig).call(c ->
                    c.customResources(customResourceDefinition, tClazz, ltClazz, dtClazz).inNamespace(namespace).withResourceVersion(listOptions.getResourceVersion()).watch(watcher));
        } else {
            return client.withRequestConfig(requestConfig).call(c ->
                    c.customResources(customResourceDefinition, tClazz, ltClazz, dtClazz).inAnyNamespace().withResourceVersion(listOptions.getResourceVersion()).watch(watcher));
        }
    }

    @Override
    public Watch watchResources(Watcher<T> watcher, Duration resyncInterval) {
        WorkQueue<T> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        Reflector.Config<T, LT> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(tClazz);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(queue);
        config.setProcessor(map -> {
            if (queue.hasSynced()) {
                watcher.onUpdate(new ArrayList<>(queue.list()));
            }
        });

        Reflector<T, LT> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }
}
