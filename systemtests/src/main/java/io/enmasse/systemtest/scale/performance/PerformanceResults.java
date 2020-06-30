/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.performance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public class PerformanceResults {

    private int totalAddressesCreated;
    private int totalConnectionsCreated;
    private int totalClientsDeployed;

    private Map<String, AddressTypePerformanceResults> addresses = new HashMap<>();

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

    public Map<String, AddressTypePerformanceResults> getAddresses() {
        return addresses;
    }

    public void setAddresses(Map<String, AddressTypePerformanceResults> addresses) {
        this.addresses = addresses;
    }

}
