/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public enum Group {
    MANAGE,
    SEND_ALL,
    RECV_ALL,
    MONITOR;

    @Override
    public String toString() {
        return super.toString().toLowerCase().replace("_all", "_*");
    }
}
