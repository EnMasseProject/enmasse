/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public enum DestinationPlan {
    BROKERED_QUEUE("brokered-queue"),
    BROKERED_TOPIC("brokered-topic"),
    STANDARD_SMALL_QUEUE("standard-small-queue"),
    STANDARD_SMALL_TOPIC("standard-small-topic"),
    STANDARD_LARGE_QUEUE("standard-large-queue"),
    STANDARD_LARGE_TOPIC("standard-large-topic"),
    STANDARD_XLARGE_QUEUE("standard-xlarge-queue"),
    STANDARD_XLARGE_TOPIC("standard-xlarge-topic"),
    STANDARD_SMALL_ANYCAST("standard-small-anycast"),
    STANDARD_SMALL_MULTICAST("standard-small-multicast"),
    STANDARD_SMALL_SUBSCRIPTION("standard-small-subscription"),
    STANDARD_LARGE_SUBSCRIPTION("standard-large-subscription");

    private String plan;

    DestinationPlan(String plan) {
        this.plan = plan;
    }

    public String plan() {
        return plan;
    }
}
