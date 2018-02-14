/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.ResourceDatabase;
import io.enmasse.config.service.model.Subscriber;
import io.enmasse.k8s.api.ResourceController;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ResourceDatabase backed by OpenShift/Kubernetes REST API supporting subscription for a resource of a particular type
 */
public class KubernetesResourceDatabase<T> implements AutoCloseable, ResourceDatabase {
    private static final Logger log = LoggerFactory.getLogger(KubernetesResourceDatabase.class.getName());
    private final KubernetesClient client;

    private final Map<ObserverKey, DatabaseEntry<T>> entryMap = new LinkedHashMap<>();

    private final SubscriptionConfig<T> subscriptionConfig;

    public KubernetesResourceDatabase(KubernetesClient client, SubscriptionConfig<T> subscriptionConfig) {
        this.client = client;
        this.subscriptionConfig = subscriptionConfig;
    }

    @Override
    public synchronized void close() throws Exception {
        for (DatabaseEntry<T> entry : entryMap.values()) {
            entry.getController().stop();
        }
    }

    public synchronized void subscribe(ObserverKey observerKey, Subscriber subscriber) throws Exception {
        DatabaseEntry<T> entry = entryMap.get(observerKey);
        if (entry == null) {
            log.info("Creating new observer with filter " + observerKey);
            SubscriptionManager<T> subscriptionManager = new SubscriptionManager<>(observerKey, subscriptionConfig.getMessageEncoder(), subscriptionConfig.getResourceFilter());
            ResourceController<T> controller = ResourceController.create(subscriptionConfig.getResource(observerKey, client), subscriptionManager);
            entry = new DatabaseEntry<>(controller, subscriptionManager);
            entryMap.put(observerKey, entry);

            subscriptionManager.subscribe(subscriber);
            controller.start();
        } else {
            log.info("Subscribed to existing observer with filter " + observerKey);
            entry.getSubscriptionManager().subscribe(subscriber);
        }
    }

    private static class DatabaseEntry<T> {
        private final ResourceController<T> controller;
        private final SubscriptionManager<T> subscriptionManager;

        private DatabaseEntry(ResourceController<T> controller, SubscriptionManager<T> subscriptionManager) {
            this.controller = controller;
            this.subscriptionManager = subscriptionManager;
        }

        public ResourceController<T> getController() {
            return controller;
        }

        public SubscriptionManager<T> getSubscriptionManager() {
            return subscriptionManager;
        }
    }
}
