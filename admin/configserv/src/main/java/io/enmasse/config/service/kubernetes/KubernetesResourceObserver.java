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
import io.enmasse.config.service.model.ResourceFactory;
import io.enmasse.config.service.model.Subscriber;
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

    private final ObserverKey observerKey;
    private final Set<T> resourceSet = new LinkedHashSet<>();
    private final ResourceFactory<T> resourceFactory;
    private final SubscriptionManager<T> subscriptionManager;

    public KubernetesResourceObserver(ObserverKey observerKey, ResourceFactory<T> resourceFactory, ObserverOptions observerOptions, SubscriptionManager<T> subscriptionManager) {
        this.observerKey = observerKey;
        this.resourceFactory = resourceFactory;
        this.observerOptions = observerOptions;
        this.subscriptionManager = subscriptionManager;
    }

    @SuppressWarnings("unchecked")
    public void open() {
        Map<Operation<? extends HasMetadata, ?, ?, ?>, KubernetesResourceList>  initialResources = new LinkedHashMap<>();
        Map<String, String> labelFilter = new LinkedHashMap<>(observerKey.getLabelFilter());
        labelFilter.putAll(observerOptions.getObserverFilter());
        for (Operation<? extends HasMetadata, ?, ?, ?> operation : observerOptions.getOperations()) {
            KubernetesResourceList list = (KubernetesResourceList) operation.withLabels(labelFilter).list();
            initialResources.put(operation, list);
        }
        initializeResources(initialResources.values());
        for (Map.Entry<Operation<? extends HasMetadata, ?, ?, ?>, KubernetesResourceList> entry : initialResources.entrySet()) {
            watches.add(entry.getKey().withLabels(labelFilter).withResourceVersion(entry.getValue().getMetadata().getResourceVersion()).watch(this));
        }
    }

    private boolean annotationFilter(HasMetadata resource) {
        Map<String, String> annotationFilter = observerKey.getAnnotationFilter();
        Map<String, String> annotations = resource.getMetadata().getAnnotations();
        if (annotationFilter.isEmpty()) {
            return true;
        }

        if (annotations == null) {
            return false;
        }

        for (Map.Entry<String, String> filterEntry : annotationFilter.entrySet()) {
            String annotationValue = annotations.get(filterEntry.getKey());
            if (annotationValue == null || !annotationValue.equals(filterEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private synchronized void initializeResources(Collection<KubernetesResourceList> initialResources) {
        for (KubernetesResourceList list : initialResources) {
            for (Object item : list.getItems()) {
                if (item instanceof HasMetadata && annotationFilter((HasMetadata) item)) {
                    resourceSet.add(resourceFactory.createResource((HasMetadata) item));
                }
            }
        }
        subscriptionManager.resourcesUpdated(resourceSet);
    }

    @Override
    public void close() {
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
            throw new IllegalArgumentException("Invalid resource addressspace: " + obj.getClass().getName());
        }

        if (!annotationFilter((HasMetadata) obj)) {
            return;
        }

        T resource = resourceFactory.createResource((HasMetadata) obj);
        if (action.equals(Action.ADDED)) {
            deleteFromSet(resource);
            resourceSet.add(resource);
            log.debug("Resource " + resource + " added!");
        } else if (action.equals(Action.DELETED)) {
            deleteFromSet(resource);
            resourceSet.remove(resource);
            log.debug("Resource " + resource + " deleted!");
        } else if (action.equals(Action.MODIFIED)) {
            deleteFromSet(resource);
            resourceSet.add(resource);
            log.debug("Resource " + resource + " updated!");
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
            log.info("Exception from watcher: ", cause);
            close();
            log.info("Watch for " + observerOptions.getObserverFilter() + " + closed, restarting");
            open();
        } else {
            log.info("Watch for " + observerOptions.getObserverFilter() + " force closed, stopping");
        }
    }
}
