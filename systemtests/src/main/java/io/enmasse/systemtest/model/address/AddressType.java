/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.model.address;


public enum AddressType {
    QUEUE, TOPIC, MULTICAST, ANYCAST, SUBSCRIPTION;

    public static AddressType getEnum(String type) {
        return valueOf(type.toUpperCase());
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}


