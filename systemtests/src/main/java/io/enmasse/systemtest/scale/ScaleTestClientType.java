/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

public enum ScaleTestClientType {

    probe("probe-client"),
    messaging("messaging-client");

    private final String value;

    private ScaleTestClientType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
