/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    inter_router("inter-router"),
    route_container("route-container");

    private final String desc;
    Role(String desc) {
        this.desc = desc;
    }

    @JsonValue
    public String toValue() {
        return desc;
    }

    @JsonCreator
    public static Role forValue(String value) {
        switch (value) {
            case "route-container":
                return Role.route_container;
            case "inter-router":
                return Role.inter_router;
            default:
                throw new IllegalArgumentException("Unknown role '" + value + "'");
        }
    }

    @Override
    public String toString() {
        return "Role{" +
                "desc='" + desc + '\'' +
                '}';
    }
}
