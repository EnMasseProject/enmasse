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

import enmasse.config.service.model.Resource;
import enmasse.config.service.model.ResourceFactory;
import enmasse.config.service.model.Subscriber;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A subscription to a set of resources;
 */
public class KubernetesResourceObserver<T extends Resource> implements AutoCloseable, Watcher {
    private static final Logger log = LoggerFactory.getLogger(KubernetesResourceObserver.class.getName());

    private final List<Watch> watches = new ArrayList<>();

    private final ObserverOptions observerOptions;

    private final Set<T> resourceSet = new LinkedHashSet<>();
    private final ResourceFactory<T> resourceFactory;
    private final SubscriptionManager<T> subscriptionManager;

    public KubernetesResourceObserver(ResourceFactory<T> resourceFactory, ObserverOptions observerOptions, SubscriptionManager<T> subscriptionManager) {
        this.resourceFactory = resourceFactory;
        this.observerOptions = observerOptions;
        this.subscriptionManager = subscriptionManager;
    }

    @SuppressWarnings("unchecked")
    public void start() {
        Map<Operation<? extends HasMetadata, ?, ?, ?>, KubernetesResourceList>  initialResources = new LinkedHashMap<>();
        for (Operation<? extends HasMetadata, ?, ?, ?> operation : observerOptions.getOperations()) {
            KubernetesResourceList list = (KubernetesResourceList) operation.withLabels(observerOptions.getLabelMap()).list();
            initialResources.put(operation, list);
        }
        initializeResources(initialResources.values());
        for (Map.Entry<Operation<? extends HasMetadata, ?, ?, ?>, KubernetesResourceList> entry : initialResources.entrySet()) {
            watches.add(entry.getKey().withLabels(observerOptions.getLabelMap()).withResourceVersion(entry.getValue().getMetadata().getResourceVersion()).watch(this));
        }
    }

    private synchronized void initializeResources(Collection<KubernetesResourceList> initialResources) {
        for (KubernetesResourceList list : initialResources) {
            for (Object item : list.getItems()) {
                if (item instanceof HasMetadata) {
                    resourceSet.add(resourceFactory.createResource((HasMetadata) item));
                }
            }
        }
        subscriptionManager.resourcesUpdated(resourceSet);
    }

    @Override
    public void close() throws Exception {
        for (Watch watch : watches) {
            watch.close();
        }
        watches.clear();
    }

    public void subscribe(Subscriber subscriber) {
        subscriptionManager.subscribe(subscriber);
    }

    @Override
    public synchronized void eventReceived(Action action, Object obj) {
        if (!(obj instanceof HasMetadata)) {
            throw new IllegalArgumentException("Invalid resource instance: " + obj.getClass().getName());
        }
        T resource = resourceFactory.createResource((HasMetadata) obj);
        if (action.equals(Action.ADDED)) {
            resourceSet.add(resource);
            log.info("Resource " + resource + " added!");
        } else if (action.equals(Action.DELETED)) {
            deleteFromSet(resource);
            resourceSet.remove(resource);
            log.info("Resource " + resource + " deleted!");
        } else if (action.equals(Action.MODIFIED)) {
            deleteFromSet(resource);
            resourceSet.add(resource);
            log.info("Resource " + resource + " updated!");
        } else if (action.equals(Action.ERROR)) {
            log.error("Received an error event for resource " + resource);
        }
        subscriptionManager.resourcesUpdated(resourceSet);
    }

    private void deleteFromSet(Resource resource) {
        resourceSet.removeIf(next -> next.getName().equals(resource.getName()) && next.getKind().equals(resource.getKind()));
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.error("Exception from watcher: ", cause);
        }
    }
}
