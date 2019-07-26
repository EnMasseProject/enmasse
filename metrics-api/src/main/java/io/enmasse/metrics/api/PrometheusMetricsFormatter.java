/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


public class PrometheusMetricsFormatter implements MetricsFormatter {
    public String format(List<Metric> metricList) {
        StringBuilder sb = new StringBuilder();

        for (Metric metric : metricList) {
            if (!MetricType.up.equals(metric.getType())) {
                sb.append("# HELP ").append("enmasse_").append(metric.getName()).append(" ").append(metric.getDescription()).append("\n");
                sb.append("# TYPE ").append("enmasse_").append(metric.getName()).append(" ").append(metric.getType().name()).append("\n");
            }
            for (MetricValue metricValue : metric.getValues()) {
                sb.append("enmasse_" + metric.getName());
                if (!metricValue.getLabels().isEmpty()) {
                    sb.append("{");
                    sb.append(metricValue.getLabels().stream()
                            .map(label -> label.getKey() + "=\"" + escapeLabelValue(label.getValue()) + "\"")
                            .collect(Collectors.joining(",")));
                    sb.append("}");
                }
                sb.append(" ").append(metricValue.getValue());
                if (metricValue.getTimestamp() != 0) {
                    sb.append(" ").append(metricValue.getTimestamp());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String escapeLabelValue(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

}
