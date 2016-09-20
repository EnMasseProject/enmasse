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

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IConfigMap;
import com.openshift.restclient.model.IResource;
import enmasse.config.bridge.model.ConfigMapSet;
import enmasse.config.bridge.model.LabelSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A subscription to a set of config maps.
 */
public class ConfigMapSetSubscription implements IOpenShiftWatchListener, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ConfigMapSetSubscription.class.getName());
    private final ConfigMapSet set = new ConfigMapSet();
    private final LabelSet labelSet;
    private final String namespace;
    private final IClient restClient;
    private IWatcher watcher = null;

    public ConfigMapSetSubscription(IClient restClient, LabelSet labelSet, String namespace) {
        this.labelSet = labelSet;
        this.restClient = restClient;
        this.namespace = namespace;
    }

    public void start() {
        this.watcher = restClient.watch(namespace, this, ResourceKind.CONFIG_MAP);
    }

    @Override
    public void connected(List<IResource> resources) {
        log.info("Connected, got " + resources.size() + " resources");
        List<IResource> filtered = resources.stream()
                .filter(res -> LabelSet.fromMap(res.getLabels()).contains(labelSet))
                .collect(Collectors.toList());

        for (IResource resource : filtered) {
            IConfigMap configMap = (IConfigMap) resource;

            set.mapAdded(configMap.getName(), configMap.getData());
            log.info("Added config map '" + configMap.getName() + "'");
        }
    }

    @Override
    public void disconnected() {
        log.info("Disconnected, restarting watch");
        reconnect();
    }

    private void reconnect() {
        watcher.stop();
        try {
            this.watcher = restClient.watch(namespace, this, ResourceKind.CONFIG_MAP);
        } catch (Exception e) {
            log.error("Error re-watching on disconnect from " + restClient.getBaseURL(), e);
        }
    }

    @Override
    public void received(IResource resource, ChangeType change) {
        IConfigMap configMap = (IConfigMap) resource;
        if (LabelSet.fromMap(configMap.getLabels()).contains(labelSet)) {
            if (change.equals(ChangeType.DELETED)) {
                // TODO: Notify subscribers?
                set.mapDeleted(configMap.getName());
                log.info("ConfigMap " + configMap.getName() + " deleted!");
            } else if (change.equals(ChangeType.ADDED)) {
                set.mapAdded(configMap.getName(), configMap.getData());
                log.info("ConfigMap " + configMap.getName() + " added!");
            } else if (change.equals(ChangeType.MODIFIED)) {
                // TODO: Can we assume that it exists at this point?
                set.mapUpdated(configMap.getName(), configMap.getData());
                log.info("ConfigMap " + configMap.getName() + " updated!");
            }
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

    public ConfigMapSet getSet() {
        return set;
    }

}
