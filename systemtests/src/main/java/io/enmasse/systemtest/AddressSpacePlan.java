/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public enum AddressSpacePlan {
    BROKERED("brokered-single-broker"),
    STANDARD_SMALL("standard-small"),
    STANDARD_MEDIUM("standard-medium"),
    STANDARD_UNLIMITED("standard-unlimited"),
    STANDARD_UNLIMITED_WITH_MQTT("standard-unlimited-with-mqtt");

    private String plan;

    AddressSpacePlan(String plan) {
        this.plan = plan;
    }

    public String plan() {
        return plan;
    }
}
