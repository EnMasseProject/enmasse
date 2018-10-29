/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public enum DestinationPlan {
    BROKERED_QUEUE("brokered-queue"),
    BROKERED_TOPIC("brokered-topic"),
    STANDARD_POOLED_QUEUE("pooled-queue"),
    STANDARD_POOLED_TOPIC("pooled-topic"),
    STANDARD_SHARDED_QUEUE("sharded-queue"),
    STANDARD_SHARDED_TOPIC("sharded-topic"),
    STANDARD_ANYCAST("standard-anycast"),
    STANDARD_MULTICAST("standard-multicast");

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
