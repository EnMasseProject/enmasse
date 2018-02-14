/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class AuthenticationServiceDetail {
    private final String name;
    private final Class type;
    private final boolean mandatory;

    public AuthenticationServiceDetail(String name, Class type, boolean mandatory) {
        this.name = name;
        this.type = type;
        this.mandatory = mandatory;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
