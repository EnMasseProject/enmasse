/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class UnresolvedAddressSpaceException extends RuntimeException {
    public UnresolvedAddressSpaceException(String s) {
        super(s);
    }
}
