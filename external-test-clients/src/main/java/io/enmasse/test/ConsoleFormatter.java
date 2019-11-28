/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.test;

import io.enmasse.metrics.api.Metric;
import io.enmasse.metrics.api.MetricSnapshot;
import io.enmasse.metrics.api.MetricValue;
import io.enmasse.metrics.api.MetricsFormatter;

import java.util.Collection;
import java.util.stream.Collectors;

public class ConsoleFormatter implements MetricsFormatter {
    @Override
    public String format(Collection<Metric> metricList, long timestamp) {
        StringBuilder sb = new StringBuilder();
        for (Metric metric : metricList) {
            MetricSnapshot snapshot = metric.getSnapshot();
            for (MetricValue value : snapshot.getValues()) {

                String labels = value.getLabels().stream()
                        .map(label -> label.getKey() + "=\"" + label.getValue() + "\"")
                        .collect(Collectors.joining(","));

                sb.append(metric.getName());
                if (!labels.isEmpty()) {
                    sb.append("(").append(labels).append(")");
                }
                sb.append(" = ").append(value.getValue());
            }
        }
        return sb.toString();
    }
}
