/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MetricsTest {
    @Test
    public void testMetrics() {
        Metrics metrics = new Metrics();
        metrics.registerMetric(new ScalarMetric(
                "m1",
                "m1",
                MetricType.gauge,
                () -> Collections.singletonList(new MetricValue(2))));

        metrics.registerMetric(new ScalarMetric(
                "m2",
                "m2",
                MetricType.gauge,
                () -> List.of(
                        new MetricValue(2, new MetricLabel("k1", "v1")),
                        new MetricValue(3, new MetricLabel("k1", "v2")))));

        metrics.registerMetric(new ScalarMetric(
                "m3",
                "m3",
                MetricType.gauge,
                () -> Collections.singletonList(
                        new MetricValue(5, new MetricLabel("k1", "v1")))));

        Collection<Metric> metricList = metrics.getMetrics();
        assertEquals(3, metricList.size());
    }
    @Test
    public void testMetricsUpdate() {
        Metrics metrics = new Metrics();
        AtomicInteger val = new AtomicInteger(2);

        metrics.registerMetric(new ScalarMetric(
                "m2",
                "m2",
                MetricType.gauge,
                () -> List.of(
                        new MetricValue(val.get(), new MetricLabel("k1", "v1")))));

        Metric m = metrics.getMetrics().iterator().next();
        assertNotNull(m);

        MetricSnapshot snapshot = m.getSnapshot();
        assertContainsValue(snapshot, new MetricValue(2, new MetricLabel("k1", "v1")));
        val.incrementAndGet();
        assertContainsValue(snapshot, new MetricValue(2, new MetricLabel("k1", "v1")));

        assertContainsValue(m.getSnapshot(), new MetricValue(3, new MetricLabel("k1", "v1")));
    }

    private void assertContainsValue(MetricSnapshot snapshot, MetricValue value) {
        Iterator<MetricValue> it = snapshot.getValues().iterator();
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
    }
}
