/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.downtime;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DowntimeMonitoringResult {

    private int addresses;
    private int clientsDeployed;
    private String normalTimeToCreateAddress;
    private List<DowntimeData> downtimeData = new ArrayList<>();

    //summary
    private DowntimeData routerSummary;

    private DowntimeData brokerSummary;

    public int getAddresses() {
        return addresses;
    }

    public void setAddresses(int addresses) {
        this.addresses = addresses;
    }

    public int getClientsDeployed() {
        return clientsDeployed;
    }

    public void setClientsDeployed(int clientsDeployed) {
        this.clientsDeployed = clientsDeployed;
    }

    public String getNormalTimeToCreateAddress() {
        return normalTimeToCreateAddress;
    }

    public void setNormalTimeToCreateAddress(String normalTimeToCreateAddress) {
        this.normalTimeToCreateAddress = normalTimeToCreateAddress;
    }

    public List<DowntimeData> getDowntimeData() {
        return downtimeData;
    }

    public void setDowntimeData(List<DowntimeData> downtimeData) {
        this.downtimeData = downtimeData;
    }

    public DowntimeData getRouterSummary() {
        return routerSummary;
    }

    public void setRouterSummary(DowntimeData routerSummary) {
        this.routerSummary = routerSummary;
    }

    public DowntimeData getBrokerSummary() {
        return brokerSummary;
    }

    public void setBrokerSummary(DowntimeData brokerSummary) {
        this.brokerSummary = brokerSummary;
    }

}
