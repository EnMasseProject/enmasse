/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import java.net.MalformedURLException;
import java.net.URL;

public class Endpoint {
    private final String host;
    private final int port;

    public Endpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Endpoint(String url) throws MalformedURLException {
        URL myurl = new URL(url);
        this.host = myurl.getHost();
        this.port = myurl.getPort();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
