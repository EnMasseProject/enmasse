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
package enmasse.config.service.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Represents a set of labels, and supports checking if another labelset is a subset of this set.
 */
public class LabelSet {
    private final Map<String, String> labelMap;


    private LabelSet(Map<String, String> labelMap) {
        this.labelMap = labelMap;
    }


    public static LabelSet fromString(String address) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        String[] labelPairs = address.split(",");
        for (String labelPair : labelPairs) {
            String [] label = labelPair.split("=");
            labelMap.put(label[0], label[1]);
        }
        return fromMap(labelMap);
    }

    public static LabelSet fromMap(Map<String, String> labelMap) {
        return new LabelSet(labelMap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LabelSet labelSet = (LabelSet) o;

        return labelMap.equals(labelSet.labelMap);

    }

    @Override
    public int hashCode() {
        return labelMap.hashCode();
    }

    public boolean contains(LabelSet labelSet) {
        for (Map.Entry<String, String> label : labelSet.labelMap.entrySet()) {
            if (!this.labelMap.containsKey(label.getKey()) || !this.labelMap.get(label.getKey()).equals(label.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, String> entry : labelMap.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    public Map<String,String> getLabelMap() {
        return Collections.unmodifiableMap(labelMap);
    }
}
