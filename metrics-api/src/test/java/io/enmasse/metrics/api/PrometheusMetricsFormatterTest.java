/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PrometheusMetricsFormatterTest {
    @Test
    public void testEscaping() {
        PrometheusMetricsFormatter formatter = new PrometheusMetricsFormatter();

        Metric metric = new Metric("m1", "mdesc", MetricType.gauge, new MetricValue(2, 3, new MetricLabel("key1", "value1"), new MetricLabel("key2", "\\this\"is\nescaped\\")));

        String value = formatter.format(Collections.singletonList(metric));
        System.out.println(value);
        String expected = "# HELP m1 mdesc\n# TYPE m1 gauge\nm1{key1=\"value1\",key2=\"\\\\this\\\"is\\nescaped\\\\\"} 2 3\n";
        assertEquals(expected, value);
    }
}
