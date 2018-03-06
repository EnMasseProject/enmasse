/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public enum Group {
    MANAGE,
    MANAGE_ALL_BROKERED,
    SEND_ALL_BROKERED,
    SEND_ALL_STANDARD,
    RECV_ALL_BROKERED,
    RECV_ALL_STANDARD,
    VIEW_ALL_STANDARD,
    VIEW_ALL_BROKERED,
    MONITOR,
    ADMIN;

    @Override
    public String toString() {
        return super.toString().toLowerCase()
                .replace("_all_standard", "_*")
                .replace("_all_brokered", "_#");
    }
}
