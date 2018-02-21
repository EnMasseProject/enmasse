/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public class Address {

    private String address;
    private String type;
    private String plan;

    public Address(String address, String type, String plan) {
        this.address = address;
        this.type = type;
        this.plan = plan;
    }

    public String getAddress() {
        return address;
    }

    public String getType() {
        return type;
    }

    public String getPlan() {
        return plan;
    }
}
