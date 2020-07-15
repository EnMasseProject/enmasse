/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

public enum MessageType {

    TELEMETRY("telemetry", "telemetry"),
    EVENT("event", "event"),
    COMMAND("command", "command"),
    COMMAND_RESPONSE("command_response", "command/res"),
    ;

    private final String address;
    private final String path;

    MessageType(final String address, final String path) {
        this.address = address;
        this.path = path;
    }

    public String path() {
        return "/" + this.path;
    }

    public String address() {
        return this.address;
    }

    public String address(final String tenantId) {
        return this.address + "/" + tenantId;
    }

}