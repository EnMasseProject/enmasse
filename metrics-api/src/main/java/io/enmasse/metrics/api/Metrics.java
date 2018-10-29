/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.*;

public class Metrics {
    private final Map<String, Metric> metrics = new LinkedHashMap<>();

    public synchronized List<Metric> snapshot() {
        List<Metric> snapshot = new ArrayList<>();
        for (Metric metric : metrics.values()) {
            List<MetricValue> values = new ArrayList<>();
            for (MetricValue value : metric.getValues()) {
                values.add(new MetricValue(value.getValue(), value.getTimestamp(), value.getLabels()));
            }
            Metric copy = new Metric(metric.getName(), metric.getDescription(), metric.getType(), values);
            snapshot.add(copy);
        }
        return snapshot;
    }

    public synchronized void reportMetric(Metric metric) {
        Metric existing = metrics.get(metric.getName());
        if (existing == null) {
            metrics.put(metric.getName(), metric);
            return;
        }

        existing.update(metric);
    }
}
