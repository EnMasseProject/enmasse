/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PrometheusMetricsFormatterTest {

    @Test
    public void testEscaping() {
        PrometheusMetricsFormatter formatter = new PrometheusMetricsFormatter();

        ScalarMetric metric = new ScalarMetric("m1", "mdesc", MetricType.gauge,
                () -> List.of(new MetricValue(2, new MetricLabel("key1", "value1"), new MetricLabel("key2", "\\this\"is\nescaped\\"))));

        String value = formatter.format(Collections.singletonList(metric), 3);
        System.out.println(value);
        String expected = "# HELP m1 mdesc\n# TYPE m1 gauge\nm1{key1=\"value1\",key2=\"\\\\this\\\"is\\nescaped\\\\\"} 2 3\n";
        assertEquals(expected, value);
    }

    @Test
    public void testEmpty() {
        Metrics metrics = new Metrics();
        metrics.registerMetric(new ScalarMetric(
                "address_spaces_ready_total",
                "Total number of address spaces in ready state",
                MetricType.gauge,
                Collections::emptyList));
        metrics.registerMetric(new ScalarMetric(
                "address_spaces_not_ready_total",
                "Total number of address spaces in a not ready state",
                MetricType.gauge,
                Collections::emptyList));
        metrics.registerMetric(new ScalarMetric(
                "address_spaces_total",
                "Total number of address spaces",
                MetricType.gauge,
                () -> Collections.singletonList(new MetricValue(0))));

        PrometheusMetricsFormatter formatter = new PrometheusMetricsFormatter();
        System.out.print(formatter.format(metrics.getMetrics(), 0));
    }
}
