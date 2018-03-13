/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.List;

public class Address {

    private String addressSpace;
    private String address;
    private String name;
    private String uuid;
    private String type;
    private String plan;
    private String phase;
    private boolean isReady;
    private List<String> statusMessages;


    public Address(String addressSpace, String address, String name, String type, String plan, String phase, boolean isReady, List<String> statusMessages, String uuid) {
        this.addressSpace = addressSpace;
        this.address = address;
        this.name = name;
        this.uuid = uuid;
        this.type = type;
        this.plan = plan;
        this.phase = phase;
        this.isReady = isReady;
        this.statusMessages = statusMessages;
    }

    public String getAddressSpace() {
        return addressSpace;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPlan() {
        return plan;
    }

    public String getPhase() {
        return phase;
    }

    public boolean isReady() {
        return isReady;
    }

    public List<String> getStatusMessages() {
        return statusMessages;
    }

    public String getUuid() {
        return uuid;
    }
}
