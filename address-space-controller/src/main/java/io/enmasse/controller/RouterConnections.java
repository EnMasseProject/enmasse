/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import java.util.List;

public class RouterConnections {
    private final List<String> hosts;
    private final List<Boolean> opened;
    private final List<String> operStatus;

    public RouterConnections(List<String> hosts, List<Boolean> opened, List<String> operStatus) {
        this.hosts = hosts;
        this.opened = opened;
        this.operStatus = operStatus;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public List<Boolean> getOpened() {
        return opened;
    }

    public List<String> getOperStatus() {
        return operStatus;
    }
}
