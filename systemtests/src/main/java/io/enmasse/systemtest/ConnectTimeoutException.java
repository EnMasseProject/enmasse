/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.concurrent.TimeoutException;

public class ConnectTimeoutException extends TimeoutException {
    public ConnectTimeoutException(String message) {
        super(message);
    }
}
