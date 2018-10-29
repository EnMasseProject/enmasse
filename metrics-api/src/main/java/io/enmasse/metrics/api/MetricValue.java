/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetricValue {

    private Number value;
    private final long timestamp;
    private final List<MetricLabel> labels = new ArrayList<>();

    public MetricValue(Number value, long timestamp, MetricLabel ... labels) {
        this(value, timestamp,Arrays.asList(labels));
    }

    public MetricValue(Number value, long timestamp, List<MetricLabel> labels) {
        this.value = value;
        this.timestamp = timestamp;
        this.labels.addAll(labels);
    }

    public List<MetricLabel> getLabels() {
        return labels;
    }

    public Number getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
