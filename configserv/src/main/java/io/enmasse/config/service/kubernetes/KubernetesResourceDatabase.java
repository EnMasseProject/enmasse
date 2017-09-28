/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.Resource;
import io.enmasse.config.service.model.ResourceDatabase;
import io.enmasse.config.service.model.Subscriber;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ResourceDatabase backed by OpenShift/Kubernetes REST API supporting subscription for a resource of a particular type
 */
public class KubernetesResourceDatabase<T extends Resource> implements AutoCloseable, ResourceDatabase {
    private static final Logger log = LoggerFactory.getLogger(KubernetesResourceDatabase.class.getName());
    private final KubernetesClient client;

    private final Map<ObserverKey, KubernetesResourceObserver<T>> observerMap = new LinkedHashMap<>();
    private final SubscriptionConfig<T> subscriptionConfig;

    public KubernetesResourceDatabase(KubernetesClient client, SubscriptionConfig<T> subscriptionConfig) {
        this.client = client;
        this.subscriptionConfig = subscriptionConfig;
    }

    @Override
    public synchronized void close() throws Exception {
        for (KubernetesResourceObserver<T> observer : observerMap.values()) {
            observer.close();
        }
    }

    public synchronized void subscribe(ObserverKey observerKey, Subscriber subscriber) throws Exception {
        KubernetesResourceObserver<T> observer = observerMap.get(observerKey);
        if (observer == null) {
            log.info("Creating new observer with filter " + observerKey);
            SubscriptionManager<T> subscriptionManager = new SubscriptionManager<>(subscriptionConfig.getMessageEncoder(), subscriptionConfig.getResourceFilter());
            observer = new KubernetesResourceObserver<>(observerKey, subscriptionConfig.getResourceFactory(), subscriptionConfig.getObserverOptions(client), subscriptionManager);
            observerMap.put(observerKey, observer);

            observer.subscribe(subscriber);
            observer.open();
        } else {
            log.info("Subscribed to existing observer with filter " + observerKey);
            observer.subscribe(subscriber);
        }
    }
}
