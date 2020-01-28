/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.Collection;
import java.util.stream.Collectors;

public class PrometheusMetricsFormatter implements MetricsFormatter {
    public String format(Collection<Metric> metricList, long timestamp) {
        StringBuilder sb = new StringBuilder();

        for (Metric metric : metricList) {
            if (!MetricType.up.equals(metric.getType())) {
                sb.append("# HELP ").append(metric.getName()).append(" ").append(metric.getDescription()).append("\n");
                sb.append("# TYPE ").append(metric.getName()).append(" ").append(metric.getType().name()).append("\n");
            }
            String suffix = metric.getType().equals(MetricType.histogram) ? "_bucket" : "";
            MetricSnapshot snapshot = metric.getSnapshot();
            for (MetricValue metricValue : snapshot.getValues()) {
                sb.append(formatMetricValue(metric.getName() + suffix, metricValue, timestamp));
            }

            for (MetricValue metricValue : snapshot.getSumValues()) {
                sb.append(formatMetricValue(metric.getName() + "_sum", metricValue, timestamp));
            }

            for (MetricValue metricValue : snapshot.getCountValues()) {
                sb.append(formatMetricValue(metric.getName() + "_count", metricValue, timestamp));
            }
        }
        return sb.toString();
    }

    private static String formatMetricValue(String name, MetricValue metricValue, long timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (!metricValue.getLabels().isEmpty()) {
            sb.append("{");
            sb.append(metricValue.getLabels().stream()
                    .map(label -> label.getKey() + "=\"" + escapeLabelValue(label.getValue()) + "\"")
                    .collect(Collectors.joining(",")));
            sb.append("}");
        }
        sb.append(" ").append(metricValue.getValue());
        if (timestamp != 0) {
            sb.append(" ").append(timestamp);
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String escapeLabelValue(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

}
