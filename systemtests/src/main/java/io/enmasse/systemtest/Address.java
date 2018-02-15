/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public class Address {

    private String name;
    private String type;
    private String plan;

    public Address(String name, String type, String plan) {
        this.name = name;
        this.type = type;
        this.plan = plan;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPlan() {
        return plan;
    }
}
