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

package enmasse.config.service.kubernetes;

import enmasse.config.service.model.LabelSet;
import enmasse.config.service.model.Resource;
import enmasse.config.service.model.Subscriber;
import enmasse.config.service.model.ResourceDatabase;
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

    private final Map<LabelSet, KubernetesResourceObserver<T>> observerMap = new LinkedHashMap<>();
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

    public synchronized void subscribe(Map<String, String> filter, Subscriber subscriber) throws Exception {
        LabelSet key = LabelSet.fromMap(filter);
        KubernetesResourceObserver<T> observer = observerMap.get(key);
        if (observer == null) {
            log.debug("Creating new observer with filter " + filter);
            SubscriptionManager<T> subscriptionManager = new SubscriptionManager<>(subscriptionConfig.getMessageEncoder(), subscriptionConfig.getResourceFilter(filter));
            observer = new KubernetesResourceObserver<>(subscriptionConfig.getResourceFactory(), subscriptionConfig.getObserverOptions(client, filter), subscriptionManager);
            observerMap.put(key, observer);

            observer.subscribe(subscriber);
            observer.start();
        } else {
            log.debug("Subscribing to existing observer");
            observer.subscribe(subscriber);
        }
    }
}
