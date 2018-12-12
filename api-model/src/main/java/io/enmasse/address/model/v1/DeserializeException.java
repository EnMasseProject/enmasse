/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

public class DeserializeException extends RuntimeException {
    public DeserializeException(String s) {
        super(s);
    }
}
