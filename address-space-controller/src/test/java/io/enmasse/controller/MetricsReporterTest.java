/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.metrics.api.Metric;
import io.enmasse.metrics.api.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MetricsReporterTest {
    @Test
    public void testController() throws Exception {
        Metrics metrics = new Metrics();

        MetricsReporterController controller = new MetricsReporterController(metrics, "1.0");

        controller.reconcileAll(Arrays.asList(
                createAddressSpace("s1", true),
                createAddressSpace("s2", true),
                createAddressSpace("s3", false)));

        List<Metric> metricList = metrics.snapshot();
        assertEquals(7, metricList.size());
        Metric numReady = findMetric("address_space_status_ready", metricList);
        assertNotNull(numReady);
        assertEquals(3, numReady.getValues().size());

        Metric numNotReady = findMetric("address_space_status_not_ready", metricList);
        assertNotNull(numNotReady);
        assertEquals(3, numNotReady.getValues().size());

        Metric total = findMetric("address_spaces_total", metricList);
        assertNotNull(total);
        assertEquals(3, total.getValues().iterator().next().getValue());

    }

    private Metric findMetric(String name, List<Metric> metricList) {
        for (Metric metric : metricList) {
            if (name.equals(metric.getName())) {
                return metric;
            }
        }
        return null;
    }

    private AddressSpace createAddressSpace(String name, boolean isReady) {
        return new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName(name)
                .endMetadata()
                .editOrNewSpec()
                .withType("standard")
                .withPlan("plan1")
                .endSpec()
                .editOrNewStatus()
                .withReady(isReady)
                .endStatus()
                .build();

    }
}
