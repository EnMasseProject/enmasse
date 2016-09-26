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

import enmasse.config.bridge.model.ConfigDatabase;
import enmasse.config.bridge.model.ConfigSubscriber;
import enmasse.config.bridge.model.LabelSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ConfigDatabase backed by OpenShift/Kubernetes REST API supporting subscriptions.
 */
public class OpenshiftConfigDatabase implements AutoCloseable, ConfigDatabase {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftConfigDatabase.class.getName());
    private final OpenshiftClient client;

    private final Map<LabelSet, ConfigResourceListener> aggregatorMap = new LinkedHashMap<>();
    private final List<OpenshiftResourceObserver> observerList = new ArrayList<>();

    public OpenshiftConfigDatabase(OpenshiftClient client) {
        this.client = client;
    }

    @Override
    public void close() throws Exception {
        for (OpenshiftResourceObserver observer : observerList) {
            observer.close();
        }
    }

    public synchronized boolean subscribe(String labelSetName, ConfigSubscriber configSubscriber) {
        try {
            LabelSet labelSet = fetchLabelSet(labelSetName);
            ConfigResourceListener listener = getOrCreateAggregator(labelSet);
            listener.subscribe(configSubscriber);
            return true;
        } catch (Exception e) {
            log.error("Error subscribing to " + labelSetName + ": ", e);
            return false;
        }
    }

    private ConfigResourceListener getOrCreateAggregator(LabelSet labelSet) {
        ConfigResourceListener aggregator = aggregatorMap.get(labelSet);
        if (aggregator == null) {
            aggregator = new ConfigResourceListener();
            OpenshiftResourceObserver observer = new OpenshiftResourceObserver(client, labelSet, aggregator);
            observer.start();
            observerList.add(observer);
            aggregatorMap.put(labelSet, aggregator);
        }
        return aggregator;
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
