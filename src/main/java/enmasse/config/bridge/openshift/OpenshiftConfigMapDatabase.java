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
import enmasse.config.bridge.model.ConfigMapDatabase;
import enmasse.config.bridge.model.ConfigSubscriber;
import enmasse.config.bridge.model.LabelSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ConfigMapDatabase backed by OpenShift/Kubernetes REST API supporting subscriptions.
 */
public class OpenshiftConfigMapDatabase implements AutoCloseable, ConfigMapDatabase {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftConfigMapDatabase.class.getName());
    private final IClient restClient;
    private final String namespace;

    private final Map<String, ConfigMapSetSubscription> configMapMap = new LinkedHashMap<>();

    public OpenshiftConfigMapDatabase(IClient restClient, String namespace) {
        this.restClient = restClient;
        this.namespace = namespace;
    }

    @Override
    public void close() throws Exception {
        for (ConfigMapSetSubscription sub : configMapMap.values()) {
            sub.close();
        }
    }

    private synchronized ConfigMapSetSubscription getOrCreateConfigMap(String labelSetName, LabelSet labelSet) {
        ConfigMapSetSubscription map = configMapMap.get(labelSetName);
        if (map == null) {
            map = new ConfigMapSetSubscription(restClient, labelSet, namespace);
            configMapMap.put(labelSetName, map);
        }
        return map;
    }

    public synchronized boolean subscribe(String labelSetName, ConfigSubscriber configSubscriber) {
        try {
            LabelSet labelSet = fetchLabelSet(labelSetName);
            ConfigMapSetSubscription sub = getOrCreateConfigMap(labelSetName, labelSet);
            sub.getSet().subscribe(configSubscriber);
            sub.start();
            return true;
        } catch (Exception e) {
            log.error("Error subscribing to " + labelSetName + ": ", e);
            return false;
        }
    }

    private LabelSet fetchLabelSet(String labelSetName) {
        // TODO: Make this mapping configurable
        if (labelSetName.equals("maas")) {
            return LabelSet.fromMap(Collections.singletonMap("type", "address-config"));
        } else {
            throw new IllegalArgumentException("Unknown label set name " + labelSetName);
        }
    }
}
