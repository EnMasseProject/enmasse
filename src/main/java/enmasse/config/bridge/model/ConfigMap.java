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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a ConfigMap that can be subscribed to.
 */
public class ConfigMap {
    private final String name;
    private final List<ConfigSubscriber> subscriberList = new ArrayList<>();

    private String version = null;
    private Map<String, String> values = Collections.emptyMap();

    public ConfigMap(String name) {
        this.name = name;
    }

    /**
     * Subscribe for updates to this configuration.
     *
     * @param subscriber The subscriber handle.
     */
    public synchronized void subscribe(ConfigSubscriber subscriber) {
        subscriberList.add(subscriber);
        // Notify only when we have values
        if (version != null) {
            subscriber.configUpdated(name, version, values);
        }
    }

    /**
     * Notify this instance that the config has been updated. It is the responsibility of this ConfigMap to notify subscribers.
     *
     * @param resourceVersion
     * @param data
     */
    public synchronized void configUpdated(String resourceVersion, Map<String, String> data) {
        version = resourceVersion;
        values = data;
        subscriberList.stream().forEach(subscription -> subscription.configUpdated(name, version, values));
    }
}
