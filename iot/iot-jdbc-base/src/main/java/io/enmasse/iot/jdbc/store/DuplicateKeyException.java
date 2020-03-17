/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

public class DuplicateKeyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DuplicateKeyException() {}

    public DuplicateKeyException(final Throwable cause) {
        super(cause);
    }
}
