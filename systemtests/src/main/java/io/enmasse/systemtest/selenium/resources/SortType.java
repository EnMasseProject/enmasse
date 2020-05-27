/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

public enum SortType {
    NAME,
    ADDRESS,
    SENDERS,
    RECEIVERS,
    HOSTNAME,
    CONTAINER_ID,
    PROTOCOL,
    TIME_CREATED,
    MESSAGE_IN,
    MESSAGE_OUT,
    STORED_MESSAGES;

    @Override
    public String toString() {
        return super.toString().replace('_', ' ').toUpperCase();
    }
}
