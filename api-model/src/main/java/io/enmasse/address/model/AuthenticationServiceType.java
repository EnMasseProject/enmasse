/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The type of authentication services supported in EnMasse.
 */
public enum AuthenticationServiceType {
    NONE,
    STANDARD,
    EXTERNAL;

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }

    public io.enmasse.admin.model.v1.AuthenticationServiceType toAdminType() {
        return io.enmasse.admin.model.v1.AuthenticationServiceType.valueOf(name().toLowerCase());
    }

    @JsonCreator
    public static AuthenticationServiceType create(String name) {
        return valueOf(name.toUpperCase());
    }
}
