/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public enum DestinationPlan {
    BROKERED_QUEUE("brokered-queue"),
    BROKERED_TOPIC("brokered-topic"),
    STANDARD_POOLED_QUEUE("small-standard-queue"),
    STANDARD_POOLED_TOPIC("small-standard-topic"),
    STANDARD_SHARDED_QUEUE("large-standard-queue"),
    STANDARD_SHARDED_TOPIC("large-standard-topic"),
    STANDARD_ANYCAST("small-standard-anycast"),
    STANDARD_MULTICAST("small-standard-multicast");

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
