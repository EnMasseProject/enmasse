/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class ResourceRequest {
    private final String resourceName;
    private final double amount;

    public ResourceRequest(String resourceName, double amount) {
        this.resourceName = resourceName;
        this.amount = amount;
    }

    public String getResourceName() {
        return resourceName;
    }

    public double getAmount() {
        return amount;
    }
}
