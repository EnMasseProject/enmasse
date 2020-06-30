/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import io.enmasse.systemtest.Endpoint;
import org.hawkular.agent.prometheus.types.Counter;

import java.io.IOException;

public class ProbeClientMetricsClient extends ScaleTestClientMetricsClient {

    private static final String TEST_PROBE_SUCCESS_TOTAL_METRIC = "test_probe_success_total";
    private static final String TEST_PROBE_FAILURE_TOTAL_METRIC = "test_probe_failure_total";

    public ProbeClientMetricsClient(Endpoint metricsEndpoint) throws IOException {
        super(metricsEndpoint);
    }

    public Counter getSuccessTotal() {
        return getCounter(TEST_PROBE_SUCCESS_TOTAL_METRIC);
    }

    public Counter getFailureTotal() {
        return getCounter(TEST_PROBE_FAILURE_TOTAL_METRIC);
    }

}