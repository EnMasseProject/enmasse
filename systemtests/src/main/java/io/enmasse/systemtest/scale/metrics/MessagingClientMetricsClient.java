/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.io.IOException;

import org.hawkular.agent.prometheus.types.Counter;

import io.enmasse.systemtest.Endpoint;

public class MessagingClientMetricsClient extends ScaleTestClientMetricsClient {

    //counters
    private static final String TEST_CONNECT_SUCCESS_TOTAL_METRIC = "test_connect_success_total";
    private static final String TEST_CONNECT_FAILURE_TOTAL_METRIC = "test_connect_failure_total";
    private static final String TEST_DISCONNECTS_TOTAL_METRIC = "test_disconnects_total";
    private static final String TEST_RECONNECTS_TOTAL_METRIC = "test_reconnects_total";
    private static final String TEST_ATTACHES_TOTAL_METRIC = "test_attaches_total";
    private static final String TEST_DETACHES_TOTAL_METRIC = "test_detaches_total";
    private static final String TEST_REATTACHES_TOTAL_METRIC = "test_reattaches_total";
    private static final String TEST_RECEIVED_TOTAL_METRIC = "test_received_total";
    private static final String TEST_ACCEPTED_TOTAL_METRIC = "test_accepted_total";
    private static final String TEST_REJECTED_TOTAL_METRIC = "test_rejected_total";
    private static final String TEST_RELEASED_TOTAL_METRIC = "test_released_total";
    private static final String TEST_MODIFIED_TOTAL_METRIC = "test_modified_total";

    public MessagingClientMetricsClient(Endpoint metricsEndpoint) throws IOException {
        super(metricsEndpoint);
    }

    public Counter getConnectSuccess() {
        return getCounter(TEST_CONNECT_SUCCESS_TOTAL_METRIC);
    }

    public Counter getConnectFailure() {
        return getCounter(TEST_CONNECT_FAILURE_TOTAL_METRIC);
    }

    public Counter getDisconnects() {
        return getCounter(TEST_DISCONNECTS_TOTAL_METRIC);
    }

    public Counter getReconnects() {
        return getCounter(TEST_RECONNECTS_TOTAL_METRIC);
    }

    public Counter getAttaches() {
        return getCounter(TEST_ATTACHES_TOTAL_METRIC);
    }

    public Counter getDetaches() {
        return getCounter(TEST_DETACHES_TOTAL_METRIC);
    }

    public Counter getReattaches() {
        return getCounter(TEST_REATTACHES_TOTAL_METRIC);
    }

    public Counter getReceivedDeliveries() {
        return getCounter(TEST_RECEIVED_TOTAL_METRIC);
    }

    public Counter getAcceptedDeliveries() {
        return getCounter(TEST_ACCEPTED_TOTAL_METRIC);
    }

    public Counter getRejectedDeliveries() {
        return getCounter(TEST_REJECTED_TOTAL_METRIC);
    }

    public Counter getReleasedDeliveries() {
        return getCounter(TEST_RELEASED_TOTAL_METRIC);
    }

    public Counter getModifiedDeliveries() {
        return getCounter(TEST_MODIFIED_TOTAL_METRIC);
    }

//
//
//    private static final Map<AddressType, Histogram> reconnectTime = Map.of(
//            AddressType.anycast, new AtomicHistogram(Long.MAX_VALUE, 2),
//            AddressType.queue, new AtomicHistogram(Long.MAX_VALUE, 2));
//
//    private static final io.prometheus.client.Histogram reconnectHist = io.prometheus.client.Histogram.build()
//            .name("test_reconnect_duration")
//            .help("N/A")
//            .buckets(1.0, 2.5, 7.5, 10.0, 25.0, 50.0, 75.0, 100.0)
//            .register();
//
//    private static final Map<AddressType, Histogram> reattachTime = Map.of(
//            AddressType.anycast, new AtomicHistogram(Long.MAX_VALUE, 2),
//            AddressType.queue, new AtomicHistogram(Long.MAX_VALUE, 2));
//
//    private static final io.prometheus.client.Histogram reattachHist = io.prometheus.client.Histogram.build()
//            .name("test_reattach_duration")
//            .help("N/A")
//            .labelNames("addressType")
//            .buckets(1.0, 2.5, 7.5, 10.0, 25.0, 50.0, 75.0, 100.0)
//            .register();
//

}
