/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

public class IncomparableValueException extends ClassCastException {

    public IncomparableValueException() {
    }

    public IncomparableValueException(String s) {
        super(s);
    }

    public IncomparableValueException(Throwable e) {
        this();
        initCause(e);
    }
}
