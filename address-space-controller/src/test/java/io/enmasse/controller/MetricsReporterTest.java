/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.Phase;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.metrics.api.Metric;
import io.enmasse.metrics.api.MetricSnapshot;
import io.enmasse.metrics.api.Metrics;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MetricsReporterTest {
    @Test
    public void testController() throws Exception {
        Metrics metrics = new Metrics();
        Kubernetes kubernetes = mock(Kubernetes.class);

        MetricsReporterController controller = new MetricsReporterController(metrics, "1.0", kubernetes);

        controller.reconcileAll(Arrays.asList(
                createAddressSpace("s1", true),
                createAddressSpace("s2", true),
                createAddressSpace("s3", false)));

        List<Metric> metricList = metrics.getMetrics();
        assertEquals(15, metricList.size());
        MetricSnapshot numReady = createSnapshot("address_space_status_ready", metricList);
        assertNotNull(numReady);
        assertEquals(3, numReady.getValues().size());

        MetricSnapshot numNotReady = createSnapshot("address_space_status_not_ready", metricList);
        assertNotNull(numNotReady);
        assertEquals(3, numNotReady.getValues().size());

        MetricSnapshot total = createSnapshot("address_spaces_total", metricList);
        assertNotNull(total);
        assertEquals(3, total.getValues().iterator().next().getValue());

        MetricSnapshot numActive = createSnapshot("address_spaces_active_total", metricList);
        assertNotNull(numActive);
        assertEquals(Long.valueOf(2), numActive.getValues().iterator().next().getValue());
    }

    private MetricSnapshot createSnapshot(String name, List<Metric> metricList) {
        for (Metric metric : metricList) {
            if (name.equals(metric.getName())) {
                return metric.getSnapshot();
            }
        }
        return null;
    }

    private AddressSpace createAddressSpace(String name, boolean isReady) {
        Phase phase = isReady ? Phase.Active : Phase.Configuring;
        return new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withAnnotations(Map.of(AnnotationKeys.INFRA_UUID, UUID.randomUUID().toString()))
                .withName(name)
                .endMetadata()
                .editOrNewSpec()
                .withType("standard")
                .withPlan("plan1")
                .endSpec()
                .editOrNewStatus()
                .withReady(isReady)
                .withPhase(phase)
                .endStatus()
                .build();

    }
}
