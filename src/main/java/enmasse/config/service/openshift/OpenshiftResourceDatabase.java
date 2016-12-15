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

import enmasse.config.service.model.Subscriber;
import enmasse.config.service.model.ResourceDatabase;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ResourceDatabase backed by OpenShift/Kubernetes REST API supporting subscriptions.
 */
public class OpenshiftResourceDatabase implements AutoCloseable, ResourceDatabase {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftResourceDatabase.class.getName());
    private final OpenShiftClient client;

    private final Map<String, OpenshiftResourceListener> listenerMap = new LinkedHashMap<>();
    private final List<OpenshiftResourceObserver> observerList = new ArrayList<>();
    private final Map<String, SubscriptionConfig> listenerFactoryMap;

    public OpenshiftResourceDatabase(OpenShiftClient client, Map<String, SubscriptionConfig> listenerFactoryMap) {
        this.client = client;
        this.listenerFactoryMap = listenerFactoryMap;
    }

    @Override
    public void close() throws Exception {
        for (OpenshiftResourceObserver observer : observerList) {
            observer.close();
        }
    }

    public synchronized boolean subscribe(String address, Map<String, String> filter, Subscriber subscriber) {
        try {
            OpenshiftResourceListener listener = getOrCreateListener(address, filter);
            listener.subscribe(subscriber);
            return true;
        } catch (Exception e) {
            log.error("Error subscribing to " + address + ": ", e);
            return false;
        }
    }

    private OpenshiftResourceListener getOrCreateListener(String address, Map<String, String> filter) {
        OpenshiftResourceListener listener = listenerMap.get(address);
        if (listener == null) {
            SubscriptionConfig listenerFactory = getListenerFactory(address);
            listener = new OpenshiftResourceListener(listenerFactory.getMessageEncoder());
            OpenshiftResourceObserver observer = new OpenshiftResourceObserver(listenerFactory.getObserverOptions(client, filter), listener);
            observer.start();
            observerList.add(observer);
            listenerMap.put(address, listener);
        }
        return listener;
    }

    private SubscriptionConfig getListenerFactory(String address) {
        if (listenerFactoryMap.containsKey(address)) {
            return listenerFactoryMap.get(address);
        } else {
            throw new IllegalArgumentException("Unknown listener factory for address " + address);
        }
    }
}
