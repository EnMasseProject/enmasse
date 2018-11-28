/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

public class AddressSpaceResource {

    private String name;
    private double max;

    public AddressSpaceResource(String name, double max) {
        this.name = name;
        this.max = max;
    }

    public String getName() {
        return name;
    }

    public double getMax() {
        return max;
    }

}
