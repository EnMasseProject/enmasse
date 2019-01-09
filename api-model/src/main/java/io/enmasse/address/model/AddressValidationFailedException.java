/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

@SuppressWarnings("serial")
public class AddressValidationFailedException extends RuntimeException {

    public AddressValidationFailedException(String message) {
        super(message);
    }

    public AddressValidationFailedException(Exception e) {
        super(e);
    }

}
