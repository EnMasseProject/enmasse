/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public interface DestinationPlan {
    String BROKERED_QUEUE = "brokered-queue";
    String BROKERED_TOPIC = "brokered-topic";
    String STANDARD_SMALL_QUEUE = "standard-small-queue";
    String STANDARD_SMALL_TOPIC = "standard-small-topic";
    String STANDARD_LARGE_QUEUE = "standard-large-queue";
    String STANDARD_LARGE_TOPIC = "standard-large-topic";
    String STANDARD_XLARGE_QUEUE = "standard-xlarge-queue";
    String STANDARD_XLARGE_TOPIC = "standard-xlarge-topic";
    String STANDARD_SMALL_ANYCAST = "standard-small-anycast";
    String STANDARD_MEDIUM_ANYCAST = "standard-medium-anycast";
    String STANDARD_SMALL_MULTICAST = "standard-small-multicast";
    String STANDARD_MEDIUM_MULTICAST = "standard-medium-multicast";
    String STANDARD_SMALL_SUBSCRIPTION = "standard-small-subscription";
    String STANDARD_LARGE_SUBSCRIPTION = "standard-large-subscription";
}
