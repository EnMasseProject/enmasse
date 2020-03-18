/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PerformanceResults {

    private int totalAddressesCreated;
    private int totalConnectionsCreated;
    private int totalClientsDeployed;

    private ThroughputData senders;
    private ThroughputData receivers;

    public int getTotalAddressesCreated() {
        return totalAddressesCreated;
    }

    public void setTotalAddressesCreated(int totalAddressesCreated) {
        this.totalAddressesCreated = totalAddressesCreated;
    }

    public int getTotalConnectionsCreated() {
        return totalConnectionsCreated;
    }

    public void setTotalConnectionsCreated(int totalConnectionsCreated) {
        this.totalConnectionsCreated = totalConnectionsCreated;
    }

    public int getTotalClientsDeployed() {
        return totalClientsDeployed;
    }

    public void setTotalClientsDeployed(int totalClientsDeployed) {
        this.totalClientsDeployed = totalClientsDeployed;
    }

    public ThroughputData getSenders() {
        return senders;
    }

    public void setSenders(ThroughputData senders) {
        this.senders = senders;
    }

    public ThroughputData getReceivers() {
        return receivers;
    }

    public void setReceivers(ThroughputData receivers) {
        this.receivers = receivers;
    }

}
