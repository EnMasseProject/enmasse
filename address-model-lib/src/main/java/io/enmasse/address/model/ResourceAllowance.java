/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class ResourceAllowance {
    private final String resourceName;
    private final double min;
    private final double max;

    public ResourceAllowance(String resourceName, double min, double max) {
        this.resourceName = resourceName;
        this.min = min;
        this.max = max;
    }

    public String getResourceName() {
        return resourceName;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
