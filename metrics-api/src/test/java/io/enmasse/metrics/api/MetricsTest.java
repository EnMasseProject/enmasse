/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MetricsTest {
    @Test
    public void testMetrics() {
        Metrics metrics = new Metrics();
        metrics.reportMetric(new Metric(
                "m1",
                new MetricValue(2, 0)));
        metrics.reportMetric(new Metric(
                "m2",
                new MetricValue(2, 0, new MetricLabel("k1", "v1")),
                new MetricValue(3, 0, new MetricLabel("k1", "v2"))));
        metrics.reportMetric(new Metric(
                "m3",
                new MetricValue(5, 0, new MetricLabel("k1", "v1"))));

        List<Metric> metricList = metrics.snapshot();
        assertEquals(3, metricList.size());
    }
    @Test
    public void testMetricsUpdate() {
        Metrics metrics = new Metrics();
        metrics.reportMetric(new Metric(
                "m2",
                new MetricValue(2, 0, new MetricLabel("k1", "v1")),
                new MetricValue(3, 0, new MetricLabel("k1", "v2"))));
        metrics.reportMetric(new Metric(
                "m2",
                new MetricValue(5, 3, new MetricLabel("k1", "v1"))));

        List<Metric> metricList = metrics.snapshot();
        assertEquals(1, metricList.size());
        assertContainsValue(metricList.get(0), new MetricValue(5, 3, new MetricLabel("k1", "v1")));
        assertContainsValue(metricList.get(0), new MetricValue(3, 0, new MetricLabel("k1", "v2")));
    }

    private void assertContainsValue(Metric metric, MetricValue value) {
        Iterator<MetricValue> it = metric.getValues().iterator();
        MetricValue v = null;
        while (it.hasNext()) {
            v = it.next();
            if (v.getLabels().equals(value.getLabels())) {
                break;
            }
        }
        assertNotNull(v);
        assertEquals(value.getLabels(), v.getLabels());
        assertEquals(value.getValue(), v.getValue());
        assertEquals(value.getTimestamp(), v.getTimestamp());
    }
}
