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

import java.util.Map;

/**
 * A config map represents the config payload for a config map.
 */
public class ConfigMap {
    private final Map<String, String> data;
    public ConfigMap(Map<String, String> data) {
        this.data = data;
    }

    public Map<String, String> getData() {
        return data;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigMap configMap = (ConfigMap) o;

        return data.equals(configMap.data);

    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
