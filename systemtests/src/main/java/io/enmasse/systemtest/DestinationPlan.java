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
    STANDARD_SMALL_ANYCAST("standard-small-anycast"),
    STANDARD_SMALL_MULTICAST("standard-small-multicast");

    private String plan;

    DestinationPlan(String plan) {
        this.plan = plan;
    }

    /**
     * Gets command for external client
     *
     * @return string command
     */
    public String plan() {
        return plan;
    }
}
