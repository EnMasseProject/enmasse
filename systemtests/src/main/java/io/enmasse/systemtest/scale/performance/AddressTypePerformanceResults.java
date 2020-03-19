/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.performance;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AddressTypePerformanceResults {

    private List<ThroughputData> senders = new ArrayList<>();
    private List<ThroughputData> receivers = new ArrayList<>();

    private ThroughputData globalSenders;
    private ThroughputData globalReceivers;

    public List<ThroughputData> getSenders() {
        return senders;
    }

    public void setSenders(List<ThroughputData> senders) {
        this.senders = senders;
    }

    public List<ThroughputData> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<ThroughputData> receivers) {
        this.receivers = receivers;
    }

    public ThroughputData getGlobalSenders() {
        return globalSenders;
    }

    public void setGlobalSenders(ThroughputData globalSenders) {
        this.globalSenders = globalSenders;
    }

    public ThroughputData getGlobalReceivers() {
        return globalReceivers;
    }

    public void setGlobalReceivers(ThroughputData globalReceivers) {
        this.globalReceivers = globalReceivers;
    }

}
