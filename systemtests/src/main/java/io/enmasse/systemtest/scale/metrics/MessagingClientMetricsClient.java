/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.HdrHistogram.DoubleHistogram;
import org.HdrHistogram.DoubleRecorder;
import org.hawkular.agent.prometheus.types.Counter;
import org.hawkular.agent.prometheus.types.MetricFamily;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.model.address.AddressType;

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

    private double receivedMsgPerSecond = 0;
    private double acceptedMsgPerSecond = 0;

//    private DoubleHi
    private DoubleRecorder receivedPerSecondRecorder = new DoubleRecorder(2);
    private DoubleRecorder acceptedPerSecondRecorder = new DoubleRecorder(2);

    private DoubleHistogram receivedPerSecondHistogram = new DoubleHistogram(2);
    private DoubleHistogram acceptedPerSecondHistogram = new DoubleHistogram(2);

    public MessagingClientMetricsClient(Endpoint metricsEndpoint) throws IOException {
        super(metricsEndpoint);
    }

    @Override
    protected void afterScrape(List<MetricFamily> metrics) {

        for (AddressType type : Arrays.asList(AddressType.ANYCAST, AddressType.QUEUE)) {
            getCounter(metrics, TEST_ACCEPTED_TOTAL_METRIC, "addressType", type.toString())
                .filter(c -> c.getValue() > 0)
                .ifPresent(counter -> {
                    double acceptedPerSecond = counter.getValue() / (METRICS_UPDATE_PERIOD_MILLIS / 1000);
                    acceptedPerSecondRecorder.recordValue(acceptedPerSecond);
                    acceptedPerSecondHistogram.recordValue(acceptedPerSecond);

                    acceptedMsgPerSecond = acceptedPerSecond;

                });

            getCounter(metrics, TEST_RECEIVED_TOTAL_METRIC, "addressType", type.toString())
                .ifPresent(counter -> {
                    double receivedPerSecond = counter.getValue() / (METRICS_UPDATE_PERIOD_MILLIS / 1000);
//                    receivedPerSecondRecorder.recordValue(receivedPerSecond);

                    receivedMsgPerSecond = receivedPerSecond;
                });
        }

    }

    public double getReceivedMsgPerSecond() {
        return receivedMsgPerSecond;
    }

    public void setReceivedMsgPerSecond(double receivedMsgPerSecond) {
        this.receivedMsgPerSecond = receivedMsgPerSecond;
    }

    public double getAcceptedMsgPerSecond() {
        return acceptedMsgPerSecond;
    }

    public void setAcceptedMsgPerSecond(double acceptedMsgPerSecond) {
        this.acceptedMsgPerSecond = acceptedMsgPerSecond;
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

    public DoubleHistogram getMessagesSendPerSecondHistogram() {
        return acceptedPerSecondRecorder.getIntervalHistogram();
    }

    public DoubleHistogram getMessagesReceivedPerSecondHistogram() {
        return receivedPerSecondRecorder.getIntervalHistogram();
    }
}
