/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.model.address.AddressType;
import org.hawkular.agent.prometheus.types.Counter;
import org.hawkular.agent.prometheus.types.Histogram;
import org.hawkular.agent.prometheus.types.MetricFamily;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//works for messaging client and tenant client
public class MessagingClientMetricsClient extends ScaleTestClientMetricsClient {

    //counters
    private static final String TEST_CONNECT_SUCCESS_TOTAL_METRIC = "test_connect_success_total";
    private static final String TEST_CONNECT_FAILURE_TOTAL_METRIC = "test_connect_failure_total";
    private static final String TEST_DISCONNECTS_TOTAL_METRIC = "test_disconnects_total";
    private static final String TEST_RECONNECTS_TOTAL_METRIC = "test_reconnects_total";
    private static final String TEST_RECONNECT_SUCCESS_TOTAL_METRIC = "test_reconnect_success_total";
    private static final String TEST_RECONNECT_FAILURE_TOTAL_METRIC = "test_reconnect_failure_total";
    private static final String TEST_ATTACHES_TOTAL_METRIC = "test_attaches_total";
    private static final String TEST_DETACHES_TOTAL_METRIC = "test_detaches_total";
    private static final String TEST_REATTACHES_TOTAL_METRIC = "test_reattaches_total";
    //conters with addressType label
    private static final String TEST_RECEIVED_TOTAL_METRIC = "test_received_total";
    private static final String TEST_ACCEPTED_TOTAL_METRIC = "test_accepted_total";
    private static final String TEST_REJECTED_TOTAL_METRIC = "test_rejected_total";
    private static final String TEST_RELEASED_TOTAL_METRIC = "test_released_total";
    private static final String TEST_MODIFIED_TOTAL_METRIC = "test_modified_total";
    //histograms
    private static final String TEST_RECONNECT_DURATION_HIST = "test_reconnect_duration";
    private static final String TEST_REATTACH_DURATION_HIST = "test_reattach_duration";

    private final long startMillis;

    private final Map<String, List<MessagesCountRecord>> acceptedMessages = new HashMap<>();
    private final Map<String, List<MessagesCountRecord>> receivedMessages = new HashMap<>();

    public MessagingClientMetricsClient(Endpoint metricsEndpoint) throws IOException {
        super(metricsEndpoint);
        startMillis = System.currentTimeMillis();
    }

    @Override
    protected void afterScrape(List<MetricFamily> metrics) {

        long now = System.currentTimeMillis();
        for (AddressType type : Arrays.asList(AddressType.ANYCAST, AddressType.QUEUE)) {
            long accepted = getCounter(metrics, TEST_ACCEPTED_TOTAL_METRIC, "addressType", type.toString())
                    .map(c -> c.getValue())
                    .orElse(0d)
                    .longValue();
            long received = getCounter(metrics, TEST_RECEIVED_TOTAL_METRIC, "addressType", type.toString())
                    .map(c -> c.getValue())
                    .orElse(0d)
                    .longValue();
            acceptedMessages.computeIfAbsent(type.toString(), k -> new ArrayList<>()).add(new MessagesCountRecord(now, accepted));
            receivedMessages.computeIfAbsent(type.toString(), k -> new ArrayList<>()).add(new MessagesCountRecord(now, received));
        }

    }

    public long getStartTimeMillis() {
        return startMillis;
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

    public Counter getSuccessfulReconnects() {
        return getCounter(TEST_RECONNECT_SUCCESS_TOTAL_METRIC);
    }

    public Counter getFailedReconnects() {
        return getCounter(TEST_RECONNECT_FAILURE_TOTAL_METRIC);
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

    public Optional<Counter> getReceivedDeliveries(AddressType addressType) {
        return getCounter(TEST_RECEIVED_TOTAL_METRIC, addressType);
    }

    public Optional<Counter> getAcceptedDeliveries(AddressType addressType) {
        return getCounter(TEST_ACCEPTED_TOTAL_METRIC, addressType);
    }

    public Optional<Counter> getRejectedDeliveries(AddressType addressType) {
        return getCounter(TEST_REJECTED_TOTAL_METRIC, addressType);
    }

    public Optional<Counter> getReleasedDeliveries(AddressType addressType) {
        return getCounter(TEST_RELEASED_TOTAL_METRIC, addressType);
    }

    public Optional<Counter> getModifiedDeliveries(AddressType addressType) {
        return getCounter(TEST_MODIFIED_TOTAL_METRIC, addressType);
    }

    public Optional<Histogram> getReconnectDurationHistogram() {
        return getHistogram(TEST_RECONNECT_DURATION_HIST);
    }

    public Optional<Histogram> getReattachDurationHistogram() {
        return getHistogram(TEST_REATTACH_DURATION_HIST);
    }

    public Map<String, List<MessagesCountRecord>> getAcceptedMessages() {
        return acceptedMessages;
    }

    public Map<String, List<MessagesCountRecord>> getReceivedMessages() {
        return receivedMessages;
    }

}
