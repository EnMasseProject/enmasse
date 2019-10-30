/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

public enum MessageType {

    TELEMETRY("telemetry"),
    EVENT("event"),
    COMMAND_RESPONSE("control/res");

    private String address;

    MessageType(final String address) {
        this.address = address;
    }

    public String path() {
        return "/" + this.address;
    }

    public String address() {
        return this.address;
    }

}