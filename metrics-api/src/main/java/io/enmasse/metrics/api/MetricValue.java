/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.List;

public class MetricValue {
    private Number value;
    private final List<MetricLabel> labels;

    public MetricValue(Number value, MetricLabel ... labels) {
        this(value, List.of(labels));
    }

    public MetricValue(Number value, List<MetricLabel> labels) {
        if (value == null) {
            this.value = Float.NaN;
        } else {
            this.value = value;
        }
        this.labels = labels;
    }

    public List<MetricLabel> getLabels() {
        return labels;
    }

    public Number getValue() {
        return value;
    }
}
