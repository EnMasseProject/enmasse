/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.ArrayList;
import java.util.List;

public class HistogramMetric implements Metric {
    private final String name;
    private final String description;
    private final MetricType type;
    private final MetricValueSupplier sumSupplier;
    private final MetricValueSupplier countSupplier;
    private final List<MetricValueSupplier> bucketSupplier;

    public HistogramMetric(String name, String description, MetricType type, MetricValueSupplier sumSupplier, MetricValueSupplier countSupplier, List<MetricValueSupplier> bucketSupplier) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.sumSupplier = sumSupplier;
        this.countSupplier = countSupplier;
        this.bucketSupplier = bucketSupplier;
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
        List<MetricValue> values = new ArrayList<>();
        for (MetricValueSupplier bucket : bucketSupplier) {
            values.addAll(bucket.get());
        }
        return MetricSnapshot.histogram(values, sumSupplier.get(), countSupplier.get());
    }
}
