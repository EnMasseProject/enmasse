/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

public class NoneAuthServiceInfo {
    private final String host;
    private final int amqpPort;

    public NoneAuthServiceInfo(String host, int amqpPort) {
        this.host = host;
        this.amqpPort = amqpPort;
    }

    public String getHost() {
        return host;
    }

    public int getAmqpPort() {
        return amqpPort;
    }
}
