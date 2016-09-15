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
import enmasse.config.bridge.model.ConfigMap;
import enmasse.config.bridge.model.ConfigMapDatabase;
import enmasse.config.bridge.model.ConfigSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConfigMapDatabase backed by OpenShift/Kubernetes REST API supporting subscriptions.
 */
public class OpenshiftConfigMapDatabase implements IOpenShiftWatchListener, AutoCloseable, ConfigMapDatabase {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftConfigMapDatabase.class.getName());
    private final IClient restClient;
    private final String namespace;
    private IWatcher watcher = null;

    private final Map<String, ConfigMap> configMapMap = new ConcurrentHashMap<>();

    public OpenshiftConfigMapDatabase(IClient restClient, String namespace) {
        this.restClient = restClient;
        this.namespace = namespace;
    }

    public void start() {
        log.info("Starting to watch configs");
        this.watcher = restClient.watch(namespace, this, ResourceKind.CONFIG_MAP);
    }

    @Override
    public void connected(List<IResource> resources) {
        log.info("Connected, got " + resources.size() + " resources");
        for (IResource resource : resources) {
            log.info("Resource kind is " + resource.getKind());
            IConfigMap configMap = (IConfigMap) resource;

            ConfigMap map = getOrCreateConfigMap(configMap.getName());
            map.configUpdated(configMap.getResourceVersion(), configMap.getData());
            log.info("Added config map '" + configMap.getName() + "'");
        }
    }

    @Override
    public void disconnected() {
        log.info("Disconnected, restarting watch");
        try {
            this.watcher = restClient.watch(namespace, this, ResourceKind.CONFIG_MAP);
        } catch (Exception e) {
            log.error("Error re-watching on disconnect from " + restClient.getBaseURL(), e);
        }
    }

    @Override
    public void received(IResource resource, ChangeType change) {
        IConfigMap configMap = (IConfigMap) resource;
        if (change.equals(ChangeType.DELETED)) {
            // TODO: Notify subscribers?
            configMapMap.remove(configMap.getName());
            log.info("ConfigMap " + configMap.getName() + " deleted!");
        } else if (change.equals(ChangeType.ADDED)) {
            ConfigMap map = getOrCreateConfigMap(configMap.getName());
            map.configUpdated(configMap.getResourceVersion(), configMap.getData());
            log.info("ConfigMap " + configMap.getName() + " added!");
        } else if (change.equals(ChangeType.MODIFIED)) {
            // TODO: Can we assume that it exists at this point?
            configMapMap.get(configMap.getName()).configUpdated(configMap.getResourceVersion(), configMap.getData());
            log.info("ConfigMap " + configMap.getName() + " updated!");
        }
    }

    @Override
    public void error(Throwable err) {
        log.error("Got error from watcher", err);
    }

    @Override
    public void close() throws Exception {
        if (this.watcher != null) {
            watcher.stop();
        }
    }

    /**
     * This method is synchronized so that we can support atomically getOrCreate on top of the configMapMap.
     */
    private synchronized ConfigMap getOrCreateConfigMap(String name)
    {
        ConfigMap map = configMapMap.get(name);
        if (map == null) {
            map = new ConfigMap(name);
            configMapMap.put(name, map);
        }
        return map;
    }

    public void subscribe(String name, ConfigSubscriber configSubscriber) {
        ConfigMap configMap = getOrCreateConfigMap(name);
        configMap.subscribe(configSubscriber);
    }
}
