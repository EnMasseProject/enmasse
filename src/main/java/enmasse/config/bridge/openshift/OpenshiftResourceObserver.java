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

package enmasse.config.bridge.openshift;

import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.model.IResource;
import enmasse.config.bridge.model.LabelSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A subscription to a set of resources;
 */
public class OpenshiftResourceObserver implements IOpenShiftWatchListener, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftResourceObserver.class.getName());
    private final LabelSet labelSet;
    private final OpenshiftClient client;
    private final OpenshiftResourceListener listener;
    private final Set<IResource> resourceSet = new LinkedHashSet<>();
    private IWatcher watcher = null;

    public OpenshiftResourceObserver(OpenshiftClient client, LabelSet labelSet, OpenshiftResourceListener listener) {
        this.client = client;
        this.labelSet = labelSet;
        this.listener = listener;
    }

    public void start() {
        this.watcher = client.watch(this, listener.getKinds());
    }

    @Override
    public void connected(List<IResource> resources) {
        log.info("Connected, got " + resources.size() + " resources");
        resourceSet.clear();
        resourceSet.addAll(resources.stream()
                .filter(res -> LabelSet.fromMap(res.getLabels()).contains(labelSet))
                .map(res -> { log.info("Added resource '" + res.getName() + "'"); return res; })
                .collect(Collectors.toSet()));

        listener.resourcesUpdated(resourceSet);
    }

    @Override
    public void disconnected() {
        log.info("Disconnected, restarting watch");
        reconnect();
    }

    private void reconnect() {
        watcher.stop();
        try {
            this.watcher = client.watch(this, listener.getKinds());
        } catch (Exception e) {
            log.error("Error re-watching on disconnect from " + client.getBaseURL(), e);
        }
    }

    @Override
    public void received(IResource resource, ChangeType change) {
        if (LabelSet.fromMap(resource.getLabels()).contains(labelSet)) {
            if (change.equals(ChangeType.DELETED)) {
                resourceSet.remove(resource);
                log.info("Resource " + resource.getName() + " deleted!");
            } else if (change.equals(ChangeType.ADDED)) {
                resourceSet.add(resource);
                log.info("Resource " + resource.getName() + " added!");
            } else if (change.equals(ChangeType.MODIFIED)) {
                // TODO: Can we assume that it exists at this point?
                resourceSet.add(resource);
                log.info("Resource " + resource.getName() + " updated!");
            }
            listener.resourcesUpdated(resourceSet);
        }
    }

    @Override
    public void close() throws Exception {
        if (this.watcher != null) {
            watcher.stop();
        }
    }

    @Override
    public void error(Throwable err) {
        log.error("Got error from watcher: " +  err.getMessage());
        reconnect();
    }
}
