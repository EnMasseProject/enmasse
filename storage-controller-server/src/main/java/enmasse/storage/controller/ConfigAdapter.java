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

package enmasse.storage.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.internal.restclient.model.ConfigMap;
import com.openshift.internal.restclient.model.KubernetesResource;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IConfigMap;
import com.openshift.restclient.model.IResource;
import enmasse.storage.controller.admin.ConfigSubscriber;
import enmasse.storage.controller.openshift.OpenshiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConfigAdapter implements IOpenShiftWatchListener {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ConfigAdapter.class.getName());

    private IWatcher watcher;
    private final OpenshiftClient openshiftClient;
    private final String configName;
    private final ConfigSubscriber configSubscriber;

    public ConfigAdapter(OpenshiftClient openshiftClient, String configName, ConfigSubscriber configSubscriber) {
        this.openshiftClient = openshiftClient;
        this.configName = configName;
        this.configSubscriber = configSubscriber;
    }

    private void configUpdated(IConfigMap configMap) {
        if (!configName.equals(configMap.getName())) {
            return;
        }
        try {
            if (configMap.getData().containsKey("json")) {
                log.info("Got new config for " + configName + " with data: " + configMap.getData().get("json"));
                JsonNode root = mapper.readTree(configMap.getData().get("json"));
                configSubscriber.configUpdated(root);
            } else {
                log.info("Got empty config for " + configName);
                configSubscriber.configUpdated(mapper.createObjectNode());
            }
        } catch (Exception e) {
            log.warn("Error handling address config update", e);
        }

    }

    public void start() {
        watcher = openshiftClient.watch(this, ResourceKind.CONFIG_MAP);
    }

    public void stop() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    @Override
    public void connected(List<IResource> resources) {
        IConfigMap map = null;
        for (IResource resource : resources) {
            if (resource.getName().equals(configName)) {
                map = createMap(resource);
            }
        }

        if (map != null) {
            configUpdated(map);
        }
    }

    // TODO: This is a workaroaund for https://github.com/openshift/openshift-restclient-java/issues/208
    private IConfigMap createMap(IResource resource) {
        KubernetesResource r = (KubernetesResource) resource;
        return new ConfigMap(r.getNode(), r.getClient(), r.getPropertyKeys());
    }

    @Override
    public void disconnected() {
        log.info("Disconnected, restarting watch");
        try {
            this.watcher = openshiftClient.watch(this, ResourceKind.CONFIG_MAP);
        } catch (Exception e) {
            log.error("Error re-watching on disconnect", e);
        }
    }

    @Override
    public void received(IResource resource, ChangeType change) {
        configUpdated(createMap(resource));
    }

    @Override
    public void error(Throwable err) {
        log.error("Got error from watcher", err);
    }
}
