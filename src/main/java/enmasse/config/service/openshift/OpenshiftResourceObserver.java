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

package enmasse.config.service.openshift;

import enmasse.config.service.model.LabelSet;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A subscription to a set of resources;
 */
public class OpenshiftResourceObserver implements AutoCloseable, Watcher {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftResourceObserver.class.getName());
    private final LabelSet labelSet;
    private final OpenShiftClient client;
    private final OpenshiftResourceListener listener;
    private final Set<HasMetadata> resourceSet = new LinkedHashSet<>();
    private final List<Watch> watches = new ArrayList<>();

    public OpenshiftResourceObserver(OpenShiftClient client, LabelSet labelSet, OpenshiftResourceListener listener) {
        this.client = client;
        this.labelSet = labelSet;
        this.listener = listener;
    }

    public void start() {
        Map<ClientOperation<? extends HasMetadata, ?, ?, ?>, KubernetesResourceList>  initialResources = new LinkedHashMap<>();
        for (ClientOperation<? extends HasMetadata, ?, ?, ?> operation : listener.getOperations(client)) {
            KubernetesResourceList list = (KubernetesResourceList) operation.withLabels(labelSet.getLabelMap()).list();
            initialResources.put(operation, list);
        }
        initializeResources(initialResources.values());
        for (Map.Entry<ClientOperation<? extends HasMetadata, ?, ?, ?>, KubernetesResourceList> entry : initialResources.entrySet()) {
            watches.add(entry.getKey().withLabels(labelSet.getLabelMap()).withResourceVersion(entry.getValue().getMetadata().getResourceVersion()).watch(this));
        }
    }

    private synchronized void initializeResources(Collection<KubernetesResourceList> initialResources) {
        for (KubernetesResourceList list : initialResources) {
            for (Object item : list.getItems()) {
                if (item instanceof HasMetadata) {
                    resourceSet.add((HasMetadata) item);
                }
            }
        }
        listener.resourcesUpdated(resourceSet);
    }

    @Override
    public void close() throws Exception {
        for (Watch watch : watches) {
            watch.close();
        }
        watches.clear();
    }

    @Override
    public synchronized void eventReceived(Action action, Object obj) {
        if (!(obj instanceof HasMetadata)) {
            throw new IllegalArgumentException("Invalid resource instance: " + obj.getClass().getName());
        }
        HasMetadata resource = (HasMetadata) obj;
        if (action.equals(Action.ADDED)) {
            resourceSet.add(resource);
            log.info("Resource " + resource.getMetadata().getName() + " added!");
        } else if (action.equals(Action.DELETED)) {
            resourceSet.remove(resource);
            log.info("Resource " + resource.getMetadata().getName() + " deleted!");
        } else if (action.equals(Action.MODIFIED)) {
            resourceSet.add(resource);
            log.info("Resource " + resource.getMetadata().getName() + " updated!");
        } else if (action.equals(Action.ERROR)) {
            log.error("Received an error event for resource " + resource.getMetadata().getName());
        }
        listener.resourcesUpdated(resourceSet);
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.error("Exception from watcher: ", cause);
        }
    }
}
