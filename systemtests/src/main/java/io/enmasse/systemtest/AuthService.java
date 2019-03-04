/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public class AuthService {
    private String type;
    private String name;

    public AuthService(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public AuthService() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static AuthService NONE = new AuthService("none", "none-authservice");
    public static AuthService STANDARD = new AuthService("standard", "standard-authservice");
}
