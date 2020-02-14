/*
 * Copyright 2016-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import java.util.List;

class RouterStatus {
    private final String routerId;
    private final RouterConnections connections;
    private final List<String> neighbors;
    private final long undelivered;

    RouterStatus(String routerId, RouterConnections connections, List<String> neighbors, long undelivered) {
        this.routerId = routerId;
        this.connections = connections;
        this.neighbors = neighbors;
        this.undelivered = undelivered;
    }

    public String getRouterId() {
        return routerId;
    }

    public RouterConnections getConnections() {
        return connections;
    }

    public List<String> getNeighbors() {
        return neighbors;
    }

    public long getUndelivered() {
        return undelivered;
    }
}
