package io.enmasse.controller.api.osb.v2;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public class GoneException extends ClientErrorException {

    public GoneException(String message, Response response) {
        super(message, response);
    }
}
