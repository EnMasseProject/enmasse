/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.quota;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AddressSpaceQuotaMetadata {
    private final String name;


    private final Map<String, String> labels = new HashMap<>();
    private final Map<String, String> annotations;

    @JsonCreator
    public AddressSpaceQuotaMetadata(@JsonProperty("name") String name,
                                     @JsonProperty("labels") Map<String, String> labels,
                                     @JsonProperty("annotations") Map<String, String> annotations) {
        this.name = name;
        this.annotations = annotations;
        if (labels != null) {
            this.labels.putAll(labels);
        }
    }

    public String getName() {
        return name;
    }

    public Map<String,String> getLabels() {
        return labels;
    }

    public void putLabel(String key, String value) {
        labels.put(key, value);
    }
}
