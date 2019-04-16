/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.*;

public class Metric {
    private final String name;
    private String description;
    private MetricType type;
    private final Map<List<MetricLabel>, MetricValue> values = new LinkedHashMap<>();

    public Metric(String name, MetricValue ... values) {
        this(name, name, MetricType.up, values);
    }

    public Metric(String name, String description, MetricType type, MetricValue... values) {
        this(name, description, type, Arrays.asList(values));
    }

    public Metric(String name, String description, MetricType type, List<MetricValue> values) {
        this.name = name;
        this.description = description;
        this.type = type;
        for (MetricValue value : values) {
            this.values.put(value.getLabels(), value);
        }
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

    public Collection<MetricValue> getValues() {
        return values.values();
    }

    public void update(Metric updated) {
        this.description = updated.getDescription();
        this.type = updated.getType();

        if (MetricType.up.equals(type) || MetricType.gauge.equals(type) || MetricType.counter.equals(type)) {
            values.putAll(updated.values);
        }
    }
}
