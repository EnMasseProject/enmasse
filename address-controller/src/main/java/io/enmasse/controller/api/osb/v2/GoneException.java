/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public class GoneException extends ClientErrorException {

    public GoneException(String message, Response response) {
        super(message, response);
    }
}
