/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

public class ScalarMetric implements Metric {
    private final String name;
    private final String description;
    private final MetricType type;
    private final MetricValueSupplier metricValueSupplier;

    public ScalarMetric(String name, String description, MetricType type, MetricValueSupplier metricValueSupplier) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.metricValueSupplier = metricValueSupplier;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MetricType getType() {
        return type;
    }

    @Override
    public MetricSnapshot getSnapshot() {
        return MetricSnapshot.scalar(metricValueSupplier.get());
    }
}
