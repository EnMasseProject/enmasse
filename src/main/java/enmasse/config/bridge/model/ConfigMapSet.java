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

package enmasse.config.bridge.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a ConfigMapSet that can be subscribed to.
 */
public class ConfigMapSet {
    private final List<ConfigSubscriber> subscriberList = new ArrayList<>();
    private Map<String, ConfigMap> configMaps = new LinkedHashMap<>();

    /**
     * Subscribe for updates to this configuration.
     *
     * @param subscriber The subscriber handle.
     */
    public synchronized void subscribe(ConfigSubscriber subscriber) {
        subscriberList.add(subscriber);
        // Notify only when we have values
        if (!configMaps.isEmpty()) {
            Map<String, ConfigMap> maps = new LinkedHashMap<>(configMaps);
            subscriber.configUpdated(maps);
        }
    }

    /**
     * Notify subscribers that the set of configs has been updated.
     */
    private void notifySubscribers() {
        Map<String, ConfigMap> maps = new LinkedHashMap<>(configMaps);
        subscriberList.stream().forEach(subscription -> subscription.configUpdated(maps));
    }

    public synchronized void mapDeleted(String name) {
        configMaps.remove(name);
        notifySubscribers();
    }

    public synchronized void mapAdded(String name, Map<String, String> data) {
        configMaps.put(name, new ConfigMap(data));
        notifySubscribers();
    }

    public synchronized void mapUpdated(String name, Map<String, String> data) {
        configMaps.put(name, new ConfigMap(data));
        notifySubscribers();
    }
}
