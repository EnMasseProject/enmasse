/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.discovery;

/**
 * An endpoint for a host and port;
 */
public class Endpoint {
    private final String hostname;
    private final int port;

    public Endpoint(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String hostname() {
        return hostname;
    }

    public int port() {
        return port;
    }
}
