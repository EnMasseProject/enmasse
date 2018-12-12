/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class UnresolvedAddressException extends RuntimeException {
    public UnresolvedAddressException(String message) {
        super(message);
    }
}
