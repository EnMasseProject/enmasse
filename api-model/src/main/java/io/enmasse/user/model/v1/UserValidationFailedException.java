/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

@SuppressWarnings("serial")
public class UserValidationFailedException extends RuntimeException {

    public UserValidationFailedException(String message) {
        super(message);
    }

    public UserValidationFailedException(Exception e) {
        super(e);
    }

}
