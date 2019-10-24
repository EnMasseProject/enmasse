/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Metrics {
    private final Map<String, Metric> metrics = new LinkedHashMap<>();

    public synchronized List<Metric> getMetrics() {
        return new ArrayList<>(metrics.values());
    }

    public synchronized void registerMetric(Metric metric) {
        Metric existing = metrics.get(metric.getName());
        if (existing != null) {
            throw new IllegalArgumentException("Metric " + metric.getName() + " already registered");
        }
        metrics.put(metric.getName(), metric);
    }
}
